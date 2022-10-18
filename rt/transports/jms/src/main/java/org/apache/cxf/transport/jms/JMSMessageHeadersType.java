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
import java.util.Map.Entry;
import java.util.Set;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.Topic;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.transport.jms.spec.JMSSpecConstants;

public class JMSMessageHeadersType {
    private Map<String, Object> properties;
    private String jmsCorrelationID;
    private Integer jmsDeliveryMode;
    private Long jmsExpiration;
    private String jmsMessageID;
    private Integer jmsPriority;
    private Boolean jmsRedelivered;
    private String jmsReplyTo;
    private Long jmsTimeStamp;
    private String jmsType;
    private Long timeToLive;
    private String soapjmsTargetService;
    private String soapjmsBindingVersion;
    private String soapjmsContentType;
    private String soapjmsContentEncoding;
    private String soapjmssoapAction;
    private Boolean soapjmsIsFault;
    private String soapjmsRequestURI;

    public JMSMessageHeadersType() {
        this.properties = new HashMap<>();
    }

    @Deprecated
    public List<JMSPropertyType> getProperty() {
        List<JMSPropertyType> props = new ArrayList<>();
        for (Entry<String, Object> entry : properties.entrySet()) {
            JMSPropertyType prop = new JMSPropertyType();
            prop.setName(entry.getKey());
            prop.setValue(entry.getValue());
            props.add(prop);
        }
        return Collections.unmodifiableList(props);
    }

    public void putProperty(String key, Object value) {
        properties.put(key, value);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public Set<String> getPropertyKeys() {
        return properties.keySet();
    }

    public String getJMSCorrelationID() {
        return jmsCorrelationID;
    }

    public void setJMSCorrelationID(String value) {
        this.jmsCorrelationID = value;
    }

    public boolean isSetJMSCorrelationID() {
        return this.jmsCorrelationID != null;
    }

    public String getJMSMessageID() {
        return jmsMessageID;
    }

    public void setJMSMessageID(String value) {
        this.jmsMessageID = value;
    }

    public boolean isSetJMSMessageID() {
        return this.jmsMessageID != null;
    }

    public String getJMSReplyTo() {
        return jmsReplyTo;
    }

    public void setJMSReplyTo(String value) {
        this.jmsReplyTo = value;
    }

    public boolean isSetJMSReplyTo() {
        return this.jmsReplyTo != null;
    }

    public String getJMSType() {
        return jmsType;
    }

    public void setJMSType(String value) {
        this.jmsType = value;
    }

    public boolean isSetJMSType() {
        return this.jmsType != null;
    }

    public String getSOAPJMSTargetService() {
        return soapjmsTargetService;
    }

    public void setSOAPJMSTargetService(String value) {
        this.soapjmsTargetService = value;
    }

    public boolean isSetSOAPJMSTargetService() {
        return this.soapjmsTargetService != null;
    }

    public String getSOAPJMSBindingVersion() {
        return soapjmsBindingVersion;
    }

    public void setSOAPJMSBindingVersion(String value) {
        this.soapjmsBindingVersion = value;
    }

    public boolean isSetSOAPJMSBindingVersion() {
        return this.soapjmsBindingVersion != null;
    }

    public String getSOAPJMSContentType() {
        return soapjmsContentType;
    }

    public void setSOAPJMSContentType(String value) {
        this.soapjmsContentType = value;
    }

    public boolean isSetSOAPJMSContentType() {
        return this.soapjmsContentType != null;
    }

    public String getSOAPJMSContentEncoding() {
        return soapjmsContentEncoding;
    }

    public void setSOAPJMSContentEncoding(String value) {
        this.soapjmsContentEncoding = value;
    }

    public boolean isSetSOAPJMSContentEncoding() {
        return this.soapjmsContentEncoding != null;
    }

    public String getSOAPJMSSOAPAction() {
        return soapjmssoapAction;
    }

    public void setSOAPJMSSOAPAction(String value) {
        this.soapjmssoapAction = value;
    }

    public boolean isSetSOAPJMSSOAPAction() {
        return this.soapjmssoapAction != null;
    }

    public String getSOAPJMSRequestURI() {
        return soapjmsRequestURI;
    }

    public void setSOAPJMSRequestURI(String value) {
        this.soapjmsRequestURI = value;
    }

    public boolean isSetSOAPJMSRequestURI() {
        return this.soapjmsRequestURI != null;
    }

    public void setJMSDeliveryMode(int value) {
        jmsDeliveryMode = value;
    }

    public void unsetJMSDeliveryMode() {
        jmsDeliveryMode = null;
    }

    public boolean isSetJMSDeliveryMode() {
        return this.jmsDeliveryMode != null;
    }

    public int getJMSDeliveryMode() {
        return jmsDeliveryMode;
    }

    public void setJMSExpiration(long value) {
        jmsExpiration = value;
    }

    public void unsetJMSExpiration() {
        jmsExpiration = null;
    }

    public boolean isSetJMSExpiration() {
        return this.jmsExpiration != null;
    }

    public long getJMSExpiration() {
        return jmsExpiration;
    }

    public void setJMSPriority(int value) {
        jmsPriority = value;
    }

    public void unsetJMSPriority() {
        jmsPriority = null;
    }

    public boolean isSetJMSPriority() {
        return this.jmsPriority != null;
    }

    public int getJMSPriority() {
        return jmsPriority;
    }

    public void setJMSRedelivered(boolean value) {
        jmsRedelivered = value;
    }

    public void unsetJMSRedelivered() {
        jmsRedelivered = null;
    }

    public boolean isSetJMSRedelivered() {
        return this.jmsRedelivered != null;
    }

    public boolean isJMSRedelivered() {
        return jmsRedelivered;
    }

    public void setJMSTimeStamp(long value) {
        jmsTimeStamp = value;
    }

    public void unsetJMSTimeStamp() {
        jmsTimeStamp = null;
    }

    public boolean isSetJMSTimeStamp() {
        return this.jmsTimeStamp != null;
    }

    public long getJMSTimeStamp() {
        return jmsTimeStamp;
    }

    public void setTimeToLive(long value) {
        timeToLive = value;
    }

    public void unsetTimeToLive() {
        timeToLive = null;
    }

    public boolean isSetTimeToLive() {
        return this.timeToLive != null;
    }

    public long getTimeToLive() {
        return timeToLive;
    }

    public void setSOAPJMSIsFault(boolean value) {
        soapjmsIsFault = value;
    }

    public void unsetSOAPJMSIsFault() {
        soapjmsIsFault = null;
    }

    public boolean isSetSOAPJMSIsFault() {
        return this.soapjmsIsFault != null;
    }

    public boolean isSOAPJMSIsFault() {
        return soapjmsIsFault;
    }

    public String getContentType() {
        String contentType = getSOAPJMSContentType();
        if (contentType == null) {
            contentType = (String)getProperty(JMSSpecConstants.CONTENTTYPE_FIELD);
        }
        if (contentType == null) {
            contentType = (String)getProperty(JMSConstants.RS_CONTENT_TYPE);
        }
        if (contentType == null) {
            contentType = (String)getProperty(org.apache.cxf.message.Message.CONTENT_TYPE);
        }
        return contentType;
    }

    public static JMSMessageHeadersType from(Message message) throws JMSException {
        JMSMessageHeadersType messageHeaders = new JMSMessageHeadersType();
        messageHeaders.read(message);
        return messageHeaders;
    }

    private void read(Message message) throws JMSException {
        setJMSCorrelationID(message.getJMSCorrelationID());
        setJMSDeliveryMode(Integer.valueOf(message.getJMSDeliveryMode()));
        setJMSExpiration(Long.valueOf(message.getJMSExpiration()));
        setJMSMessageID(message.getJMSMessageID());
        setJMSPriority(Integer.valueOf(message.getJMSPriority()));
        setJMSRedelivered(Boolean.valueOf(message.getJMSRedelivered()));
        setJMSTimeStamp(Long.valueOf(message.getJMSTimestamp()));
        setJMSType(message.getJMSType());
        setSOAPJMSTargetService(message.getStringProperty(JMSSpecConstants.TARGETSERVICE_FIELD));
        setSOAPJMSBindingVersion(message.getStringProperty(JMSSpecConstants.BINDINGVERSION_FIELD));
        setSOAPJMSContentType(message.getStringProperty(JMSSpecConstants.CONTENTTYPE_FIELD));
        setSOAPJMSContentEncoding(message.getStringProperty(JMSSpecConstants.CONTENTENCODING_FIELD));
        setSOAPJMSSOAPAction(message.getStringProperty(JMSSpecConstants.SOAPACTION_FIELD));
        if (message.propertyExists(JMSSpecConstants.ISFAULT_FIELD)) {
            setSOAPJMSIsFault(message.getBooleanProperty(JMSSpecConstants.ISFAULT_FIELD));
        }
        setSOAPJMSRequestURI(message.getStringProperty(JMSSpecConstants.REQUESTURI_FIELD));

        setJMSReplyTo(getDestName(message));
        readProperties(message);
    }

    private String getDestName(Message message) throws JMSException {
        Destination replyTo = message.getJMSReplyTo();
        if (replyTo instanceof Queue) {
            return ((Queue)replyTo).getQueueName();
        } else if (replyTo instanceof Topic) {
            return ((Topic)replyTo).getTopicName();
        }
        return null;
    }

    private void readProperties(Message message) throws JMSException {
        Enumeration<String> enm = CastUtils.cast(message.getPropertyNames());
        while (enm.hasMoreElements()) {
            String name = enm.nextElement();
            String val = message.getStringProperty(name);
            String unescapedName = name.replace("_$_", "-").replace("__", ".");
            putProperty(unescapedName, val);
        }
    }

    public void writeProp(Message jmsMessage, String origName, Object value) throws JMSException {
        String name = origName.replace(".", "__").replace("-", "_$_");
        if (value == null) {
            jmsMessage.setStringProperty(name, null);
            return;
        }
        Class<?> cls = value.getClass();
        if (cls == String.class) {
            jmsMessage.setStringProperty(name, (String)value);
        } else if (cls == Integer.TYPE || cls == Integer.class) {
            jmsMessage.setIntProperty(name, (Integer)value);
        } else if (cls == Double.TYPE || cls == Double.class) {
            jmsMessage.setDoubleProperty(name, (Double)value);
        } else if (cls == Float.TYPE || cls == Float.class) {
            jmsMessage.setFloatProperty(name, (Float)value);
        } else if (cls == Long.TYPE || cls == Long.class) {
            jmsMessage.setLongProperty(name, (Long)value);
        } else if (cls == Boolean.TYPE || cls == Boolean.class) {
            jmsMessage.setBooleanProperty(name, (Boolean)value);
        } else if (cls == Short.TYPE || cls == Short.class) {
            jmsMessage.setShortProperty(name, (Short)value);
        } else if (cls == Byte.TYPE || cls == Byte.class) {
            jmsMessage.setShortProperty(name, (Byte)value);
        } else {
            jmsMessage.setObjectProperty(name, value);
        }
    }

    public void writeTo(Message jmsMessage)
        throws JMSException {

        setProp(jmsMessage, JMSSpecConstants.TARGETSERVICE_FIELD, soapjmsTargetService);
        setProp(jmsMessage, JMSSpecConstants.BINDINGVERSION_FIELD, soapjmsBindingVersion);
        setProp(jmsMessage, JMSSpecConstants.CONTENTTYPE_FIELD, soapjmsContentType);
        setProp(jmsMessage, JMSSpecConstants.CONTENTENCODING_FIELD, soapjmsContentEncoding);
        setProp(jmsMessage, JMSSpecConstants.SOAPACTION_FIELD, soapjmssoapAction);
        setProp(jmsMessage, JMSSpecConstants.REQUESTURI_FIELD, soapjmsRequestURI);

        if (isSetSOAPJMSIsFault()) {
            jmsMessage.setBooleanProperty(JMSSpecConstants.ISFAULT_FIELD, isSOAPJMSIsFault());
        }

        for (Entry<String, Object> entry : properties.entrySet()) {
            writeProp(jmsMessage, entry.getKey(), entry.getValue());
        }
    }

    private void setProp(Message jmsMessage, String name, String value) throws JMSException {
        if (value != null) {
            jmsMessage.setStringProperty(name, value);
        }
    }



}
