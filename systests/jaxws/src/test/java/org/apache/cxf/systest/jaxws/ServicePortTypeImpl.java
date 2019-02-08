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
package org.apache.cxf.systest.jaxws;

import org.apache.cxf.jaxws.schemavalidation.ActionCheckMajType;
import org.apache.cxf.jaxws.schemavalidation.CkRequestType;
import org.apache.cxf.jaxws.schemavalidation.CkResponseType;
import org.apache.cxf.jaxws.schemavalidation.ProductPostActionType;
import org.apache.cxf.jaxws.schemavalidation.RequestHeader;

@javax.xml.ws.BindingType(value = "http://www.w3.org/2003/05/soap/bindings/HTTP/")
@javax.jws.WebService(endpointInterface = "org.apache.cxf.jaxws.schemavalidation.ServicePortType")
public class ServicePortTypeImpl {

    public CkResponseType ckR(CkRequestType ckRIn, RequestHeader header) {
        CkResponseType result = new CkResponseType();
        ActionCheckMajType action = new ActionCheckMajType();
        action.setStatus(4);
        ProductPostActionType pdt = new ProductPostActionType();
        pdt.setAction(action);
        result.getProduct().add(pdt);
        return result;
    }
}
