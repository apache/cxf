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

package org.apache.cxf.jms.testsuite.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.apache.cxf.testsuite.testcase.MessagePropertiesType;
import org.apache.cxf.testsuite.testcase.TestCaseType;
import org.apache.cxf.testsuite.testcase.TestCasesType;
import org.apache.cxf.transport.jms.JMSConfiguration;
import org.apache.cxf.transport.jms.JMSFactory;
import org.apache.cxf.transport.jms.JMSOldConfigHolder;
import org.apache.cxf.transport.jms.JNDIConfiguration;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;
import org.apache.cxf.transport.jms.uri.JMSEndpoint;
import org.apache.cxf.transport.jms.uri.JMSEndpointParser;
import org.apache.cxf.transport.jms.uri.JMSURIConstants;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.jndi.JndiTemplate;

/**
 * 
 */
public final class JMSTestUtil {

    private static TestCasesType testcases;

    private JMSTestUtil() {
    }
    
    public static List<TestCaseType> getTestCases() {
        try {
            if (testcases == null) {
                loadTestCases();
            }
            return testcases.getTestCase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<TestCaseType>();
    }

    public static TestCaseType getTestCase(String testId) {
        if (testId == null) {
            return null;
        }
        Iterator<TestCaseType> iter = getTestCases().iterator();
        while (iter.hasNext()) {
            TestCaseType testcase = iter.next();
            if (testId.equals(testcase.getId())) {
                return testcase;
            }
        }
        return null;
    }

    private static void loadTestCases() throws Exception {
        JAXBContext context = JAXBContext.newInstance("org.apache.cxf.testsuite.testcase");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        JAXBElement e = (JAXBElement)unmarshaller.unmarshal(new JMSTestUtil().getClass()
            .getResource("/org/apache/cxf/jms/testsuite/util/testcases.xml"));
        testcases = (TestCasesType)e.getValue();
    }

    public static JmsTemplate getJmsTemplate(String address) throws Exception {
        return JMSFactory.createJmsTemplate(getInitJMSConfiguration(address), null);
    }

    public static Destination getJmsDestination(JmsTemplate jmsTemplate, String destinationName,
                                                boolean pubSubDomain) {
        return JMSFactory.resolveOrCreateDestination(jmsTemplate, destinationName, pubSubDomain);
    }

    public static JMSConfiguration getInitJMSConfiguration(String address) throws Exception {
        JMSEndpoint endpoint = JMSEndpointParser.createEndpoint(address);

        JMSConfiguration jmsConfig = new JMSConfiguration();

        if (endpoint.isSetDeliveryMode()) {
            int deliveryMode = endpoint.getDeliveryMode()
                .equals(JMSURIConstants.DELIVERYMODE_PERSISTENT)
                ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT;
            jmsConfig.setDeliveryMode(deliveryMode);
        }

        if (endpoint.isSetPriority()) {
            int priority = endpoint.getPriority();
            jmsConfig.setPriority(priority);
        }

        if (endpoint.isSetTimeToLive()) {
            long timeToLive = endpoint.getTimeToLive();
            jmsConfig.setTimeToLive(timeToLive);
        }

        if (jmsConfig.isUsingEndpointInfo()) {
            JndiTemplate jt = new JndiTemplate();
            jt.setEnvironment(JMSOldConfigHolder.getInitialContextEnv(endpoint));
            boolean pubSubDomain = false;
            pubSubDomain = endpoint.getJmsVariant().equals(JMSURIConstants.TOPIC);
            JNDIConfiguration jndiConfig = new JNDIConfiguration();
            jndiConfig.setJndiConnectionFactoryName(endpoint.getJndiConnectionFactoryName());
            jmsConfig.setJndiTemplate(jt);
            jmsConfig.setJndiConfig(jndiConfig);
            jmsConfig.setExplicitQosEnabled(true);
            jmsConfig.setPubSubDomain(pubSubDomain);
            jmsConfig.setPubSubNoLocal(true);
            boolean useJndi = endpoint.getJmsVariant().equals(JMSURIConstants.JNDI);
            if (useJndi) {
                // Setup Destination jndi destination resolver
                final JndiDestinationResolver jndiDestinationResolver = new JndiDestinationResolver();
                jndiDestinationResolver.setJndiTemplate(jt);
                jmsConfig.setDestinationResolver(jndiDestinationResolver);
                jmsConfig.setTargetDestination(endpoint.getDestinationName());
                jmsConfig.setReplyDestination(endpoint.getReplyToName());
            } else {
                // Use the default dynamic destination resolver
                jmsConfig.setTargetDestination(endpoint.getDestinationName());
                jmsConfig.setReplyDestination(endpoint.getReplyToName());
            }
        }
        return jmsConfig;
    }

    /**
     * @param testcase
     * @param session
     * @param rtd
     * @return
     * @throws JMSException 
     */
    public static Message buildJMSMessageFromTestCase(TestCaseType testcase, Session session,
                                                      Destination rtd) throws JMSException {
        MessagePropertiesType messageProperties = testcase.getRequestMessage();
        Message jmsMessage = null;
        String messageType = messageProperties.getMessageType();
        if ("text".equals(messageType)) {
            jmsMessage = session.createTextMessage();
            ((TextMessage)jmsMessage).setText("test");
        } else if ("byte".equals(messageType)) {
            jmsMessage = session.createBytesMessage();
        } else if ("stream".equals(messageType)) {
            jmsMessage = session.createStreamMessage();
            ((StreamMessage)jmsMessage).writeString("test");
        } else {
            jmsMessage = session.createBytesMessage();
        }

        jmsMessage.setJMSReplyTo(rtd);

        if (messageProperties.isSetDeliveryMode()) {
            jmsMessage.setJMSDeliveryMode(messageProperties.getDeliveryMode());
        }
        if (messageProperties.isSetExpiration()) {
            jmsMessage.setJMSExpiration(messageProperties.getExpiration());
        }
        if (messageProperties.isSetPriority()) {
            jmsMessage.setJMSPriority(messageProperties.getPriority());
        }
        if (messageProperties.isSetExpiration()) {
            jmsMessage.setJMSPriority(messageProperties.getExpiration());
        }
        
        if (messageProperties.isSetTargetService()
            && !"".equals(messageProperties.getTargetService().trim())) {
            jmsMessage.setStringProperty(JMSSpecConstants.TARGETSERVICE_FIELD, messageProperties
                .getTargetService().trim());
        }

        if (messageProperties.isSetBindingVersion()
            && !"".equals(messageProperties.getBindingVersion().trim())) {
            jmsMessage.setStringProperty(JMSSpecConstants.BINDINGVERSION_FIELD, messageProperties
                                         .getBindingVersion().trim());
        }

        if (messageProperties.isSetContentType()
            && !"".equals(messageProperties.getContentType().trim())) {
            jmsMessage.setStringProperty(JMSSpecConstants.CONTENTTYPE_FIELD, messageProperties
                .getContentType().trim());
        }

        if (messageProperties.isSetSoapAction()
            && !"".equals(messageProperties.getSoapAction().trim())) {
            jmsMessage.setStringProperty(JMSSpecConstants.SOAPACTION_FIELD, messageProperties
                .getSoapAction().trim());
        }

        if (messageProperties.isSetRequestURI()
            && !"".equals(messageProperties.getRequestURI().trim())) {
            jmsMessage.setStringProperty(JMSSpecConstants.REQUESTURI_FIELD, messageProperties
                .getRequestURI().trim());
        }
        return jmsMessage;
    }
}
