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
package org.apache.cxf.jaxws.logging;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import jakarta.jws.WebService;
import jakarta.xml.ws.Endpoint;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.SoapBinding;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.ext.logging.event.DefaultLogEventMapper;
import org.apache.cxf.ext.logging.event.EventType;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxws.AbstractJaxWsTest;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;

import org.junit.Assert;
import org.junit.Test;

public class SOAPLoggingTest extends AbstractJaxWsTest {

    private static final String SERVICE_URI = "http://localhost:5678/test";

    @WebService(endpointInterface = "org.apache.cxf.jaxws.logging.TestService")
    public final class TestServiceImplementation implements TestService {
        @Override
        public String echo(String msg) {
            return msg;
        }
    }


    @Test
    public void testSoap() {
        DefaultLogEventMapper mapper = new DefaultLogEventMapper();
        Message message = new MessageImpl();
        ExchangeImpl exchange = new ExchangeImpl();
        ServiceInfo service = new ServiceInfo();
        BindingInfo info = new BindingInfo(service, "bindingId");
        SoapBinding value = new SoapBinding(info);
        exchange.put(Binding.class, value);
        OperationInfo opInfo = new OperationInfo();
        opInfo.setName(new QName("http://my", "Operation"));
        BindingOperationInfo boi = new BindingOperationInfo(info, opInfo);
        exchange.put(BindingOperationInfo.class, boi);
        message.setExchange(exchange);
        LogEvent event = mapper.map(message, Collections.emptySet());
        Assert.assertEquals("{http://my}Operation", event.getOperationName());
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
        Assert.assertEquals(StandardCharsets.UTF_8.name(), requestOut.getEncoding());
        Assert.assertNotNull(requestOut.getExchangeId());
        Assert.assertNotNull(requestOut.getMessageId());
        Assert.assertTrue(requestOut.getPayload().contains("<arg0>test</arg0>"));
        Assert.assertEquals("TestServicePort", requestOut.getPortName().getLocalPart());
        Assert.assertEquals("TestService", requestOut.getPortTypeName().getLocalPart());
        Assert.assertEquals("TestServiceService", requestOut.getServiceName().getLocalPart());
    }

    private void checkRequestIn(LogEvent requestIn) {
        Assert.assertEquals(SERVICE_URI, requestIn.getAddress());
        Assert.assertTrue(requestIn.getContentType(), requestIn.getContentType().contains("text/xml"));
        Assert.assertEquals(EventType.REQ_IN, requestIn.getType());
        Assert.assertEquals(StandardCharsets.UTF_8.name(), requestIn.getEncoding());
        Assert.assertNotNull(requestIn.getExchangeId());
        Assert.assertNotNull(requestIn.getMessageId());
        Assert.assertTrue(requestIn.getPayload().contains("<arg0>test</arg0>"));
        Assert.assertEquals("TestServiceImplementationPort", requestIn.getPortName().getLocalPart());
        Assert.assertEquals("TestService", requestIn.getPortTypeName().getLocalPart());
        Assert.assertEquals("TestServiceImplementationService", requestIn.getServiceName().getLocalPart());
    }

    private void checkResponseOut(LogEvent responseOut) {
        // Not yet available
        Assert.assertEquals(SERVICE_URI, responseOut.getAddress());
        Assert.assertEquals("text/xml", responseOut.getContentType());
        Assert.assertEquals(EventType.RESP_OUT, responseOut.getType());
        Assert.assertEquals(StandardCharsets.UTF_8.name(), responseOut.getEncoding());
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
        Assert.assertEquals(SERVICE_URI, responseIn.getAddress());
        Assert.assertTrue(responseIn.getContentType(), responseIn.getContentType().contains("text/xml"));
        Assert.assertEquals(EventType.RESP_IN, responseIn.getType());
        Assert.assertEquals(StandardCharsets.UTF_8.name(), responseIn.getEncoding());
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
