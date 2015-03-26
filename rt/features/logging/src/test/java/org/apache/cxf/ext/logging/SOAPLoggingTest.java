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

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import org.apache.cxf.ext.logging.event.EventType;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.junit.Assert;
import org.junit.Test;

public class SOAPLoggingTest {

    private static final String SERVICE_URI = "http://localhost:5678/test";

    @WebService(endpointInterface = "org.apache.cxf.ext.logging.TestService")
    public final class TestServiceImplementation implements TestService {
        @Override
        public String echo(String msg) {
            return msg;
        }
    }

    @Test
    public void testSlf4j() throws MalformedURLException {
        TestService serviceImpl = new TestServiceImplementation();
        LoggingFeature loggingFeature = new LoggingFeature();
        loggingFeature.setPrettyLogging(true);
        // Setting the limit should omit parts of the body but the result should still be well formed xml
        loggingFeature.setLimit(140);
        Endpoint ep = Endpoint.publish(SERVICE_URI, serviceImpl, loggingFeature);
        TestService client = createTestClient(loggingFeature);
        client.echo("test");
        ep.stop();
    }
    
    @Test
    public void testEvents() throws MalformedURLException {
        TestService serviceImpl = new TestServiceImplementation();
        LoggingFeature loggingFeature = new LoggingFeature();
        TestEventSender sender = new TestEventSender();
        loggingFeature.setSender(sender);
        Endpoint ep = Endpoint.publish(SERVICE_URI, serviceImpl, loggingFeature);
        TestService client = createTestClient(loggingFeature);
        client.echo("test");
        ep.stop();

        List<LogEvent> events = sender.getEvents();
        Assert.assertEquals(4, events.size());
        checkRequestOut(events.get(0));
        checkRequestIn(events.get(1));
        checkResponseOut(events.get(2));
        checkResponseIn(events.get(3));
    }

    private void checkRequestOut(LogEvent requestOut) {
        Assert.assertEquals(SERVICE_URI, requestOut.getAddress());
        Assert.assertEquals("text/xml", requestOut.getContentType());
        Assert.assertEquals(EventType.REQ_OUT, requestOut.getType());
        Assert.assertEquals("UTF-8", requestOut.getEncoding());
        Assert.assertNotNull(requestOut.getExchangeId());
        Assert.assertEquals("POST", requestOut.getHttpMethod());
        Assert.assertNotNull(requestOut.getMessageId());
        Assert.assertTrue(requestOut.getPayload().contains("<arg0>test</arg0>"));
        Assert.assertEquals("TestServicePort", requestOut.getPortName().getLocalPart());
        Assert.assertEquals("TestService", requestOut.getPortTypeName().getLocalPart());
        Assert.assertEquals("TestServiceService", requestOut.getServiceName().getLocalPart());
    }
    
    private void checkRequestIn(LogEvent requestIn) {
        Assert.assertEquals(SERVICE_URI, requestIn.getAddress());
        Assert.assertEquals("text/xml; charset=UTF-8", requestIn.getContentType());
        Assert.assertEquals(EventType.REQ_IN, requestIn.getType());
        Assert.assertEquals("UTF-8", requestIn.getEncoding());
        Assert.assertNotNull(requestIn.getExchangeId());
        Assert.assertEquals("POST", requestIn.getHttpMethod());
        Assert.assertNotNull(requestIn.getMessageId());
        Assert.assertTrue(requestIn.getPayload().contains("<arg0>test</arg0>"));
        Assert.assertEquals("TestServiceImplementationPort", requestIn.getPortName().getLocalPart());
        Assert.assertEquals("TestService", requestIn.getPortTypeName().getLocalPart());
        Assert.assertEquals("TestServiceImplementationService", requestIn.getServiceName().getLocalPart());
    }
    
    private void checkResponseOut(LogEvent responseOut) {
        // Not yet available
        Assert.assertNull(responseOut.getAddress());
        Assert.assertEquals("text/xml", responseOut.getContentType());
        Assert.assertEquals(EventType.RESP_OUT, responseOut.getType());
        Assert.assertEquals("UTF-8", responseOut.getEncoding());
        Assert.assertNotNull(responseOut.getExchangeId());
        
        // Not yet available
        Assert.assertNull(responseOut.getHttpMethod());
        Assert.assertNotNull(responseOut.getMessageId());
        Assert.assertTrue(responseOut.getPayload().contains("<return>test</return>"));
        Assert.assertEquals("TestServiceImplementationPort", responseOut.getPortName().getLocalPart());
        Assert.assertEquals("TestService", responseOut.getPortTypeName().getLocalPart());
        Assert.assertEquals("TestServiceImplementationService", responseOut.getServiceName().getLocalPart());
    }
    
    private void checkResponseIn(LogEvent responseIn) {
        Assert.assertNull(responseIn.getAddress());
        Assert.assertEquals("text/xml; charset=UTF-8", responseIn.getContentType());
        Assert.assertEquals(EventType.RESP_IN, responseIn.getType());
        Assert.assertEquals("UTF-8", responseIn.getEncoding());
        Assert.assertNotNull(responseIn.getExchangeId());
        
        // Not yet available
        Assert.assertNull(responseIn.getHttpMethod());
        Assert.assertNotNull(responseIn.getMessageId());
        Assert.assertTrue(responseIn.getPayload().contains("<return>test</return>"));
        Assert.assertEquals("TestServicePort", responseIn.getPortName().getLocalPart());
        Assert.assertEquals("TestService", responseIn.getPortTypeName().getLocalPart());
        Assert.assertEquals("TestServiceService", responseIn.getServiceName().getLocalPart());
    }

    private TestService createTestClient(Feature feature) throws MalformedURLException {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setAddress(SERVICE_URI);
        factory.setFeatures(Collections.singletonList(feature));
        return factory.create(TestService.class);
    }

}
