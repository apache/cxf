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
package org.apache.cxf.performance.basic_type.server;

import java.util.logging.Logger;

import org.apache.cxf.performance.basic_type.BasicPortType;
@javax.jws.WebService(portName = "SoapHttpPort", serviceName = "BasicService",                                                                                
                      targetNamespace = "http://cxf.apache.org/performance/basic_type",
                      endpointInterface = "org.apache.cxf.performance.basic_type.BasicPortType",
                      wsdlLocation = "wsdl/basic_type.wsdl" 
)

public class ServerImpl implements BasicPortType {

    private static final Logger LOG = 
        Logger.getLogger(ServerImpl.class.getPackage().getName());
    
    public byte[] echoBase64(byte[] inputBase64) {
        //LOG.info("Executing operation echoBase64 ");
        //System.out.println("Executing operation echoBase64");
        //System.out.println("Message received: " + inputBase64 + "\n");
        return inputBase64;
    }
    
    public String echoString(String inputString) {
        //LOG.info("Executing operation echoString");
        //System.out.println("Executing operation echoString\n");
        //System.out.println("Message received: " + inputString + "\n");
        return inputString;
    }
}
    
