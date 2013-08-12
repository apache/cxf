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

package org.apache.cxf.service.model;

import java.util.List;

public class BindingMessageInfo extends AbstractPropertiesHolder {

    MessageInfo msg;
    BindingOperationInfo op;
    private List<MessagePartInfo> messageParts;
    
    public BindingMessageInfo() {
    }

    protected BindingMessageInfo(MessageInfo m, BindingOperationInfo boi) {
        op = boi;
        msg = m;
        messageParts = msg.getMessageParts();
    }
    
    public MessageInfo getMessageInfo() {
        return msg;
    }
    
    public BindingOperationInfo getBindingOperation() {
        return op;
    }

    public List<MessagePartInfo> getMessageParts() {
        return messageParts;
    }

    public void setMessageParts(List<MessagePartInfo> messageParts) {
        this.messageParts = messageParts;
    }
}
