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
package org.apache.cxf.systest.http_jetty.continuations;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;


@WebService(name = "HelloContinuation", 
            serviceName = "HelloContinuationService", 
            portName = "HelloContinuationPort", 
            targetNamespace = "http://cxf.apache.org/systest/jaxws",
            endpointInterface = "org.apache.cxf.systest.http_jetty.continuations.HelloContinuation")
public class HelloImplWithWrapppedContinuation implements HelloContinuation {
    
    
    private Map<String, Continuation> suspended = 
        new HashMap<String, Continuation>();
    
    
    @Resource
    private WebServiceContext context;
    
    public String sayHi(String firstName, String secondName) {
        
        Continuation continuation = getContinuation(firstName);
        if (continuation == null) {
            throw new RuntimeException("Failed to get continuation");
        }
        synchronized (continuation) {
            if (continuation.isNew()) {
                Object userObject = secondName != null && secondName.length() > 0 
                                    ? secondName : null;
                continuation.setObject(userObject);
                long timeout = 20000;
                if (secondName.startsWith("to:")) {
                    timeout = Long.parseLong(secondName.substring(3));
                }
                suspendInvocation(firstName, continuation, timeout);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(firstName);
                
                // if the actual parameter is not null 
                if (secondName != null && secondName.length() > 0) {
                    String surname = continuation.getObject().toString();
                    sb.append(' ').append(surname);
                }
                System.out.println("Saying hi to " + sb.toString());
                return "Hi " + sb.toString();
            }
        }
        // unreachable
        return null;
    }

    public boolean isRequestSuspended(String name) {
        synchronized (suspended) {
            while (!suspended.containsKey(name)) {
                try {
                    suspended.wait(10000);
                } catch (InterruptedException ex) {
                    return false;
                }
            }
        }
        return true;
    }

    public void resumeRequest(final String name) {
        
        Continuation suspendedCont = null;
        synchronized (suspended) {
            suspendedCont = suspended.get(name);
        }
        
        if (suspendedCont != null) {
            synchronized (suspendedCont) {
                suspendedCont.resume();
            }
        }
    }
    
    private void suspendInvocation(String name, Continuation cont, long timeout) {
        try {
            cont.suspend(timeout);    
        } finally {
            synchronized (suspended) {
                suspended.put(name, cont);
                suspended.notifyAll();
            }
        }
    }
    
    private Continuation getContinuation(String name) {
        synchronized (suspended) {
            Continuation suspendedCont = suspended.remove(name);
            if (suspendedCont != null) {
                return suspendedCont;
            }
        }
        
        ContinuationProvider provider = 
            (ContinuationProvider)context.getMessageContext().get(ContinuationProvider.class.getName());
        return provider.getContinuation();
    }
}
