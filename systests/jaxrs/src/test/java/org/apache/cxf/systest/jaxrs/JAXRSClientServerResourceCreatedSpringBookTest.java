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

package org.apache.cxf.systest.jaxrs;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JAXRSClientServerResourceCreatedSpringBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerResourceCreatedSpring.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(BookServerResourceCreatedSpring.class, true));
        createStaticBus();
    }

    @Test
    public void testGetBookSimple() throws Exception {

        String address =
            "http://localhost:" + PORT + "/webapp/rest/simplebooks/444";
        assertEquals(444L, WebClient.create(address).get(Book.class).getId());
    }

    @Test
    public void testGetBookSimpleMatrixEnd() throws Exception {

        String address =
            "http://localhost:" + PORT + "/webapp/rest/simplebooks/444;bookId=ssn";
        assertEquals(444L, WebClient.create(address).get(Book.class).getId());
    }

    @Test
    public void testGetBookSimpleMatrixMiddle() throws Exception {

        String address =
            "http://localhost:" + PORT + "/webapp/rest/simplebooks/444;bookId=ssn/book";
        assertEquals(444L, WebClient.create(address).get(Book.class).getId());
    }

    @Test
    public void testGetBookSimpleProxy() throws Exception {

        String address = "http://localhost:" + PORT + "/webapp/rest";
        BookStoreSimple bookStore = JAXRSClientFactory.create(address, BookStoreSimple.class);
        Book book = bookStore.getBook(444L);
        assertEquals(444L, book.getId());
    }

    @Test
    public void testGetBookSimpleBeanParamProxy() throws Exception {

        String address = "http://localhost:" + PORT + "/webapp/rest";
        BookStoreSimple bookStore = JAXRSClientFactory.create(address, BookStoreSimple.class);
        Book book = bookStore.getBookBeanParam(new BookStoreSimple.BookBean(444));
        assertEquals(444L, book.getId());
    }

    @Test
    public void testGetBook123() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/rest/bookstore/books/123";
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", "application/xml");
        InputStream in = connect.getInputStream();
        assertNotNull(in);

        InputStream expected = getClass()
            .getResourceAsStream("resources/expected_get_book123.txt");

        assertEquals(stripXmlInstructionIfNeeded(getStringFromInputStream(expected)),
                     stripXmlInstructionIfNeeded(getStringFromInputStream(in)));
    }
    private String stripXmlInstructionIfNeeded(String str) {
        if (str != null && str.startsWith("<?xml")) {
            int index = str.indexOf("?>");
            str = str.substring(index + 2);
        }
        return str;
    }

    @Test
    public void testPetStore() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/rest/petstore/pets/24";
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", "text/xml");
        InputStream in = connect.getInputStream();
        assertNotNull(in);
        assertEquals(PetStore.CLOSED, getStringFromInputStream(in));
    }

    private String getStringFromInputStream(InputStream in) throws Exception {
        return IOUtils.toString(in);
    }

}
