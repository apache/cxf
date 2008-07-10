/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.systest.ws.addressing;


import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.ProtocolException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.ServiceImpl;
import org.apache.cxf.jaxws.support.ServiceDelegateAccessor;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.ws.addressing.soap.VersionTransformer;
import org.apache.cxf.wsdl.EndpointReferenceUtils;
import org.apache.hello_world_soap_http.BadRecordLitFault;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.NoSuchCodeLitFault;
import org.apache.hello_world_soap_http.SOAPService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES;


/**
 * Tests the addition of WS-Addressing Message Addressing Properties.
 */
public abstract class MAPTestBase extends AbstractClientServerTestBase implements VerificationCache {

    protected static Bus staticBus;

    static final String INBOUND_KEY = "inbound";
    static final String OUTBOUND_KEY = "outbound";

    static final QName CUSTOMER_NAME =
        new QName("http://example.org/customer", "CustomerKey", "customer");
    static final String CUSTOMER_KEY = "Key#123456789";

    private static MAPVerifier mapVerifier;
    private static HeaderVerifier headerVerifier;

    private static final QName SERVICE_NAME = 
        new QName("http://apache.org/hello_world_soap_http", "SOAPServiceAddressing");
    private static final String NOWHERE = "http://nowhere.nada.nothing.nought:5555";
    private static final String DECOUPLED = "http://localhost:9999/decoupled_endpoint";
    
    private static Map<Object, Map<String, String>> messageIDs =
        new HashMap<Object, Map<String, String>>();
    protected Greeter greeter;
    private String verified;
    
    

    @BeforeClass
    public static void startServers() throws Exception {
        // special case handling for WS-Addressing system test to avoid
        // UUID related issue when server is run as separate process
        // via maven on Win2k
        boolean inProcess = "Windows 2000".equals(System.getProperty("os.name"));
        assertTrue("server did not launch correctly", 
                   launchServer(Server.class, inProcess));
    }
    
    @AfterClass
    public static void shutdownBus() throws Exception {
        staticBus.shutdown(true);
    }
    
    private void addInterceptors(List<Interceptor> chain,
                                     Interceptor[] interceptors) {
        for (int i = 0; i < interceptors.length; i++) {
            chain.add(interceptors[i]);
        }
    }
    private void removeInterceptors(List<Interceptor> chain,
                                 Interceptor[] interceptors) {
        for (int i = 0; i < interceptors.length; i++) {
            chain.remove(interceptors[i]);
        }
    }
    
    public abstract String getConfigFileName();
    
    @Before
    public void setUp() throws Exception {
        //super.setUp();
        
        if (staticBus == null) {
            SpringBusFactory bf = new SpringBusFactory();
            staticBus = bf.createBus(getConfigFileName());
            BusFactory.setDefaultBus(staticBus);
        }
                
        messageIDs.clear();
        mapVerifier = new MAPVerifier();
        headerVerifier = new HeaderVerifier();
        Interceptor[] interceptors = {mapVerifier, headerVerifier };
        addInterceptors(staticBus.getInInterceptors(), interceptors);
        addInterceptors(staticBus.getOutInterceptors(), interceptors);
        addInterceptors(staticBus.getOutFaultInterceptors(), interceptors);
        addInterceptors(staticBus.getInFaultInterceptors(), interceptors);
        
        EndpointReferenceType target = 
            EndpointReferenceUtils.getEndpointReference(Server.ADDRESS);
        ReferenceParametersType params = 
            ContextUtils.WSA_OBJECT_FACTORY.createReferenceParametersType();
        JAXBElement<String> param =
             new JAXBElement<String>(CUSTOMER_NAME, String.class, CUSTOMER_KEY);
        params.getAny().add(param);
        target.setReferenceParameters(params);
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        ServiceImpl serviceImpl = 
            ServiceDelegateAccessor.get(new SOAPService(wsdl, SERVICE_NAME));
        greeter = serviceImpl.getPort(target, Greeter.class);

        mapVerifier.verificationCache = this;
        headerVerifier.verificationCache = this;
    }
    
    @After
    public void tearDown() throws Exception {
        Interceptor[] interceptors = {mapVerifier, headerVerifier };
        removeInterceptors(staticBus.getInInterceptors(), interceptors);
        removeInterceptors(staticBus.getOutInterceptors(), interceptors);
        removeInterceptors(staticBus.getOutFaultInterceptors(), interceptors);
        removeInterceptors(staticBus.getInFaultInterceptors(), interceptors);
        
        mapVerifier = null;
        headerVerifier = null;
        verified = null;
        messageIDs.clear();
    }
        
    //--Tests
    @Test
    public void testImplicitMAPs() throws Exception {
        try {
            String greeting = greeter.greetMe("implicit1");
            assertEquals("unexpected response received from service", 
                         "Hello implicit1",
                         greeting);
            checkVerification();
            greeting = greeter.greetMe("implicit2");
            assertEquals("unexpected response received from service", 
                         "Hello implicit2",
                         greeting);
            checkVerification();
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    @Test
    @Ignore("randomly fails quite often, but not in the debugger so not sure what is going on yet")
    public void testExplicitMAPs() throws Exception {
        try {
            String msgId = "urn:uuid:12345-" + Math.random();
            Map<String, Object> requestContext = 
                ((BindingProvider)greeter).getRequestContext();
            AddressingProperties maps = new AddressingPropertiesImpl();
            AttributedURIType id = 
                ContextUtils.getAttributedURI(msgId);
            maps.setMessageID(id);
            requestContext.put(CLIENT_ADDRESSING_PROPERTIES, maps);
            String greeting = greeter.greetMe("explicit1");
            assertEquals("unexpected response received from service", 
                         "Hello explicit1",
                         greeting);
            checkVerification();

            // the previous addition to the request context impacts
            // on all subsequent invocations on this proxy => a duplicate
            // message ID fault is expected
            try {
                greeter.greetMe("explicit2");
                fail("expected ProtocolException on duplicate message ID");
            } catch (ProtocolException pe) {
                assertEquals("expected duplicate message ID failure",
                           "Duplicate Message ID " + msgId, pe.getMessage());
                checkVerification();
            }

            // clearing the message ID ensure a duplicate is not sent
            maps.setMessageID(null);
            //maps.setRelatesTo(ContextUtils.getRelatesTo(id.getValue()));
            greeting = greeter.greetMe("explicit3");
            assertEquals("unexpected response received from service", 
                         "Hello explicit3",
                         greeting);
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    
    @Test
    @Ignore
    public void testFaultTo() throws Exception {
        try {
            String greeting = greeter.greetMe("warmup");
            assertEquals("unexpected response received from service", 
                         "Hello warmup",
                         greeting);
            checkVerification();

            Map<String, Object> requestContext = 
                ((BindingProvider)greeter).getRequestContext();
            AddressingProperties maps = new AddressingPropertiesImpl();
            maps.setReplyTo(EndpointReferenceUtils.getEndpointReference(NOWHERE));
            maps.setFaultTo(EndpointReferenceUtils.getEndpointReference(DECOUPLED));
            requestContext.put(CLIENT_ADDRESSING_PROPERTIES, maps);
            try {
                greeter.testDocLitFault("BadRecordLitFault");
                fail("expected fault from service");
            } catch (BadRecordLitFault brlf) {
                checkVerification();
            } catch (UndeclaredThrowableException ex) {
                throw (Exception)ex.getCause();
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    
    @Test
    public void testOneway() throws Exception {
        try {
            greeter.greetMeOneWay("implicit_oneway1");
            checkVerification();
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    
    
    @Test
    @Ignore("Random failure on Linux")
    public void testApplicationFault() throws Exception {
        try {
            greeter.testDocLitFault("BadRecordLitFault");
            fail("expected fault from service");
        } catch (BadRecordLitFault brlf) {
            //checkVerification();
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
        String greeting = greeter.greetMe("intra-fault");
        assertEquals("unexpected response received from service", 
                     "Hello intra-fault",
                     greeting);
        try {
            greeter.testDocLitFault("NoSuchCodeLitFault");
            fail("expected NoSuchCodeLitFault");
        } catch (NoSuchCodeLitFault nsclf) {
            //checkVerification();
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }
    

    @Test
    public void testVersioning() throws Exception {
        try {
            // expect two MAPs instances versioned with 200408, i.e. for both 
            // the partial and full responses
            mapVerifier.expectedExposedAs.add(VersionTransformer.Names200408.WSA_NAMESPACE_NAME);
            mapVerifier.expectedExposedAs.add(VersionTransformer.Names200408.WSA_NAMESPACE_NAME);
            String greeting = greeter.greetMe("versioning1");
            assertEquals("unexpected response received from service", 
                         "Hello versioning1",
                         greeting);
            checkVerification();
            greeting = greeter.greetMe("versioning2");
            assertEquals("unexpected response received from service", 
                         "Hello versioning2",
                         greeting);
            checkVerification();
        } catch (UndeclaredThrowableException ex) {
            throw (Exception)ex.getCause();
        }
    }

    //--VerificationCache implementation

    public void put(String verification) {
        if (verification != null) {
            verified = verified == null
                       ? verification
                : verified + "; " + verification;
        }
    }

    //--Verification methods called by handlers

    /**
     * Verify presence of expected MAPs.
     *
     * @param maps the MAPs to verify
     * @param checkPoint the check point
     * @return null if all expected MAPs present, otherwise an error string.
     */
    protected static String verifyMAPs(AddressingProperties maps, 
                                       Object checkPoint) {
        if (maps == null) {
            return "expected MAPs";
        }
        //String rt = maps.getReplyTo() != null ? maps.getReplyTo().getAddress().getValue() : "null"; 
        //System.out.println("verifying MAPs: " + maps
        //                   + " id: " + maps.getMessageID().getValue() 
        //                   + " to: " + maps.getTo().getValue()
        //                   + " reply to: " + rt);
        // MessageID
        String id = maps.getMessageID().getValue();
        if (id == null) {
            return "expected MessageID MAP";
        }
        if (!id.startsWith("urn:uuid")) {
            return "bad URN format in MessageID MAP: " + id;
        }
        // ensure MessageID is unique for this check point
        Map<String, String> checkPointMessageIDs = messageIDs.get(checkPoint);
        if (checkPointMessageIDs != null) { 
            if (checkPointMessageIDs.containsKey(id)) {
                //return "MessageID MAP duplicate: " + id;
                return null;
            }
        } else {
            checkPointMessageIDs = new HashMap<String, String>();
            messageIDs.put(checkPoint, checkPointMessageIDs);    
        }
        checkPointMessageIDs.put(id, id);
        // To
        if (maps.getTo() == null) {
            return "expected To MAP";
        }
        return null;
    }

    /**
     * Verify presence of expected MAP headers.
     *
     * @param wsaHeaders a list of the wsa:* headers present in the SOAP
     * message
     * @param parial true if partial response
     * @return null if all expected headers present, otherwise an error string.
     */
    protected static String verifyHeaders(List<String> wsaHeaders,
                                          boolean partial,
                                          boolean requestLeg) {
        //System.out.println("verifying headers: " + wsaHeaders);
        String ret = null;
        if (!wsaHeaders.contains(Names.WSA_MESSAGEID_NAME)) {
            ret = "expected MessageID header"; 
        }
        if (!wsaHeaders.contains(Names.WSA_TO_NAME)) {
            ret = "expected To header";
        }
       
        if (!(wsaHeaders.contains(Names.WSA_REPLYTO_NAME)
              || wsaHeaders.contains(Names.WSA_RELATESTO_NAME))) {
            ret = "expected ReplyTo or RelatesTo header";
        }
        /*
        if (partial) { 
            if (!wsaHeaders.contains(Names.WSA_FROM_NAME)) {
                ret = "expected From header";
            }
        } else {
            // REVISIT Action missing from full response
            //if (!wsaHeaders.contains(Names.WSA_ACTION_NAME)) {
            //    ret = "expected Action header";
            //}            
        }
        */
        if (requestLeg && !(wsaHeaders.contains(CUSTOMER_NAME.getLocalPart()))) {
            ret = "expected CustomerKey header";
        }
        return ret;
    }

    private void checkVerification() {
        assertNull("MAP/Header verification failed: " + verified, verified);
    }
}


