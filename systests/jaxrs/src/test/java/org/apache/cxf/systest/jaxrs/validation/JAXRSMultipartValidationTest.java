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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import jakarta.activation.DataHandler;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.ext.multipart.AttachmentBuilder;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.InputStreamDataSource;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.validation.ValidationExceptionMapper;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;
import org.apache.cxf.validation.BeanValidationFeature;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class JAXRSMultipartValidationTest extends AbstractJAXRSValidationTest {
    public static final String PORT = allocatePort(JAXRSMultipartValidationTest.class);

    public static class Server extends AbstractServerTestServerBase {
        @Override
        protected org.apache.cxf.endpoint.Server createServer(Bus bus) throws Exception {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(MultipartBookStoreWithValidation.class);
            sf.setFeatures(List.of(new BeanValidationFeature()));
            sf.setProvider(new ValidationExceptionMapper());
            sf.setResourceProvider(new SingletonResourceProvider(new MultipartBookStoreWithValidation()));
            sf.setAddress("http://localhost:" + PORT + "/");
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
    public void testAddBookWithDetailsAsMultipartBadRequest() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/details";

        final Client client = ClientBuilder.newClient();
        try (InputStream is = getClass()
                .getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/attachmentData")) {
            final MultipartBody builder = new MultipartBody(Arrays.asList(
                new AttachmentBuilder()
                    .mediaType("application/xml")
                    .id("book")
                    .object(new BookWithValidation())
                    .build(),
                new AttachmentBuilder()
                    .id("upfile1Detail")
                    .object(is)
                    .contentDisposition(new ContentDisposition("form-data; name=\"field1\";"))
                    .build(),
                new AttachmentBuilder()
                    .id("upfile2Detail")
                    .dataHandler(new DataHandler(
                        new InputStreamDataSource(new ByteArrayInputStream(new byte[0]), "text/xml")))
                    .contentDisposition(new ContentDisposition("form-data; name=\"field2\";"))
                    .build(),
                new AttachmentBuilder()
                    .id("upfile3Detail")
                    .dataHandler(new DataHandler(new InputStreamDataSource(
                        new ByteArrayInputStream(new byte[0]), "text/xml")))
                    .contentDisposition(new ContentDisposition("form-data; name=\"field3\";"))
                    .build()));
        
            final Response response = client
                .target(address)
                .request("text/xml")
                .post(Entity.entity(builder, "multipart/form-data"));

            // Book's name is 'null', validation should fail with 400
            assertThat("Unexpected status code for response:" + response, 
                response.getStatus(), equalTo(400));
        }
    }

    @Test
    public void testAddBookWithDetailsAsMultipart() throws Exception {
        String address = "http://localhost:" + PORT + "/bookstore/books/details";

        final Client client = ClientBuilder.newClient();
        try (InputStream is = getClass()
                .getResourceAsStream("/org/apache/cxf/systest/jaxrs/resources/attachmentData")) {
            final MultipartBody builder = new MultipartBody(Arrays.asList(
                new AttachmentBuilder()
                    .mediaType("application/xml")
                    .id("book")
                    .object(new BookWithValidation("Book", "book1"))
                    .build(),
                new AttachmentBuilder()
                    .id("upfile1Detail")
                    .object(is)
                    .contentDisposition(new ContentDisposition("form-data; name=\"field1\";"))
                    .build(),
                new AttachmentBuilder()
                    .id("upfile2Detail")
                    .dataHandler(new DataHandler(
                        new InputStreamDataSource(new ByteArrayInputStream(new byte[0]), "text/xml")))
                    .contentDisposition(new ContentDisposition("form-data; name=\"field2\";"))
                    .build(),
                new AttachmentBuilder()
                    .id("upfile3Detail")
                    .dataHandler(new DataHandler(new InputStreamDataSource(
                        new ByteArrayInputStream(new byte[0]), "text/xml")))
                    .contentDisposition(new ContentDisposition("form-data; name=\"field3\";"))
                    .build()));
        
            final Response response = client
                .target(address)
                .request("text/xml")
                .post(Entity.entity(builder, "multipart/form-data"));

            final BookWithValidation book = response.readEntity(BookWithValidation.class);
            assertThat("Unexpected status code for response:" + response, 
                response.getStatus(), equalTo(200));
            assertThat(book.getName(), equalTo("upfile1Detail,upfile2Detail,upfile3Detail"));
        }
    }

    @Override
    protected String getPort() {
        return PORT;
    }
}

