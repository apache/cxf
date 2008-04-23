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

import javax.jws.WebService;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import org.apache.hello_world_soap_http.types.BareDocumentResponse;


@WebService(serviceName = "SOAPService_DocLitBare", 
            portName = "SoapPort2", 
            endpointInterface = "org.apache.hello_world_soap_http.DocLitBare",
            targetNamespace = "http://apache.org/hello_world_soap_http",
            wsdlLocation = "testutils/hello_world.wsdl")

public class DocLitBareGreeterImpl implements DocLitBare {

    private int invocationCount;
    
    public BareDocumentResponse testDocLitBare(String in) {
        invocationCount++;
        BareDocumentResponse res = new BareDocumentResponse();
        res.setCompany("CXF");
        res.setId(1);
        return res;
    }

    public Future<?> testDocLitBareAsync(String in, AsyncHandler<BareDocumentResponse> asyncHandler) {

        return null;
    }

    public Response<BareDocumentResponse> testDocLitBareAsync(String in) {

        return null;
    }
    
    public int getInvocationCount() {
        return invocationCount;
    }
}
