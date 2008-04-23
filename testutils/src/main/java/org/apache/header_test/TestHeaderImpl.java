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

package org.apache.header_test;


import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.apache.header_test.types.TestHeader1;
import org.apache.header_test.types.TestHeader1Response;
import org.apache.header_test.types.TestHeader2;
import org.apache.header_test.types.TestHeader2Response;
import org.apache.header_test.types.TestHeader3;
import org.apache.header_test.types.TestHeader3Response;
import org.apache.header_test.types.TestHeader5;
import org.apache.header_test.types.TestHeader5ResponseBody;
import org.apache.header_test.types.TestHeader6;
import org.apache.header_test.types.TestHeader6Response;
import org.apache.tests.type_test.all.SimpleAll;
import org.apache.tests.type_test.choice.SimpleChoice;
import org.apache.tests.type_test.sequence.SimpleStruct;


@WebService(serviceName = "SOAPHeaderService", 
            portName = "SoapHeaderPort", 
            endpointInterface = "org.apache.header_test.TestHeader",
            targetNamespace = "http://apache.org/header_test",
            wsdlLocation = "testutils/soapheader.wsdl")
public class TestHeaderImpl implements TestHeader {

    public TestHeader1Response testHeader1(
        TestHeader1 in,
        TestHeader1 inHeader) {
        if (in == null || inHeader == null) {
            throw new IllegalArgumentException("TestHeader1 part not found.");
        }
        TestHeader1Response returnVal = new TestHeader1Response();
        
        returnVal.setResponseType(inHeader.getClass().getSimpleName());
        return returnVal;        
    }

    /**
     * 
     * @param out
     * @param outHeader
     * @param in
     */
    public void testHeader2(
        TestHeader2 in,
        Holder<TestHeader2Response> out,
        Holder<TestHeader2Response> outHeader) {
        TestHeader2Response outVal = new TestHeader2Response();
        outVal.setResponseType(in.getRequestType());
        out.value = outVal;
        
        TestHeader2Response outHeaderVal = new TestHeader2Response();
        outHeaderVal.setResponseType(in.getRequestType());
        outHeader.value = outHeaderVal;        
    }

    public TestHeader3Response testHeader3(
        TestHeader3 in,
        Holder<TestHeader3> inoutHeader) {
        
        if (inoutHeader.value == null) {
            throw new IllegalArgumentException("TestHeader3 part not found.");
        }
        TestHeader3Response returnVal = new TestHeader3Response();
        returnVal.setResponseType(inoutHeader.value.getRequestType());
        
        inoutHeader.value.setRequestType(in.getRequestType());
        return returnVal;
    }

    /**
     * 
     * @param requestType
     */
    public void testHeader4(
        String requestType) {
        
    }

    public void testHeader5(Holder<TestHeader5ResponseBody> out,
                            Holder<TestHeader5> outHeader,
                            org.apache.header_test.types.TestHeader5 in) {
        TestHeader5ResponseBody outVal = new TestHeader5ResponseBody();
        outVal.setResponseType(1000);
        out.value = outVal;
        
        TestHeader5 outHeaderVal = new TestHeader5();
        outHeaderVal.setRequestType(in.getRequestType());
        outHeader.value = outHeaderVal;
        
    }
    
    public TestHeader6Response testHeaderPartBeforeBodyPart(
        Holder<TestHeader3> inoutHeader,
        TestHeader6 in) {
        
        if (inoutHeader.value == null) {
            throw new IllegalArgumentException("TestHeader3 part not found.");
        }
        TestHeader6Response returnVal = new TestHeader6Response();
        returnVal.setResponseType(inoutHeader.value.getRequestType());
        
        inoutHeader.value.setRequestType(in.getRequestType());
        return returnVal;
    }

    public SimpleStruct sendReceiveAnyType(Holder<SimpleAll> x, SimpleChoice y) {
        SimpleAll sa = new SimpleAll();
        sa.setVarString(y.getVarString());
        
        SimpleStruct ss = new SimpleStruct();
        ss.setVarAttrString(x.value.getVarAttrString() + "Ret");
        ss.setVarInt(x.value.getVarInt() + 100);
        x.value = sa;
        return ss;
    }

}
