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

package org.apache.cxf.ws.mex;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.ws.Action;
import jakarta.xml.ws.soap.Addressing;

@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@Addressing(required = true, enabled = true)
@XmlSeeAlso({
    org.apache.cxf.ws.mex.model._2004_09.ObjectFactory.class })
@WebService(targetNamespace = "http://www.w3.org/2009/09/ws-mex")
public interface MetadataExchange {

    @WebResult(name = "Metadata",
        targetNamespace = "http://schemas.xmlsoap.org/ws/2004/09/mex",
        partName = "body")
    @Action(input = "http://schemas.xmlsoap.org/ws/2004/09/transfer/Get",
        output = "http://schemas.xmlsoap.org/ws/2004/09/transfer/GetResponse")
    @WebMethod(operationName = "Get2004")
    org.apache.cxf.ws.mex.model._2004_09.Metadata get2004();

    @WebResult(name = "Metadata",
        targetNamespace = "http://schemas.xmlsoap.org/ws/2004/09/mex",
        partName = "body")
    @Action(input = "http://schemas.xmlsoap.org/ws/2004/09/mex/GetMetadata/Request",
        output = "http://schemas.xmlsoap.org/ws/2004/09/mex/GetMetadata/Response")
    @WebMethod(operationName = "GetMetadata2004")
    org.apache.cxf.ws.mex.model._2004_09.Metadata getMetadata(
        @WebParam(partName = "body", name = "GetMetadata",
            targetNamespace = "http://schemas.xmlsoap.org/ws/2004/09/mex")
        org.apache.cxf.ws.mex.model._2004_09.GetMetadata body
    );

}
