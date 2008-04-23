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

package org.apache.cxf.systest.clustering;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.greeter_control.AbstractGreeterImpl;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.JAXWSAConstants;

@WebService(serviceName = "GreeterService",
            portName = "ReplicatedPortA",
            endpointInterface = "org.apache.cxf.greeter_control.Greeter",
            targetNamespace = "http://cxf.apache.org/greeter_control",
            wsdlLocation = "testutils/greeter_control.wsdl")
public class GreeterImplA extends AbstractGreeterImpl {
    @Resource
    private WebServiceContext context;

    private String address;
    
    GreeterImplA() {
        address = FailoverTest.REPLICA_A;    
    }
    
    public String greetMe(String s) {
        return super.greetMe(s)
               + " on message: " + getMessageID()
               + " from: " + address;
    }
 
    private String getMessageID() {
        String id = null;
        if (context.getMessageContext() != null) {
            String property =
                JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_INBOUND;
            AddressingProperties maps = (AddressingProperties)
                context.getMessageContext().get(property);
            id = maps != null && maps.getMessageID() != null
                 ? maps.getMessageID().getValue()
                 : null;
        }
        return id;
    }
}
