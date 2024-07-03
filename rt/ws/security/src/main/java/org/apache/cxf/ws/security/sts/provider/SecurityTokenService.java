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

package org.apache.cxf.ws.security.sts.provider;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.ws.Action;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseCollectionType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenResponseType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;

@WebService(targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/wsdl",
            name = "SecurityTokenService")
@XmlSeeAlso({org.apache.cxf.ws.security.sts.provider.model.ObjectFactory.class,
    org.apache.cxf.ws.security.sts.provider.model.wstrust14.ObjectFactory.class,
    org.apache.cxf.ws.security.sts.provider.model.secext.ObjectFactory.class,
    org.apache.cxf.ws.security.sts.provider.model.utility.ObjectFactory.class,
    org.apache.cxf.ws.security.sts.provider.model.xmldsig.ObjectFactory.class,
    org.apache.cxf.ws.addressing.ObjectFactory.class })
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface SecurityTokenService {

    @WebResult(name = "RequestSecurityTokenResponse",
               targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512",
               partName = "response")
    @Action(input = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/KET",
            output = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTR/KETFinal")
    @WebMethod(operationName = "KeyExchangeToken")
    RequestSecurityTokenResponseType keyExchangeToken(
        @WebParam(partName = "request",
                  name = "RequestSecurityToken",
                  targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512")
        RequestSecurityTokenType request
    );

    @WebResult(name = "RequestSecurityTokenResponseCollection",
               targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512",
               partName = "responseCollection")
    @Action(input = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue",
            output = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTRC/IssueFinal")
    @WebMethod(operationName = "Issue")
    RequestSecurityTokenResponseCollectionType issue(
        @WebParam(partName = "request",
                  name = "RequestSecurityToken",
                  targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512")
        RequestSecurityTokenType request
    );

    @WebResult(name = "RequestSecurityTokenResponse",
              targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512",
              partName = "response")
    @Action(input = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue",
            output = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTRC/IssueFinal")
    @WebMethod(operationName = "IssueSingle")
    RequestSecurityTokenResponseType issueSingle(
        @WebParam(partName = "request",
                  name = "RequestSecurityToken",
                  targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512")
        RequestSecurityTokenType request
    );

    @WebResult(name = "RequestSecurityTokenResponse",
               targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512",
               partName = "response")
    @Action(input = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Cancel",
            output = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTR/CancelFinal")
    @WebMethod(operationName = "Cancel")
    RequestSecurityTokenResponseType cancel(
        @WebParam(partName = "request", name = "RequestSecurityToken",
                  targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512")
        RequestSecurityTokenType request
    );

    @WebResult(name = "RequestSecurityTokenResponse",
               targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512",
               partName = "response")
    @Action(input = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Validate",
            output = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTR/ValidateFinal")
    @WebMethod(operationName = "Validate")
    RequestSecurityTokenResponseType validate(
        @WebParam(partName = "request", name = "RequestSecurityToken",
                  targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512")
        RequestSecurityTokenType request
    );

    @WebResult(name = "RequestSecurityTokenResponseCollection",
               targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512",
               partName = "responseCollection")
    @WebMethod(operationName = "RequestCollection")
    RequestSecurityTokenResponseCollectionType requestCollection(
        @WebParam(partName = "requestCollection",
                  name = "RequestSecurityTokenCollection",
                  targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512")
        RequestSecurityTokenCollectionType requestCollection
    );

    @WebResult(name = "RequestSecurityTokenResponse",
               targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512",
               partName = "response")
    @Action(input = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Renew",
            output = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTR/RenewFinal")
    @WebMethod(operationName = "Renew")
    RequestSecurityTokenResponseType renew(
        @WebParam(partName = "request",
                  name = "RequestSecurityToken",
                  targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512")
        RequestSecurityTokenType request
    );
}