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
package org.apache.cxf.jms.testsuite.testcases;

import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.jms.DeliveryMode;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jms.testsuite.services.GreeterSpecImpl;
import org.apache.cxf.jms.testsuite.services.GreeterSpecWithPortError;
import org.apache.cxf.jms_greeter.JMSGreeterPortType;
import org.apache.cxf.jms_greeter.JMSGreeterService;
import org.apache.cxf.jms_greeter.JMSGreeterService2;
import org.apache.cxf.systest.jms.AbstractVmJMSTest;
import org.apache.cxf.transport.common.gzip.GZIPFeature;
import org.apache.cxf.transport.jms.JMSConstants;
import org.apache.cxf.transport.jms.JMSMessageHeadersType;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SoapJmsSpecTest extends AbstractVmJMSTest {
    private static final String SERVICE_NS = "http://cxf.apache.org/jms_greeter";
    private static final String WSDL = "/wsdl/jms_spec_test.wsdl";

    @BeforeClass
    public static void startServers() throws Exception {
        startBusAndJMS(SoapJmsSpecTest.class);

        publish("jms:queue:test.cxf.jmstransport.queue2", new GreeterSpecImpl());
        publish("jms:queue:test.cxf.jmstransport.queue5", new GreeterSpecWithPortError());

        EndpointImpl ep = (EndpointImpl)Endpoint.create(null, new GreeterSpecImpl());
        ep.setBus(bus);
        ep.getFeatures().add(new GZIPFeature());
        ep.getFeatures().add(cff);
        ep.publish("jms:queue:test.cxf.jmstransport.queue6");
    }

    @Test
    public void testSpecJMS() throws Exception {
        QName serviceName = new QName(SERVICE_NS, "JMSGreeterService");
        QName portName = new QName(SERVICE_NS, "GreeterPort");
        URL wsdl = getWSDLURL(WSDL);
        JMSGreeterService service = new JMSGreeterService(wsdl, serviceName);

        JMSGreeterPortType greeter = markForClose(service.getPort(portName, JMSGreeterPortType.class, cff));
        for (int idx = 0; idx < 5; idx++) {

            greeter.greetMeOneWay("test String");

            String greeting = greeter.greetMe("Milestone-" + idx);
            Assert.assertEquals("Hello Milestone-" + idx, greeting);

            String reply = greeter.sayHi();
            Assert.assertEquals(new String("Bonjour"), reply);
        }
    }

    @Test
    public void testWsdlExtensionSpecJMS() throws Exception {
        QName serviceName = new QName(SERVICE_NS, "JMSGreeterService");
        QName portName = new QName(SERVICE_NS, "GreeterPort");
        URL wsdl = getWSDLURL(WSDL);
        JMSGreeterService service = new JMSGreeterService(wsdl, serviceName);
        JMSGreeterPortType greeter = markForClose(service.getPort(portName, JMSGreeterPortType.class, cff));
        Map<String, Object> requestContext = ((BindingProvider)greeter).getRequestContext();
        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        requestContext.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, requestHeader);

        String reply = greeter.sayHi();
        Assert.assertEquals("Bonjour", reply);

        requestContext = ((BindingProvider)greeter).getRequestContext();
        requestHeader = (JMSMessageHeadersType)requestContext.get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);
        Assert.assertEquals("1.0", requestHeader.getSOAPJMSBindingVersion());
        Assert.assertEquals("\"test\"", requestHeader.getSOAPJMSSOAPAction());
        Assert.assertEquals(3000, requestHeader.getTimeToLive());
        Assert.assertEquals(DeliveryMode.PERSISTENT, requestHeader.getJMSDeliveryMode());
        Assert.assertEquals(7, requestHeader.getJMSPriority());

        Map<String, Object> responseContext = ((BindingProvider)greeter).getResponseContext();
        JMSMessageHeadersType responseHeader = (JMSMessageHeadersType)responseContext
            .get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
        Assert.assertEquals("1.0", responseHeader.getSOAPJMSBindingVersion());
        Assert.assertEquals("\"test\"", responseHeader.getSOAPJMSSOAPAction());
        Assert.assertEquals(DeliveryMode.PERSISTENT, responseHeader.getJMSDeliveryMode());
        Assert.assertEquals(7, responseHeader.getJMSPriority());
    }

    @Test
    public void testWsdlExtensionSpecJMSPortError() throws Exception {
        QName serviceName = new QName(SERVICE_NS, "JMSGreeterService2");
        QName portName = new QName(SERVICE_NS, "GreeterPort2");
        URL wsdl = getWSDLURL(WSDL);
        JMSGreeterService2 service = new JMSGreeterService2(wsdl, serviceName);
        JMSGreeterPortType greeter = markForClose(service.getPort(portName, JMSGreeterPortType.class, cff));

        String reply = greeter.sayHi();
        Assert.assertEquals("Bonjour", reply);
    }

    @Test
    public void testBindingVersionError() throws Exception {
        QName serviceName = new QName(SERVICE_NS, "JMSGreeterService");
        QName portName = new QName(SERVICE_NS, "GreeterPort");
        URL wsdl = getWSDLURL(WSDL);
        JMSGreeterService service = new JMSGreeterService(wsdl, serviceName);

        JMSGreeterPortType greeter = markForClose(service.getPort(portName, JMSGreeterPortType.class, cff));
        BindingProvider bp = (BindingProvider)greeter;

        Map<String, Object> requestContext = bp.getRequestContext();
        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        requestHeader.setSOAPJMSBindingVersion("0.3");
        requestContext.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, requestHeader);

        try {
            greeter.greetMe("Milestone-");
            Assert.fail("Should have thrown a fault");
        } catch (SOAPFaultException ex) {
            Assert.assertTrue(ex.getMessage().contains("0.3"));
            Map<String, Object> responseContext = bp.getResponseContext();
            JMSMessageHeadersType responseHdr = (JMSMessageHeadersType)responseContext
                .get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
            if (responseHdr == null) {
                Assert.fail("response Header should not be null");
            }
            Assert.assertTrue(responseHdr.isSOAPJMSIsFault());
        }
    }

    @Test
    public void testGzip() throws Exception {
        URL wsdl = getWSDLURL(WSDL);
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setBus(bus);
        factory.setServiceClass(JMSGreeterPortType.class);
        factory.setWsdlURL(wsdl.toExternalForm());
        factory.getFeatures().add(cff);
        factory.getFeatures().add(new GZIPFeature());
        factory.setAddress("jms:queue:test.cxf.jmstransport.queue6");
        JMSGreeterPortType greeter = (JMSGreeterPortType)markForClose(factory.create());

        for (int idx = 0; idx < 5; idx++) {

            greeter.greetMeOneWay("test String");

            String greeting = greeter.greetMe("Milestone-" + idx);
            Assert.assertEquals("Hello Milestone-" + idx, greeting);

            String reply = greeter.sayHi();
            Assert.assertEquals("Bonjour", reply);
        }
    }

}
