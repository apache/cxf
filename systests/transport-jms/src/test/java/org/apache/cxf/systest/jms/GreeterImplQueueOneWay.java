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
package org.apache.cxf.systest.jms;

import javax.jws.WebService;
import javax.xml.ws.soap.Addressing;

import org.apache.cxf.hello_world_jms.HelloWorldOneWayPort;



@WebService(serviceName = "HelloWorldOneWayQueueService",
            portName = "HelloWorldOneWayQueuePort",
            endpointInterface = "org.apache.cxf.hello_world_jms.HelloWorldOneWayPort",
            targetNamespace = "http://cxf.apache.org/hello_world_jms",
            wsdlLocation = "testutils/jms_test.wsdl")
@Addressing(required = true)
public class GreeterImplQueueOneWay implements HelloWorldOneWayPort {

    public void greetMeOneWay(String stringParam0) {
        //System.out.println("*********  greetMeOneWay: " + stringParam0);

    }
}
