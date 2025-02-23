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
package org.apache.cxf.jaxrs.client.logging;


import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.AbstractLoggingInterceptor;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.ext.logging.event.EventType;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.io.CachedOutputStreamCleaner;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.local.LocalTransportFactory;

import org.junit.Assert;
import org.junit.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;

public class RESTLoggingTest {

    private static final String SERVICE_URI = "local://testrest";
    private static final String SERVICE_URI_BINARY = "local://testrestbin";

    @Test
    public void testSlf4j() throws IOException {
        LoggingFeature loggingFeature = new LoggingFeature();
        Server server = createService(SERVICE_URI, new TestServiceRest(), loggingFeature);
        server.start();
        WebClient client = createClient(SERVICE_URI, loggingFeature);
        String result = client.get(String.class);
        server.destroy();
        Assert.assertEquals("test1", result);
    }

    @Test
    public void testCacheCleanUp() throws Exception {
        LoggingFeature loggingFeature = new LoggingFeature();
        loggingFeature.setInMemThreshold(1); // To activate usage of the CachedOutputStream

        Server server = createService(SERVICE_URI, new TestServiceRest(), loggingFeature);
        server.start();

        try {
            final JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
            bean.setAddress(SERVICE_URI);
            bean.setTransportId(LocalTransportFactory.TRANSPORT_ID);
    
            final LongAdder registers = new LongAdder();
            final WebClient client = bean.createWebClient();
            final Bus bus = bean.getBus();

            // See please https://issues.apache.org/jira/browse/CXF-9110
            final CachedOutputStreamCleaner cleaner = bus.getExtension(CachedOutputStreamCleaner.class);
            bus.setExtension(new CachedOutputStreamCleaner() {
                @Override
                public void clean() {
                    cleaner.clean();
                }

                @Override
                public void unregister(Closeable closeable) {
                    cleaner.unregister(closeable);
                }

                @Override
                public void register(Closeable closeable) {
                    cleaner.register(closeable);
                    registers.increment();
                }

                @Override
                public int size() {
                    return cleaner.size();
                }
            }, CachedOutputStreamCleaner.class);

            String response = null;
            for (int i = 0; i < 1_000; i++) { // ~2...5 seconds of the execution
                response = client.post("DATA", String.class);
            }
            assertEquals("DATA", response);

            assertThat(registers.longValue(), equalTo(3000L));
            assertThat(cleaner.size(), equalTo(0));
        } finally {
            server.destroy();
        }
    }

    @Test
    public void testBinary() throws IOException, InterruptedException {
        LoggingFeature loggingFeature = new LoggingFeature();
        TestEventSender sender = new TestEventSender();
        loggingFeature.setSender(sender);
        loggingFeature.setLogBinary(true);

        Server server = createService(SERVICE_URI_BINARY, new TestServiceRestBinary(), loggingFeature);
        server.start();
        WebClient client = createClient(SERVICE_URI_BINARY, loggingFeature);
        client.get(InputStream.class).close();
        client.close();

        List<LogEvent> events = sender.getEvents();
        await().until(() -> events.size(), is(4));
        server.stop();
        server.destroy();

        Assert.assertEquals(4, events.size());
        
        assertContentLogged(events.get(0));
        assertContentLogged(events.get(1));
        assertContentLogged(events.get(2));
        assertContentLogged(events.get(3));
    }
    
    @Test
    public void testNonBinary() throws IOException, InterruptedException {
        LoggingFeature loggingFeature = new LoggingFeature();
        TestEventSender sender = new TestEventSender();
        loggingFeature.setSender(sender);
        Server server = createService(SERVICE_URI_BINARY, new TestServiceRestBinary(), loggingFeature);
        server.start();
        WebClient client = createClient(SERVICE_URI_BINARY, loggingFeature);
        client.get(InputStream.class).close();
        client.close();
        List<LogEvent> events = sender.getEvents();
        await().until(() -> events.size(), is(4));
        server.stop();
        server.destroy();

        Assert.assertEquals(4, events.size());
        
        assertContentLogged(events.get(0));
        assertContentLogged(events.get(1));
        assertContentNotLogged(events.get(2));
        assertContentNotLogged(events.get(3));
    }

    @Test
    public void testEvents() throws MalformedURLException {
        LoggingFeature loggingFeature = new LoggingFeature();
        loggingFeature.setLogBinary(true);
        TestEventSender sender = new TestEventSender();
        loggingFeature.setSender(sender);
        Server server = createService(SERVICE_URI, new TestServiceRest(), loggingFeature);
        server.start();
        WebClient client = createClient(SERVICE_URI, loggingFeature);
        String result = client.get(String.class);
        Assert.assertEquals("test1", result);

        List<LogEvent> events = sender.getEvents();
        await().until(() -> events.size(), is(4));
        server.stop();
        server.destroy();

        Assert.assertEquals(4, events.size());
        checkRequestOut(events.get(0));
        checkRequestIn(events.get(1));
        checkResponseOut(events.get(2));
        checkResponseIn(events.get(3));
    }
    
    private void assertContentLogged(LogEvent event) {
        Assert.assertNotEquals(AbstractLoggingInterceptor.CONTENT_SUPPRESSED, event.getPayload());
    }

    private void assertContentNotLogged(LogEvent event) {
        Assert.assertEquals(AbstractLoggingInterceptor.CONTENT_SUPPRESSED, event.getPayload());
    }

    private WebClient createClient(String serviceURI, LoggingFeature loggingFeature) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(serviceURI);
        bean.setFeatures(Collections.singletonList(loggingFeature));
        bean.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        return bean.createWebClient().path("test1");
    }

    private Server createService(String serviceURI, Object serviceImpl, LoggingFeature loggingFeature) {
        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setAddress(serviceURI);
        factory.setFeatures(Collections.singletonList(loggingFeature));
        factory.setServiceBean(serviceImpl);
        factory.setTransportId(LocalTransportFactory.TRANSPORT_ID);
        return factory.create();
    }


    private void checkRequestOut(LogEvent requestOut) {
        Assert.assertEquals(SERVICE_URI + "/test1", requestOut.getAddress());
        Assert.assertNull(requestOut.getContentType());
        Assert.assertEquals(EventType.REQ_OUT, requestOut.getType());
        Assert.assertNull(requestOut.getEncoding());
        Assert.assertNotNull(requestOut.getExchangeId());
        Assert.assertEquals("GET", requestOut.getHttpMethod());
        Assert.assertNotNull(requestOut.getMessageId());
        Assert.assertEquals("", requestOut.getPayload());
    }

    private void checkRequestIn(LogEvent requestIn) {
        Assert.assertEquals(SERVICE_URI + "/test1", requestIn.getAddress());
        Assert.assertNull(requestIn.getContentType());
        Assert.assertEquals(EventType.REQ_IN, requestIn.getType());
        Assert.assertNull(requestIn.getEncoding());
        Assert.assertNotNull(requestIn.getExchangeId());
        Assert.assertEquals("GET", requestIn.getHttpMethod());
        Assert.assertNotNull(requestIn.getMessageId());
    }

    private void checkResponseOut(LogEvent responseOut) {
        // Not yet available
        Assert.assertEquals(SERVICE_URI + "/test1", responseOut.getAddress());
        Assert.assertEquals("application/octet-stream", responseOut.getContentType());
        Assert.assertEquals(EventType.RESP_OUT, responseOut.getType());
        Assert.assertNull(responseOut.getEncoding());
        Assert.assertNotNull(responseOut.getExchangeId());

        // Not yet available
        Assert.assertNull(responseOut.getHttpMethod());
        Assert.assertNotNull(responseOut.getMessageId());
        Assert.assertEquals("test1", responseOut.getPayload());
    }

    private void checkResponseIn(LogEvent responseIn) {
        // Not yet available
        Assert.assertEquals(SERVICE_URI + "/test1", responseIn.getAddress());
        Assert.assertEquals("application/octet-stream", responseIn.getContentType());
        Assert.assertEquals(EventType.RESP_IN, responseIn.getType());
        Assert.assertNotNull(responseIn.getExchangeId());

        // Not yet available
        Assert.assertNull(responseIn.getHttpMethod());
        Assert.assertNotNull(responseIn.getMessageId());
        Assert.assertEquals("test1", responseIn.getPayload());
    }


}
