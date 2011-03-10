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

package org.apache.cxf.systest.ws.security;

import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class TestOutHandler implements SOAPHandler<SOAPMessageContext> {
    boolean handleFaultCalledOutbound;
    boolean handleMessageCalledOutbound;

    public Set<QName> getHeaders() {
        return null;
    }

    public void close(MessageContext mc) {
    }

    public boolean handleFault(SOAPMessageContext smc) {
        if (isOutbound(smc)) {
            handleFaultCalledOutbound = true;
        }
        return true;
    }

    public boolean handleMessage(SOAPMessageContext smc) {
        if (isOutbound(smc)) {
            handleMessageCalledOutbound = true;
        }
        return true;
    }

    private static boolean isOutbound(SOAPMessageContext smc) {
        return smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY) != null 
               && ((Boolean)smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY)).booleanValue();
    }
 
}
