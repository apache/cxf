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

package org.apache.cxf.greeter_control;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

/**
 * 
 */

@WebService(targetNamespace = "http://cxf.apache.org/greeter_control", name = "Greeter")

public interface GreeterNoWsdl {

    @ResponseWrapper(targetNamespace = "http://cxf.apache.org/greeter_control/types", 
                     className = "org.apache.cxf.greeter_control.types.SayHiResponse", 
                     localName = "sayHiResponse")
    @RequestWrapper(targetNamespace = "http://cxf.apache.org/greeter_control/types", 
                    className = "org.apache.cxf.greeter_control.types.SayHi", 
                    localName = "sayHi")
    @WebResult(targetNamespace = "http://cxf.apache.org/greeter_control/types", name = "responseType")
    @WebMethod(operationName = "sayHi")
    java.lang.String sayHi();

    @ResponseWrapper(targetNamespace = "http://cxf.apache.org/greeter_control/types", 
                     className = "org.apache.cxf.greeter_control.types.GreetMeResponse", 
                     localName = "greetMeResponse")
    @RequestWrapper(targetNamespace = "http://cxf.apache.org/greeter_control/types", 
                    className = "org.apache.cxf.greeter_control.types.GreetMe", 
                    localName = "greetMe")
    @WebResult(targetNamespace = "http://cxf.apache.org/greeter_control/types", name = "responseType")
    @WebMethod(operationName = "greetMe")
    java.lang.String greetMe(
        @WebParam(targetNamespace = "http://cxf.apache.org/greeter_control/types", name = "requestType")
        java.lang.String requestType
    );

    @Oneway
    @RequestWrapper(targetNamespace = "http://cxf.apache.org/greeter_control/types", 
                    className = "org.apache.cxf.greeter_control.types.GreetMeOneWay", 
                    localName = "greetMeOneWay")
    @WebMethod(operationName = "greetMeOneWay")
    void greetMeOneWay(
        @WebParam(targetNamespace = "http://cxf.apache.org/greeter_control/types", name = "requestType")
        java.lang.String requestType
    );

    @ResponseWrapper(targetNamespace = "http://cxf.apache.org/greeter_control/types", 
                     className = "org.apache.cxf.greeter_control.types.PingMeResponse", 
                     localName = "pingMeResponse")
    @RequestWrapper(targetNamespace = "http://cxf.apache.org/greeter_control/types", 
                    className = "org.apache.cxf.greeter_control.types.PingMe", 
                    localName = "pingMe")
    @WebMethod(operationName = "pingMe")
    void pingMe() throws PingMeFault;
}
