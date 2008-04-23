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

/**
 * 
 */

public class SequenceFault extends Exception {
    
    private SequenceFaultType sequenceFault;
    private boolean sender;
    private Object detail;

    public SequenceFault(String message) {
        super(message);
    }

    public SequenceFault(String message, Throwable cause) {
        super(message, cause);
    }

    public SequenceFaultType getSequenceFault() {
        return sequenceFault;
    }
    
    public void setSequenceFault(SequenceFaultType sf) {
        sequenceFault = sf;
    }
    
    public boolean isSender() {
        return sender;
    }
    
    public void setSender(boolean s) {
        sender = s;
    }
    
    public QName getSubCode() {
        return sequenceFault.faultCode;
    }
    
    public String getReason() {
        return getMessage();
    }
    
    public void setDetail(Object d) {
        detail = d;
    }
    
    public Object getDetail() {
        return detail;
    }
}
