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
import java.util.List;

//CHECKSTYLE:OFF
public class JMSMessageHeadersType {
    protected List<JMSPropertyType> property;
    protected String jmsCorrelationID;
    protected Integer jmsDeliveryMode;
    protected Long jmsExpiration;
    protected String jmsMessageID;
    protected Integer jmsPriority;
    protected Boolean jmsRedelivered;
    protected String jmsReplyTo;
    protected Long jmsTimeStamp;
    protected String jmsType;
    protected Long timeToLive;
    protected String soapjmsTargetService;
    protected String soapjmsBindingVersion;
    protected String soapjmsContentType;
    protected String soapjmsContentEncoding;
    protected String soapjmssoapAction;
    protected Boolean soapjmsIsFault;
    protected String soapjmsRequestURI;

    public List<JMSPropertyType> getProperty() {
        if (property == null) {
            property = new ArrayList<JMSPropertyType>();
        }
        return this.property;
    }

    public boolean isSetProperty() {
        return ((this.property != null) && (!this.property.isEmpty()));
    }

    public void unsetProperty() {
        this.property = null;
    }

    public String getJMSCorrelationID() {
        return jmsCorrelationID;
    }

    public void setJMSCorrelationID(String value) {
        this.jmsCorrelationID = value;
    }

    public boolean isSetJMSCorrelationID() {
        return (this.jmsCorrelationID != null);
    }

    public String getJMSMessageID() {
        return jmsMessageID;
    }

    public void setJMSMessageID(String value) {
        this.jmsMessageID = value;
    }

    public boolean isSetJMSMessageID() {
        return (this.jmsMessageID != null);
    }

    public String getJMSReplyTo() {
        return jmsReplyTo;
    }

    public void setJMSReplyTo(String value) {
        this.jmsReplyTo = value;
    }

    public boolean isSetJMSReplyTo() {
        return (this.jmsReplyTo != null);
    }

    public String getJMSType() {
        return jmsType;
    }

    public void setJMSType(String value) {
        this.jmsType = value;
    }

    public boolean isSetJMSType() {
        return (this.jmsType != null);
    }

    public String getSOAPJMSTargetService() {
        return soapjmsTargetService;
    }

    public void setSOAPJMSTargetService(String value) {
        this.soapjmsTargetService = value;
    }

    public boolean isSetSOAPJMSTargetService() {
        return (this.soapjmsTargetService != null);
    }

    public String getSOAPJMSBindingVersion() {
        return soapjmsBindingVersion;
    }

    public void setSOAPJMSBindingVersion(String value) {
        this.soapjmsBindingVersion = value;
    }

    public boolean isSetSOAPJMSBindingVersion() {
        return (this.soapjmsBindingVersion != null);
    }

    public String getSOAPJMSContentType() {
        return soapjmsContentType;
    }

    public void setSOAPJMSContentType(String value) {
        this.soapjmsContentType = value;
    }

    public boolean isSetSOAPJMSContentType() {
        return (this.soapjmsContentType != null);
    }

    public String getSOAPJMSContentEncoding() {
        return soapjmsContentEncoding;
    }

    public void setSOAPJMSContentEncoding(String value) {
        this.soapjmsContentEncoding = value;
    }

    public boolean isSetSOAPJMSContentEncoding() {
        return (this.soapjmsContentEncoding != null);
    }

    public String getSOAPJMSSOAPAction() {
        return soapjmssoapAction;
    }

    public void setSOAPJMSSOAPAction(String value) {
        this.soapjmssoapAction = value;
    }

    public boolean isSetSOAPJMSSOAPAction() {
        return (this.soapjmssoapAction != null);
    }

    public String getSOAPJMSRequestURI() {
        return soapjmsRequestURI;
    }

    public void setSOAPJMSRequestURI(String value) {
        this.soapjmsRequestURI = value;
    }

    public boolean isSetSOAPJMSRequestURI() {
        return (this.soapjmsRequestURI != null);
    }

    public void setJMSDeliveryMode(int value) {
        jmsDeliveryMode = value;
    }

    public void unsetJMSDeliveryMode() {
        jmsDeliveryMode = null;
    }

    public boolean isSetJMSDeliveryMode() {
        return (this.jmsDeliveryMode != null);
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
        return (this.jmsExpiration != null);
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
        return (this.jmsPriority != null);
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
        return (this.jmsRedelivered != null);
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
        return (this.jmsTimeStamp != null);
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
        return (this.timeToLive != null);
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
        return (this.soapjmsIsFault != null);
    }

    public boolean isSOAPJMSIsFault() {
        return soapjmsIsFault;
    }

}
//CHECKSTYLE:ON