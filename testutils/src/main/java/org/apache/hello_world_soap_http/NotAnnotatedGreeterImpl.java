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

import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import org.apache.hello_world_soap_http.types.BareDocumentResponse;
import org.apache.hello_world_soap_http.types.ErrorCode;
import org.apache.hello_world_soap_http.types.GreetMeLaterResponse;
import org.apache.hello_world_soap_http.types.GreetMeResponse;
import org.apache.hello_world_soap_http.types.GreetMeSometimeResponse;
import org.apache.hello_world_soap_http.types.NoSuchCodeLit;
import org.apache.hello_world_soap_http.types.SayHiResponse;
import org.apache.hello_world_soap_http.types.TestDocLitFaultResponse;
import org.apache.hello_world_soap_http.types.TestNillableResponse;



                
public class NotAnnotatedGreeterImpl implements Greeter {

    private static final Logger LOG = 
        Logger.getLogger(NotAnnotatedGreeterImpl.class.getName());
    
    public String greetMe(String me) {
        LOG.info("Executing operation greetMe");
        return me;
    }

    public String greetMeLater(long delay) {
        LOG.info("Executing operation greetMeLater");
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                /// ignore
            }
        }
        return "Hello, finally!";
    }
    
    public String greetMeSometime(String me) {
        LOG.info("Executing operation greetMeSometime");
        return me;
    }
    
    public Future<?>  greetMeSometimeAsync(String requestType, 
                                           AsyncHandler<GreetMeSometimeResponse> asyncHandler) { 
        return null; 
        /* to be implemented */
    }
    
    public Response<GreetMeSometimeResponse> greetMeSometimeAsync(String requestType) { 
        return null; 
        /* to be implemented" */
    }
    
    public Future<?>  greetMeAsync(String requestType, AsyncHandler<GreetMeResponse> asyncHandler) { 
        return null; 
        /*not called */
    }
    
    public Response<GreetMeResponse> greetMeAsync(String requestType) { 
        return null; 
        /*not called */
    }

    public Future<?> greetMeLaterAsync(long requestType, AsyncHandler<GreetMeLaterResponse> asyncHandler) {
        return null;
        /*not called */
    }

    public Response<GreetMeLaterResponse> greetMeLaterAsync(long requestType) {
        return null;
        /*not called */
    }
    
    public Future<?> sayHiAsync(AsyncHandler<SayHiResponse> asyncHandler) { 
        return null; 
        /*not called */
    }
    
    public Response<SayHiResponse> sayHiAsync() { 
        return null; 
        /*not called */
    }
  
    public String sayHi() {
        LOG.info("Executing operation sayHi");
        return "Bonjour";
    }
    
    public void greetMeOneWay(String me) {
        LOG.info("Executing operation greetMeOneWay");
    }
    
    public Response<TestDocLitFaultResponse> testDocLitFaultAsync(String faultType) {
        return null; 
        /*not called */
    }
    
    public Future<?> testDocLitFaultAsync(String faultType,
                                          AsyncHandler<TestDocLitFaultResponse> ah) {  
        return null; 
        /*not called */
    }
    
    public Future<?> testDocLitBareAsync(String bare, AsyncHandler<BareDocumentResponse> ah) {
        return null;
        /* not called */
    }
    
    public Response<BareDocumentResponse> testDocLitBareAsync(String bare) {
        return null;
        /* not called */
    }
    

    public void testDocLitFault(String faultType)  throws BadRecordLitFault, NoSuchCodeLitFault {
        ErrorCode ec = new ErrorCode();
        ec.setMajor((short)1);
        ec.setMinor((short)1);
        NoSuchCodeLit nscl = new NoSuchCodeLit();
        nscl.setCode(ec);
        
        throw new NoSuchCodeLitFault("TestException", nscl);
    }
    
    public BareDocumentResponse testDocLitBare(String in) {
        LOG.info("Executin operation testDocLitBare");
        BareDocumentResponse res = new BareDocumentResponse();
        res.setCompany("CXF");
        res.setId(1);
        return res;
    }

    public String testNillable(String nillElem, int intElem) {
        // TODO Auto-generated method stub
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
