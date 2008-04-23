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

package org.apache.headers;

import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.apache.headers.types.InHeader;
import org.apache.headers.types.InHeaderResponse;
import org.apache.headers.types.InoutHeader;
import org.apache.headers.types.InoutHeaderResponse;
import org.apache.headers.types.OutHeader;
import org.apache.headers.types.OutHeaderResponse;
import org.apache.headers.types.SOAPHeaderData;

@WebService(serviceName = "XMLHeaderService", 
        portName = "XMLPort9000", 
        endpointInterface = "org.apache.headers.HeaderTester",
        targetNamespace = "http://apache.org/headers",
        wsdlLocation = "testutils/soapheader2.wsdl")
        
public class HeaderTesterImpl implements HeaderTester {

    public InHeaderResponse inHeader(InHeader me, SOAPHeaderData headerInfo) {
        // TODO Auto-generated method stub
        InHeaderResponse resp = new InHeaderResponse();
        resp.setResponseType("requestType=" + me.getRequestType() + "\nheaderData.message="
                + headerInfo.getMessage() + "\nheaderData.getOriginator=" + headerInfo.getOriginator());
        return resp;
    }

    public InoutHeaderResponse inoutHeader(InoutHeader me, Holder<SOAPHeaderData> headerInfo) {
        // TODO Auto-generated method stub
        InoutHeaderResponse resp = new InoutHeaderResponse();
        resp.setResponseType("requestType=" + me.getRequestType());
        if (headerInfo.value != null) {
            headerInfo.value.setMessage("message=" + headerInfo.value.getMessage());
            headerInfo.value.setOriginator("orginator=" + headerInfo.value.getOriginator());
        }
        return resp;
    }

    public void outHeader(OutHeader me, Holder<OutHeaderResponse> theResponse,
            Holder<SOAPHeaderData> headerInfo) {
        theResponse.value = new OutHeaderResponse();
        theResponse.value.setResponseType("requestType=" + me.getRequestType());
        
        headerInfo.value = new SOAPHeaderData(); 
        headerInfo.value.setMessage("message=outMessage");
        headerInfo.value.setOriginator("orginator=outOriginator");
    }

}
