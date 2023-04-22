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
import java.util.Collections;

import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationOutInterceptor;
import org.apache.cxf.jaxrs.validation.JAXRSParameterNameProvider;
import org.apache.cxf.jaxrs.validation.ValidationExceptionMapper;
import org.apache.cxf.message.Message;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;
import org.apache.cxf.validation.BeanValidationInInterceptor;
import org.apache.cxf.validation.BeanValidationProvider;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSClientServerValidationTest extends AbstractJAXRSValidationTest {
    public static final String PORT = allocatePort(JAXRSClientServerValidationTest.class);

    public static class Server extends AbstractServerTestServerBase {
        @Override
        protected org.apache.cxf.endpoint.Server createServer(Bus bus) throws Exception {
            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();

            sf.setResourceClasses(BookStoreWithValidation.class);
            sf.setResourceProvider(BookStoreWithValidation.class,
                new SingletonResourceProvider(new BookStoreWithValidation()));
            sf.setProvider(new ValidationExceptionMapper() {
                @Override
                public Response toResponse(ValidationException exception) {
                    Response r = super.toResponse(exception);
                    return JAXRSUtils.toResponseBuilder(
                        r.getStatus()).type("application/xml").entity(new Book("Validation", 123L)).build();
                }
            });

            sf.setAddress("http://localhost:" + PORT + "/");
            BeanValidationInInterceptor in = new JAXRSBeanValidationInInterceptor();
            in.setProvider(new BeanValidationProvider(new JAXRSParameterNameProvider()));
            sf.setInInterceptors(Arrays.< Interceptor< ? extends Message > >asList(
                in));

            sf.setOutInterceptors(Arrays.< Interceptor< ? extends Message > >asList(
                new JAXRSBeanValidationOutInterceptor()));

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
    public void testThatNotNullValidationSkipped()  {
        final Response r = createWebClient("/bookstore/booksNoValidate").post(new Form());
        assertEquals(200, r.getStatus());
    }
    @Test
    public void testThatNotNullValidationNotSkipped()  {
        final Response r = createWebClient("/bookstore/booksValidate").post(new Form());
        assertEquals(400, r.getStatus());
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
    public void testThatNoValidationConstraintsAreViolatedWithBook()  {
        final Response r = createWebClient("/bookstore/books/direct").type("text/xml").post(
              new BookWithValidation("BeanVal", "1"));
        assertEquals(Status.CREATED.getStatusCode(), r.getStatus());
    }

    @Test
    public void testThatValidationConstraintsAreViolatedWithBook()  {
        final Response r = createWebClient("/bookstore/books/direct").type("text/xml").post(
              new BookWithValidation("BeanVal"));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }

    @Test
    public void testThatValidationConstraintsAreViolatedWithBooks()  {
        final Response r = createWebClient("/bookstore/books/directmany").type("text/xml").postCollection(
              Collections.singletonList(new BookWithValidation("BeanVal")), BookWithValidation.class);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), r.getStatus());
    }

    @Test
    public void testThatResponseValidationForOneBookFails()  {
        Response r = createWebClient("/bookstore/books").post(new Form().param("id", "1234"));
        assertEquals(Status.CREATED.getStatusCode(), r.getStatus());

        r = createWebClient("/bookstore/books/1234").get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());
    }

    @Test
    public void testThatResponseValidationForOneBookNotFails()  {
        Response r = createWebClient("/bookstore/books").post(new Form().param("id", "1234").param("name", "cxf"));
        assertEquals(Status.CREATED.getStatusCode(), r.getStatus());

        r = createWebClient("/bookstore/books/1234").get();
        assertEquals(200, r.getStatus());
    }

    @Test
    public void testThatResponseValidationForOneBookSubNotFails()  {
        Response r = createWebClient("/bookstore/books").post(new Form().param("id", "1234").param("name", "cxf"));
        assertEquals(Status.CREATED.getStatusCode(), r.getStatus());

        r = createWebClient("/bookstore/sub/books/1234").get();
        assertEquals(200, r.getStatus());
    }

    @Test
    public void testThatResponseValidationForNullBookFails()  {
        Response r = createWebClient("/bookstore/books").post(new Form().param("id", "1234").param("name", "cxf"));
        assertEquals(Status.CREATED.getStatusCode(), r.getStatus());

        r = createWebClient("/bookstore/books/1235").get();
        assertEquals(500, r.getStatus());
    }

    @Test
    public void testThatResponseValidationForOneResponseBookFails()  {
        Response r = createWebClient("/bookstore/booksResponse/1234").get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());

        r = createWebClient("/bookstore/books").post(new Form().param("id", "1234"));
        assertEquals(Status.CREATED.getStatusCode(), r.getStatus());

        r = createWebClient("/bookstore/booksResponse/1234").get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());
    }


    @Test
    public void testThatResponseValidationForBookPassesWhenNoConstraintsAreDefined()  {
        Response r = createWebClient("/bookstore/booksResponseNoValidation/1234").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());

        r = createWebClient("/bookstore/books").post(new Form().param("id", "1234"));
        assertEquals(Status.CREATED.getStatusCode(), r.getStatus());

        r = createWebClient("/bookstore/booksResponseNoValidation/1234").get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
    }

    @Test
    public void testThatResponseValidationForAllBooksFails()  {
        Response r = createWebClient("/bookstore/books").post(new Form().param("id", "1234"));
        assertEquals(Status.CREATED.getStatusCode(), r.getStatus());

        r = createWebClient("/bookstore/books").get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), r.getStatus());
    }

    @Test
    public void testThatResponseValidationIsNotTriggeredForUnacceptableMediaType()  {
        final Response r = createWebClient("/bookstore/books/direct")
            .type(MediaType.APPLICATION_JSON)
            .post(new BookWithValidation("BeanVal", "1"));
        assertEquals(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), r.getStatus());
    }

    @Override
    protected String getPort() {
        return PORT;
    }


}

