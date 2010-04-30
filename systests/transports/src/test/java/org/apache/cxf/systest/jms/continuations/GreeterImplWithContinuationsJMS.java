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
package org.apache.cxf.systest.jms.continuations;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.hello_world_jms.types.TestRpcLitFaultResponse;

import org.apache.cxf.systest.jms.TwoWayJMSImplBase;

@WebService(serviceName = "HelloWorldService", 
            portName = "HelloWorldPort",
            endpointInterface = "org.apache.cxf.hello_world_jms.HelloWorldPortType",
            targetNamespace = "http://cxf.apache.org/hello_world_jms",
            wsdlLocation = "testutils/jms_test.wsdl")
public class GreeterImplWithContinuationsJMS extends TwoWayJMSImplBase {    
    
    @Resource
    protected WebServiceContext context;
    private volatile boolean suspended; 
    
    public void greetMeOneWay(String name) {
        throw new UnsupportedOperationException();
    }
    
    public String sayHi() {
        throw new UnsupportedOperationException();
    }
    
    public TestRpcLitFaultResponse testRpcLitFault(String faultType) {
        throw new UnsupportedOperationException();
    }
    
    public String greetMe(String name) {
        
        Continuation continuation = getContinuation(name);
        if (continuation == null) {
            throw new RuntimeException("Failed to get continuation");
        }
        synchronized (continuation) {
            if (continuation.isNew()) {
                if (suspended) {
                    throw new RuntimeException("Was already suspended");
                }
                Object userObject = "Fred".equals(name) ? "Ruby" : null;
                continuation.setObject(userObject);
                suspended = true;
                continuation.suspend(2000);
            } else {
                if (!suspended) {
                    throw new RuntimeException("Was not suspended yet");
                }
                if (continuation.isResumed()) {
                    throw new RuntimeException("It must be a timeout");
                }
                StringBuilder sb = new StringBuilder();
                sb.append(name);
                
                Object userObject = continuation.getObject();
                if (userObject != null) {
                    sb.append(' ').append(userObject.toString());
                }
                System.out.println("Saying hi to " + sb.toString());
                return "Hi " + sb.toString();
            }
        }
        // unreachable
        return null;        
    }
    
    private Continuation getContinuation(String name) {
        
        ContinuationProvider provider = 
            (ContinuationProvider)context.getMessageContext().get(ContinuationProvider.class.getName());
        return provider.getContinuation();
    }
}
