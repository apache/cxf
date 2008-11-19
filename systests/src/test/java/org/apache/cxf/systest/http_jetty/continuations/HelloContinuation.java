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

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@SOAPBinding(style = SOAPBinding.Style.RPC, use = SOAPBinding.Use.LITERAL)
@WebService(name = "HelloContinuation", targetNamespace = "http://cxf.apache.org/systest/jaxws")
public interface HelloContinuation {
    @WebMethod(operationName = "sayHi", exclude = false)
    String sayHi(String firstName, String secondName);
    
    @WebMethod(operationName = "isRequestSuspended", exclude = false)
    boolean isRequestSuspended(String name);
    
    @WebMethod(operationName = "resumeRequest", exclude = false)
    void resumeRequest(String name);
}
