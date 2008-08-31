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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;

public final class JMSUtils {

    private static final Logger LOG = LogUtils.getL7dLogger(JMSUtils.class);

    private JMSUtils() {

    }

    public static Context getInitialContext(AddressType addrType) throws NamingException {
        Properties env = new Properties();
        populateContextEnvironment(addrType, env);

        if (LOG.isLoggable(Level.FINE)) {
            Enumeration props = env.propertyNames();

            while (props.hasMoreElements()) {
                String name = (String)props.nextElement();
                String value = env.getProperty(name);
                LOG.log(Level.FINE, "Context property: " + name + " | " + value);
            }
        }

        Context context = new InitialContext(env);

        return context;
    }

    protected static void populateContextEnvironment(AddressType addrType, Properties env) {

        java.util.ListIterator listIter = addrType.getJMSNamingProperty().listIterator();

        while (listIter.hasNext()) {
            JMSNamingPropertyType propertyPair = (JMSNamingPropertyType)listIter.next();

            if (null != propertyPair.getValue()) {
                env.setProperty(propertyPair.getName(), propertyPair.getValue());
            }
        }
    }

    public static int getJMSDeliveryMode(JMSMessageHeadersType headers) {
        int deliveryMode = Message.DEFAULT_DELIVERY_MODE;

        if (headers != null && headers.isSetJMSDeliveryMode()) {
            deliveryMode = headers.getJMSDeliveryMode();
        }
        return deliveryMode;
    }

    public static int getJMSPriority(JMSMessageHeadersType headers) {
        int priority = Message.DEFAULT_PRIORITY;
        if (headers != null && headers.isSetJMSPriority()) {
            priority = headers.getJMSPriority();
        }
        return priority;
    }

    public static long getTimeToLive(JMSMessageHeadersType headers) {
        long ttl = -1;
        if (headers != null && headers.isSetTimeToLive()) {
            ttl = headers.getTimeToLive();
        }
        return ttl;
    }

    public static String getCorrelationId(JMSMessageHeadersType headers) {
        String correlationId = null;
        if (headers != null && headers.isSetJMSCorrelationID()) {
            correlationId = headers.getJMSCorrelationID();
        }
        return correlationId;
    }

    public static void setMessageProperties(JMSMessageHeadersType headers, Message message)
        throws JMSException {

        if (headers != null && headers.isSetProperty()) {
            List<JMSPropertyType> props = headers.getProperty();
            for (int x = 0; x < props.size(); x++) {
                message.setStringProperty(props.get(x).getName(), props.get(x).getValue());
            }
        }
    }

    /**
     * Create a JMS of the appropriate type populated with the given payload.
     * 
     * @param payload the message payload, expected to be either of type String or byte[] depending on payload
     *                type
     * @param session the JMS session
     * @param replyTo the ReplyTo destination if any
     * @return a JMS of the appropriate type populated with the given payload
     */
    public static Message marshal(Object payload, Session session, Destination replyTo, String messageType)
        throws JMSException {
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
     * @return the unmarshalled message payload, either of type String or byte[] depending on payload type
     */
    public static Object unmarshal(Message message) throws JMSException {
        Object ret = null;

        if (message instanceof TextMessage) {
            ret = ((TextMessage)message).getText();
        } else if (message instanceof BytesMessage) {
            byte[] retBytes = new byte[(int)((BytesMessage)message).getBodyLength()];
            ((BytesMessage)message).readBytes(retBytes);
            ret = retBytes;
        } else {
            ret = (byte[])((ObjectMessage)message).getObject();
        }

        return ret;
    }

    public static JMSMessageHeadersType populateIncomingContext(javax.jms.Message message,
                                                                org.apache.cxf.message.Message inMessage,
                                                                String headerType) throws JMSException {
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
            if (name.equals(org.apache.cxf.message.Message.CONTENT_TYPE) && val != null) {
                inMessage.put(org.apache.cxf.message.Message.CONTENT_TYPE, val);
            }

        }
        inMessage.put(org.apache.cxf.message.Message.PROTOCOL_HEADERS, protHeaders);
        return headers;
    }

    protected static void addProtocolHeaders(Message message, Map<String, List<String>> headers)
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

    public static Map<String, List<String>> getSetProtocolHeaders(org.apache.cxf.message.Message message) {
        Map<String, List<String>> headers = CastUtils.cast((Map<?, ?>)message
            .get(org.apache.cxf.message.Message.PROTOCOL_HEADERS));
        if (null == headers) {
            headers = new HashMap<String, List<String>>();
            message.put(org.apache.cxf.message.Message.PROTOCOL_HEADERS, headers);
        }
        return headers;
    }

    public static void setContentToProtocalHeader(org.apache.cxf.message.Message message) {
        String contentType = (String)message.get(org.apache.cxf.message.Message.CONTENT_TYPE);

        Map<String, List<String>> headers = JMSUtils.getSetProtocolHeaders(message);
        if (headers.get(org.apache.cxf.message.Message.CONTENT_TYPE) == null) {
            List<String> ct = new ArrayList<String>();
            ct.add(contentType);
            headers.put(org.apache.cxf.message.Message.CONTENT_TYPE, ct);
        } else {
            List<String> ct = headers.get(org.apache.cxf.message.Message.CONTENT_TYPE);
            ct.add(contentType);
        }
    }

    public static boolean isDestinationStyleQueue(AddressType address) {
        return JMSConstants.JMS_QUEUE.equals(address.getDestinationStyle().value());
    }
}
