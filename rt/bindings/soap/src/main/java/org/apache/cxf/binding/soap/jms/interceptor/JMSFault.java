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

package org.apache.cxf.binding.soap.jms.interceptor;

import javax.xml.namespace.QName;

/**
 * 
 */
public class JMSFault extends Exception {

    private JMSFaultType jmsFaultType;
    private boolean sender;
    private Object detail;

    public JMSFault(String message) {
        super(message);
    }

    public JMSFault(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * * @return Returns the jmsFaultType.
     */
    public JMSFaultType getJmsFaultType() {
        return jmsFaultType;
    }

    /**
     * @param jmsFaultType The jmsFaultType to set.
     */
    public void setJmsFaultType(JMSFaultType jmsFaultType) {
        this.jmsFaultType = jmsFaultType;
    }

    /**
     * * @return Returns the sender.
     */
    public boolean isSender() {
        return sender;
    }

    /**
     * @param sender The sender to set.
     */
    public void setSender(boolean sender) {
        this.sender = sender;
    }

    /**
     * * @return Returns the detail.
     */
    public Object getDetail() {
        return detail;
    }

    /**
     * @param detail The detail to set.
     */
    public void setDetail(Object detail) {
        this.detail = detail;
    }

    public QName getSubCode() {
        return jmsFaultType.faultCode;
    }

    public String getReason() {
        return getMessage();
    }
}
