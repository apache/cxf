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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.systest.http_jetty.continuations.HelloContinuation;



@WebService(name = "HelloContinuation", 
            serviceName = "HelloContinuationService", 
            portName = "HelloContinuationPort", 
            targetNamespace = "http://cxf.apache.org/systest/jaxws",
            endpointInterface = "org.apache.cxf.systest.http_jetty.continuations.HelloContinuation",
            wsdlLocation = "org/apache/cxf/systest/jms/continuations/test2.wsdl")
public class HelloWorldWithContinuationsJMS2 implements HelloContinuation {    
    
    private Map<String, Continuation> suspended = 
        new HashMap<String, Continuation>();
    private Executor executor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS,
                                        new ArrayBlockingQueue<Runnable>(10));
    
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
                suspendInvocation(firstName, continuation);
            } else {
                if (!continuation.isResumed() && !"Fred".equals(firstName)) {
                    throw new RuntimeException("No timeout expected");
                }
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
                    suspended.wait(1000);
                } catch (InterruptedException ex) {
                    return false;
                }
            }
        }
        System.out.println("Invocation for " + name + " has been suspended");
        
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
    
    private void suspendInvocation(final String name, Continuation cont) {
        
        System.out.println("Suspending invocation for " + name);
        
        try {
            long timeout = "Fred".equals(name) ? 8000 : 4000;
            cont.suspend(timeout);    
        } finally {
            synchronized (suspended) {
                suspended.put(name, cont);
            }
            if (!"Fred".equals(name)) {
                executor.execute(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ex) {
                            // ignore
                        }       
                        resumeRequest(name);
                    }
                });
            }
        }
    }
    
    private Continuation getContinuation(String name) {
        
        System.out.println("Getting continuation for " + name);
        
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
