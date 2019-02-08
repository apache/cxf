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

package org.apache.cxf.systest.ws.addressing;

import java.util.Date;
import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.hello_world_soap_http.BadRecordLitFault;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.NoSuchCodeLitFault;
import org.apache.hello_world_soap_http.types.BareDocumentResponse;
import org.apache.hello_world_soap_http.types.ErrorCode;
import org.apache.hello_world_soap_http.types.GreetMeLaterResponse;
import org.apache.hello_world_soap_http.types.GreetMeResponse;
import org.apache.hello_world_soap_http.types.GreetMeSometimeResponse;
import org.apache.hello_world_soap_http.types.NoSuchCodeLit;
import org.apache.hello_world_soap_http.types.SayHiResponse;
import org.apache.hello_world_soap_http.types.TestDocLitFaultResponse;
import org.apache.hello_world_soap_http.types.TestNillableResponse;

import static org.apache.cxf.ws.addressing.JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND;

public abstract class AbstractGreeterImpl implements Greeter {
    VerificationCache verificationCache;

    /**
     * Injectable context.
     */
    @Resource
    private WebServiceContext context;
    private boolean quiet;

    public AbstractGreeterImpl() {
    }

    public AbstractGreeterImpl(boolean q) {
        quiet = q;
    }


    public String greetMe(String me) {
        if (!quiet) {
            System.out.println("\n\n*** GreetMe called with: " + me + "***\n\n");
        }
        verifyMAPs();
        return "Hello " + me;
    }

    public String greetMeLater(long delay) {
        if (!quiet) {
            System.out.println("\n\n*** GreetMeLater called with: " + delay
                           + " at: " + new Date().toString()
                           + "***\n\n");
        }
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                // ignore
            }
        }
        verifyMAPs();
        return "Hello, finally";
    }

    public void greetMeOneWay(String requestType) {
        if (!quiet) {
            System.out.println("\n\n*** GreetMeOneWay called with: " + requestType + "***\n\n");
        }
        verifyMAPs();
    }

    public String sayHi() {
        verifyMAPs();
        return "Bonjour";
    }

    public void testDocLitFault(String faultType) throws BadRecordLitFault, NoSuchCodeLitFault {
        verifyMAPs();
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
    }

    public BareDocumentResponse testDocLitBare(String in) {
        BareDocumentResponse res = new BareDocumentResponse();
        res.setCompany("Celtix");
        res.setId(1);
        return res;
    }

    private void verifyMAPs() {
        if (context.getMessageContext() != null) {
            String property = ADDRESSING_PROPERTIES_INBOUND;
            AddressingProperties maps = (AddressingProperties)
                context.getMessageContext().get(property);
            verificationCache.put(MAPTestBase.verifyMAPs(maps, this));
        }
    }

    public String greetMeSometime(String me) {
        return "How are you " + me;
    }

    public Future<?>  greetMeSometimeAsync(String requestType,
                                           AsyncHandler<GreetMeSometimeResponse> asyncHandler) {
        return null;
        /*not called */
    }

    public Response<GreetMeSometimeResponse> greetMeSometimeAsync(String requestType) {
        return null;
        /*not called */
    }

    public Response<TestDocLitFaultResponse> testDocLitFaultAsync(String faultType) {
        return null;
        /*not called */
    }


    public Response<BareDocumentResponse> testDocLitBareAsync(String bare) {
        return null;
        /* not called */
    }

    public Future<?> greetMeAsync(String requestType, AsyncHandler<GreetMeResponse> asyncHandler) {
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
    public Future<?> testDocLitFaultAsync(String faultType,
                                          AsyncHandler<TestDocLitFaultResponse> asyncHandler) {
        return null;
    }

}
