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
import javax.jws.soap.SOAPBinding;

@WebService(
    name = "MYJ2WDLSharedEndpoint",
    targetNamespace = "http://doclitservice.org/wsdl"
)

@SOAPBinding(
    style = javax.jws.soap.SOAPBinding.Style.DOCUMENT,
    use = javax.jws.soap.SOAPBinding.Use.LITERAL,
    parameterStyle = javax.jws.soap.SOAPBinding.ParameterStyle.WRAPPED
)
interface B extends C {
    // An overloaded method helloWorld
    String helloWorld();
    
    // Annotation to disambiguate name of overloaded method helloWorld
    // and to disambiguate name of Wrappers from HelloWorld - > HelloWorld2
    @javax.jws.WebMethod(operationName = "helloWorld2")
    @javax.xml.ws.RequestWrapper(
        localName = "helloWorld2",
        targetNamespace = "http://doclitservice.org/wsdl",
        className = "com.sun.ts.tests.jaxws.mapping.j2wmapping.document.literal.wrapped.HelloWorld2"
    )
    @javax.xml.ws.ResponseWrapper(
        localName = "helloWorld2Response",
        targetNamespace = "http://doclitservice.org/wsdl",
        className = "com.sun.ts.tests.jaxws.mapping.j2wmapping.document.literal.wrapped.HelloWorld2Response"
    )
    java.lang.String helloWorld(String hello);

    @javax.jws.WebMethod
    @javax.jws.Oneway
    void oneWayOperation();
}