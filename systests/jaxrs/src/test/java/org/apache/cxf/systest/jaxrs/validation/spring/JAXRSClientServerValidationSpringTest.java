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
package org.apache.cxf.systest.jaxrs.validation.spring;

import java.util.Arrays;

import javax.xml.namespace.QName;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.SOAPBinding;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.validation.JAXRSClientBeanValidationFeature;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.jaxrs.AbstractSpringServer;
import org.apache.cxf.systest.jaxrs.validation.AbstractJAXRSValidationTest;
import org.apache.cxf.systest.jaxrs.validation.BookWithValidation;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JAXRSClientServerValidationSpringTest extends AbstractJAXRSValidationTest {
    public static final String PORT = allocatePort(JAXRSClientServerValidationSpringTest.class);

    @Ignore
    public static class Server extends AbstractSpringServer {
        public Server() {
            super("/jaxrs_spring_validation", Integer.parseInt(PORT));
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

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }

    @Test
    public void testProgrammaticValidationFailsIfNameIsNull()  {
        final Response r = createWebClient("/jaxrs/bookstore/books").post(new Form().param("id", "1"));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }

    @Test
    public void testProgrammaticValidationPassesButParameterValidationFailesIfIdIsNull()  {
        final Response r = createWebClient("/jaxrs/bookstore/books").post(new Form().param("name", "aa"));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }

    @Test
    public void testHelloRestValidationFailsIfNameIsNull() throws Exception {
        String address = "http://localhost:" + PORT + "/bwrest";

        BookWorld service = JAXRSClientFactory.create(address, BookWorld.class);

        BookWithValidation bw = service.echoBook(new BookWithValidation("RS", "123"));
        assertEquals("123", bw.getId());

        try {
            service.echoBook(new BookWithValidation(null, "123"));
            fail("Validation failure expected");
        } catch (BadRequestException ex) {
            // complete
        }

    }

    @Test
    public void testHelloRestValidationFailsIfNameIsNullClient() throws Exception {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress("http://localhost:" + PORT + "/bwrest");
        bean.setServiceClass(BookWorld.class);
        bean.setFeatures(Arrays.asList(new JAXRSClientBeanValidationFeature()));
        BookWorld service = bean.create(BookWorld.class);
        BookWithValidation bw = service.echoBook(new BookWithValidation("RS", "123"));
        assertEquals("123", bw.getId());

        try {
            service.echoBook(new BookWithValidation(null, "123"));
            fail("Validation failure expected");
        } catch (ConstraintViolationException ex) {
            // complete
        }

    }
    @Test
    public void testHelloSoapValidationFailsIfNameIsNull() throws Exception {
        final QName serviceName = new QName("http://bookworld.com", "BookWorld");
        final QName portName = new QName("http://bookworld.com", "BookWorldPort");
        final String address = "http://localhost:" + PORT + "/bwsoap";

        Service service = Service.create(serviceName);
        service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, address);

        BookWorld bwService = service.getPort(BookWorld.class);
        BookWithValidation bw = bwService.echoBook(new BookWithValidation("WS", "123"));
        assertEquals("123", bw.getId());
        try {
            bwService.echoBook(new BookWithValidation(null, "123"));
            fail("Validation failure expected");
        } catch (SOAPFaultException ex) {
            // complete
        }
    }

    @Override
    protected String getPort() {
        return PORT;
    }
}

