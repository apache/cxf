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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
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
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JavaFirstSchemaValidationTest {
    static final String PORT = TestUtil.getNewPortNumber(JavaFirstSchemaValidationTest.class);
    static final String PORT2 = TestUtil.getNewPortNumber(JavaFirstSchemaValidationTest.class);
    static final String PORT_UNUSED = TestUtil.getNewPortNumber(JavaFirstSchemaValidationTest.class);

    private static List<Server> serverList = new ArrayList<>();
    private static PersonServiceAnnotated annotatedClient;
    private static PersonService client;
    private static PersonServiceRPC rpcClient;

    private static PersonServiceWithRequestResponseAnns annotatedNonValidatingClient;
    private static PersonServiceWithRequestResponseAnns disconnectedClient;
    private static PersonServiceWithRequestResponseAnns noValidationServerClient;

    @BeforeClass
    public static void startServers() throws Exception {
        createServer(PORT, PersonService.class, new PersonServiceImpl(), null, createSchemaValidationFeature());

        createServer(PORT2, PersonServiceWithRequestResponseAnns.class,
                new PersonServiceWithRequestResponseAnnsImpl(), SchemaValidationType.NONE,
                    createNoSchemaValidationFeature());

        createServer(PORT, PersonServiceAnnotated.class, new PersonServiceAnnotatedImpl(), null);

        createServer(PORT, PersonServiceRPC.class, new PersonServiceRPCImpl(), null,
                createSchemaValidationFeature());

        createServer(PORT, PersonServiceWithRequestResponseAnns.class,
                new PersonServiceWithRequestResponseAnnsImpl(), null);

        annotatedClient = createClient(PORT, PersonServiceAnnotated.class, SchemaValidationType.NONE);
        annotatedNonValidatingClient = createClient(PORT, PersonServiceWithRequestResponseAnns.class,
                SchemaValidationType.NONE);
        client = createClient(PORT, PersonService.class, SchemaValidationType.NONE);
        disconnectedClient = createClient(PORT_UNUSED, PersonServiceWithRequestResponseAnns.class, null);
        rpcClient = createClient(PORT, PersonServiceRPC.class, SchemaValidationType.NONE);
        noValidationServerClient = createClient(PORT2, PersonServiceWithRequestResponseAnns.class, null);
    }

    private static SchemaValidationFeature createSchemaValidationFeature() {
        Map<String, SchemaValidationType> operationMap = new HashMap<>();
        operationMap.put("saveInheritEndpoint", SchemaValidationType.BOTH);
        operationMap.put("saveNoValidation", SchemaValidationType.NONE);
        operationMap.put("saveValidateIn", SchemaValidationType.IN);
        operationMap.put("saveValidateOut", SchemaValidationType.OUT);
        DefaultSchemaValidationTypeProvider provider = new DefaultSchemaValidationTypeProvider(operationMap);
        return new SchemaValidationFeature(provider);
    }
    private static SchemaValidationFeature createNoSchemaValidationFeature() {
        Map<String, SchemaValidationType> operationMap = new HashMap<>();
        operationMap.put("*", SchemaValidationType.NONE);
        DefaultSchemaValidationTypeProvider provider = new DefaultSchemaValidationTypeProvider(operationMap);
        return new SchemaValidationFeature(provider);
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
            assertTrue(sfe.getMessage().contains("Unmarshalling Error"));
        }

        try {
            person.setFirstName(""); // empty string is valid
            annotatedClient.saveInheritEndpoint(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            assertTrue(sfe.getMessage().contains("Unmarshalling Error"));
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
            assertTrue(sfe.getMessage().contains("Unmarshalling Error"));
        }

        try {
            person.setFirstName(""); // empty string is valid
            annotatedClient.saveValidateIn(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            assertTrue(sfe.getMessage().contains("Unmarshalling Error"));
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
    public void testRequestValidationWithClientValidationDisabled() {
        Person person = new Person();

        try {
            annotatedNonValidatingClient.saveValidateIn(person);
        } catch (SOAPFaultException sfe) {
            // has to be server side exception, as all validation is disabled on client
            assertTrue(sfe.getMessage().contains("Marshalling Error"));
        }

        person.setFirstName(""); // empty string is valid
        try {
            annotatedNonValidatingClient.saveValidateIn(person);
        } catch (SOAPFaultException sfe) {
            // has to be server side exception, as all validation is disabled on client
            assertTrue(sfe.getMessage().contains("Marshalling Error"));
        }

        person.setLastName(""); // empty string is valid
        annotatedNonValidatingClient.saveValidateIn(person);
    }

    @Test
    public void testResponseValidationWithClientValidationDisabled() {
        Person person = new Person();

        try {
            person.setFirstName("InvalidResponse");
            person.setLastName("WhoCares");
            annotatedNonValidatingClient.saveValidateOut(person);
        } catch (SOAPFaultException sfe) {
            // has to be server side exception, as all validation is disabled on client
            assertTrue(sfe.getMessage().contains("Marshalling Error"));
        }
    }

    @Test
    public void testEndpointSchemaValidationProvider() {
        Person person = new Person();

        try {
            client.saveInheritEndpoint(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            assertTrue(sfe.getMessage().contains("Unmarshalling Error"));
        }

        try {
            person.setFirstName(""); // empty string is valid
            client.saveInheritEndpoint(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            assertTrue(sfe.getMessage().contains("Unmarshalling Error"));
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
            assertTrue(sfe.getMessage().contains("Unmarshalling Error"));
        }

        try {
            person.setFirstName(""); // empty string is valid
            client.saveValidateIn(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            assertTrue(sfe.getMessage().contains("Unmarshalling Error"));
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
    public void testRequestClientValidation() {
        Person person = new Person();

        try {
            disconnectedClient.saveValidateOut(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            assertTrue(sfe.getMessage().contains("Marshalling Error"));
        }

        person.setFirstName(""); // empty string is valid
        try {
            disconnectedClient.saveValidateOut(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            assertTrue(sfe.getMessage().contains("Marshalling Error"));
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
    public void testResponseClientValidation() {
        Person person = new Person();

        try {
            noValidationServerClient.saveValidateIn(person);
            fail("Expected exception");
        } catch (SOAPFaultException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("Unmarshalling Error"));
        }

        person.setFirstName(""); // empty string is valid
        try {
            noValidationServerClient.saveValidateIn(person);
            fail("Expected exception");
        } catch (SOAPFaultException sfe) {
            assertTrue(sfe.getMessage().contains("Unmarshalling Error"));
        }

        person.setLastName(""); // empty string is valid

        noValidationServerClient.saveValidateIn(person);
    }

    @Test
    public void testSaveValidationOutProvider() {
        Person person = new Person();

        try {
            client.saveValidateOut(person);
        } catch (SOAPFaultException sfe) {
            // verify its server side outgoing
            assertTrue(sfe.getMessage().contains("Marshalling Error"));
        }

        person.setFirstName(""); // empty string is valid
        try {
            client.saveValidateOut(person);
        } catch (SOAPFaultException sfe) {
            assertTrue(sfe.getMessage().contains("Marshalling Error"));
        }

        person.setLastName(""); // empty string is valid
        client.saveValidateOut(person);
    }

    private static <T> T createClient(String port, Class<T> serviceClass, SchemaValidationType type,
            Feature ... features) {
        JaxWsProxyFactoryBean clientFactory = new JaxWsProxyFactoryBean();
        clientFactory.setServiceClass(serviceClass);


        clientFactory.setAddress(getAddress(port, serviceClass));

        if (features != null) {
            Collections.addAll(clientFactory.getFeatures(), features);
        }

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

    public static Server createServer(String port, Class<?> serviceInterface, Object serviceImpl,
            SchemaValidationType type, Feature ... features)
        throws IOException {
        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setServiceClass(serviceImpl.getClass());

        if (features != null) {
            Collections.addAll(svrFactory.getFeatures(), features);
        }

        if (type != null) {
            Map<String, Object> properties = new HashMap<>();
            properties.put(Message.SCHEMA_VALIDATION_ENABLED, type);
            svrFactory.setProperties(properties);
        }

        svrFactory.setAddress(getAddress(port, serviceInterface));
        svrFactory.setServiceBean(serviceImpl);
        Server server = svrFactory.create();
        serverList.add(server);
        return server;
    }
}