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

package org.apache.cxf.systest.soap;

import javax.jws.WebService;

import org.apache.hello_world_soap_action.Greeter;

@WebService(endpointInterface = "org.apache.hello_world_soap_action.RPCGreeter",
            serviceName = "SOAPRPCService")
public class RPCLitSoapActionGreeterImpl implements Greeter {

    @Override
    public String sayHi(String in) {
        return "sayHi";
    }

    @Override
    public String sayHi2(String in) {
        return "sayHi2";
    }

}
