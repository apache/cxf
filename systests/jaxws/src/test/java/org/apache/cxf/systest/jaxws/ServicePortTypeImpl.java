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
import org.apache.cxf.jaxws.schemavalidation.CkReponseType;
import org.apache.cxf.jaxws.schemavalidation.CkRequeteType;
import org.apache.cxf.jaxws.schemavalidation.ProduitPosteActionType;
import org.apache.cxf.jaxws.schemavalidation.ServicePortType;

@javax.xml.ws.BindingType(value = "http://www.w3.org/2003/05/soap/bindings/HTTP/")
public class ServicePortTypeImpl implements ServicePortType {
    @Override
    public CkReponseType ckR(CkRequeteType ckRIn) {
        CkReponseType result = new CkReponseType();
        ActionCheckMajType action = new ActionCheckMajType();
        action.setStatus(4);
        ProduitPosteActionType pdt = new ProduitPosteActionType();
        pdt.setAction(action);
        result.getProduit().add(pdt);
        return result;
    }
}
