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

package org.apache.cxf.ws.rm;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;

/**
 * Holder for SequenceFault information.
 */
public class SequenceFault extends Exception {
    
    private boolean sender;
    private QName faultCode;
    private Object detail;
    private Element extraDetail;

    public SequenceFault(String message) {
        super(message);
    }
    
    public boolean isSender() {
        return sender;
    }
    
    public void setSender(boolean s) {
        sender = s;
    }
    
    public String getReason() {
        return getMessage();
    }
    
    public QName getFaultCode() {
        return faultCode;
    }

    public void setFaultCode(QName faultCode) {
        this.faultCode = faultCode;
    }

    /**
     * Set detail content as arbitrary element.
     * 
     * @param d (<code>null</code> if none)
     */
    public void setDetail(Element d) {
        detail = d;
    }

    /**
     * Set detail content as Identifier.
     * 
     * @param d (<code>null</code> if none)
     */
    public void setDetail(Identifier d) {
        detail = d;
    }

    /**
     * Set detail content as SequenceAcknowledgement.
     * 
     * @param d (<code>null</code> if none)
     */
    public void setDetail(SequenceAcknowledgement d) {
        detail = d;
    }
    
    /**
     * Get Fault detail object, which may be an {@link Element}, an {@link Identifier}, or a
     * {@link SequenceAcknowledgement}.
     * 
     * @return detail object (<code>null</code> if none)
     */
    public Object getDetail() {
        return detail;
    }

    /**
     * Get extra element appended to main fault detail.
     * 
     * @return element (<code>null</code> if none)
     */
    public Element getExtraDetail() {
        return extraDetail;
    }

    /**
     * Set extra element appended to main fault detail.
     * 
     * @param ex (<code>null</code> if none)
     */
    public void setExtraDetail(Element ex) {
        extraDetail = ex;
    }
}
