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
package org.apache.cxf.systest.handlers;

import java.util.Map;

import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;


public class TestUnusedHandler<T extends LogicalMessageContext> 
    extends TestHandlerBase implements LogicalHandler<T> {
    
    public TestUnusedHandler() {
        this(true); 
    } 
    
    public TestUnusedHandler(boolean serverSide) {
        super(serverSide);
    }
    
    public String getHandlerId() { 
        return "handler" + getId();
    } 

    public boolean handleMessage(T ctx) {
        throw new RuntimeException("should not be called");
    }
    
    public boolean handleFault(LogicalMessageContext ctx) {
        return true;
    }

    public void close(MessageContext arg0) {
        methodCalled("close");
    }

    public void init(Map arg0) {
        methodCalled("init");
    }

    public void destroy() {
        methodCalled("destroy");
    }

    public String toString() { 
        return getHandlerId(); 
    } 
}    
