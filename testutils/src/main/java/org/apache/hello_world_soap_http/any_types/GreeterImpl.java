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

package org.apache.hello_world_soap_http.any_types;

import java.util.List;

import javax.jws.WebService;

import org.w3c.dom.Element;

import org.apache.hello_world_soap_http.any.Greeter;
import org.apache.hello_world_soap_http.any_types.SayHi.Port;

@WebService(serviceName = "SOAPService",
            portName = "SoapPort",
            endpointInterface = "org.apache.hello_world_soap_http.any.Greeter",
            targetNamespace = "http://apache.org/hello_world_soap_http/any")
public class GreeterImpl implements Greeter {

    public String sayHi(List<Port> port) {
        String ret = null;
        if (port.get(0).getAny() instanceof Element) {
            Element ele = (Element)port.get(0).getAny();
            ret =  ele.getFirstChild().getTextContent();
        }
        if (port.get(1).getAny() instanceof Element) {
            Element ele = (Element)port.get(1).getAny();
            ret +=  ele.getFirstChild().getTextContent();
        }
        return ret;
    }

    public String sayHi1(List<org.apache.hello_world_soap_http.any_types.SayHi1.Port> port) {
        return port.get(0).getRequestType() + port.get(1).getRequestType();
        
    }

    
    

}
