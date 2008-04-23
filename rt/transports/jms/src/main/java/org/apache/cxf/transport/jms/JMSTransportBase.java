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

package org.apache.cxf.transport.jms;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.EndpointInfo;


public class JMSTransportBase {    
    
    protected Destination targetDestination;
    protected Destination replyDestination;
    protected JMSSessionFactory sessionFactory;    
    protected Bus bus;
    //protected EndpointReferenceType targetEndpoint;
    protected EndpointInfo endpointInfo;
    protected String beanNameSuffix;
    private JMSTransport transport;
    
    //--Constructors------------------------------------------------------------
    public JMSTransportBase(Bus b, 
                            EndpointInfo endpoint, 
                            boolean isServer, 
                            String suffix,
                            JMSTransport transport) {
        bus = b;
        endpointInfo = endpoint;
        beanNameSuffix = suffix;
        this.transport = transport;
    }
    
    /**
     * Callback from the JMSProviderHub indicating the ClientTransport has
     * been sucessfully connected.
     *
     * @param targetDestination the target destination
     * @param sessionFactory used to get access to a pooled JMS resources
     */
    protected void connected(Destination target, Destination reply, JMSSessionFactory factory) {
        targetDestination = target;
        replyDestination = reply;
        sessionFactory = factory;       
    }


    /**
     * Create a JMS of the appropriate type populated with the given payload.
     *
     * @param payload the message payload, expected to be either of type
     * String or byte[] depending on payload type
     * @param session the JMS session
     * @param replyTo the ReplyTo destination if any
     * @return a JMS of the appropriate type populated with the given payload
     */
    protected Message marshal(Object payload, Session session, Destination replyTo,
                              String messageType) throws JMSException {
        Message message = null;

        if (JMSConstants.TEXT_MESSAGE_TYPE.equals(messageType)) {
            message = session.createTextMessage((String)payload);
        } else if (JMSConstants.BYTE_MESSAGE_TYPE.equals(messageType)) {
            message = session.createBytesMessage();
            ((BytesMessage)message).writeBytes((byte[])payload);
        } else {
            message = session.createObjectMessage();
            ((ObjectMessage)message).setObject((byte[])payload);
        }

        if (replyTo != null) {
            message.setJMSReplyTo(replyTo);
        }

        return message;
    }


    /**
     * Unmarshal the payload of an incoming message.
     *
     * @param message the incoming message
     * @return the unmarshalled message payload, either of type String or
     * byte[] depending on payload type
     */
    protected Object unmarshal(Message message) throws JMSException {
        Object ret = null;

        if (message instanceof TextMessage) {
            ret = ((TextMessage)message).getText();
        } else if (message instanceof BytesMessage) {
            byte[] retBytes = new byte[(int) ((BytesMessage) message).getBodyLength()];
            ((BytesMessage) message).readBytes(retBytes);
            ret = retBytes;
        } else {
            ret = (byte[])((ObjectMessage)message).getObject();
        }

        return ret;
    }

    protected JMSMessageHeadersType populateIncomingContext(javax.jms.Message message,
                                                            org.apache.cxf.message.Message inMessage,
                                                     String headerType)  throws JMSException {
        JMSMessageHeadersType headers = null;

        headers = (JMSMessageHeadersType)inMessage.get(headerType);

        if (headers == null) {           
            headers = new JMSMessageHeadersType();
            inMessage.put(headerType, headers);
        }

        headers.setJMSCorrelationID(message.getJMSCorrelationID());
        headers.setJMSDeliveryMode(new Integer(message.getJMSDeliveryMode()));
        headers.setJMSExpiration(new Long(message.getJMSExpiration()));
        headers.setJMSMessageID(message.getJMSMessageID());
        headers.setJMSPriority(new Integer(message.getJMSPriority()));
        headers.setJMSRedelivered(Boolean.valueOf(message.getJMSRedelivered()));
        headers.setJMSTimeStamp(new Long(message.getJMSTimestamp()));
        headers.setJMSType(message.getJMSType());

        Map<String, List<String>> protHeaders = new HashMap<String, List<String>>();
        List<JMSPropertyType> props = headers.getProperty();
        Enumeration enm = message.getPropertyNames();
        while (enm.hasMoreElements()) {
            String name = (String)enm.nextElement();
            String val = message.getStringProperty(name);
            JMSPropertyType prop = new JMSPropertyType();
            prop.setName(name);
            prop.setValue(val);
            props.add(prop);
            
            protHeaders.put(name, Collections.singletonList(val));
        }
        inMessage.put(org.apache.cxf.message.Message.PROTOCOL_HEADERS, protHeaders);
        return headers;
    }

    protected int getJMSDeliveryMode(JMSMessageHeadersType headers) {
        int deliveryMode = Message.DEFAULT_DELIVERY_MODE;

        if (headers != null && headers.isSetJMSDeliveryMode()) {
            deliveryMode = headers.getJMSDeliveryMode();
        }
        return deliveryMode;
    }

    protected int getJMSPriority(JMSMessageHeadersType headers) {
        int priority = Message.DEFAULT_PRIORITY;
        if (headers != null && headers.isSetJMSPriority()) {
            priority = headers.getJMSPriority();
        }
        return priority;
    }

    protected long getTimeToLive(JMSMessageHeadersType headers) {
        long ttl = -1;
        if (headers != null && headers.isSetTimeToLive()) {
            ttl = headers.getTimeToLive();
        }
        return ttl;
    }

    protected String getCorrelationId(JMSMessageHeadersType headers) {
        String correlationId  = null;
        if (headers != null
            && headers.isSetJMSCorrelationID()) {
            correlationId = headers.getJMSCorrelationID();
        }
        return correlationId;
    }

    protected void addProtocolHeaders(Message message, Map<String, List<String>> headers) 
        throws JMSException {
        if (headers == null) {
            return;
        }
        StringBuilder value = new StringBuilder(256);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            value.setLength(0);
            boolean first = true;
            for (String s : entry.getValue()) {
                if (!first) {
                    value.append("; ");
                }
                value.append(s);
                first = false;
            }
            message.setStringProperty(entry.getKey(), value.toString());
        }
    }
    protected void setMessageProperties(JMSMessageHeadersType headers, Message message)
        throws JMSException {

        if (headers != null
                && headers.isSetProperty()) {
            List<JMSPropertyType> props = headers.getProperty();
            for (int x = 0; x < props.size(); x++) {
                message.setStringProperty(props.get(x).getName(), props.get(x).getValue());
            }
        }
    }
    
    protected String getAddrUriFromJMSAddrPolicy() {
        AddressType jmsAddressPolicy = transport.getJMSAddress();
        return "jms:" + jmsAddressPolicy.getJndiConnectionFactoryName() 
                        + "#"
                        + jmsAddressPolicy.getJndiDestinationName();
    }
    
    protected String getReplyTotAddrUriFromJMSAddrPolicy() {
        AddressType jmsAddressPolicy = transport.getJMSAddress();
        return "jms:" 
                        + jmsAddressPolicy.getJndiConnectionFactoryName() 
                        + "#"
                        + jmsAddressPolicy.getJndiReplyDestinationName();
    }

    protected boolean isDestinationStyleQueue() {
        return JMSConstants.JMS_QUEUE.equals(
            transport.getJMSAddress().getDestinationStyle().value());
    }
}
