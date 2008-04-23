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
package org.apache.cxf.performance.complex_type.server;

import java.util.logging.Logger;

import org.apache.cxf.cxf.performance.DocPortType;
import org.apache.cxf.cxf.performance.DocPortTypeWrapped;
import org.apache.cxf.cxf.performance.RPCPortType;
import org.apache.cxf.cxf.performance.PerfService;
import org.apache.cxf.cxf.performance.types.NestedComplexType;
import org.apache.cxf.cxf.performance.types.NestedComplexTypeSeq;


@javax.jws.WebService(portName = "SoapHttpDocLitPort", serviceName = "PerfService",                                      
                      targetNamespace = "http://cxf.apache.org/cxf/performance",
                      endpointInterface = "org.apache.cxf.cxf.performance.DocPortType")

public class ServerImpl implements DocPortType {

    private static final Logger LOG = 
        Logger.getLogger(ServerImpl.class.getPackage().getName());
    
    public NestedComplexTypeSeq echoComplexTypeDoc(NestedComplexTypeSeq request) {
        //System.out.println("Executing operation echoComplexTypeDoc\n");
        //System.out.println("Message received: " + request + "\n");
        return request;
    }
   
    public String echoStringDoc(String request) {
        //System.out.println("Executing operation echoStringDoc\n");
        //System.out.println("Message received: " + request + "\n");
        return request;
    }
   
    public byte[] echoBase64Doc(byte[] request) {
        //System.out.println("Executing operation echoBase64Doc\n");
        //System.out.println("Message received: " + request + "\n");
        return request;
    }
}
    
