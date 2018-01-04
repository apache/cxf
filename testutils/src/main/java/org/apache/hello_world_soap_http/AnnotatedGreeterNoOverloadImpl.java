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

package org.apache.hello_world_soap_http;


import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.jws.HandlerChain;
import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import javax.xml.ws.WebServiceContext;

@javax.jws.WebService(name = "Greeter", serviceName = "SOAPService", 
                      targetNamespace = "http://apache.org/hello_world_soap_http")
@HandlerChain(name = "TestHandlerChain", file = "handlers.xml")
public class AnnotatedGreeterNoOverloadImpl {

    private static final Logger LOG = 
        Logger.getLogger(AnnotatedGreeterImpl.class.getName());

    @Resource
    private int foo; 

    private WebServiceContext context;

    private final Map<String, Integer> invocationCount = new HashMap<String, Integer>();

    public AnnotatedGreeterNoOverloadImpl() {
        invocationCount.put("sayHi", 0);
        invocationCount.put("greetMe", 0);
        invocationCount.put("overloadedSayHi", 0);
    }

    public int getInvocationCount(String method) {
        if (invocationCount.containsKey(method)) {
            return invocationCount.get(method).intValue();
        } else {
            System.out.println("No invocation count for method: " + method);
            return 0;
        }
    }

    @WebMethod
    @WebResult(name = "responseType",
               targetNamespace = "http://apache.org/hello_world_soap_http/types")
    @RequestWrapper(className = "org.apache.hello_world_soap_http.types.SayHi",
                    localName = "sayHi",
                    targetNamespace = "http://apache.org/hello_world_soap_http/types")
    @ResponseWrapper(className = "org.apache.hello_world_soap_http.types.SayHiResponse",
                     localName = "sayHiResponse",
                     targetNamespace = "http://apache.org/hello_world_soap_http/types")
    public String sayHi() {
        incrementInvocationCount("sayHi");
        return "Hi";
    }

    @WebMethod
    @WebResult(name = "responseType",
               targetNamespace = "http://apache.org/hello_world_soap_http/types")
    @RequestWrapper(className = "org.apache.hello_world_soap_http.types.GreetMe",
                    localName = "greetMe",
                    targetNamespace = "http://apache.org/hello_world_soap_http/types")
    @ResponseWrapper(className = "org.apache.hello_world_soap_http.types.GreetMeResponse",
                     localName = "greetMeResponse",
                     targetNamespace = "http://apache.org/hello_world_soap_http/types")
    public String greetMe(String me) {
        incrementInvocationCount("greetMe");
        return "Bonjour " + me + "!";
    }
    
    @WebMethod
    @RequestWrapper(className = "org.apache.hello_world_soap_http.types.GreetMeOneWay",
                    localName = "greetMeOneWay",
                    targetNamespace = "http://apache.org/hello_world_soap_http/types")
    @Oneway
    public void greetMeOneWay(String me) {
        incrementInvocationCount("greetMeOneWay");
        System.out.println("Hello there " + me);
        System.out.println("That was OneWay to say hello");
    }

    public void testDocLitFault(String faultType)  throws BadRecordLitFault, NoSuchCodeLitFault {        
    }

    @Resource
    public void setContext(WebServiceContext ctx) { 
        context = ctx;
    }

    public WebServiceContext getContext() {
        return context;
    }

    /**
     * stop eclipse from whinging 
     */
    public int getFoo() {         
        return foo;
    }
    
    private void incrementInvocationCount(String method) {
        LOG.info("Executing " + method);
        int n = invocationCount.get(method);
        invocationCount.put(method, n + 1);
    }

}
