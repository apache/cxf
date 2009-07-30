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
import java.net.URL;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.testsuite.testcase.MessagePropertiesType;
import org.apache.cxf.testsuite.testcase.TestCaseType;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.jms.JMSMessageHeadersType;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;

/**
 * 
 */
public abstract class AbstractSOAPJMSTestSuite extends AbstractBusClientServerTestBase {

    /**
     * 
     */
    public AbstractSOAPJMSTestSuite() {
        super();
    }

    public <T1, T2> T2 getPort(String serviceName, String portName, Class<T1> serviceClass,
                               Class<T2> portTypeClass) throws Exception {
        String namespace = "http://cxf.apache.org/jms_simple";
        QName qServiceName = new QName(namespace, serviceName);
        QName qPortName = new QName(namespace, portName);
        URL wsdl = getClass().getResource("/wsdl/jms_spec_testsuite.wsdl");

        Class<? extends Service> svcls = serviceClass.asSubclass(Service.class);

        Constructor<? extends Service> serviceConstructor = svcls.getConstructor(URL.class,
                                                                                 QName.class);
        Service service = serviceConstructor.newInstance(new Object[] {
            wsdl, qServiceName
        });
        return service.getPort(qPortName, portTypeClass);
    }

    public void checkJMSProperties(Message message, MessagePropertiesType messageProperties,
                                   boolean noResponse) throws JMSException {
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
        if (noResponse) {
            assertEquals(message.getJMSReplyTo(), null);
            assertEquals(message.getJMSCorrelationID(), null);
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
            assertEquals(message.getStringProperty(JMSSpecConstants.ISFAULT_FIELD),
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
            assertEquals(header.getJMSDeliveryMode(), messageProperties.getDeliveryMode()
                .intValue());
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
}
