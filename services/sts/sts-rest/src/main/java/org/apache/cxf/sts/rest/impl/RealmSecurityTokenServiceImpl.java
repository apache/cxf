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
package org.apache.cxf.sts.rest.impl;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.sts.rest.RESTSecurityTokenServiceImpl;
import org.apache.cxf.sts.rest.api.GetTokenRequest;
import org.apache.cxf.sts.rest.api.RealmSecurityTokenService;
import org.apache.cxf.sts.rest.api.TokenRequest;

import io.swagger.v3.oas.annotations.Hidden;

import static org.apache.cxf.jaxrs.utils.JAXRSUtils.getCurrentMessage;
import static org.apache.cxf.sts.rest.RESTSecurityTokenService.Action.renew;
import static org.apache.cxf.sts.rest.RESTSecurityTokenService.Action.validate;


public class RealmSecurityTokenServiceImpl implements RealmSecurityTokenService {

    @Context
    private MessageContext messageContext;

    @Context
    private javax.ws.rs.core.SecurityContext securityContext;

    @Hidden
    private RESTSecurityTokenServiceImpl restSecurityTokenService;

    @Override
    public Response getXMLToken(String realm, String tokenType, String keyType, List<String> claims, String appliesTo,
        boolean wstrustResponse) {
        setContext();
        return restSecurityTokenService.getXMLToken(tokenType, keyType, claims, appliesTo, wstrustResponse);
    }

    @Override
    public Response getJSONToken(String realm, String tokenType, String keyType, List<String> claims,
        String appliesTo) {
        setContext();
        return restSecurityTokenService.getJSONToken(tokenType, keyType, claims, appliesTo);
    }

    @Override
    public Response getPlainToken(String realm, String tokenType, String keyType, List<String> claims,
        String appliesTo) {
        setContext();
        return restSecurityTokenService.getPlainToken(tokenType, keyType, claims, appliesTo);
    }

    @Override
    public Response getToken(String realm, GetTokenRequest request) {
        setContext();
        if (request.getTokenType().equals("jwt")) {
            return restSecurityTokenService.getJSONToken(request.getTokenType(), request.getKeyType(),
                request.getClaims(), request.getAudience());
        } else {
            return restSecurityTokenService.getXMLToken(request.getTokenType(), request.getKeyType(),
                request.getClaims(), request.getAudience(), false);
        }
    }

    public Response validate(String realm, TokenRequest request) {
        setContext();
        return restSecurityTokenService.getToken(validate,
            TokenUtils.createValidateRequestSecurityTokenType(request.getToken(), request.getTokenType()));
    }

    public Response renew(String realm, TokenRequest request) {
        setContext();
        return restSecurityTokenService.getToken(renew,
            TokenUtils.createRenewRequestSecurityTokenType(request.getToken(), request.getTokenType()));
    }

    public Response remove(String realm, TokenRequest request) {
        setContext();
        return restSecurityTokenService.removeToken(
            TokenUtils.createRemoveRequestSecurityTokenType(request.getToken(), request.getTokenType()));
    }

    @Override
    public Response getKeyExchangeToken(String realm, GetTokenRequest request) {
        setContext();
        return restSecurityTokenService.getKeyExchangeToken(TokenUtils.createRequestSecurityTokenType(request));
    }

    @Override
    public JsonWebKeys getPublicVerificationKeys() {
        setContext();
        return JwkOperation.loadPublicKeys(getCurrentMessage());
    }

    public void setRestSecurityTokenService(RESTSecurityTokenServiceImpl restSecurityTokenService) {
        this.restSecurityTokenService = restSecurityTokenService;
    }

    private void setContext() {
        restSecurityTokenService.setMessageContext(messageContext);
        restSecurityTokenService.setSecurityContext(securityContext);
    }
}
