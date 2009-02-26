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

package org.apache.cxf.systest.http_jetty;

import java.util.Date;
import java.util.concurrent.Future;

import javax.jws.WebService;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import org.apache.hello_world_soap_http.BadRecordLitFault;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.NoSuchCodeLitFault;
import org.apache.hello_world_soap_http.types.BareDocumentResponse;
import org.apache.hello_world_soap_http.types.GreetMeLaterResponse;
import org.apache.hello_world_soap_http.types.GreetMeResponse;
import org.apache.hello_world_soap_http.types.GreetMeSometimeResponse;
import org.apache.hello_world_soap_http.types.SayHiResponse;
import org.apache.hello_world_soap_http.types.TestDocLitFaultResponse;
import org.apache.hello_world_soap_http.types.TestNillableResponse;


@WebService(serviceName = "SOAPServiceAddressing", 
            portName = "SoapPort", 
            endpointInterface = "org.apache.hello_world_soap_http.Greeter", 
            targetNamespace = "http://apache.org/hello_world_soap_http",
            wsdlLocation = "testutils/hello_world.wsdl")
public class GreeterImpl implements Greeter {

    public String greetMe(String me) {
        return null;
    }

    public String greetMeLater(long delay) {
        System.out.println("\n\n*** GreetMeLater called with: " + delay
                           + " at: " + new Date().toString()
                           + "***\n\n");
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                // ignore
            }
        }
        return "Hello, finally";
    }

    public void greetMeOneWay(String requestType) {   
    }

    public String sayHi() {
        return null;
    }
    
    public void testDocLitFault(String faultType) throws BadRecordLitFault, NoSuchCodeLitFault {
    }

    public BareDocumentResponse testDocLitBare(String in) {
        return null;
    }

    public String greetMeSometime(String me) {
        return null;
    }
    
    public Future<?>  greetMeSometimeAsync(String requestType, 
                                           AsyncHandler<GreetMeSometimeResponse> asyncHandler) { 
        return null; 
    }
    
    public Response<GreetMeSometimeResponse> greetMeSometimeAsync(String requestType) { 
        return null; 
    }
    
    public Response<TestDocLitFaultResponse> testDocLitFaultAsync(String faultType) {  
        return null; 
    }
    
    public Future<?> testDocLitFaultAsync(String faultType, AsyncHandler ah) {  
        return null; 
    }
    
    public Future<?> testDocLitBareAsync(String bare, AsyncHandler ah) {
        return null;
    }
    
    public Response<BareDocumentResponse> testDocLitBareAsync(String bare) {
        return null;
    }
    
    public Future<?> greetMeAsync(String requestType, AsyncHandler<GreetMeResponse> asyncHandler) { 
        return null; 
    }
    
    public Response<GreetMeResponse> greetMeAsync(String requestType) { 
        return null; 
    }
    
    public Future<?> greetMeLaterAsync(long requestType, AsyncHandler<GreetMeLaterResponse> asyncHandler) { 
        return null; 
    }
    
    public Response<GreetMeLaterResponse> greetMeLaterAsync(long requestType) { 
        return null; 
    }
    
    public Future<?> sayHiAsync(AsyncHandler<SayHiResponse> asyncHandler) { 
        return null; 
    }
    
    public Response<SayHiResponse> sayHiAsync() { 
        return null; 
    }

    public String testNillable(String nillElem, int intElem) {
        return null;
    }

    public Response<TestNillableResponse> testNillableAsync(String nillElem,
                                                            int intElem) {
        return null;
    }
    
    public Future<?> testNillableAsync(String nillElem, 
                                       int intElem,
                                       AsyncHandler<TestNillableResponse> asyncHandler) {
        return null;
    }
    
}
