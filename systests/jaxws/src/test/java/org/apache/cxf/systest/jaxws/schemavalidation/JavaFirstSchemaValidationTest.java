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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.soap.SOAPFaultException;

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
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * TODO - test where the default is NONE at the service level test where the default is IN or OUT, and then
 * override at operation levels
 */
public class JavaFirstSchemaValidationTest extends Assert {
    static final String PORT = TestUtil.getPortNumber(JavaFirstSchemaValidationTest.class);

    private static List<Server> serverList = new ArrayList<Server>();
    private static PersonServiceAnnotated annotatedClient;
    private static PersonService client;

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

        createServer(PersonServiceAnnotated.class, new PersonServiceAnnotatedImpl(), null);

        annotatedClient = createClient(PersonServiceAnnotated.class);
        client = createClient(PersonService.class);
    }

    @AfterClass
    public static void cleanup() throws Exception {
        for (Server server : serverList) {
            server.stop();
        }
    }

    static String getAddress(Class<?> sei) {
        return "http://localhost:" + PORT + "/" + sei.getSimpleName();
    }

    // so this is the default, we are inheriting from the service level SchemaValidation annotation
    // which is set to BOTH
    @Test
    public void testEndpointSchemaValidationAnnotated() {
        Person person = new Person();

        try {
            annotatedClient.saveInheritEndpoint(person);
        } catch (SOAPFaultException sfe) {
            // verify its server side and a schema validation
            assertTrue(sfe.getMessage().contains("Unmarshalling Error"));
        }

        try {
            person.setFirstName(""); // empty string is valid
            annotatedClient.saveInheritEndpoint(person);
        } catch (SOAPFaultException sfe) {
            // verify its server side and a schema validation
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
        } catch (SOAPFaultException sfe) {
            // verify its server side and a schema validation
            assertTrue(sfe.getMessage().contains("Unmarshalling Error"));
        }

        try {
            person.setFirstName(""); // empty string is valid
            annotatedClient.saveValidateIn(person);
        } catch (SOAPFaultException sfe) {
            // verify its server side and a schema validation
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

    // no validation is required for incoming
    @Test
    public void testSaveValidationOutAnnotated() {
        Person person = new Person();

        annotatedClient.saveValidateOut(person);

        person.setFirstName(""); // empty string is valid
        annotatedClient.saveValidateOut(person);

        person.setLastName(""); // empty string is valid
        annotatedClient.saveValidateOut(person);
    }

    // so this is the default, we are inheriting from the service level SchemaValidation annotation
    // which is set to BOTH
    @Test
    public void testEndpointSchemaValidationProvider() {
        Person person = new Person();

        try {
            client.saveInheritEndpoint(person);
        } catch (SOAPFaultException sfe) {
            // verify its server side and a schema validation
            assertTrue(sfe.getMessage().contains("Unmarshalling Error"));
        }

        try {
            person.setFirstName(""); // empty string is valid
            client.saveInheritEndpoint(person);
        } catch (SOAPFaultException sfe) {
            // verify its server side and a schema validation
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
        } catch (SOAPFaultException sfe) {
            // verify its server side and a schema validation
            assertTrue(sfe.getMessage().contains("Unmarshalling Error"));
        }

        try {
            person.setFirstName(""); // empty string is valid
            client.saveValidateIn(person);
        } catch (SOAPFaultException sfe) {
            // verify its server side and a schema validation
            assertTrue(sfe.getMessage().contains("Unmarshalling Error"));
        }

        person.setLastName(""); // empty string is valid
        client.saveValidateIn(person);
    }

    // no validation at all is required
    @Test
    public void testSaveNoValidationProvider() {
        Person person = new Person();
        client.saveNoValidation(person);

        person.setFirstName(""); // empty string is valid
        client.saveNoValidation(person);

        person.setLastName(""); // empty string is valid
        client.saveNoValidation(person);
    }

    // no validation is required for incoming
    @Test
    public void testSaveValidationOutProvider() {
        Person person = new Person();

        client.saveValidateOut(person);

        person.setFirstName(""); // empty string is valid
        client.saveValidateOut(person);

        person.setLastName(""); // empty string is valid
        client.saveValidateOut(person);
    }

    private static <T> T createClient(Class<T> serviceClass) {
        JaxWsProxyFactoryBean clientFactory = new JaxWsProxyFactoryBean();
        clientFactory.setServiceClass(serviceClass);

        // ensure all client schema validation is disabled
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(Message.SCHEMA_VALIDATION_ENABLED, SchemaValidationType.NONE);
        clientFactory.setProperties(properties);

        clientFactory.setAddress(getAddress(serviceClass));

        @SuppressWarnings("unchecked")
        T newClient = (T)clientFactory.create();

        Client clientProxy = ClientProxy.getClient(newClient);

        // ensure all client schema validation is disabled
        for (BindingOperationInfo bop : clientProxy.getEndpoint().getEndpointInfo().getBinding()
            .getOperations()) {
            bop.getOperationInfo().setProperty(Message.SCHEMA_VALIDATION_ENABLED, SchemaValidationType.NONE);
        }

        return newClient;
    }

    public static Server createServer(Class<?> serviceInterface, Object serviceImpl, Feature feature)
        throws IOException {
        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setServiceClass(serviceImpl.getClass());
        if (feature != null) {
            svrFactory.getFeatures().add(feature);
        }
        svrFactory.setAddress(getAddress(serviceInterface));
        svrFactory.setServiceBean(serviceImpl);
        Server server = svrFactory.create();
        serverList.add(server);
        return server;
    }
}
