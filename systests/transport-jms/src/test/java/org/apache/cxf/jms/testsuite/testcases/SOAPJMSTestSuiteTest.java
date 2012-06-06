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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Map;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;

import org.apache.cxf.BusFactory;
import org.apache.cxf.jms.testsuite.services.Server;
import org.apache.cxf.jms.testsuite.util.JMSTestUtil;
import org.apache.cxf.jms_simple.JMSSimplePortType;
import org.apache.cxf.jms_simple.JMSSimpleService0001;
import org.apache.cxf.jms_simple.JMSSimpleService0003;
import org.apache.cxf.jms_simple.JMSSimpleService0005;
import org.apache.cxf.jms_simple.JMSSimpleService0006;
import org.apache.cxf.jms_simple.JMSSimpleService0008;
import org.apache.cxf.jms_simple.JMSSimpleService0009;
import org.apache.cxf.jms_simple.JMSSimpleService0010;
import org.apache.cxf.jms_simple.JMSSimpleService0011;
import org.apache.cxf.jms_simple.JMSSimpleService0012;
import org.apache.cxf.jms_simple.JMSSimpleService0013;
import org.apache.cxf.jms_simple.JMSSimpleService0014;
import org.apache.cxf.jms_simple.JMSSimpleService0101;
import org.apache.cxf.jms_simple.JMSSimpleService1001;
import org.apache.cxf.jms_simple.JMSSimpleService1009;
import org.apache.cxf.jms_simple.JMSSimpleService1101;
import org.apache.cxf.jms_simple.JMSSimpleService1105;
import org.apache.cxf.jms_simple.JMSSimpleService1109;
import org.apache.cxf.testsuite.testcase.MessagePropertiesType;
import org.apache.cxf.testsuite.testcase.TestCaseType;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.EmbeddedJMSBrokerLauncher;
import org.apache.cxf.transport.jms.JMSConfiguration;
import org.apache.cxf.transport.jms.JMSConstants;
import org.apache.cxf.transport.jms.JMSFactory;
import org.apache.cxf.transport.jms.JMSMessageHeadersType;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;


/**
 * 
 */
public class SOAPJMSTestSuiteTest extends AbstractBusClientServerTestBase {
    static EmbeddedJMSBrokerLauncher broker;
    
    java.io.Closeable closeable;
    
    @BeforeClass
    public static void startServers() throws Exception {
        broker = new EmbeddedJMSBrokerLauncher("vm://SOAPJMSTestSuiteTest");
        launchServer(broker);
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        createStaticBus();
    }
    @After
    public void close() throws Exception {
        if (closeable != null) {
            closeable.close();
            closeable = null;
        }
    }
    
    private void oneWayTest(TestCaseType testcase, JMSSimplePortType port) throws Exception {
        closeable = (java.io.Closeable)port;
        InvocationHandler handler = Proxy.getInvocationHandler(port);
        BindingProvider bp = (BindingProvider)handler;

        Map<String, Object> requestContext = bp.getRequestContext();
        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();

        requestContext.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, requestHeader);
        Exception e = null;
        try {
            port.ping("test");
        } catch (Exception e1) {
            e = e1;
        }
        checkJMSProperties(testcase, requestHeader);
        if (e != null) {
            throw e;
        }
    }

    private void twoWayTest(TestCaseType testcase, final JMSSimplePortType port)
        throws Exception {
        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        twoWayTestWithRequestHeader(testcase, port, requestHeader);
    }

    private void twoWayTestWithRequestHeader(TestCaseType testcase, final JMSSimplePortType port,
                                             JMSMessageHeadersType requestHeader)
        throws Exception {
        closeable = (java.io.Closeable)port;
        InvocationHandler handler = Proxy.getInvocationHandler(port);
        BindingProvider bp = (BindingProvider)handler;

        Map<String, Object> requestContext = bp.getRequestContext();
        if (requestHeader == null) {
            requestHeader = new JMSMessageHeadersType();
        }
        requestContext.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, requestHeader);
        Exception e = null;
        try {
            String response = port.echo("test");
            assertEquals(response, "test");
        } catch (WebServiceException ew) {
            throw ew;
        } catch (Exception e1) {
            e = e1;
        }
        Map<String, Object> responseContext = bp.getResponseContext();
        JMSMessageHeadersType responseHeader = (JMSMessageHeadersType)responseContext
            .get(JMSConstants.JMS_CLIENT_RESPONSE_HEADERS);
        checkJMSProperties(testcase, requestHeader, responseHeader);
        if (e != null) {
            throw e;
        }
    }

    @Test
    public void test0001() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0001");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0001", "SimplePort",
                                                     JMSSimpleService0001.class,
                                                     JMSSimplePortType.class);
        oneWayTest(testcase, simplePort);
    }

    @Test
    public void test0101() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0101");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0101", "SimplePort",
                                                     JMSSimpleService0101.class,
                                                     JMSSimplePortType.class);
        oneWayTest(testcase, simplePort);
    }

    @Test
    public void test0002() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0002");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0001", "SimplePort",
                                                     JMSSimpleService0001.class,
                                                     JMSSimplePortType.class);
        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        requestHeader.setJMSCorrelationID("Correlator0002");

        twoWayTestWithRequestHeader(testcase, simplePort, requestHeader);
    }

    @Test
    public void test0102() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0102");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0101", "SimplePort",
                                                     JMSSimpleService0101.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }

    @Test
    public void test0003() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0003");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0003", "SimplePort",
                                                     JMSSimpleService0003.class,
                                                     JMSSimplePortType.class);
        oneWayTest(testcase, simplePort);
    }

    @Test
    public void test0004() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0004");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0003", "SimplePort",
                                                     JMSSimpleService0003.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }

    @Test
    public void test0005() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0005");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0005", "SimplePort",
                                                     JMSSimpleService0005.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }

    @Test
    public void test0006() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0006");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0006", "SimplePort",
                                                     JMSSimpleService0006.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }

    @Test
    public void test0008() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0008");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0008", "SimplePort",
                                                     JMSSimpleService0008.class,
                                                     JMSSimplePortType.class);

        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        requestHeader.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
        requestHeader.setTimeToLive(14400000);
        requestHeader.setJMSPriority(8);
        requestHeader.setJMSReplyTo("dynamicQueues/replyqueue0008");

        twoWayTestWithRequestHeader(testcase, simplePort, requestHeader);
    }

    @Test
    public void test0009() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0009");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0009", "SimplePort",
                                                     JMSSimpleService0009.class,
                                                     JMSSimplePortType.class);

        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        requestHeader.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
        requestHeader.setTimeToLive(10800000);
        requestHeader.setJMSPriority(3);
        requestHeader.setJMSReplyTo("dynamicQueues/replyqueue00093");

        twoWayTestWithRequestHeader(testcase, simplePort, requestHeader);
    }

    @Test
    public void test0010() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0010");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0010", "SimplePort",
                                                     JMSSimpleService0010.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }

    @Test
    public void test0011() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test0011");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0011", "SimplePort",
                                                     JMSSimpleService0011.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }

    @Test
    public void test0012() throws Exception {
        // same to test0002
        TestCaseType testcase = JMSTestUtil.getTestCase("test0012");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0012", "SimplePort",
                                                     JMSSimpleService0012.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }

    @Test
    public void test0013() throws Exception {
        // same to test0002
        TestCaseType testcase = JMSTestUtil.getTestCase("test0013");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0013", "SimplePort",
                                                     JMSSimpleService0013.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }

    @Test
    public void test0014() throws Exception {
        // same to test0002
        TestCaseType testcase = JMSTestUtil.getTestCase("test0014");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService0014", "SimplePort",
                                                     JMSSimpleService0014.class,
                                                     JMSSimplePortType.class);
        twoWayTest(testcase, simplePort);
    }

    @Test
    public void test1001() throws Exception {
        // same to test0002
        TestCaseType testcase = JMSTestUtil.getTestCase("test1001");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService1001", "SimplePort",
                                                     JMSSimpleService1001.class,
                                                     JMSSimplePortType.class);

        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        requestHeader.setSOAPJMSBindingVersion("0.3");
        try {
            twoWayTestWithRequestHeader(testcase, simplePort, requestHeader);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Unrecognized BindingVersion"));
        }
    }
    
    @Test
    public void test1002() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1002");
        
        twoWayTestWithCreateMessage(testcase);
    }
    
    @Test
    public void test1003() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1003");
        
        twoWayTestWithCreateMessage(testcase);
    }
    
    @Test
    public void test1004() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1004");
        
        twoWayTestWithCreateMessage(testcase);
    }
    
    @Test
    public void test1006() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1006");
        
        twoWayTestWithCreateMessage(testcase);
    }
    
    @Test
    public void test1007() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1007");
        
        twoWayTestWithCreateMessage(testcase);
    }
    
    @Test
    public void test1008() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1008");
        
        twoWayTestWithCreateMessage(testcase);
    }

    @Test
    public void test1009() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1009");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService1009", "SimplePort",
                                                     JMSSimpleService1009.class,
                                                     JMSSimplePortType.class);

        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        try {
            twoWayTestWithRequestHeader(testcase, simplePort, requestHeader);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Unknow JMS Variant"));
        }
    }

    @Test
    public void test1101() throws Exception {
        // same to test0002
        TestCaseType testcase = JMSTestUtil.getTestCase("test1101");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService1101", "SimplePort",
                                                     JMSSimpleService1101.class,
                                                     JMSSimplePortType.class);

        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        requestHeader.setSOAPJMSBindingVersion("0.3");
        try {
            twoWayTestWithRequestHeader(testcase, simplePort, requestHeader);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Unrecognized BindingVersion"));
        }
    }
    
    @Test
    public void test1102() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1102");
        
        twoWayTestWithCreateMessage(testcase);
    }
    
    @Test
    public void test1103() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1103");
        
        twoWayTestWithCreateMessage(testcase);
    }
    
    @Test
    public void test1104() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1104");
        
        twoWayTestWithCreateMessage(testcase);
    }
    
    @Test
    public void test1105() throws Exception {
 
        TestCaseType testcase = JMSTestUtil.getTestCase("test1105");
         
        final JMSSimplePortType simplePort = getPort("JMSSimpleService1105", "SimplePort",
                                                     JMSSimpleService1105.class,
                                                     JMSSimplePortType.class);

        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        requestHeader.setSOAPJMSSOAPAction("mismatch");
        try {
            twoWayTestWithRequestHeader(testcase, simplePort, requestHeader);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Mismatched SoapAction"));
        }
    }
    
    @Test
    public void test1106() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1106");
        
        twoWayTestWithCreateMessage(testcase);
    }
    
    @Test
    public void test1107() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1107");
        
        twoWayTestWithCreateMessage(testcase);
    }
    
    @Test
    public void test1108() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1108");
        
        twoWayTestWithCreateMessage(testcase);
    }
    
    @Test
    public void test1109() throws Exception {
        TestCaseType testcase = JMSTestUtil.getTestCase("test1109");
        final JMSSimplePortType simplePort = getPort("JMSSimpleService1109", "SimplePort",
                                                     JMSSimpleService1109.class,
                                                     JMSSimplePortType.class);

        JMSMessageHeadersType requestHeader = new JMSMessageHeadersType();
        try {
            twoWayTestWithRequestHeader(testcase, simplePort, requestHeader);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Unknow JMS Variant"));
        }
    }

    
    
    
    
    public <T1, T2> T2 getPort(String serviceName, String portName, Class<T1> serviceClass,
                               Class<T2> portTypeClass) throws Exception {
        String namespace = "http://cxf.apache.org/jms_simple";
        QName qServiceName = new QName(namespace, serviceName);
        QName qPortName = new QName(namespace, portName);
        URL wsdl = getClass().getResource("/wsdl/jms_spec_testsuite.wsdl");
        String wsdlString = wsdl.toString();
        broker.updateWsdl(getBus(), wsdlString);
        BusFactory.setThreadDefaultBus(getBus());
        BusFactory.setDefaultBus(getBus());
        Class<? extends Service> svcls = serviceClass.asSubclass(Service.class);

        Constructor<? extends Service> serviceConstructor = svcls.getConstructor(URL.class,
                                                                                 QName.class);
        Service service = serviceConstructor.newInstance(new Object[] {
            wsdl, qServiceName
        });
        broker.updateWsdl(getBus(), wsdlString);
        return service.getPort(qPortName, portTypeClass);
    }

    public void checkJMSProperties(Message message, MessagePropertiesType messageProperties)
        throws JMSException {
        // todo messagetype
        // todo messageid
        if (messageProperties.isSetDeliveryMode()) {
            assertEquals(message.getJMSDeliveryMode(), messageProperties.getDeliveryMode()
                .intValue());
        }
        if (messageProperties.isSetPriority()) {
            assertEquals(message.getJMSPriority(), messageProperties.getPriority().intValue());
        }
        if (messageProperties.isSetExpiration()) {
            assertEquals(message.getJMSExpiration(), messageProperties.getExpiration().intValue());
        }
        if (messageProperties.isSetReplyTo() && !messageProperties.getReplyTo().trim().equals("")) {
            assertEquals(message.getJMSReplyTo().toString(), messageProperties.getReplyTo());
        }
        if (messageProperties.isSetCorrelationID()
            && !messageProperties.getCorrelationID().trim().equals("")) {
            assertEquals(message.getJMSCorrelationID(), messageProperties.getCorrelationID());
        }
        if (messageProperties.isSetDestination()
            && !messageProperties.getDestination().trim().equals("")) {
            assertEquals(message.getJMSDestination().toString(), messageProperties.getDestination());
        }
        if (messageProperties.isSetRedelivered()) {
            assertEquals(message.getJMSRedelivered(), messageProperties.isRedelivered());
        }
        if (messageProperties.isSetBindingVersion()
            && !messageProperties.getBindingVersion().trim().equals("")) {
            assertEquals(message.getStringProperty(JMSSpecConstants.BINDINGVERSION_FIELD),
                         messageProperties.getBindingVersion());
        }
        if (messageProperties.isSetTargetService()
            && !messageProperties.getTargetService().trim().equals("")) {
            assertEquals(message.getStringProperty(JMSSpecConstants.TARGETSERVICE_FIELD),
                         messageProperties.getTargetService());
        }
        if (messageProperties.isSetContentType()
            && !messageProperties.getContentType().trim().equals("")) {
            assertEquals(message.getStringProperty(JMSSpecConstants.CONTENTTYPE_FIELD),
                         messageProperties.getContentType());
        }
        if (messageProperties.isSetSoapAction()
            && !messageProperties.getSoapAction().trim().equals("")) {
            assertEquals(message.getStringProperty(JMSSpecConstants.SOAPACTION_FIELD),
                         messageProperties.getSoapAction());
        }
        if (messageProperties.isSetRequestURI()
            && !messageProperties.getRequestURI().trim().equals("")) {
            assertEquals(message.getStringProperty(JMSSpecConstants.REQUESTURI_FIELD),
                         messageProperties.getRequestURI().trim());
        }
        if (messageProperties.isSetIsFault()) {
            assertEquals(message.getBooleanProperty(JMSSpecConstants.ISFAULT_FIELD),
                         messageProperties.isIsFault());
        }
        // todo messagebody
    }

    public void checkJMSProperties(TestCaseType testcase, JMSMessageHeadersType requestHeader)
        throws JMSException {
        if (testcase.getRequestMessage() != null) {
            checkJMSProperties(testcase.getRequestMessage(), requestHeader);
        }
    }

    public void checkJMSProperties(TestCaseType testcase, JMSMessageHeadersType requestHeader,
                                   JMSMessageHeadersType responseHeader) throws JMSException {
        if (testcase.getRequestMessage() != null) {
            checkJMSProperties(testcase.getRequestMessage(), requestHeader);
        }
        if (testcase.getResponseMessage() != null) {
            checkJMSProperties(testcase.getResponseMessage(), responseHeader);
        }
        if (requestHeader.getJMSCorrelationID() != null) {
            assertEquals(requestHeader.getJMSCorrelationID(), responseHeader.getJMSCorrelationID());
        } else {
            assertEquals(requestHeader.getJMSMessageID(), responseHeader.getJMSCorrelationID());
        }
    }

    private void checkJMSProperties(MessagePropertiesType messageProperties,
                                    JMSMessageHeadersType header) {
        // todo messagetype
        // todo messageid
        if (messageProperties.isSetDeliveryMode()) {
            int dm = 0;
            if (header.isSetJMSDeliveryMode()) {
                dm = header.getJMSDeliveryMode();
            }
            assertEquals(dm, 
                         messageProperties.getDeliveryMode().intValue());
        }
        if (messageProperties.isSetPriority()) {
            assertEquals(header.getJMSPriority(), messageProperties.getPriority().intValue());
        }
        /*
         * if (messageProperties.isSetExpiration()) { assertEquals(header.getJMSExpiration(),
         * messageProperties.getExpiration().intValue()); }
         */
        /*
         * if (messageProperties.isSetReplyTo() && !messageProperties.getReplyTo().trim().equals("")) {
         * assertEquals(header.getJMSReplyTo().toString(), messageProperties.getReplyTo()); }
         */
        // correlationid
        /*
         * if (messageProperties.isSetDestination() && !messageProperties.getDestination().trim().equals(""))
         * { assertEquals(header.get.toString(), messageProperties.getDestination()); }
         */
        if (messageProperties.isSetBindingVersion()
            && !messageProperties.getBindingVersion().trim().equals("")) {
            assertEquals(header.getSOAPJMSBindingVersion(), messageProperties.getBindingVersion());
        }
        if (messageProperties.isSetTargetService()
            && !messageProperties.getTargetService().trim().equals("")) {
            assertEquals(header.getSOAPJMSTargetService(), messageProperties.getTargetService());
        }
        if (messageProperties.isSetContentType()
            && !messageProperties.getContentType().trim().equals("")) {
            assertEquals(header.getSOAPJMSContentType(), messageProperties.getContentType());
        }
        if (messageProperties.isSetSoapAction()
            && !messageProperties.getSoapAction().trim().equals("")) {
            assertEquals(header.getSOAPJMSSOAPAction(), messageProperties.getSoapAction());
        }
        if (messageProperties.isSetRequestURI()
            && !messageProperties.getRequestURI().trim().equals("")) {
            assertEquals(header.getSOAPJMSRequestURI(), messageProperties.getRequestURI().trim());
        }
        if (messageProperties.isSetIsFault()) {
            assertEquals(header.isSOAPJMSIsFault(), messageProperties.isIsFault());
        }
        // todo messagebody
    }
    
    public void twoWayTestWithCreateMessage(final TestCaseType testcase) throws Exception {
        String address = testcase.getAddress();
        JMSConfiguration jmsConfig = JMSTestUtil.getInitJMSConfiguration(address);
        final JmsTemplate jmsTemplate = JMSFactory.createJmsTemplate(jmsConfig, null);
        
        final Destination replyToDestination = JMSFactory.resolveOrCreateDestination(jmsTemplate,
                                                                                     null, false);
        class JMSConduitMessageCreator implements MessageCreator {
            private javax.jms.Message jmsMessage;

            public javax.jms.Message createMessage(Session session) throws JMSException {
                jmsMessage = JMSTestUtil.buildJMSMessageFromTestCase(testcase, session, replyToDestination);
                return jmsMessage;
            }

            public String getMessageID() {
                if (jmsMessage != null) {
                    try {
                        return jmsMessage.getJMSMessageID();
                    } catch (JMSException e) {
                        return null;
                    }
                }
                return null;
            }
        }
        JMSConduitMessageCreator messageCreator = new JMSConduitMessageCreator();    

        jmsTemplate.send(jmsConfig.getTargetDestination(), messageCreator);
        String messageId = messageCreator.getMessageID();

        String messageSelector = "JMSCorrelationID = '" + messageId + "'";
        javax.jms.Message replyMessage = jmsTemplate.receiveSelected(replyToDestination,
                                                                     messageSelector);
        checkReplyMessage(replyMessage, testcase);
    }

    private void checkReplyMessage(Message replyMessage, TestCaseType testcase) throws JMSException {
        checkJMSProperties(replyMessage, testcase.getResponseMessage());
    }    
}
