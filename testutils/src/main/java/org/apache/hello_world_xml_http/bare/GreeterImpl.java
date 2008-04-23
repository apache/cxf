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

package org.apache.hello_world_xml_http.bare;

import org.apache.hello_world_xml_http.bare.types.MyComplexStructType;

@javax.jws.WebService(serviceName = "XMLService", 
                portName = "XMLPort",
                endpointInterface = "org.apache.hello_world_xml_http.bare.Greeter",
                targetNamespace = "http://apache.org/hello_world_xml_http/bare")

@javax.xml.ws.BindingType(value = "http://cxf.apache.org/bindings/xformat")

public class GreeterImpl implements Greeter {

    public String greetMe(String me) {
        // TODO Auto-generated method stub
        return "Hello " + me;        
    }

    public String sayHi() {
        // TODO Auto-generated method stub
        return "Bonjour";
    }

    public MyComplexStructType sendReceiveData(MyComplexStructType in) {
        // TODO Auto-generated method stub        
        return in;
    }

    public String testMultiParamPart(MyComplexStructType in2, String in1) {
        // TODO Auto-generated method stub
        in2.setElem1(in1);
        return "Bonjour";
    }

    public String testTriPart(MyComplexStructType in1, String in3, String in2) {
        // TODO Auto-generated method stub
        return null;
    }

    public String testTriPartNoOrder(String in3, MyComplexStructType in1, String in2) {
        // TODO Auto-generated method stub
        return null;
    }

}
