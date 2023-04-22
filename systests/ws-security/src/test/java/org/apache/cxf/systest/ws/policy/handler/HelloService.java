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
package org.apache.cxf.systest.ws.policy.handler;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import org.apache.cxf.annotations.Policies;
import org.apache.cxf.annotations.Policy;

@WebService(serviceName = "HelloPolicyService")
@Policy(placement = Policy.Placement.BINDING, uri = "classpath:/handler_policies/x509SecurityPolicy.xml")
public interface HelloService {

    @WebMethod(action = "checkHello")
    @WebResult(name = "result")
    @Policies({
        @Policy(uri = "classpath:/handler_policies/inputPolicy.xml",
            placement = Policy.Placement.PORT_TYPE_OPERATION_INPUT),
        @Policy(uri = "classpath:/handler_policies/outputPolicy.xml",
        placement = Policy.Placement.PORT_TYPE_OPERATION_OUTPUT)
    })
    boolean checkHello(@WebParam(name = "input") String input) throws MyFault;
}
