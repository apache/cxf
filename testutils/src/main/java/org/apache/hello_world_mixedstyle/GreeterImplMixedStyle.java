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
package org.apache.hello_world_mixedstyle;

import javax.jws.WebService;

import org.apache.hello_world_mixedstyle.types.FaultDetail;
import org.apache.hello_world_mixedstyle.types.GreetMe1;
import org.apache.hello_world_mixedstyle.types.GreetMeResponse;


@WebService(serviceName = "SOAPService",
            portName = "SoapPort",
            endpointInterface = "org.apache.hello_world_mixedstyle.Greeter",
            targetNamespace = "http://apache.org/hello_world_mixedstyle",
            wsdlLocation = "testutils/hello_world_mixedstyle.wsdl")
public class GreeterImplMixedStyle implements Greeter {
    private String version;

    public GreeterImplMixedStyle() {
        version = "";
    }

    public GreeterImplMixedStyle(String v) {
        version = v;
    }

    public String sayHi() {
        System.out.println("Call sayHi here ");
        return "Bonjour" + version;
    }

    public GreetMeResponse greetMe(GreetMe1 requestType) {
        System.out.println("Call greetMe here: " + requestType.getRequestType());
        GreetMeResponse response = new GreetMeResponse();
        response.setResponseType("Hello " + requestType.getRequestType() + version);
        return response;
    }

    public void greetMeOneWay(String requestType) {
        System.out.println("*********  greetMeOneWay: " + requestType);
    }

    public void pingMe() throws PingMeFault {
        FaultDetail faultDetail = new FaultDetail();
        faultDetail.setMajor((short)2);
        faultDetail.setMinor((short)1);
        throw new PingMeFault("PingMeFault raised by server", faultDetail);

    }

}
