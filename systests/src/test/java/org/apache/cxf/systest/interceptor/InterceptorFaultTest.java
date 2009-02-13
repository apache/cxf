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

package org.apache.cxf.systest.interceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.greeter_control.Control;
import org.apache.cxf.greeter_control.ControlImpl;
import org.apache.cxf.greeter_control.ControlService;
import org.apache.cxf.greeter_control.FaultThrowingInterceptor;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.greeter_control.PingMeFault;
import org.apache.cxf.greeter_control.types.FaultLocation;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseComparator;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class InterceptorFaultTest extends AbstractBusClientServerTestBase {
    private static final Logger LOG = LogUtils.getLogger(InterceptorFaultTest.class);

    private static final QName SOAP_FAULT_CODE = new QName("http://schemas.xmlsoap.org/soap/envelope/",
                                                           "Server");
    private static final String FAULT_CODE = "COULD_NOT_SEND";
    private static final String FAULT_MESSAGE = "Could not send Message.";
    
    private static final String CONTROL_PORT_ADDRESS = 
        "http://localhost:9001/SoapContext/ControlPort";
    
    private static int decoupledEndpointPort = 10000;
    private static String decoupledEndpoint;
    

    /**
     * Tests that a fault thrown by a server side interceptor is reported back to
     * the client in appropriate form (plain Fault in case of one way requests, 
     * SoapFault in case of two way requests).
     * Also demonstrates how an interceptor on the server out fault chain can 
     * distinguish different fault modes (the capability to do so is crucial to
     * QOS interceptors such as the RM, addressing and policy interceptors).
     *
     */
    public static class Server extends AbstractBusTestServerBase {

        protected void run() {
            SpringBusFactory factory = new SpringBusFactory();
            Bus bus = factory.createBus();
            BusFactory.setDefaultBus(bus);
            setBus(bus);

            ControlImpl implementor = new ControlImpl();
            GreeterImpl greeterImplementor = new GreeterImpl();
            greeterImplementor.setThrowAlways(true);
            implementor.setImplementor(greeterImplementor);
            Endpoint.publish(CONTROL_PORT_ADDRESS, implementor);
            LOG.fine("Published control endpoint.");
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }
    
    private Bus controlBus;
    private Control control;
    private Bus greeterBus;
    private Greeter greeter;
    private List<Phase> inPhases;
    private PhaseComparator comparator;
    private Phase preLogicalPhase;
    
    

    @BeforeClass
    public static void startServers() throws Exception {
        TestUtilities.setKeepAliveSystemProperty(false);
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }
    
    @AfterClass
    public static void cleanup() {
        TestUtilities.recoverKeepAliveSystemProperty();
    }
    
    @After
    public void tearDown() {
        if (null != greeter) {
            assertTrue("Failed to stop greeter.", control.stopGreeter(null));
            greeterBus.shutdown(true);
            greeterBus = null;
        }
        if (null != control) {  
            assertTrue("Failed to stop greeter", control.stopGreeter(null));
            controlBus.shutdown(true);
        }
    }

    @Test
    public void testWithoutAddressing() throws Exception {
        
        setupGreeter("org/apache/cxf/systest/interceptor/no-addr.xml", false);

        // all interceptors pass

        /*
        greeter.greetMeOneWay("one");
        assertEquals("TWO", greeter.greetMe("two"));
        try {
            greeter.pingMe();
            fail("Expected PingMeFault not thrown.");
        } catch (PingMeFault f) {
            assertEquals(20, (int)f.getFaultInfo().getMajor());
            assertEquals(10, (int)f.getFaultInfo().getMinor());
        }
        */

        // behaviour is identicial for all phases
        
        Iterator<Phase> it = inPhases.iterator();
        Phase p = null;
        FaultLocation location = new org.apache.cxf.greeter_control.types.ObjectFactory()
            .createFaultLocation();        
        
        while (it.hasNext()) {
            p = it.next();
            location.setPhase(p.getName());
            if (Phase.PRE_LOGICAL.equals(p.getName())) {
                break;
            }             
            testFail(location);
        }
    }
    
    @Test
    public void testWithAddressingAnonymousReplies() throws Exception {
        setupGreeter("org/apache/cxf/systest/interceptor/addr.xml", false);

        // all interceptors pass

        greeter.greetMeOneWay("one");
        assertEquals("TWO", greeter.greetMe("two"));
        try {
            greeter.pingMe();
            fail("Expected PingMeFault not thrown.");
        } catch (PingMeFault f) {
            assertEquals(20, (int)f.getFaultInfo().getMajor());
            assertEquals(10, (int)f.getFaultInfo().getMinor());
        }
        
        // test failure in phases before Phase.PRE_LOGICAL
        
        Iterator<Phase> it = inPhases.iterator();
        Phase p = null;
        FaultLocation location = new org.apache.cxf.greeter_control.types.ObjectFactory()
            .createFaultLocation();
        location.setAfter(MAPAggregator.class.getName());
        
        // test failure occuring before logical addressing interceptor 

        while (it.hasNext()) {
            p = it.next();
            location.setPhase(p.getName());
            if (Phase.PRE_LOGICAL.equals(p.getName())) {
                break;
            }   
            testFail(location, true);
        }
        
        // test failure occuring after logical addressing interceptor -
        // won't get a fault in case of oneways (partial response already sent)
        
        do {  
            location.setPhase(p.getName());
            if (Phase.INVOKE.equals(p.getName())) {
                //faults from the PRE_LOGICAL and later phases won't make 
                //it back to the client, the 200/202 response has already 
                //been returned.  The server has accepted the message
                break;
            }             
            testFail(location, true);
            p = it.hasNext() ? it.next() : null;
        } while (null != p);
    }
    
    private void testFail(FaultLocation location) throws PingMeFault {
        testFail(location, false);
    }
   
    private void testFail(FaultLocation location, boolean usingAddressing) throws PingMeFault {
        // System.out.print("Test interceptor failing in phase: " + location.getPhase()); 
        
        control.setFaultLocation(location);       
       
        String expectedMsg = getExpectedInterceptorFaultMessage(location.getPhase());

        // oneway reports a plain fault (although server sends a soap fault)

        boolean expectOnewayFault = !usingAddressing 
            || comparator.compare(preLogicalPhase, getPhase(location.getPhase())) > 0;
        
        try {
            greeter.greetMeOneWay("oneway");
            if (expectOnewayFault) {
                fail("Oneway operation unexpectedly succeded for phase " + location.getPhase());
            }
        } catch (WebServiceException ex) {
            if (!expectOnewayFault) {
                fail("Oneway operation unexpectedly failed.");
            }
            Throwable cause = ex.getCause();
            Fault f = (Fault)cause;
            assertEquals(FAULT_CODE, f.getCode());
            assertEquals(FAULT_MESSAGE, f.getMessage());
        }
        
        try {
            greeter.greetMe("cxf");
            fail("Twoway operation unexpectedly succeded.");
        } catch (WebServiceException ex) {
            Throwable cause = ex.getCause();
            SoapFault sf = (SoapFault)cause;
            
            assertEquals(expectedMsg, sf.getReason());
            assertEquals(SOAP_FAULT_CODE, sf.getFaultCode());
        }
        
        try {
            greeter.pingMe();
            fail("Expected PingMeFault not thrown.");
        } catch (WebServiceException ex) {
            Throwable cause = ex.getCause();
            SoapFault sf = (SoapFault)cause;
            assertEquals(expectedMsg, sf.getReason());
            assertEquals(SOAP_FAULT_CODE, sf.getFaultCode());
        }  
    }
   
    
    
    private void setupGreeter(String cfgResource, boolean useDecoupledEndpoint) {
        
        SpringBusFactory bf = new SpringBusFactory();
        
        controlBus = bf.createBus();
        BusFactory.setDefaultBus(controlBus);

        ControlService cs = new ControlService();
        control = cs.getControlPort();
        
        assertTrue("Failed to start greeter", control.startGreeter(cfgResource));
        
        greeterBus = bf.createBus(cfgResource);
        BusFactory.setDefaultBus(greeterBus);
        LOG.fine("Initialised greeter bus with configuration: " + cfgResource);
        
        if (null == comparator) {
            comparator = new PhaseComparator();
        }
        if (null == inPhases) {
            inPhases = new ArrayList<Phase>();
            inPhases.addAll(greeterBus.getExtension(PhaseManager.class).getInPhases());
            Collections.sort(inPhases, comparator);
        }        
        if (null == preLogicalPhase) {
            preLogicalPhase = getPhase(Phase.PRE_LOGICAL);
        }
       
        GreeterService gs = new GreeterService();

        greeter = gs.getGreeterPort();
        LOG.fine("Created greeter client.");
       
        if (!useDecoupledEndpoint) {
            return;
        }

        // programatically configure decoupled endpoint that is guaranteed to
        // be unique across all test cases
        
        decoupledEndpointPort--;
        decoupledEndpoint = "http://localhost:" + decoupledEndpointPort + "/decoupled_endpoint";

        Client c = ClientProxy.getClient(greeter);
        HTTPConduit hc = (HTTPConduit)(c.getConduit());
        HTTPClientPolicy cp = hc.getClient();
        cp.setDecoupledEndpoint(decoupledEndpoint);

        LOG.fine("Using decoupled endpoint: " + cp.getDecoupledEndpoint());
    }

    private String getExpectedInterceptorFaultMessage(String phase) {
        return FaultThrowingInterceptor.MESSAGE_FORMAT.format(new Object[] {phase}).toUpperCase();
    }
    
    private Phase getPhase(String name) {
        for (Phase p : inPhases) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }
}
