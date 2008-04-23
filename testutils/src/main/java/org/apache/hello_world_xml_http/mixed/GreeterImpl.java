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

package org.apache.hello_world_xml_http.mixed;

import org.apache.hello_world_xml_http.mixed.types.FaultDetail;
import org.apache.hello_world_xml_http.mixed.types.SayHi;
import org.apache.hello_world_xml_http.mixed.types.SayHiResponse;

@javax.jws.WebService(serviceName = "XMLService",
                      portName = "XMLPort",
                      endpointInterface = "org.apache.hello_world_xml_http.mixed.Greeter",
                      targetNamespace = "http://apache.org/hello_world_xml_http/mixed",
                      wsdlLocation = "testutils/hello_world_xml_mixed.wsdl")

@javax.xml.ws.BindingType(value = "http://cxf.apache.org/bindings/xformat")

public class GreeterImpl implements Greeter {

    public String greetMe(String me) {
        System.out.println("Executing operation greetMe\n");
        return "Hello " + me;
    }

    public void greetMeOneWay(String me) {
        System.out.println("Executing operation greetMeOneWay\n");
        System.out.println("Hello there " + me);
    }

    public SayHiResponse sayHi1(SayHi in) {
        System.out.println("Executing operation sayHi1\n");
        SayHiResponse response = new SayHiResponse();
        response.setResponseType("Bonjour");
        return response;

    }

    public void pingMe() throws PingMeFault {
        FaultDetail faultDetail = new FaultDetail();
        faultDetail.setMajor((short)2);
        faultDetail.setMinor((short)1);
        throw new PingMeFault("PingMeFault raised by server", faultDetail);
    }
}
