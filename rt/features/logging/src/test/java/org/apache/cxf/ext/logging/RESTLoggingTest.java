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
package org.apache.cxf.ext.logging;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.ext.logging.event.EventType;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Assert;
import org.junit.Test;

public class RESTLoggingTest {

    private static final String SERVICE_URI = "http://localhost:5679/testrest";

    @Test
    public void testSlf4j() throws IOException {
        LoggingFeature loggingFeature = new LoggingFeature();
        Server server = createService(loggingFeature);
        server.start();
        WebClient client = createClient(loggingFeature);
        String result = client.get(String.class);
        Assert.assertEquals("test1", result);
        server.destroy();
    }

    private WebClient createClient(LoggingFeature loggingFeature) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(SERVICE_URI + "/test1");
        bean.setFeatures(Collections.singletonList(loggingFeature));
        return bean.createWebClient();
    }

    private Server createService(LoggingFeature loggingFeature) {
        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setAddress(SERVICE_URI);
        factory.setFeatures(Collections.singletonList(loggingFeature));
        factory.setServiceBean(new TestServiceRest());
        return factory.create();
    }
    
    @Test
    public void testEvents() throws MalformedURLException {
        LoggingFeature loggingFeature = new LoggingFeature();
        TestEventSender sender = new TestEventSender();
        loggingFeature.setSender(sender);
        Server server = createService(loggingFeature);
        server.start();
        WebClient client = createClient(loggingFeature);
        String result = client.get(String.class);
        Assert.assertEquals("test1", result);
        server.destroy();
        List<LogEvent> events = sender.getEvents();
        Assert.assertEquals(4, events.size());
        checkRequestOut(events.get(0));
        checkRequestIn(events.get(1));
        checkResponseOut(events.get(2));
        checkResponseIn(events.get(3));
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
        Assert.assertEquals("", requestIn.getPayload());
    }
    
    private void checkResponseOut(LogEvent responseOut) {
        // Not yet available
        Assert.assertNull(responseOut.getAddress());
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
        Assert.assertNull(responseIn.getAddress());
        Assert.assertEquals("application/octet-stream", responseIn.getContentType());
        Assert.assertEquals(EventType.RESP_IN, responseIn.getType());
        Assert.assertEquals("ISO-8859-1", responseIn.getEncoding());
        Assert.assertNotNull(responseIn.getExchangeId());
        
        // Not yet available
        Assert.assertNull(responseIn.getHttpMethod());
        Assert.assertNotNull(responseIn.getMessageId());
        Assert.assertEquals("test1", responseIn.getPayload());
    }
    

}
