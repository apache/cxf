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

import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.SimpleMessageConverter102;

public final class JMSUtils {

    static final Logger LOG = LogUtils.getL7dLogger(JMSUtils.class);

    private static final char[] CORRELATTION_ID_PADDING = {
        '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0'
    };

    private JMSUtils() {

    }

    public static long getTimeToLive(JMSMessageHeadersType headers) {
        long ttl = -1;
        if (headers != null && headers.isSetTimeToLive()) {
            ttl = headers.getTimeToLive();
        }
        return ttl;
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
     *            type
     * @param session the JMS session
     * @param replyTo the ReplyTo destination if any
     * @return a JMS of the appropriate type populated with the given payload
     */
    public static Message createAndSetPayload(Object payload, Session session, String messageType)
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
        return message;
    }

    /**
     * Extract the payload of an incoming message.
     * 
     * @param message the incoming message
     * @param encoding the message encoding
     * @return the message payload as byte[]
     * @throws UnsupportedEncodingException
     */
    public static byte[] retrievePayload(Message message, String encoding)
        throws UnsupportedEncodingException {
        Object converted;
        try {
            converted = new SimpleMessageConverter102().fromMessage(message);
        } catch (MessageConversionException e) {
            throw new RuntimeException("Conversion failed", e);
        } catch (JMSException e) {
            throw JmsUtils.convertJmsAccessException(e);
        }
        if (converted instanceof String) {
            if (encoding != null) {
                return ((String)converted).getBytes(encoding);
            } else {
                // Using the UTF-8 encoding as default
                return ((String)converted).getBytes("UTF-8");
            }
        } else if (converted instanceof byte[]) {
            return (byte[])converted;
        } else {
            return (byte[])converted; // TODO is this correct?
        }
    }

    public static void populateIncomingContext(javax.jms.Message message,
                                               org.apache.cxf.message.Message inMessage, String messageType)
        throws UnsupportedEncodingException {
        try {
            JMSMessageHeadersType messageProperties = null;
            messageProperties = (JMSMessageHeadersType)inMessage.get(messageType);
            if (messageProperties == null) {
                messageProperties = new JMSMessageHeadersType();
                inMessage.put(messageType, messageProperties);
            }
            messageProperties.setJMSCorrelationID(message.getJMSCorrelationID());
            messageProperties.setJMSDeliveryMode(new Integer(message.getJMSDeliveryMode()));
            messageProperties.setJMSExpiration(new Long(message.getJMSExpiration()));
            messageProperties.setJMSMessageID(message.getJMSMessageID());
            messageProperties.setJMSPriority(new Integer(message.getJMSPriority()));
            messageProperties.setJMSRedelivered(Boolean.valueOf(message.getJMSRedelivered()));
            messageProperties.setJMSTimeStamp(new Long(message.getJMSTimestamp()));
            messageProperties.setJMSType(message.getJMSType());

            Map<String, List<String>> protHeaders = new HashMap<String, List<String>>();
            List<JMSPropertyType> props = messageProperties.getProperty();
            Enumeration enm = message.getPropertyNames();
            while (enm.hasMoreElements()) {
                String name = (String)enm.nextElement();
                String val = message.getStringProperty(name);
                JMSPropertyType prop = new JMSPropertyType();
                prop.setName(name);
                prop.setValue(val);
                props.add(prop);

                protHeaders.put(name, Collections.singletonList(val));
                if (name.equals(org.apache.cxf.message.Message.CONTENT_TYPE)
                    || name.equals(JMSConstants.JMS_CONTENT_TYPE) && val != null) {
                    inMessage.put(org.apache.cxf.message.Message.CONTENT_TYPE, val);
                    // set the message encoding
                    inMessage.put(org.apache.cxf.message.Message.ENCODING, getEncoding(val));
                }

            }
            inMessage.put(org.apache.cxf.message.Message.PROTOCOL_HEADERS, protHeaders);

            SecurityContext securityContext = buildSecurityContext(message);
            inMessage.put(SecurityContext.class, securityContext);

            populateIncomingMessageProperties(message, inMessage, messageProperties);
        } catch (JMSException ex) {
            throw JmsUtils.convertJmsAccessException(ex);
        }
    }

    /**
     * @param jmsMessage
     * @param inMessage
     * @param messagePropertiesType
     */
    private static void populateIncomingMessageProperties(Message jmsMessage,
                                                          org.apache.cxf.message.Message inMessage,
                                                          JMSMessageHeadersType messageProperties)
        throws UnsupportedEncodingException {
        try {
            if (jmsMessage.propertyExists(JMSSpecConstants.TARGETSERVICE_FIELD)) {
                messageProperties.setSOAPJMSTargetService(jmsMessage
                    .getStringProperty(JMSSpecConstants.TARGETSERVICE_FIELD));
            }
            if (jmsMessage.propertyExists(JMSSpecConstants.BINDINGVERSION_FIELD)) {
                messageProperties.setSOAPJMSBindingVersion(jmsMessage
                    .getStringProperty(JMSSpecConstants.BINDINGVERSION_FIELD));
            }
            if (jmsMessage.propertyExists(JMSSpecConstants.CONTENTTYPE_FIELD)) {
                messageProperties.setSOAPJMSContentType(jmsMessage
                    .getStringProperty(JMSSpecConstants.CONTENTTYPE_FIELD));
            }
            if (jmsMessage.propertyExists(JMSSpecConstants.SOAPACTION_FIELD)) {
                messageProperties.setSOAPJMSSOAPAction(jmsMessage
                    .getStringProperty(JMSSpecConstants.SOAPACTION_FIELD));
            }
            if (jmsMessage.propertyExists(JMSSpecConstants.ISFAULT_FIELD)) {
                messageProperties.setSOAPJMSIsFault(jmsMessage
                    .getBooleanProperty(JMSSpecConstants.ISFAULT_FIELD));
            }
            if (jmsMessage.propertyExists(JMSSpecConstants.REQUESTURI_FIELD)) {
                messageProperties.setSOAPJMSRequestURI(jmsMessage
                    .getStringProperty(JMSSpecConstants.REQUESTURI_FIELD));
            }

            if (messageProperties.isSetSOAPJMSContentType()) {
                String contentType = messageProperties.getSOAPJMSContentType();
                inMessage.put(org.apache.cxf.message.Message.CONTENT_TYPE, contentType);
                // set the message encoding
                inMessage.put(org.apache.cxf.message.Message.ENCODING, getEncoding(contentType));
            }
        } catch (JMSException ex) {
            throw JmsUtils.convertJmsAccessException(ex);
        }
    }

    /**
     * Extract the property JMSXUserID from the jms message and create a SecurityContext from it. 
     * For more info see Jira Issue CXF-2055
     * {@link https://issues.apache.org/jira/browse/CXF-2055}
     * 
     * @param message jms message to retrieve user information from
     * @return SecurityContext that contains the user of the producer of the message as the Principal
     * @throws JMSException if something goes wrong
     */
    private static SecurityContext buildSecurityContext(javax.jms.Message message) throws JMSException {
        final String jmsUserName = message.getStringProperty("JMSXUserID");
        if (jmsUserName == null) {
            return null;
        }
        final Principal principal = new Principal() {
            public String getName() {
                return jmsUserName;
            }

        };

        SecurityContext securityContext = new SecurityContext() {

            public Principal getUserPrincipal() {
                return principal;
            }

            public boolean isUserInRole(String role) {
                return false;
            }

        };
        return securityContext;
    }

    static String getEncoding(String ct) throws UnsupportedEncodingException {
        String contentType = ct.toLowerCase();
        String enc = null;

        String[] tokens = contentType.split(";");
        for (String token : tokens) {
            int index = token.indexOf("charset=");
            if (index >= 0) {
                enc = token.substring(index + 8);
                break;
            }
        }

        String normalizedEncoding = HttpHeaderHelper.mapCharset(enc, "UTF-8");
        if (normalizedEncoding == null) {
            String m = new org.apache.cxf.common.i18n.Message("INVALID_ENCODING_MSG", LOG, new Object[] {
                enc
            }).toString();
            LOG.log(Level.WARNING, m);
            throw new UnsupportedEncodingException(m);
        }

        return normalizedEncoding;
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
            // If the Content-Type header key is Content-Type replace with JMS_Content_Type
            if (entry.getKey().equals(org.apache.cxf.message.Message.CONTENT_TYPE)) {
                message.setStringProperty(JMSConstants.JMS_CONTENT_TYPE, value.toString());
            } else {
                message.setStringProperty(entry.getKey(), value.toString());
            }

        }
    }

    public static void addContentTypeToProtocolHeader(org.apache.cxf.message.Message message) {
        String contentType = (String)message.get(org.apache.cxf.message.Message.CONTENT_TYPE);
        String enc = (String)message.get(org.apache.cxf.message.Message.ENCODING);
        // add the encoding information
        if (null != contentType) {
            if (enc != null && contentType.indexOf("charset=") == -1) {
                contentType = contentType + "; charset=" + enc;
            }
        } else if (enc != null) {
            contentType = "text/xml; charset=" + enc;
        } else {
            contentType = "text/xml";
        }

        // Retrieve or create protocol headers
        Map<String, List<String>> headers = CastUtils.cast((Map<?, ?>)message
            .get(org.apache.cxf.message.Message.PROTOCOL_HEADERS));
        if (null == headers) {
            headers = new HashMap<String, List<String>>();
            message.put(org.apache.cxf.message.Message.PROTOCOL_HEADERS, headers);
        }

        // Add content type to the protocol headers
        List<String> ct;
        if (headers.get(JMSConstants.JMS_CONTENT_TYPE) != null) {
            ct = headers.get(JMSConstants.JMS_CONTENT_TYPE);
        } else if (headers.get(org.apache.cxf.message.Message.CONTENT_TYPE) != null) {
            ct = headers.get(org.apache.cxf.message.Message.CONTENT_TYPE);
        } else {
            ct = new ArrayList<String>();
            headers.put(JMSConstants.JMS_CONTENT_TYPE, ct);
        }
        ct.add(contentType);
    }
    
    public static String getContentType(org.apache.cxf.message.Message message) {
        String contentType = (String)message.get(org.apache.cxf.message.Message.CONTENT_TYPE);
        String enc = (String)message.get(org.apache.cxf.message.Message.ENCODING);
        // add the encoding information
        if (null != contentType) {
            if (enc != null && contentType.indexOf("charset=") == -1) {
                contentType = contentType + "; charset=" + enc;
            }
        } else if (enc != null) {
            contentType = "text/xml; charset=" + enc;
        } else {
            contentType = "text/xml";
        }

        // Retrieve or create protocol headers
        Map<String, List<String>> headers = CastUtils.cast((Map<?, ?>)message
            .get(org.apache.cxf.message.Message.PROTOCOL_HEADERS));
        if (null == headers) {
            headers = new HashMap<String, List<String>>();
            message.put(org.apache.cxf.message.Message.PROTOCOL_HEADERS, headers);
        }
        return contentType;
    }

    public static Message buildJMSMessageFromCXFMessage(JMSConfiguration jmsConfig,
                                                        org.apache.cxf.message.Message outMessage,
                                                        Object payload, String messageType,
                                                        Session session, Destination replyTo,
                                                        String correlationId) throws JMSException {
        Message jmsMessage = JMSUtils.createAndSetPayload(payload, session, messageType);

        if (replyTo != null) {
            jmsMessage.setJMSReplyTo(replyTo);
        }

        JMSMessageHeadersType messageProperties = (JMSMessageHeadersType)outMessage
            .get(JMSConstants.JMS_CLIENT_REQUEST_HEADERS);
        if (messageProperties == null) {
            messageProperties = new JMSMessageHeadersType();
            outMessage.put(JMSConstants.JMS_CLIENT_REQUEST_HEADERS, messageProperties);
        }
        JMSUtils.prepareJMSProperties(messageProperties, outMessage, jmsConfig);
        JMSUtils.setJMSProperties(jmsMessage, messageProperties);

        jmsMessage.setJMSCorrelationID(correlationId);
        return jmsMessage;
    }

    /**
     * @param messageProperties
     * @param jmsMessage
     */
    static void setJMSProperties(Message jmsMessage, JMSMessageHeadersType messageProperties)
        throws JMSException {

        setJMSMessageHeaderProperties(jmsMessage, messageProperties);
        setJMSMessageProperties(jmsMessage, messageProperties);
    }

    /**
     * @param jmsMessage
     * @param messageProperties
     */
    private static void setJMSMessageHeaderProperties(Message jmsMessage,
                                                      JMSMessageHeadersType messageProperties)
        throws JMSException {
        if (messageProperties.isSetJMSDeliveryMode()) {
            jmsMessage.setJMSDeliveryMode(messageProperties.getJMSDeliveryMode());
        }
        // if (messageProperties.isSetTimeToLive()) {
        // jmsMessage.setJMSExpiration(expiration);
        // }
        if (messageProperties.isSetJMSPriority()) {
            jmsMessage.setJMSPriority(messageProperties.getJMSPriority());
        }
    }

    /**
     * @param jmsMessage
     * @param messageProperties
     */
    private static void setJMSMessageProperties(Message jmsMessage, JMSMessageHeadersType messageProperties)
        throws JMSException {

        if (messageProperties.isSetSOAPJMSTargetService()) {
            jmsMessage.setStringProperty(JMSSpecConstants.TARGETSERVICE_FIELD, messageProperties
                .getSOAPJMSTargetService());
        }

        jmsMessage.setStringProperty(JMSSpecConstants.BINDINGVERSION_FIELD, messageProperties
            .getSOAPJMSBindingVersion());

        if (messageProperties.isSetSOAPJMSContentType()) {
            jmsMessage.setStringProperty(JMSSpecConstants.CONTENTTYPE_FIELD, messageProperties
                .getSOAPJMSContentType());
        }

        if (messageProperties.isSetSOAPJMSSOAPAction()) {
            jmsMessage.setStringProperty(JMSSpecConstants.SOAPACTION_FIELD, messageProperties
                .getSOAPJMSSOAPAction());
        }

        if (messageProperties.isSetSOAPJMSIsFault()) {
            jmsMessage.setBooleanProperty(JMSSpecConstants.ISFAULT_FIELD, messageProperties
                .isSOAPJMSIsFault());
        }

        if (messageProperties.isSetSOAPJMSRequestURI()) {
            jmsMessage.setStringProperty(JMSSpecConstants.REQUESTURI_FIELD, messageProperties
                .getSOAPJMSRequestURI());
        }

        if (messageProperties != null && messageProperties.isSetProperty()) {
            List<JMSPropertyType> props = messageProperties.getProperty();
            for (int x = 0; x < props.size(); x++) {
                jmsMessage.setStringProperty(props.get(x).getName(), props.get(x).getValue());
            }
        }
    }

    /**
     * @param messageProperteis
     * @param outMessage
     * @param jmsConfig
     */
    static void prepareJMSProperties(JMSMessageHeadersType messageProperteis,
                                     org.apache.cxf.message.Message outMessage,
                                     JMSConfiguration jmsConfig) {
        prepareJMSMessageHeaderProperties(messageProperteis, outMessage, jmsConfig);

        prepareJMSMessageProperties(messageProperteis, outMessage, jmsConfig);

    }

    /**
     * @param messageProperteis
     * @param outMessage
     * @param jmsConfig
     */
    private static void prepareJMSMessageHeaderProperties(
                                                          JMSMessageHeadersType messageProperteis,
                                                          org.apache.cxf.message.Message outMessage,
                                                          JMSConfiguration jmsConfig) {
        if (!messageProperteis.isSetJMSDeliveryMode()) {
            messageProperteis.setJMSDeliveryMode(jmsConfig.getDeliveryMode());
        }
        if (!messageProperteis.isSetTimeToLive()) {
            messageProperteis.setTimeToLive(jmsConfig.getTimeToLive());
        }
        if (!messageProperteis.isSetJMSPriority()) {
            messageProperteis.setJMSPriority(jmsConfig.getPriority());
        }
    }

    /**
     * @param messageProperties
     * @param outMessage
     * @param jmsConfig
     */
    private static void prepareJMSMessageProperties(JMSMessageHeadersType messageProperties,
                                                    org.apache.cxf.message.Message outMessage,
                                                    JMSConfiguration jmsConfig) {
        if (!messageProperties.isSetSOAPJMSTargetService()) {
            messageProperties.setSOAPJMSTargetService(jmsConfig.getTargetService());
        }
        if (!messageProperties.isSetSOAPJMSBindingVersion()) {
            messageProperties.setSOAPJMSBindingVersion("1.0");
        }
        messageProperties.setSOAPJMSContentType(getContentType(outMessage));
        String soapAction = null;
        // Retrieve or create protocol headers
        Map<String, List<String>> headers = CastUtils.cast((Map<?, ?>)outMessage
            .get(org.apache.cxf.message.Message.PROTOCOL_HEADERS));
        if (headers != null) {
            List<String> action = headers.get(SOAPConstants.SOAP_ACTION);
            if (action != null && action.size() > 0) {
                soapAction = action.get(0);
            }
        }
        if (soapAction != null) {
            messageProperties.setSOAPJMSSOAPAction(soapAction);
        }
        if (!messageProperties.isSetSOAPJMSIsFault()) {
            if (outMessage.getContent(Exception.class) != null) {
                messageProperties.setSOAPJMSIsFault(true);
            } else {
                messageProperties.setSOAPJMSIsFault(false);
            }
        }
        if (!messageProperties.isSetSOAPJMSRequestURI()) {
            messageProperties.setSOAPJMSRequestURI(jmsConfig.getRequestURI());
        }
    }

    /**
     * @param messageProperties
     * @param inMessageProperties
     */
    public static void initResponseMessageProperties(JMSMessageHeadersType messageProperties,
                                                     JMSMessageHeadersType inMessageProperties) {
        messageProperties.setJMSDeliveryMode(inMessageProperties.getJMSDeliveryMode());
        // messageProperties.setJMSExpiration(inMessageProperties.getJMSExpiration());
        messageProperties.setJMSPriority(inMessageProperties.getJMSPriority());
        //messageProperties.setJMSCorrelationID(inMessageProperties.getJMSMessageID());
        // JMSDestination

        messageProperties.setSOAPJMSRequestURI(inMessageProperties.getSOAPJMSRequestURI());
        messageProperties.setSOAPJMSBindingVersion(inMessageProperties.getSOAPJMSBindingVersion());
        // contenttype.
    }

    public static String createCorrelationId(final String prefix, long i) {
        String index = Long.toHexString(i);
        StringBuffer id = new StringBuffer(prefix);
        id.append(CORRELATTION_ID_PADDING, 0, 16 - index.length());
        id.append(index);
        return id.toString();
    }
}
