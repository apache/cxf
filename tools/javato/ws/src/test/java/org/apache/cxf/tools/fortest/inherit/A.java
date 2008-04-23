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
package org.apache.cxf.tools.fortest.inherit;

import javax.jws.WebService;

@WebService(
    portName = "J2WDLSharedEndpointPort",
    serviceName = "J2WDLSharedService",
    targetNamespace = "http://doclitservice.org/wsdl",
    endpointInterface = "org.apache.cxf.tools.fortest.inherit.B"
)
public class A implements B {

    public String helloWorld()  {
        return "hello world";
    }

    public String helloWorld(String hello)  {
        return hello;
    }

    public void oneWayOperation()  {
    }

    public String hello(String hello) {
        return hello;
    }

    public String bye(String bye) {
        return bye;
    }
}