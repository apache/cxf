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



package demo.soap_header.server;

import javax.xml.ws.Holder;
import org.apache.headers.HeaderTester;
import org.apache.headers.InHeader;
import org.apache.headers.InHeaderResponse;
import org.apache.headers.InoutHeader;
import org.apache.headers.InoutHeaderResponse;
import org.apache.headers.OutHeader;
import org.apache.headers.OutHeaderResponse;
import org.apache.headers.SOAPHeaderData;


@javax.jws.WebService(serviceName = "HeaderService", 
            portName = "SoapPort", 
            endpointInterface = "org.apache.headers.HeaderTester",
            targetNamespace = "http://apache.org/headers")


public class HeaderTesterImpl implements HeaderTester {

    public InHeaderResponse inHeader(InHeader me,
                                     SOAPHeaderData headerInfo) {
        System.out.println("inHeader invoked");

        System.out.println("\tGetting Originator: " + headerInfo.getOriginator());
        System.out.println("\tGetting Message: " + headerInfo.getMessage());

        InHeaderResponse ihr = new InHeaderResponse();
        ihr.setResponseType("Hello " + me.getRequestType());
        return ihr;
    }

    public void outHeader(OutHeader me, 
                          Holder<OutHeaderResponse> theResponse,
                          Holder<SOAPHeaderData> headerInfo) {
        System.out.println("outHeader invoked");

        System.out.println("\tSetting originator: CXF server");
        System.out.println("\tSetting message: outHeader invocation succeeded");

        SOAPHeaderData sh = new SOAPHeaderData();
        sh.setOriginator("CXF server");
        sh.setMessage("outHeader invocation succeeded");
        headerInfo.value = sh;

        OutHeaderResponse ohr = new OutHeaderResponse();
        ohr.setResponseType("Hello " + me.getRequestType());
        theResponse.value = ohr;
    }

    public InoutHeaderResponse inoutHeader(InoutHeader me,
                                           Holder<SOAPHeaderData> headerInfo) {
        System.out.println("inoutHeader invoked");

        System.out.println("\tGetting Originator: " + headerInfo.value.getOriginator());
        System.out.println("\tGetting Message: " + headerInfo.value.getMessage());

        System.out.println("\tSetting originator: CXF server");
        System.out.println("\tSetting message: inoutHeader invocation succeeded");

        headerInfo.value.setOriginator("CXF server");
        headerInfo.value.setMessage("inoutHeader invocation succeeded");

        InoutHeaderResponse iohr = new InoutHeaderResponse();
        iohr.setResponseType("Hello " + me.getRequestType());

        return iohr;
    }    
}
