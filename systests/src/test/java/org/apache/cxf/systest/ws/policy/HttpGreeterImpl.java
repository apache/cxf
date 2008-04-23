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

package org.apache.cxf.systest.ws.policy;

import javax.jws.WebService;

import org.apache.cxf.greeter_control.AbstractGreeterImpl;

/**
 * 
 */

@WebService(serviceName = "BasicGreeterService",
            portName = "GreeterPort",
            endpointInterface = "org.apache.cxf.greeter_control.Greeter",
            targetNamespace = "http://cxf.apache.org/greeter_control",
            wsdlLocation = "testutils/greeter_control.wsdl")
public class HttpGreeterImpl extends AbstractGreeterImpl {

    private int greetMeCount;
    
    @Override
    public String greetMe(String arg0) {
        if (0 == greetMeCount % 2) {
            setDelay(0);
        } else {
            setDelay(2000);
        }   
        greetMeCount++;
        return super.greetMe(arg0);
    }
    
}
