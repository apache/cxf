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
package org.apache.header_test.rpc;

import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.apache.header_test.rpc.types.HeaderMessage;


@WebService(serviceName = "SOAPRPCHeaderService", 
            portName = "SoapRPCHeaderPort", 
            endpointInterface = "org.apache.header_test.rpc.TestRPCHeader",
            targetNamespace = "http://apache.org/header_test/rpc",
            wsdlLocation = "testutils/soapheader_rpc.wsdl")
            
public class TestRPCHeaderImpl implements TestRPCHeader {

    public String testHeader1(String in, HeaderMessage inHeader) {
        if (in == null || inHeader == null) {
            throw new IllegalArgumentException("TestHeader1 part not found.");
        }
        
        return in + "/" + inHeader.getHeaderVal();
    }

    public String testInOutHeader(String in, Holder<HeaderMessage> inOutHeader) {
        String tmp = inOutHeader.value.getHeaderVal();
        inOutHeader.value.setHeaderVal(in);
        return tmp;
    }

}
