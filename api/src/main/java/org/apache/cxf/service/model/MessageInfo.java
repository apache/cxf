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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.common.util.StringUtils;

/**
 * A message for an operation.
 */
public class MessageInfo extends AbstractMessageContainer {
    
    public static enum Type {
        INPUT,
        OUTPUT;
    }
    
    private Type type;
    
    public MessageInfo(OperationInfo op, Type type, QName nm) {
        super(op, nm);
        this.type = type;
    }
    
    public void setName(QName qn) {
        mName = qn;
    }
    
    public Map<QName, MessagePartInfo> getMessagePartsMap() {
        Map<QName, MessagePartInfo> partsMap = new HashMap<QName, MessagePartInfo>();
        for (MessagePartInfo part : getMessageParts()) {
            partsMap.put(part.getName(), part);
        }
        return partsMap;
    }

    public List<MessagePartInfo> getOrderedParts(List<String> order) {  
        if (StringUtils.isEmpty(order)) {
            return getMessageParts();
        }
        
        List<MessagePartInfo> orderedParts = new ArrayList<MessagePartInfo>();
        Map<QName, MessagePartInfo> partsMap = getMessagePartsMap();
        for (String part : order) {
            QName qname = getMessagePartQName(part);
            orderedParts.add(partsMap.get(qname));
        }
        return orderedParts;
    }

    @Override
    public String toString() {
        return "[MessageInfo " + type + mName.toString() + "]";
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
    
    
}
