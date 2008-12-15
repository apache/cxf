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

package org.apache.cxf.xmlbeans;

import javax.jws.WebService;
import javax.xml.ws.BindingType;

import org.apache.cxf.xmlbeans.wsdltest.GreeterMine;
import org.apache.cxf.xmlbeans.wsdltest.StringListType;

@WebService(endpointInterface = "org.apache.cxf.xmlbeans.wsdltest.GreeterMine",
            targetNamespace = "http://org.apache.cxf/xmlbeans",
            portName = "SoapPort",
            serviceName = "SOAPMineService",
            name = "GreeterMine")
@BindingType(value = javax.xml.ws.soap.SOAPBinding.SOAP11HTTP_BINDING)
public class GreeterMineImpl implements GreeterMine {

/*
    public String sayHi() {
        System.out.println("****** Executing the operation sayHi *****");
        return "Bonjour";
    }
*/
    public void sayHi2(StringListType stringList) {
        System.out.println("****** Executing the operation sayHi2 *****");
    }



}
