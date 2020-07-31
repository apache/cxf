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
package org.apache.cxf.sts.rs.impl;

import java.util.Optional;

import javax.ws.rs.NotAuthorizedException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.rs.api.GetTokenRequest;
import org.apache.cxf.ws.security.sts.provider.model.CancelTargetType;
import org.apache.cxf.ws.security.sts.provider.model.ObjectFactory;
import org.apache.cxf.ws.security.sts.provider.model.RenewTargetType;
import org.apache.cxf.ws.security.sts.provider.model.RequestSecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.ValidateTargetType;

import static java.util.Optional.ofNullable;
import static org.apache.cxf.sts.STSConstants.WST_NS_05_12;
import static org.apache.cxf.sts.rs.RESTSecurityTokenServiceImpl.DEFAULT_KEY_TYPE_MAP;
import static org.apache.cxf.sts.rs.RESTSecurityTokenServiceImpl.DEFAULT_TOKEN_TYPE_MAP;

public final class TokenUtils {
    public static final String BEARER_AUTH_SCHEMA = "Bearer";
    public static final String TOKEN_WRAPPER_ELEMENT_NAME = "TokenWrapper";
    public static final String VALIDATE_REQUEST_TYPE = WST_NS_05_12 + "/Validate";
    public static final String RENEW_REQUEST_TYPE = WST_NS_05_12 + "/Renew";
    public static final String CANCEL_REQUEST_TYPE = WST_NS_05_12 + "/Cancel";
    public static final String ISSUE_REQUEST_TYPE = WST_NS_05_12 + "/Issue";

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    private TokenUtils() {
    }

    public static String getEncodedJwtToken(final String authorizationHeader) {
        String[] parts = ofNullable(authorizationHeader)
                .map(s -> s.split(" "))
                .orElseThrow(() -> new NotAuthorizedException("Authorization(Bearer) header format is not correct",
                    BEARER_AUTH_SCHEMA));

        if (parts == null || !BEARER_AUTH_SCHEMA.equals(parts[0]) || parts.length != 2) {
            throw new NotAuthorizedException("Bearer scheme is expected", BEARER_AUTH_SCHEMA);
        }
        return parts[1];
    }

    public static Element wrapJwtToken(final String token) {
        Document doc = DOMUtils.getEmptyDocument();
        Element tokenWrapper = doc.createElementNS(null, TOKEN_WRAPPER_ELEMENT_NAME);
        tokenWrapper.setTextContent(token);
        return tokenWrapper;
    }

    public static RequestSecurityTokenType createValidateRequestSecurityTokenType(final Object token,
        final String tokenType) {
        RequestSecurityTokenType request = createRequestSecurityTokenType(VALIDATE_REQUEST_TYPE, tokenType);

        ValidateTargetType validateTarget = OBJECT_FACTORY.createValidateTargetType();
        validateTarget.setAny(getToken(token));
        request.getAny().add(OBJECT_FACTORY.createValidateTarget(validateTarget));

        return request;
    }

    public static RequestSecurityTokenType createRenewRequestSecurityTokenType(final Object token,
        final String tokenType) {
        RequestSecurityTokenType request = createRequestSecurityTokenType(RENEW_REQUEST_TYPE, tokenType);

        RenewTargetType renewTarget = OBJECT_FACTORY.createRenewTargetType();
        renewTarget.setAny(getToken(token));
        request.getAny().add(OBJECT_FACTORY.createRenewTarget(renewTarget));
        return request;
    }

    public static RequestSecurityTokenType createRemoveRequestSecurityTokenType(final Object token,
        final String tokenType) {
        RequestSecurityTokenType request = createRequestSecurityTokenType(CANCEL_REQUEST_TYPE, tokenType);

        CancelTargetType cancelTarget = OBJECT_FACTORY.createCancelTargetType();
        cancelTarget.setAny(getToken(token));
        request.getAny().add(OBJECT_FACTORY.createCancelTarget(cancelTarget));
        return request;
    }

    public static RequestSecurityTokenType createRequestSecurityTokenType(final GetTokenRequest tokenRequest) {
        RequestSecurityTokenType request = OBJECT_FACTORY.createRequestSecurityTokenType();
        request.getAny().add(OBJECT_FACTORY.createTokenType(getTokenType(tokenRequest.getTokenType())));
        request.getAny().add(OBJECT_FACTORY.createKeyType(DEFAULT_KEY_TYPE_MAP.get(tokenRequest.getKeyType())));
        request.getAny().add(OBJECT_FACTORY.createRequestType(ISSUE_REQUEST_TYPE));
        return request;
    }

    public static RequestSecurityTokenType createRequestSecurityTokenType(final String requestType,
        final String tokenType) {
        RequestSecurityTokenType request = OBJECT_FACTORY.createRequestSecurityTokenType();
        request.getAny().add(OBJECT_FACTORY.createRequestType(requestType));
        request.getAny().add(OBJECT_FACTORY.createTokenType(getTokenType(tokenType)));
        return request;
    }

    private static Object getToken(final Object tokenObject) {
        if (tokenObject instanceof String) {
            return wrapJwtToken((String)tokenObject);
        } else if (tokenObject instanceof Element && ((Element)tokenObject).getFirstChild() != null) {
            Node node = ((Element)tokenObject).getFirstChild();
            if (node.getNodeType() == Node.TEXT_NODE) {
                return wrapJwtToken(node.getNodeValue());
            }
        }
        return tokenObject;
    }

    private static String getTokenType(final String tokenType) {
        final String type =  ofNullable(tokenType)
                .orElse(STSConstants.STATUS);
        return Optional.of(type)
                .map(t -> DEFAULT_TOKEN_TYPE_MAP.get(t))
                .orElse(type);
    }
}