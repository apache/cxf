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
package org.apache.cxf.xmlbeans;


import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import junit.framework.Assert;

import com.sample.procure.PurchaseOrderDocument;


@WebService(targetNamespace = "urn:TestService", 
            serviceName = "TestService")
public class TestService {
    @WebMethod(operationName = "GetWeatherByZipCode")
    public ResponseDocument getWeatherByZipCode(RequestDocument body) {
        return ResponseDocument.Factory.newInstance();
    }

    @WebMethod(operationName = "GetTrouble")
    public TroubleDocument getTrouble(TroubleDocument trouble) {
        return trouble;
    }

    @WebMethod(operationName = "ThrowFault")
    public TroubleDocument throwFault() throws CustomFault {
        CustomFault fault = new CustomFault();
        fault.setFaultInfo("extra");
        throw fault;
    }
    
    @WebMethod
    public ResponseDocument mixedRequest(
                                         @WebParam(name = "string") String string,
                                         @WebParam(name = "request") RequestDocument req) {
        Assert.assertEquals("foo", string);
        Assert.assertEquals("foo", req.getRequest().getSessionId());

        ResponseDocument response = ResponseDocument.Factory.newInstance();
        response.addNewResponse().addNewForm();
        return response;
    }
    
    @WebMethod
    public void submitPO(PurchaseOrderDocument doc) {
        
    }
}
