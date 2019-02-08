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
package org.apache.cxf.systest.schemaimport;

import java.util.ArrayList;
import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(targetNamespace = "http://apache.org/sayHi", name = "SayHi",
            wsdlLocation = "classpath:/wsdl_systest/e/sayHi.wsdl",
            endpointInterface = "org.apache.cxf.systest.schemaimport.SayHi")
public class SayHiImpl2 implements SayHi {

    @Override
    @WebResult(name = "return", targetNamespace = "")
    @RequestWrapper(localName = "sayHiArray",
                    targetNamespace = "http://apache.org/sayHi2",
                    className = "org.apache.sayhi2.SayHiArray")
    @WebMethod
    @ResponseWrapper(localName = "sayHiArrayResponse",
                     targetNamespace = "http://apache.org/sayHi2",
                     className = "org.apache.sayhi2.SayHiArrayResponse")
    public List<String> sayHiArray(@WebParam(name = "arg0", targetNamespace = "") List<String> arg0) {
        List<String> list = new ArrayList<>();
        list.add("Hi");
        return list;
    }

    @Override
    @WebResult(name = "return", targetNamespace = "http://apache.org/sayHi1")
    @RequestWrapper(localName = "sayHi",
                    targetNamespace = "http://apache.org/sayHi1",
                    className = "org.apache.sayhi1.SayHi")
    @WebMethod
    @ResponseWrapper(localName = "sayHiResponse",
                     targetNamespace = "http://apache.org/sayHi1",
                     className = "org.apache.sayhi1.SayHiResponse")
    public String sayHi(@WebParam(name = "arg0", targetNamespace = "http://apache.org/sayHi1") String arg0) {
        return "Hi";
    }

}
