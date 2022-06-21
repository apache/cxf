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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
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
    public static org.apache.cxf.message.Message asCXFMessage(Message message, String jmsHeadersKey)
        throws UnsupportedEncodingException, JMSException {
        org.apache.cxf.message.Message inMessage = new MessageImpl();
        JMSMessageHeadersType messageHeaders = JMSMessageHeadersType.from(message);
        inMessage.put(jmsHeadersKey, messageHeaders);
        populateIncomingContext(messageHeaders, inMessage);
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
    private static void retrieveAndSetPayload(org.apache.cxf.message.Message inMessage, Message message)
        throws UnsupportedEncodingException, JMSException {
        final String messageType;
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
        headers.put(JMSConstants.JMS_MESSAGE_TYPE, Collections.singletonList(messageType));
    }

    private static void populateIncomingContext(JMSMessageHeadersType messageHeaders,
                                               org.apache.cxf.message.Message inMessage)
                                                   throws UnsupportedEncodingException {
        String contentType = messageHeaders.getContentType();
        if (contentType != null) {
            inMessage.put(org.apache.cxf.message.Message.CONTENT_TYPE, contentType);
            inMessage.put(org.apache.cxf.message.Message.ENCODING, getEncoding(contentType));
        }
        String responseCode = (String)messageHeaders.getProperty(org.apache.cxf.message.Message.RESPONSE_CODE);
        if (responseCode != null) {
            inMessage.put(org.apache.cxf.message.Message.RESPONSE_CODE, Integer.valueOf(responseCode));
        }
        Map<String, List<String>> protHeaders = new TreeMap<>();
        for (String name : messageHeaders.getPropertyKeys()) {
            String val = (String)messageHeaders.getProperty(name);
            protHeaders.put(name, Collections.singletonList(val));
        }
        String requestURI = messageHeaders.getSOAPJMSRequestURI();
        if (requestURI != null) {
            try {
                JMSEndpoint endpoint = new JMSEndpoint(requestURI);
                if (endpoint.getTargetService() != null) {
                    protHeaders.put(JMSConstants.TARGET_SERVICE_IN_REQUESTURI,
                                    Collections.singletonList("true"));
                }
                inMessage.put(org.apache.cxf.message.Message.REQUEST_URI, requestURI);
            } catch (Exception e) {
                protHeaders.put(JMSConstants.MALFORMED_REQUESTURI, Collections.singletonList("true"));
            }
        }
        inMessage.put(org.apache.cxf.message.Message.PROTOCOL_HEADERS, protHeaders);
    }


    static String getEncoding(String ct) throws UnsupportedEncodingException {
        String enc = HttpHeaderHelper.findCharset(ct);
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
            headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
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
        JMSMessageHeadersType messageHeaders = getOrCreateHeader(outMessage, headerType);
        if (!messageHeaders.isSetJMSDeliveryMode()) {
            messageHeaders.setJMSDeliveryMode(jmsConfig.getDeliveryMode());
        }
        if (!messageHeaders.isSetTimeToLive()) {
            messageHeaders.setTimeToLive(jmsConfig.getTimeToLive());
        }
        if (!messageHeaders.isSetJMSPriority()) {
            messageHeaders.setJMSPriority(jmsConfig.getPriority());
        }
        // Retrieve or create protocol headers
        Map<String, List<String>> headers = CastUtils.cast((Map<?, ?>)outMessage
            .get(org.apache.cxf.message.Message.PROTOCOL_HEADERS));

        boolean isSoapMessage =
            !PropertyUtils.isTrue(outMessage.getExchange().get(org.apache.cxf.message.Message.REST_MESSAGE));

        if (isSoapMessage) {
            if (!messageHeaders.isSetSOAPJMSTargetService()) {
                messageHeaders.setSOAPJMSTargetService(jmsConfig.getTargetService());
            }
            if (!messageHeaders.isSetSOAPJMSBindingVersion()) {
                messageHeaders.setSOAPJMSBindingVersion("1.0");
            }
            messageHeaders.setSOAPJMSContentType(JMSMessageUtils.getContentType(outMessage));
            if (JMSMessageUtils.getContentEncoding(outMessage) != null) {
                messageHeaders.setSOAPJMSContentEncoding(JMSMessageUtils.getContentEncoding(outMessage));
            }
            String soapAction = JMSMessageUtils.getSoapAction(messageHeaders, outMessage, headers);
            if (soapAction != null) {
                messageHeaders.setSOAPJMSSOAPAction(soapAction);
            }
            if (!messageHeaders.isSetSOAPJMSIsFault()) {
                boolean isFault = outMessage.getContent(Exception.class) != null;
                messageHeaders.setSOAPJMSIsFault(isFault);
            }
            if (!messageHeaders.isSetSOAPJMSRequestURI()) {
                messageHeaders.setSOAPJMSRequestURI(jmsConfig.getRequestURI());
            }
        } else {
            if (MessageUtils.isRequestor(outMessage)) {
                addJMSPropertiesFromMessage(messageHeaders,
                                            outMessage,
                                            org.apache.cxf.message.Message.HTTP_REQUEST_METHOD,
                                            org.apache.cxf.message.Message.REQUEST_URI,
                                            org.apache.cxf.message.Message.ACCEPT_CONTENT_TYPE);
            } else {
                addJMSPropertyFromMessage(messageHeaders,
                                          outMessage,
                                          org.apache.cxf.message.Message.RESPONSE_CODE);
            }
            String contentType = (String)outMessage.remove(org.apache.cxf.message.Message.CONTENT_TYPE);
            if (contentType != null) {
                outMessage.put(JMSConstants.RS_CONTENT_TYPE, contentType);
            }
            addJMSPropertyFromMessage(messageHeaders,
                                      outMessage,
                                      JMSConstants.RS_CONTENT_TYPE);
            
            String contentLength = (String)outMessage.remove(HttpHeaderHelper.CONTENT_LENGTH);
            if (contentLength != null) {
                outMessage.put(JMSConstants.RS_CONTENT_LENGTH, contentLength);
            }
            addJMSPropertyFromMessage(messageHeaders,
                                      outMessage,
                                      JMSConstants.RS_CONTENT_LENGTH);
        }
        if (headers != null) {
            for (Map.Entry<String, List<String>> ent : headers.entrySet()) {
                if (!ent.getKey().equals(org.apache.cxf.message.Message.CONTENT_TYPE)
                    && !ent.getKey().equals(HttpHeaderHelper.CONTENT_LENGTH)) {
                    messageHeaders.putProperty(ent.getKey(), String.join(",", ent.getValue()));
                }
            }
        }
        messageHeaders.writeTo(jmsMessage);
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

    private static String getSoapAction(JMSMessageHeadersType messageProperties,
                                        org.apache.cxf.message.Message outMessage,
                                        Map<String, List<String>> headers) {
        String soapAction = null;

        if (headers != null) {
            List<String> action = headers.remove("SOAPAction");
            if (action != null && !action.isEmpty()) {
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
            messageProperties.putProperty(key, value.toString());
        }
    }

    public static String getMessageType(final jakarta.jms.Message request) {
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
        return MessageUtils.getContextualBoolean(message, org.apache.cxf.message.Message.MTOM_ENABLED);
    }
}
