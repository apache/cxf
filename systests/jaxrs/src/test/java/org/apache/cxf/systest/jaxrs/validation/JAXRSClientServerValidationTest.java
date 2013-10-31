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

import java.util.Arrays;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.interceptor.JAXRSOutExceptionMapperInterceptor;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.validation.ConstraintViolationExceptionMapper;
import org.apache.cxf.jaxrs.validation.JAXRSValidationInInterceptor;
import org.apache.cxf.jaxrs.validation.JAXRSValidationOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSClientServerValidationTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(Server.class);

    @Ignore
    public static class Server extends AbstractBusTestServerBase {        
        @SuppressWarnings("unchecked")
        protected void run() {
            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();

            sf.setResourceClasses(BookStoreWithValidation.class);
            sf.setResourceProvider(BookStoreWithValidation.class, 
                new SingletonResourceProvider(new BookStoreWithValidation()));
            sf.setProvider(new ConstraintViolationExceptionMapper());

            sf.setAddress("http://localhost:" + PORT + "/");
            sf.setInInterceptors(Arrays.< Interceptor< ? extends Message > >asList(
                new JAXRSValidationInInterceptor()));
             
            sf.setOutInterceptors(Arrays.< Interceptor< ? extends Message > >asList(
                new JAXRSOutExceptionMapperInterceptor(),
                new JAXRSValidationOutInterceptor()));

            sf.create();
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
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        createStaticBus();
    }
    
    @Before
    public void setUp() {
        final Response r = createWebClient("/bookstore/books").delete();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
    }
       
    @Test
    public void testThatPatternValidationFails() throws Exception {
        final Response r = createWebClient("/bookstore/books/blabla").get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }
    
    @Test
    public void testThatNotNullValidationFails()  {
        final Response r = createWebClient("/bookstore/books").post(new Form());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }
    
    @Test
    public void testThatSizeValidationFails()  {
        final Response r = createWebClient("/bookstore/books").post(new Form().param("id", ""));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }
    
    @Test
    public void testThatMinValidationFails()  {
        final Response r = createWebClient("/bookstore/books").query("page", "0").get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }
    
    @Test
    public void testThatNoValidationConstraintsAreViolated()  {
        final Response r = createWebClient("/bookstore/books").query("page", "2").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
    }
    
    @Test
    public void testThatNoValidationConstraintsAreViolatedWithDefaultValue()  {
        final Response r = createWebClient("/bookstore/books").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
    }
    
    @Test
    public void testThatResponseValidationForOneBookFails()  {
        Response r = createWebClient("/bookstore/books").post(new Form().param("id", "1234"));
        assertEquals(Status.CREATED.getStatusCode(), r.getStatus());

        r = createWebClient("/bookstore/books/1234").get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());
    }

    @Test
    public void testThatResponseValidationForAllBooksFails()  {
        Response r = createWebClient("/bookstore/books").post(new Form().param("id", "1234"));
        assertEquals(Status.CREATED.getStatusCode(), r.getStatus());

        r = createWebClient("/bookstore/books").get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());
    }
    
    private WebClient createWebClient(final String url) {
        WebClient wc = WebClient
            .create("http://localhost:" + PORT + url)
            .accept(MediaType.APPLICATION_JSON);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        return wc;
    }
}

