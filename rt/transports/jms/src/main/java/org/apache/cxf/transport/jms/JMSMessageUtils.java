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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;
import org.apache.cxf.transport.jms.uri.JMSEndpoint;
import org.apache.cxf.transport.jms.util.JMSMessageConverter;
import org.apache.cxf.transport.jms.util.JMSUtil;

/**
 * Static util methods for converting cxf to jms messages and vice a versa 
 */
final class JMSMessageUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(JMSMessageUtils.class);

    private JMSMessageUtils() {

    }
    public static org.apache.cxf.message.Message asCXFMessage(Message message, String headerType) 
        throws UnsupportedEncodingException, JMSException {
        org.apache.cxf.message.Message inMessage = new MessageImpl();
        populateIncomingContext(message, inMessage, headerType);
        retrieveAndSetPayload(inMessage, message);
        return inMessage;
    }
    
    /**
     * Extract the payload of an incoming JMS message
     * 
     * @param inMessage 
     * @param message the incoming message
     * @throws UnsupportedEncodingException
     * @throws JMSException 
     */
    public static void retrieveAndSetPayload(org.apache.cxf.message.Message inMessage, Message message)
        throws UnsupportedEncodingException, JMSException {
        String messageType = null;
        Object converted = new JMSMessageConverter().fromMessage(message);
        if (converted instanceof String) {
            inMessage.setContent(Reader.class, new StringReader((String)converted));
            messageType = JMSConstants.TEXT_MESSAGE_TYPE;
        } else if (converted instanceof byte[]) {
            inMessage.setContent(InputStream.class, new ByteArrayInputStream((byte[])converted));
            messageType = JMSConstants.BYTE_MESSAGE_TYPE;
        } else {
            messageType = "unknown";
        }
        Map<String, List<String>> headers = CastUtils.cast((Map<?, ?>)inMessage
            .get(org.apache.cxf.message.Message.PROTOCOL_HEADERS));
        if (headers == null) {
            headers = new TreeMap<String, List<String>>();
            inMessage.put(org.apache.cxf.message.Message.PROTOCOL_HEADERS, headers);
        }
        headers.put(JMSSpecConstants.JMS_MESSAGE_TYPE, Collections.singletonList(messageType));
    }
    
    private static void populateIncomingContext(javax.jms.Message message,
                                               org.apache.cxf.message.Message inMessage, String messageType)
        throws UnsupportedEncodingException, JMSException {
        JMSMessageHeadersType messageProperties = null;
        messageProperties = (JMSMessageHeadersType)inMessage.get(messageType);
        if (messageProperties == null) {
            messageProperties = new JMSMessageHeadersType();
            inMessage.put(messageType, messageProperties);
        }
        messageProperties.setJMSCorrelationID(message.getJMSCorrelationID());
        messageProperties.setJMSDeliveryMode(Integer.valueOf(message.getJMSDeliveryMode()));
        messageProperties.setJMSExpiration(Long.valueOf(message.getJMSExpiration()));
        messageProperties.setJMSMessageID(message.getJMSMessageID());
        messageProperties.setJMSPriority(Integer.valueOf(message.getJMSPriority()));
        messageProperties.setJMSRedelivered(Boolean.valueOf(message.getJMSRedelivered()));
        messageProperties.setJMSTimeStamp(Long.valueOf(message.getJMSTimestamp()));
        messageProperties.setJMSType(message.getJMSType());

        if (message.getJMSReplyTo() != null) {
            Destination replyTo = message.getJMSReplyTo();
            if (replyTo instanceof Queue) {
                messageProperties.setJMSReplyTo(((Queue)replyTo).getQueueName());
            } else if (replyTo instanceof Topic) {
                messageProperties.setJMSReplyTo(((Topic)replyTo).getTopicName());
            }
        }

        Map<String, List<String>> protHeaders = new TreeMap<String, List<String>>();
        List<JMSPropertyType> props = messageProperties.getProperty();
        Enumeration<String> enm = CastUtils.cast(message.getPropertyNames());
        while (enm.hasMoreElements()) {
            String name = enm.nextElement();
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
            if (name.equals(org.apache.cxf.message.Message.RESPONSE_CODE)) {
                inMessage.put(org.apache.cxf.message.Message.RESPONSE_CODE, Integer.valueOf(val));
            }
        }
        inMessage.put(org.apache.cxf.message.Message.PROTOCOL_HEADERS, protHeaders);

        populateIncomingMessageProperties(message, inMessage, messageProperties);
    }

    /**
     * @param jmsMessage
     * @param inMessage
     * @param messagePropertiesType
     * @throws UnsupportedEncodingException 
     * @throws JMSException 
     */
    private static void populateIncomingMessageProperties(Message jmsMessage,
                                                          org.apache.cxf.message.Message inMessage,
                                                          JMSMessageHeadersType messageProperties)
        throws UnsupportedEncodingException, JMSException {
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
        if (jmsMessage.propertyExists(JMSSpecConstants.CONTENTENCODING_FIELD)) {
            messageProperties.setSOAPJMSContentEncoding(jmsMessage
                .getStringProperty(JMSSpecConstants.CONTENTENCODING_FIELD));
        }
        if (jmsMessage.propertyExists(JMSSpecConstants.SOAPACTION_FIELD)) {
            messageProperties.setSOAPJMSSOAPAction(jmsMessage
                .getStringProperty(JMSSpecConstants.SOAPACTION_FIELD));
        }
        if (jmsMessage.propertyExists(JMSSpecConstants.ISFAULT_FIELD)) {
            messageProperties
                .setSOAPJMSIsFault(jmsMessage.getBooleanProperty(JMSSpecConstants.ISFAULT_FIELD));
        }
        if (jmsMessage.propertyExists(JMSSpecConstants.REQUESTURI_FIELD)) {
            messageProperties.setSOAPJMSRequestURI(jmsMessage
                .getStringProperty(JMSSpecConstants.REQUESTURI_FIELD));

            Map<String, List<String>> headers = CastUtils.cast((Map<?, ?>)inMessage
                .get(org.apache.cxf.message.Message.PROTOCOL_HEADERS));
            if (headers == null) {
                headers = new TreeMap<String, List<String>>();
                inMessage.put(org.apache.cxf.message.Message.PROTOCOL_HEADERS, headers);
            }
            try {
                String requestURI = jmsMessage.getStringProperty(JMSSpecConstants.REQUESTURI_FIELD);
                JMSEndpoint endpoint = new JMSEndpoint(requestURI);
                if (endpoint.getTargetService() != null) {
                    headers.put(JMSSpecConstants.TARGET_SERVICE_IN_REQUESTURI,
                                Collections.singletonList("true"));
                }
                if (requestURI != null) {
                    inMessage.put(org.apache.cxf.message.Message.REQUEST_URI, requestURI);
                }
            } catch (Exception e) {
                headers.put(JMSSpecConstants.MALFORMED_REQUESTURI, Collections.singletonList("true"));
            }
        }

        if (messageProperties.isSetSOAPJMSContentType()) {
            String contentType = messageProperties.getSOAPJMSContentType();
            inMessage.put(org.apache.cxf.message.Message.CONTENT_TYPE, contentType);
            // set the message encoding
            inMessage.put(org.apache.cxf.message.Message.ENCODING, getEncoding(contentType));
        }

    }

    /**
     * Extract the property JMSXUserID or JMS_TIBCO_SENDER from the jms message and 
     * create a SecurityContext from it. 
     * For more info see Jira Issue CXF-2055
     * {@link https://issues.apache.org/jira/browse/CXF-2055}
     * 
     * @param message jms message to retrieve user information from
     * @return SecurityContext that contains the user of the producer of the message as the Principal
     * @throws JMSException if something goes wrong
     */
    public static SecurityContext buildSecurityContext(javax.jms.Message message, 
                                                        JMSConfiguration config) throws JMSException {
        String tempUserName = message.getStringProperty("JMSXUserID");
        if (tempUserName == null && config.isJmsProviderTibcoEms()) {
            tempUserName = message.getStringProperty("JMS_TIBCO_SENDER");
        }
        if (tempUserName == null) {
            return null;
        }
        final String jmsUserName = tempUserName;

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

        String[] tokens = StringUtils.split(contentType, ";");
        for (String token : tokens) {
            int index = token.indexOf("charset=");
            if (index >= 0) {
                enc = token.substring(index + 8);
                break;
            }
        }

        String normalizedEncoding = HttpHeaderHelper.mapCharset(enc, StandardCharsets.UTF_8.name());
        if (normalizedEncoding == null) {
            String m = new org.apache.cxf.common.i18n.Message("INVALID_ENCODING_MSG", LOG, new Object[] {
                enc
            }).toString();
            LOG.log(Level.WARNING, m);
            throw new UnsupportedEncodingException(m);
        }

        return normalizedEncoding;
    }
    
    private static String getContentType(org.apache.cxf.message.Message message) {
        String contentType = (String)message.get(org.apache.cxf.message.Message.CONTENT_TYPE);
        String enc = (String)message.get(org.apache.cxf.message.Message.ENCODING);
        // add the encoding information
        if (null != contentType) {
            if (enc != null && contentType.indexOf("charset=") == -1
                && !contentType.toLowerCase().contains("multipart/related")) {
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
            headers = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
            message.put(org.apache.cxf.message.Message.PROTOCOL_HEADERS, headers);
        }
        return contentType;
    }
    
    private static String getContentEncoding(org.apache.cxf.message.Message message) {
        Map<String, List<String>> headers 
            = CastUtils.cast((Map<?, ?>)message.get(org.apache.cxf.message.Message.PROTOCOL_HEADERS));
        if (headers != null) {
            List<String> l = headers.get("Content-Encoding");
            if (l != null && !l.isEmpty()) {
                return l.get(0);
            }
        }
        return null;
    }

    public static Message asJMSMessage(JMSConfiguration jmsConfig,
                                       org.apache.cxf.message.Message outMessage,
                                       Object payload,
                                       String messageType,
                                       Session session,
                                       String correlationId, 
                                       String headerType)
        throws JMSException {

        Message jmsMessage = JMSUtil.createAndSetPayload(payload, session, messageType);
        JMSMessageHeadersType messageProperties = getOrCreateHeader(outMessage, headerType);
        JMSMessageUtils.prepareJMSMessageHeaderProperties(messageProperties, outMessage, jmsConfig);
        JMSMessageUtils.prepareJMSMessageProperties(messageProperties, outMessage, 
                                                    jmsConfig.getTargetService(), jmsConfig.getRequestURI());
        JMSMessageUtils.setJMSMessageProperties(jmsMessage, messageProperties);
        jmsMessage.setJMSCorrelationID(correlationId);
        return jmsMessage;
    }
    
    private static JMSMessageHeadersType getOrCreateHeader(org.apache.cxf.message.Message message, 
                                                           String headerName) {
        JMSMessageHeadersType messageProperties = (JMSMessageHeadersType)message
            .get(headerName);
        if (messageProperties == null) {
            messageProperties = new JMSMessageHeadersType();
            message.put(headerName, messageProperties);
        }
        return messageProperties;
    }

    /**
     * @param jmsMessage
     * @param messageProperties
     */
    static void setJMSMessageProperties(Message jmsMessage, JMSMessageHeadersType messageProperties)
        throws JMSException {

        if (messageProperties == null) {
            return;
        }

        setProp(jmsMessage, JMSSpecConstants.TARGETSERVICE_FIELD, messageProperties.getSOAPJMSTargetService());
        setProp(jmsMessage, JMSSpecConstants.BINDINGVERSION_FIELD, messageProperties.getSOAPJMSBindingVersion());
        setProp(jmsMessage, JMSSpecConstants.CONTENTTYPE_FIELD, messageProperties.getSOAPJMSContentType());
        setProp(jmsMessage, JMSSpecConstants.CONTENTENCODING_FIELD, messageProperties.getSOAPJMSContentEncoding());
        setProp(jmsMessage, JMSSpecConstants.SOAPACTION_FIELD, messageProperties.getSOAPJMSSOAPAction());
        setProp(jmsMessage, JMSSpecConstants.REQUESTURI_FIELD, messageProperties.getSOAPJMSRequestURI());

        if (messageProperties.isSetSOAPJMSIsFault()) {
            jmsMessage.setBooleanProperty(JMSSpecConstants.ISFAULT_FIELD, messageProperties
                .isSOAPJMSIsFault());
        }
        
        if (messageProperties.isSetProperty()) {
            for (JMSPropertyType prop : messageProperties.getProperty()) {
                jmsMessage.setStringProperty(prop.getName(), prop.getValue());
            }
        }
    }

    private static void setProp(Message jmsMessage, String name, String value) throws JMSException {
        if (value != null) {
            jmsMessage.setStringProperty(name, value);
        }
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
     * @param targetService TODO
     * @param requestURI TODO
     */
    private static void prepareJMSMessageProperties(JMSMessageHeadersType messageProperties,
                                                    org.apache.cxf.message.Message outMessage,
                                                    String targetService,
                                                    String requestURI) {
        
        // Retrieve or create protocol headers
        Map<String, List<String>> headers = CastUtils.cast((Map<?, ?>)outMessage
            .get(org.apache.cxf.message.Message.PROTOCOL_HEADERS));

        boolean isSoapMessage = 
            !MessageUtils.isTrue(outMessage.getExchange().get(org.apache.cxf.message.Message.REST_MESSAGE));
        
        if (isSoapMessage) {
            if (!messageProperties.isSetSOAPJMSTargetService()) {
                messageProperties.setSOAPJMSTargetService(targetService);
            }
            if (!messageProperties.isSetSOAPJMSBindingVersion()) {
                messageProperties.setSOAPJMSBindingVersion("1.0");
            }
            messageProperties.setSOAPJMSContentType(getContentType(outMessage));
            if (getContentEncoding(outMessage) != null) {
                messageProperties.setSOAPJMSContentEncoding(getContentEncoding(outMessage));
            }
            String soapAction = getSoapAction(messageProperties, outMessage, headers);
            if (soapAction != null) {
                messageProperties.setSOAPJMSSOAPAction(soapAction);
            }
            if (!messageProperties.isSetSOAPJMSIsFault()) {
                boolean isFault = outMessage.getContent(Exception.class) != null; 
                messageProperties.setSOAPJMSIsFault(isFault);
            }
            if (!messageProperties.isSetSOAPJMSRequestURI()) {
                messageProperties.setSOAPJMSRequestURI(requestURI);
            }
        } else {
            if (MessageUtils.isRequestor(outMessage)) {
                addJMSPropertiesFromMessage(messageProperties, 
                                            outMessage, 
                                            org.apache.cxf.message.Message.HTTP_REQUEST_METHOD,
                                            org.apache.cxf.message.Message.REQUEST_URI,
                                            org.apache.cxf.message.Message.ACCEPT_CONTENT_TYPE);
            } else {
                addJMSPropertyFromMessage(messageProperties, 
                                          outMessage, 
                                          org.apache.cxf.message.Message.RESPONSE_CODE);
            }
            addJMSPropertyFromMessage(messageProperties, 
                                      outMessage, 
                                      org.apache.cxf.message.Message.CONTENT_TYPE);
        }
        if (headers != null) {
            for (Map.Entry<String, List<String>> ent : headers.entrySet()) {
                JMSPropertyType prop = asJmsProperty(ent);
                messageProperties.getProperty().add(prop);
            }
        }
    }

    private static JMSPropertyType asJmsProperty(Map.Entry<String, List<String>> ent) {
        JMSPropertyType prop = new JMSPropertyType();
        prop.setName(ent.getKey());
        if (ent.getValue().size() > 1) {
            StringBuilder b = new StringBuilder();
            for (String s : ent.getValue()) {
                if (b.length() > 0) {
                    b.append(',');
                }
                b.append(s);
            }
            prop.setValue(b.toString());
        } else {
            prop.setValue(ent.getValue().get(0));
        }
        return prop;
    }

    private static String getSoapAction(JMSMessageHeadersType messageProperties,
                                        org.apache.cxf.message.Message outMessage,
                                        Map<String, List<String>> headers) {
        String soapAction = null;
        
        if (headers != null) {
            List<String> action = headers.remove("SOAPAction");
            if (action != null && action.size() > 0) {
                soapAction = action.get(0);
            }
        }
        
        if (soapAction == null) {
            soapAction = messageProperties.getSOAPJMSSOAPAction();
        }
        
        if (soapAction == null) {
            soapAction = extractActionFromSoap12(outMessage);
        }
        return soapAction;
    }

    private static void addJMSPropertiesFromMessage(JMSMessageHeadersType messageProperties,
                                                    org.apache.cxf.message.Message message, 
                                                    String... keys) {
        for (String key : keys) {
            addJMSPropertyFromMessage(messageProperties, message, key);
        }
        
    }
    
    private static void addJMSPropertyFromMessage(JMSMessageHeadersType messageProperties,
                                                  org.apache.cxf.message.Message message, 
                                                  String key) {
        Object value = message.get(key);
        if (value != null) {
            JMSPropertyType prop = new JMSPropertyType();
            prop.setName(key);
            prop.setValue(value.toString());
            messageProperties.getProperty().add(prop);
        }
    }

    public static String getMessageType(final javax.jms.Message request) {
        final String msgType;
        if (request instanceof TextMessage) {
            msgType = JMSConstants.TEXT_MESSAGE_TYPE;
        } else if (request instanceof BytesMessage) {
            msgType = JMSConstants.BYTE_MESSAGE_TYPE;
        } else {
            msgType = JMSConstants.BINARY_MESSAGE_TYPE;
        }
        return msgType;
    }

    private static String extractActionFromSoap12(org.apache.cxf.message.Message message) {
        String ct = (String) message.get(org.apache.cxf.message.Message.CONTENT_TYPE);
        
        if (ct == null) {
            return null;
        }
        
        int start = ct.indexOf("action=");
        if (start != -1) {
            int end;
            if (ct.charAt(start + 7) == '\"') {
                start += 8;
                end = ct.indexOf('\"', start);
            } else {
                start += 7;
                end = ct.indexOf(';', start);
                if (end == -1) {
                    end = ct.length();
                }
            }
            return ct.substring(start, end);
        }
        return null;
    }
    
    public static boolean isMtomEnabled(final org.apache.cxf.message.Message message) {
        return MessageUtils.isTrue(message.getContextualProperty(
                                                       org.apache.cxf.message.Message.MTOM_ENABLED));
    }
}
