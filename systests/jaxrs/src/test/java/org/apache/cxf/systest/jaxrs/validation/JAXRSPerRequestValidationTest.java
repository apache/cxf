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
package org.apache.cxf.systest.jaxrs.validation;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInvoker;
import org.apache.cxf.jaxrs.validation.ValidationExceptionMapper;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSPerRequestValidationTest extends AbstractJAXRSValidationTest {
    public static final String PORT = allocatePort(JAXRSPerRequestValidationTest.class);

    public static class Server extends AbstractServerTestServerBase {
        @Override
        protected org.apache.cxf.endpoint.Server createServer(Bus bus) throws Exception {
            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();

            sf.setResourceClasses(BookStoreWithValidationPerRequest.class);
            sf.setProvider(new ValidationExceptionMapper());

            sf.setAddress("http://localhost:" + PORT + "/");
            sf.setInvoker(new JAXRSBeanValidationInvoker());

            return sf.create();
        }

        public static void main(String[] args) throws Exception {
            new Server().start();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testThatNoValidationConstraintsAreViolatedWhenBookIdIsSet()  {
        final Response r = createWebClient("/bookstore/book").query("id", "123").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
    }

    @Test
    public void testThatValidationConstraintsAreViolatedWhenBookIdIsNotSet()  {
        final Response r = createWebClient("/bookstore/book").get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }

    @Test
    public void testThatValidationConstraintsAreViolatedWhenBookDoesNotExist()  {
        final Response r = createWebClient("/bookstore/book").query("id", "3333").get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());
    }

    @Test
    public void testThatValidationConstraintsAreViolatedWhenBookDoesNotExistResponse()  {
        final Response r = createWebClient("/bookstore/bookResponse").query("id", "3333").get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());
    }

    @Test
    public void testThatValidationConstraintsAreViolatedWhenBookNameIsNotSet()  {
        final Response r = createWebClient("/bookstore/book").query("id", "124").get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());
    }

    @Override
    protected String getPort() {
        return PORT;
    }
}
