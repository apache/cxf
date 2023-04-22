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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JAXRSClientServerResourceCreatedOutsideBookTest extends AbstractClientServerTestBase {
    public static final String PORT = BookServerResourceCreatedOutside.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(BookServerResourceCreatedOutside.class, true));
    }

    @Test
    public void testGetBook123() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/books/123";
        URL url = new URL(endpointAddress);
        URLConnection connect = url.openConnection();
        connect.addRequestProperty("Accept", "application/xml");
        try (InputStream in = connect.getInputStream()) {
            assertNotNull(in);

            InputStream expected = getClass()
                .getResourceAsStream("resources/expected_get_book123.txt");

            assertEquals(stripXmlInstructionIfNeeded(getStringFromInputStream(expected)),
                         stripXmlInstructionIfNeeded(getStringFromInputStream(in)));
        }
    }

    @Test
    public void testAddBookHTTPURL() throws Exception {
        String endpointAddress =
            "http://localhost:" + PORT + "/bookstore/books";

        URL url = new URL(endpointAddress);
        HttpURLConnection httpUrlConnection = (HttpURLConnection)url.openConnection();

        httpUrlConnection.setUseCaches(false);
        httpUrlConnection.setDefaultUseCaches(false);
        httpUrlConnection.setDoOutput(true);
        httpUrlConnection.setDoInput(true);
        httpUrlConnection.setRequestMethod("POST");
        httpUrlConnection.setRequestProperty("Accept",   "text/xml");
        httpUrlConnection.setRequestProperty("Content-type",   "application/xml");
        httpUrlConnection.setRequestProperty("Connection",   "close");
        //httpurlconnection.setRequestProperty("Content-Length",   String.valueOf(is.available()));

        try (OutputStream outputstream = httpUrlConnection.getOutputStream()) {
            IOUtils.copy(getClass().getResourceAsStream("resources/add_book.txt"), outputstream);
        }

        int responseCode = httpUrlConnection.getResponseCode();
        assertEquals(200, responseCode);

        InputStream expected = getClass().getResourceAsStream("resources/expected_add_book.txt");
        assertEquals(stripXmlInstructionIfNeeded(getStringFromInputStream(expected)),
                     stripXmlInstructionIfNeeded(getStringFromInputStream(httpUrlConnection
                                                                          .getInputStream())));
        httpUrlConnection.disconnect();
    }
    private String stripXmlInstructionIfNeeded(String str) {
        if (str != null && str.startsWith("<?xml")) {
            int index = str.indexOf("?>");
            str = str.substring(index + 2);
        }
        return str;
    }
    private String getStringFromInputStream(InputStream in) throws Exception {
        return IOUtils.toString(in);
    }

}
