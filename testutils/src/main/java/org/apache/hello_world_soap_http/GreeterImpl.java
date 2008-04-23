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


import java.io.IOException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.annotation.Resource;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Response;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.hello_world_soap_http.types.BareDocumentResponse;
import org.apache.hello_world_soap_http.types.ErrorCode;
import org.apache.hello_world_soap_http.types.GreetMeLaterResponse;
import org.apache.hello_world_soap_http.types.GreetMeResponse;
import org.apache.hello_world_soap_http.types.GreetMeSometimeResponse;
import org.apache.hello_world_soap_http.types.NoSuchCodeLit;
import org.apache.hello_world_soap_http.types.SayHiResponse;
import org.apache.hello_world_soap_http.types.TestDocLitFaultResponse;
import org.apache.hello_world_soap_http.types.TestNillableResponse;

@WebService(serviceName = "SOAPService",
            portName = "SoapPort",
            endpointInterface = "org.apache.hello_world_soap_http.Greeter",
            targetNamespace = "http://apache.org/hello_world_soap_http",
            wsdlLocation = "testutils/hello_world.wsdl")
public class GreeterImpl implements Greeter {

    private static final Logger LOG = Logger.getLogger(GreeterImpl.class.getName());

    @Resource
    private WebServiceContext context;
    
    private String prefix = "";

    private int invocationCount;

    public WebServiceContext getContext() {
        return context;
    }
    
    public void setPrefix(String p) {
        prefix = p;
    }    
    
    public String getPrefix() {
        return prefix;
    }
    public String greetMe(String me) {
        if ("secure".equals(me)) {
            MessageContext ctx = getContext().getMessageContext();
            return "Hello " + ctx.get(BindingProvider.USERNAME_PROPERTY);
        }
        if ("principal".equals(me)) {
            return "Hello " + getContext().getUserPrincipal().getName();
        }
        
        
        LOG.info("Invoking greetMe " + prefix + me);
        invocationCount++;
        return "Hello " + me;
    }

    public String greetMeLater(long delay) {
        LOG.info("Invoking greetMeLater " + delay);
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                /// ignore
            }
        }
        return "Hello, finally!";
    }

    public String sayHi() {
        LOG.info("Invoking sayHi");
        invocationCount++;
        return "Bonjour";
    }

    public void testDocLitFault(String faultType) throws BadRecordLitFault, NoSuchCodeLitFault {
        LOG.info("Invoking testDocLitFault");
        invocationCount++;
        if (faultType.equals(BadRecordLitFault.class.getSimpleName())) {
            throw new BadRecordLitFault("TestBadRecordLit", "BadRecordLitFault");
        }
        if (faultType.equals(NoSuchCodeLitFault.class.getSimpleName())) {
            ErrorCode ec = new ErrorCode();
            ec.setMajor((short)1);
            ec.setMinor((short)1);
            NoSuchCodeLit nscl = new NoSuchCodeLit();
            nscl.setCode(ec);
            throw new NoSuchCodeLitFault("TestNoSuchCodeLit", nscl);
        }
        throw new RuntimeException("Unknown source", new IOException("dummy io exception"));
    }

    public void greetMeOneWay(String requestType) {
        invocationCount++;
        //System.out.println("*********  greetMeOneWay: " + requestType);
    }

    public String greetMeSometime(String me) {
        invocationCount++;
        //System.err.println("In greetMeSometime: " + me);
        return "How are you " + me;
    }

    @WebMethod
    public BareDocumentResponse testDocLitBare(String in) {
        invocationCount++;
        BareDocumentResponse res = new BareDocumentResponse();
        res.setCompany("CXF");
        res.setId(1);
        return res;
    }

    public Future<?>  greetMeSometimeAsync(String requestType,
                                           AsyncHandler<GreetMeSometimeResponse> asyncHandler) {
        invocationCount++;
        System.err.println("In greetMeSometimeAsync 1");
        return null;
        /*not called */
    }

    public Response<GreetMeSometimeResponse> greetMeSometimeAsync(String requestType) {
        invocationCount++;
        System.err.println("In greetMeSometimeAsync 2");
        return null;
        /*not called */
    }

    public Response<TestDocLitFaultResponse> testDocLitFaultAsync(String faultType) {
        invocationCount++;
        System.err.println("In testDocLitFaultAsync 1");
        return null;
        /*not called */
    }

    public Future<?> testDocLitFaultAsync(String faultType, AsyncHandler ah) {
        invocationCount++;
        System.err.println("In testDocLitFaultAsync 2");
        return null;
        /*not called */
    }

    public Future<?> testDocLitBareAsync(String bare, AsyncHandler ah) {
        invocationCount++;
        return null;
        /* not called */
    }

    public Response<BareDocumentResponse> testDocLitBareAsync(String bare) {
        invocationCount++;
        return null;
        /* not called */
    }

    public Future<?> greetMeAsync(String requestType, AsyncHandler<GreetMeResponse> asyncHandler) {
        invocationCount++;
        return null;
        /*not called */
    }

    public Response<GreetMeResponse> greetMeAsync(String requestType) {
        invocationCount++;
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
        invocationCount++;
        return null;
        /*not called */
    }

    public Response<SayHiResponse> sayHiAsync() {
        invocationCount++;
        return null;
        /*not called */
    }

    public int getInvocationCount() {
        return invocationCount;
    }

    public String testNillable(String nillElem, int intElem) {
        System.out.println("the testNillable is invoked");
        return nillElem;
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
