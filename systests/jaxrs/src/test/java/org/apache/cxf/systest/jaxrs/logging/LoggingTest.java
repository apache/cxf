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

package org.apache.cxf.systest.jaxrs.logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Collections;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.hamcrest.Matchers;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LoggingTest extends AbstractBusClientServerTestBase {
    @BeforeClass
    public static void startServers() {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(LoggingServer.class, true));
    }

    @Test
    public void testEchoBookElement() {
        final Response response = createWebClient("/bookstore/books/element/echo", MediaType.APPLICATION_XML)
                .post(new Book("CXF", 123L));
        assertEquals(200, response.getStatus());
        assertEquals(96, response.getLength());

        final Book book = response.readEntity(Book.class);
        assertEquals(123L, book.getId());
        assertEquals("CXF", book.getName());
    }

    @Test
    public void testStreamingOutputBlockingRead() throws IOException, JAXBException {
        final int expectedEntry = 2;
        final long expectedDelay = 1000L;
        final WebClient webClient = createWebClient("/bookstore/books/streamingoutput", MediaType.TEXT_XML,
                new LoggingFeature());
        final long requestSent = System.currentTimeMillis();
        final Response response = webClient
                .query("times", expectedEntry)
                .query("delay", expectedDelay)
                .get();
        assertThat(System.currentTimeMillis() - requestSent, Matchers.lessThan(100L));
        assertEquals(200, response.getStatus());
        assertEquals(-1, response.getLength());

        try (InputStreamReader inputStreamReader = new InputStreamReader(response.readEntity(InputStream.class));
             BufferedReader reader = new BufferedReader(inputStreamReader)) {
            int readEntry = 0;
            long previousStartTime = -1L;
            String readLine;
            while ((readLine = reader.readLine()) != null) {
                assertDelayBetweenMessages(previousStartTime, expectedDelay);
                final Book book = readBook(readLine);
                assertEquals(123L, book.getId());
                assertEquals("CXF in Action", book.getName());

                readEntry++;
                previousStartTime = System.currentTimeMillis();
            }

            assertEquals(expectedEntry, readEntry);
        }

    }

    private void assertDelayBetweenMessages(long previous, long expectedDelay) {
        if (previous < 0L) {
            // no need to assert the delay between messages, because it is the first one
            return;
        }

        final long endTime = System.currentTimeMillis();
        final long delay = endTime - previous;
        assertThat(delay, Matchers.greaterThan(expectedDelay - 150L));
        assertThat(delay, Matchers.lessThan(expectedDelay + 150L));
    }

    protected WebClient createWebClient(final String url, final String mediaType) {
        return WebClient
                .create("http://localhost:" + LoggingServer.PORT)
                .path(url)
                .accept(mediaType);
    }

    private static Book readBook(String input) throws JAXBException {
        final Class<Book> resultClass = Book.class;
        final Unmarshaller unmarshaller = JAXBContext.newInstance(resultClass)
                .createUnmarshaller();
        final StreamSource streamSource = new StreamSource(new StringReader(input));

        return unmarshaller.unmarshal(streamSource, resultClass).getValue();
    }

    protected WebClient createWebClient(final String url, final String mediaType, Feature feature) {
        JAXRSClientFactoryBean bean = createBean("http://localhost:" + LoggingServer.PORT, feature);
        return bean.createWebClient()
                .path(url)
                .accept(mediaType);
    }

    private static JAXRSClientFactoryBean createBean(String address,
                                                     Feature feature) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        bean.setFeatures(Collections.singletonList(feature));
        return bean;
    }
}
