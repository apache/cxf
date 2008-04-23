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

package org.apache.cxf.binding.soap.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.service.model.MessagePartInfo;

public class SoapBodyInfo {
    private List <MessagePartInfo> parts = new ArrayList<MessagePartInfo>();
    private List <MessagePartInfo> attachments = new ArrayList<MessagePartInfo>();
    private String use;
    
    public List<MessagePartInfo> getParts() {
        return parts;
    }

    public void setParts(List<MessagePartInfo> parts) {
        this.parts = parts;
    }

    public String getUse() {
        return use;
    }

    public void setUse(String use) {
        this.use = use;
    }

    public List<MessagePartInfo> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<MessagePartInfo> attachments) {
        this.attachments = attachments;
    }
    
}
