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
package org.apache.cxf.testutil.handler;

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;


public class TrivialSOAPHandler implements SOAPHandler<SOAPMessageContext> {
    
    @Resource(name = "greeting")
    private String greeting;
    
    public TrivialSOAPHandler() {
        System.out.println(this + " Construct");
    }

    @PostConstruct
    public void init() {
        System.out.println(this + " PostConstruct");
    }

    @PreDestroy
    public void destroy() {
        System.out.println(this + " PreDestroy");
    }

    public boolean handleMessage(SOAPMessageContext smc) {
        System.out.println(this + " handleMessage(): " + greeting);
        return true;
    }
       
    public boolean handleFault(SOAPMessageContext smc) {
        System.out.println(this + " handleFault()");
        return true;
    }
    
    public void close(MessageContext messageContext) {
        System.out.println(this + " close()");
    }
    
    public Set<QName> getHeaders() {
        return null;
    }
    
}
