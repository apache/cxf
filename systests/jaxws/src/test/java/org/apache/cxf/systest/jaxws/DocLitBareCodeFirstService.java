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
package org.apache.cxf.systest.jaxws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@WebService(name = "DocLitBareCodeFirstService",
            targetNamespace = "http://cxf.apache.org/systest/jaxws/DocLitBareCodeFirstService")
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT,
             use = SOAPBinding.Use.LITERAL,
             parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface DocLitBareCodeFirstService {

    @WebMethod(operationName = "greetMe") 
    @WebResult(targetNamespace = "http://namespace/result", name = "GreetMeBareResponse") 
    GreetMeResponse greetMe(
                  @WebParam(targetNamespace = "http://namespace/request") 
                  GreetMeRequest gmr); 
    
    
    @XmlList
    @WebResult(name = "Items", targetNamespace = "http://namespace/result", partName = "parameter")
    @WebMethod
    java.math.BigInteger[] sayTest(
        @WebParam(partName = "parameter", name = "SayTestRequest", targetNamespace = "http://www.tum.de/test")
        SayTestRequest parameter
    );
    
    
    @XmlAccessorType(XmlAccessType.FIELD) 
    @XmlType(name = "SayTestRequest", 
             namespace = "http://cxf.apache.org/test/request/bare", 
             propOrder = { "name" }) 
    @XmlRootElement(namespace = "http://cxf.apache.org/test/request/bare",
                    name = "SayTestObject") 
    static class SayTestRequest  {
        String name;
        
        public SayTestRequest() {
        }
        public SayTestRequest(String n) {
            name = n;
        }
        
        public void setName(String n) {
            name = n;
        }
        public String getName() {
            return name;
        }
    }
    @XmlAccessorType(XmlAccessType.FIELD) 
    @XmlType(name = "GreetMeRequest", 
             namespace = "http://cxf.apache.org/test/request/bare", 
             propOrder = { "name" }) 
    @XmlRootElement(namespace = "http://cxf.apache.org/test/request/bare",
                    name = "GreetMeObject") 
    static class GreetMeRequest  {
        String name;
        
        public GreetMeRequest() {
        }
        
        public void setName(String n) {
            name = n;
        }
        public String getName() {
            return name;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD) 
    @XmlType(name = "GreetMeResponse", 
             namespace = "http://cxf.apache.org/test/request/bare", 
             propOrder = { "name" }) 
    @XmlRootElement(namespace = "http://cxf.apache.org/test/request/bare",
                    name = "GreetMeResponseObject")    
    static class GreetMeResponse  {
        String name;
        
        public GreetMeResponse() {
        }
        
        public void setName(String n) {
            name = n;
        }
        public String getName() {
            return name;
        }
    }
}
