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

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.StreamMessage;
import jakarta.jms.TextMessage;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Unmarshaller;
import org.apache.cxf.testsuite.testcase.MessagePropertiesType;
import org.apache.cxf.testsuite.testcase.TestCaseType;
import org.apache.cxf.testsuite.testcase.TestCasesType;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;

/**
 *
 */
public final class JMSTestUtil {

    private static TestCasesType testcases;

    private JMSTestUtil() {
    }

    public static String getFullAddress(String partAddress, String jndiUrl) {
        String separator = partAddress.contains("?") ? "&" : "?";
        return partAddress + separator
            + "&jndiInitialContextFactory=org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory"
            + "&jndiConnectionFactoryName=ConnectionFactory"
            + "&jndiURL=" + jndiUrl;
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
        return new ArrayList<>();
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
        JAXBElement<?> e = (JAXBElement<?>)unmarshaller.unmarshal(new JMSTestUtil().getClass()
            .getResource("/org/apache/cxf/jms/testsuite/util/testcases.xml"));
        testcases = (TestCasesType)e.getValue();
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
        if (messageProperties.isSetCorrelationID()) {
            jmsMessage.setJMSCorrelationID(messageProperties.getCorrelationID());
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
