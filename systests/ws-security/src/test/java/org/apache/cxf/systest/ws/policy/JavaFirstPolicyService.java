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

package org.apache.cxf.systest.ws.policy;

import javax.jws.WebMethod;
import javax.jws.WebService;

import org.apache.cxf.annotations.Policies;
import org.apache.cxf.annotations.Policy;

@WebService(name = "JavaFirstPolicyService",
            targetNamespace = "http://www.example.org/contract/JavaFirstPolicyService")
public interface JavaFirstPolicyService {
    @Policies({
        @Policy(uri = "#InternalTransportAndUsernamePolicy",
            placement = Policy.Placement.PORT_TYPE_OPERATION_INPUT)
    })
    @WebMethod(operationName = "doOperationOne")
    void doOperationOne();

    @Policies({
        @Policy(uri = "classpath:/java_first_policies/UsernamePasswordToken.xml",
            placement = Policy.Placement.PORT_TYPE_OPERATION_INPUT)
    })
    @WebMethod(operationName = "doOperationTwo")
    void doOperationTwo();

    @Policies({
        @Policy(uri = "#InternalTransportAndUsernamePolicy",
            placement = Policy.Placement.PORT_TYPE_OPERATION_INPUT)
    })
    @WebMethod(operationName = "doOperationThree")
    void doOperationThree();

    @Policies({
        @Policy(uri = "classpath:/java_first_policies/UsernamePasswordToken.xml",
            placement = Policy.Placement.PORT_TYPE_OPERATION_INPUT)
    })
    @WebMethod(operationName = "doOperationFour")
    void doOperationFour();


    @WebMethod(operationName = "doPing")
    void doPing();
}
