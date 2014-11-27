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

package org.apache.cxf.systest.jaxws.schemavalidation;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.feature.validation.DefaultSchemaValidationTypeProvider;
import org.apache.cxf.feature.validation.SchemaValidationFeature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JavaFirstSchemaValidationTest extends Assert {
    static final String PORT = TestUtil.getNewPortNumber(JavaFirstSchemaValidationTest.class);
    static final String PORT_UNUSED = TestUtil.getNewPortNumber(JavaFirstSchemaValidationTest.class);

    private static List<Server> serverList = new ArrayList<Server>();
    private static PersonServiceAnnotated annotatedClient;
    private static PersonServiceAnnotated annotatedValidatingClient;
    private static PersonService client;
    private static PersonServiceRPC rpcClient;
    
    private static PersonService disconnectedClient;

    @BeforeClass
    public static void startServers() throws Exception {
        Map<String, SchemaValidationType> operationMap = new HashMap<String, SchemaValidationType>();
        operationMap.put("saveInheritEndpoint", SchemaValidationType.BOTH);
        operationMap.put("saveNoValidation", SchemaValidationType.NONE);
        operationMap.put("saveValidateIn", SchemaValidationType.IN);
        operationMap.put("saveValidateOut", SchemaValidationType.OUT);
        DefaultSchemaValidationTypeProvider provider = new DefaultSchemaValidationTypeProvider(operationMap);

        SchemaValidationFeature feature = new SchemaValidationFeature(provider);

        createServer(PersonService.class, new PersonServiceImpl(), feature);

        createServer(PersonServiceAnnotated.class, new PersonServiceAnnotatedImpl());

        createServer(PersonServiceRPC.class, new PersonServiceRPCImpl(), feature, new LoggingFeature());

        annotatedClient = createClient(PORT, PersonServiceAnnotated.class, SchemaValidationType.NONE);
        annotatedValidatingClient = createClient(PORT, PersonServiceAnnotated.class, null);
        client = createClient(PORT, PersonService.class, SchemaValidationType.NONE);
        disconnectedClient = createClient(PORT_UNUSED, PersonService.class, SchemaValidationType.OUT);
        rpcClient = createClient(PORT, PersonServiceRPC.class, SchemaValidationType.NONE);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        for (Server server : serverList) {
            server.stop();
        }
    }

    static String getAddress(String port, Class<?> sei) {
        return "http://localhost:" + port + "/" + sei.getSimpleName();
    }

    @Test
    public void testRPCLit() throws Exception { 
        Person person = new Person();
        person.setFirstName("Foo");
        person.setLastName("Bar");
        //this should work
        rpcClient.saveValidateOut(person);
        
        try {
            person.setFirstName(null);
            rpcClient.saveValidateOut(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            assertTrue(sfe.getMessage().contains("Marshalling Error"));
            assertTrue(sfe.getMessage().contains("lastName"));
        }            
    }

    
    // so this is the default, we are inheriting from the service level SchemaValidation annotation
    // which is set to BOTH
    @Test
    public void testEndpointSchemaValidationAnnotated() {
        Person person = new Person();

        try {
            annotatedClient.saveInheritEndpoint(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            String stackTrace = getStackTrace(sfe);
            assertTrue(stackTrace.contains("Unmarshalling Error"));
            assertTrue(stackTrace.contains("org.apache.cxf.binding.soap.SoapFault"));
        }

        try {
            person.setFirstName(""); // empty string is valid
            annotatedClient.saveInheritEndpoint(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            String stackTrace = getStackTrace(sfe);
            assertTrue(stackTrace.contains("Unmarshalling Error"));
            assertTrue(stackTrace.contains("org.apache.cxf.binding.soap.SoapFault"));
        }

        person.setLastName(""); // empty string is valid
        annotatedClient.saveInheritEndpoint(person);
    }
    
    @Test
    public void testSaveValidateInAnnotated() {
        Person person = new Person();

        try {
            annotatedClient.saveValidateIn(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            String stackTrace = getStackTrace(sfe);
            assertTrue(stackTrace.contains("Unmarshalling Error"));
            assertTrue(stackTrace.contains("org.apache.cxf.binding.soap.SoapFault"));
        }

        try {
            person.setFirstName(""); // empty string is valid
            annotatedClient.saveValidateIn(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            String stackTrace = getStackTrace(sfe);
            assertTrue(stackTrace.contains("Unmarshalling Error"));
            assertTrue(stackTrace.contains("org.apache.cxf.binding.soap.SoapFault"));
        }

        person.setLastName(""); // empty string is valid
        annotatedClient.saveValidateIn(person);
    }

    // no validation at all is required
    @Test
    public void testSaveNoValidationAnnotated() {
        Person person = new Person();
        annotatedClient.saveNoValidation(person);

        person.setFirstName(""); // empty string is valid
        annotatedClient.saveNoValidation(person);

        person.setLastName(""); // empty string is valid
        annotatedClient.saveNoValidation(person);
    }

    @Test
    public void testSaveValidationOutAnnotatedWithClientValidationDisabled() {
        Person person = new Person();

        try {
            annotatedClient.saveValidateOut(person);
        } catch (SOAPFaultException sfe) {
            // verify its server side and a schema validation
            String stackTrace = getStackTrace(sfe);
            assertTrue(stackTrace.contains("Marshalling Error"));
            
            // it's still a server side fault, because server side validation coming in failed
            assertTrue(stackTrace.contains("org.apache.cxf.binding.soap.SoapFault"));
        }
        
        person.setFirstName(""); // empty string is valid
        try {
            annotatedClient.saveValidateOut(person);
        } catch (SOAPFaultException sfe) {
            // verify its server side and a schema validation
            String stackTrace = getStackTrace(sfe);
            assertTrue(stackTrace.contains("Marshalling Error"));
            
            // it's still a server side fault, because server side validation coming in failed
            assertTrue(stackTrace.contains("org.apache.cxf.binding.soap.SoapFault"));
        }

        person.setLastName(""); // empty string is valid
        annotatedClient.saveValidateIn(person);
    }
    
    // this will still all be server side, as the OUT validation is turned into an IN validation for
    // the client, but by then the server has already thrown the exception for the OUT
    @Test
    public void testSaveValidationOutAnnotatedWithClientValidationEnabled() {
        Person person = new Person();

        try {
            annotatedValidatingClient.saveValidateIn(person);
        } catch (SOAPFaultException sfe) {
            String stackTrace = getStackTrace(sfe);
            assertTrue(stackTrace.contains("Marshalling Error"));
            assertFalse(stackTrace.contains("org.apache.cxf.binding.soap.SoapFault"));
        }
        
        person.setFirstName(""); // empty string is valid
        try {
            annotatedValidatingClient.saveValidateIn(person);
        } catch (SOAPFaultException sfe) {
            String stackTrace = getStackTrace(sfe);
            assertTrue(stackTrace.contains("Marshalling Error"));
            assertFalse(stackTrace.contains("org.apache.cxf.binding.soap.SoapFault"));
        }

        person.setLastName(""); // empty string is valid
        annotatedValidatingClient.saveValidateIn(person);
    }
    
    @Test
    public void testSaveValidationInAnnotatedWithClientValidationEnabled() {
        Person person = new Person();

        try {
            person.setFirstName("InvalidResponse");
            person.setLastName("WhoCares");
            annotatedValidatingClient.saveValidateOut(person);
        } catch (SOAPFaultException sfe) {
            String stackTrace = getStackTrace(sfe);
            assertTrue(stackTrace.contains("Marshalling Error"));
            assertFalse(stackTrace.contains("org.apache.cxf.binding.soap.SoapFault"));
        }
    }

    @Test
    public void testEndpointSchemaValidationProvider() {
        Person person = new Person();

        try {
            client.saveInheritEndpoint(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            String stackTrace = getStackTrace(sfe);
            assertTrue(stackTrace.contains("Unmarshalling Error"));
            assertTrue(stackTrace.contains("org.apache.cxf.binding.soap.SoapFault"));
        }
       
        try {
            person.setFirstName(""); // empty string is valid
            client.saveInheritEndpoint(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            String stackTrace = getStackTrace(sfe);
            assertTrue(stackTrace.contains("Unmarshalling Error"));
            assertTrue(stackTrace.contains("org.apache.cxf.binding.soap.SoapFault"));
        }

        person.setLastName(""); // empty string is valid
        client.saveInheritEndpoint(person);
    }

    @Test
    public void testSaveValidateInProvider() {
        Person person = new Person();

        try {
            client.saveValidateIn(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            String stackTrace = getStackTrace(sfe);
            assertTrue(stackTrace.contains("Unmarshalling Error"));
            assertTrue(stackTrace.contains("org.apache.cxf.binding.soap.SoapFault"));
        }

        try {
            person.setFirstName(""); // empty string is valid
            client.saveValidateIn(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            String stackTrace = getStackTrace(sfe);
            assertTrue(stackTrace.contains("Unmarshalling Error"));
            assertTrue(stackTrace.contains("org.apache.cxf.binding.soap.SoapFault"));
        }

        person.setLastName(""); // empty string is valid
        client.saveValidateIn(person);
    }

    @Test
    public void testSaveNoValidationProvider() {
        Person person = new Person();
        client.saveNoValidation(person);

        person.setFirstName(""); // empty string is valid
        client.saveNoValidation(person);

        person.setLastName(""); // empty string is valid
        client.saveNoValidation(person);
    }

    @Test
    public void testSaveValidationOutProviderClientOnly() {
        Person person = new Person();

        try {
            disconnectedClient.saveValidateOut(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            // verify its client side outgoing
            String stackTrace = getStackTrace(sfe);
            assertTrue(stackTrace.contains("Marshalling Error"));
            assertFalse(stackTrace.contains("org.apache.cxf.binding.soap.SoapFault"));
        }
        
        person.setFirstName(""); // empty string is valid
        try {
            disconnectedClient.saveValidateOut(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            // verify its client side outgoing
            String stackTrace = getStackTrace(sfe);
            assertTrue(stackTrace.contains("Marshalling Error"));
            assertFalse(stackTrace.contains("org.apache.cxf.binding.soap.SoapFault"));
        }

        person.setLastName(""); // empty string is valid
        
        // this confirms that we passed client validation as we then got the connectivity error
        try {
            disconnectedClient.saveValidateOut(person);
            fail("Expected exception");
        } catch (WebServiceException e) {
            assertTrue(e.getMessage().contains("Could not send Message"));
        }
    }

    
    @Test
    public void testSaveValidationOutProvider() {
        Person person = new Person();

        try {
            client.saveValidateOut(person);
        } catch (SOAPFaultException sfe) {
            // verify its server side outgoing
            String stackTrace = getStackTrace(sfe);
            assertTrue(stackTrace.contains("Marshalling Error"));
            assertTrue(stackTrace.contains("org.apache.cxf.binding.soap.SoapFault"));
        }
        
        person.setFirstName(""); // empty string is valid
        try {
            client.saveValidateOut(person);
        } catch (SOAPFaultException sfe) {
            // verify its server side outgoing
            String stackTrace = getStackTrace(sfe);
            assertTrue(stackTrace.contains("Marshalling Error"));
            assertTrue(stackTrace.contains("org.apache.cxf.binding.soap.SoapFault"));
        }

        person.setLastName(""); // empty string is valid
        client.saveValidateOut(person);
    }

    private static <T> T createClient(String port, Class<T> serviceClass, SchemaValidationType type) {
        JaxWsProxyFactoryBean clientFactory = new JaxWsProxyFactoryBean();
        clientFactory.setServiceClass(serviceClass);
        
        
        clientFactory.setAddress(getAddress(port, serviceClass));
        
        
        @SuppressWarnings("unchecked")
        T newClient = (T)clientFactory.create();

        Client proxy = ClientProxy.getClient(newClient);
        
        if (type != null) {
            proxy.getRequestContext().put(Message.SCHEMA_VALIDATION_ENABLED, type);
        }
        
        HTTPConduit conduit = (HTTPConduit) proxy.getConduit();
        // give me longer debug times
        HTTPClientPolicy clientPolicy = new HTTPClientPolicy();
        clientPolicy.setConnectionTimeout(1000000);
        clientPolicy.setReceiveTimeout(1000000);
        conduit.setClient(clientPolicy);
        
        return newClient;
    }

    public static Server createServer(Class<?> serviceInterface, Object serviceImpl, Feature ... features)
        throws IOException {
        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setServiceClass(serviceImpl.getClass());
        if (features != null) {
            svrFactory.getFeatures().addAll(Arrays.asList(features));
        }
        svrFactory.setAddress(getAddress(PORT, serviceInterface));
        svrFactory.setServiceBean(serviceImpl);
        Server server = svrFactory.create();
        serverList.add(server);
        return server;
    }
    
    private String getStackTrace(Exception e) {
        StringWriter sWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(sWriter, true);
        e.printStackTrace(writer);
        return sWriter.toString();
    }
}
