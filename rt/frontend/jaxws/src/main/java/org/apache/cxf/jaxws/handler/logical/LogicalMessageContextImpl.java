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

package org.apache.cxf.jaxws.handler.logical;

import java.util.Map;

import javax.xml.ws.LogicalMessage;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Message;


public class LogicalMessageContextImpl extends WrappedMessageContext implements LogicalMessageContext {

    public LogicalMessageContextImpl(Message wrapped) {
        super(wrapped, Scope.HANDLER);
    }
      
    public LogicalMessage getMessage() {
        return new LogicalMessageImpl(this);
    }
    
    public Object get(Object key) {
        Object o = super.get(key);
        if (MessageContext.HTTP_RESPONSE_HEADERS.equals(key)
            || MessageContext.HTTP_REQUEST_HEADERS.equals(key)) {
            Map mp = (Map)o;
            if (mp != null) {
                if (mp.isEmpty()) {
                    return null;
                }
                if (!isResponse() && MessageContext.HTTP_RESPONSE_HEADERS.equals(key)) {
                    return null;
                }
                if (isRequestor() && MessageContext.HTTP_REQUEST_HEADERS.equals(key)) {
                    return null;
                }
            }
        }
        return o;
    }

}
