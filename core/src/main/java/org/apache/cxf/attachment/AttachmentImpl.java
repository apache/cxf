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

package org.apache.cxf.attachment;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.activation.DataHandler;

import org.apache.cxf.message.Attachment;

public class AttachmentImpl implements Attachment {

    private DataHandler dataHandler;
    private String id;
    private Map<String, String> headers = new HashMap<>();
    private boolean xop;

    public AttachmentImpl(String idParam) {
        this.id = idParam;
    }

    public AttachmentImpl(String idParam, DataHandler handlerParam) {
        this.id = idParam;
        this.dataHandler = handlerParam;
        this.dataHandler.setCommandMap(AttachmentUtil.getCommandMap());
    }

    public String getId() {
        return id;
    }

    public DataHandler getDataHandler() {
        return dataHandler;
    }

    public void setDataHandler(DataHandler dataHandler) {
        this.dataHandler = dataHandler;
        this.dataHandler.setCommandMap(AttachmentUtil.getCommandMap());
    }

    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    public String getHeader(String name) {
        String value = headers.get(name);
        return value == null ? headers.get(name.toLowerCase()) : value;
    }

    public Iterator<String> getHeaderNames() {
        return headers.keySet().iterator();
    }

    public boolean isXOP() {
        return xop;
    }

    public void setXOP(boolean xopParam) {
        this.xop = xopParam;
    }

}
