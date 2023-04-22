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

import jakarta.jws.HandlerChain;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;
import org.apache.cxf.rt.security.SecurityConstants;

@WebService(name = "HelloPolicyService", serviceName = "HelloPolicyService")
@EndpointProperties(value = {
    @EndpointProperty(key = SecurityConstants.CALLBACK_HANDLER,
                      value = "org.apache.cxf.systest.ws.policy.handler.CommonPasswordCallback"),
    @EndpointProperty(key = "ws-security.is-bsp-compliant", value = "false"),
    @EndpointProperty(key = SecurityConstants.SIGNATURE_PROPERTIES, value = "alice.properties"),
    @EndpointProperty(key = SecurityConstants.SIGNATURE_USERNAME, value = "alice")
})
@HandlerChain(file = "handlers.xml")
public class HelloServiceImpl implements HelloService {
    @Override
    @WebResult(name = "result")
    public boolean checkHello(@WebParam(name = "input") String input) throws MyFault {
        throw new MyFault("myMessage", "myFaultInfo");
    }

}
