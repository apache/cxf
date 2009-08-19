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

package org.apache.cxf.binding.soap.tcp;

public class SoapTcpSessionState {
    public static final byte SOAP_TCP_SESSION_STATE_NEW = 0;
    public static final byte SOAP_TCP_SESSION_STATE_AFTER_HANDSHAKE = 1;
    public static final byte SOAP_TCP_SESSION_STATE_INITIATED = 2;
    
    private byte stateId;
    
    public SoapTcpSessionState() {
        stateId = SOAP_TCP_SESSION_STATE_NEW;
    }

    public byte getStateId() {
        return stateId;
    }

    public void setStateId(byte stateId) {
        this.stateId = stateId;
    }
    
    
}
