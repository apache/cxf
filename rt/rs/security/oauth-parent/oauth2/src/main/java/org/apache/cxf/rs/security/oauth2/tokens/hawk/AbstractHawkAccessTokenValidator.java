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
package org.apache.cxf.rs.security.oauth2.tokens.hawk;

import java.net.URI;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.client.HttpRequestProperties;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenValidation;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenValidator;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.AuthorizationUtils;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rt.security.crypto.HmacUtils;

public abstract class AbstractHawkAccessTokenValidator implements AccessTokenValidator {
    protected static final String HTTP_VERB = "http.verb";
    protected static final String HTTP_URI = "http.uri";
    private NonceVerifier nonceVerifier;
    private boolean remoteSignatureValidation;
    public List<String> getSupportedAuthorizationSchemes() {
        return Collections.singletonList(OAuthConstants.HAWK_AUTHORIZATION_SCHEME);
    }

    public AccessTokenValidation validateAccessToken(MessageContext mc,
        String authScheme, String authSchemeData, MultivaluedMap<String, String> extraProps)
        throws OAuthServiceException {

        Map<String, String> schemeParams = getSchemeParameters(authSchemeData);
        AccessTokenValidation atv =
            getAccessTokenValidation(mc, authScheme, authSchemeData, extraProps, schemeParams);
        if (isRemoteSignatureValidation()) {
            return atv;
        }

        String macKey = atv.getExtraProps().get(OAuthConstants.HAWK_TOKEN_KEY);
        String macAlgo = atv.getExtraProps().get(OAuthConstants.HAWK_TOKEN_ALGORITHM);


        final HttpRequestProperties httpProps;
        if (extraProps != null && extraProps.containsKey(HTTP_VERB) && extraProps.containsKey(HTTP_URI)) {
            httpProps = new HttpRequestProperties(URI.create(extraProps.getFirst(HTTP_URI)),
                                                  extraProps.getFirst(HTTP_VERB));
        } else {
            httpProps = new HttpRequestProperties(mc.getUriInfo().getRequestUri(),
                                                  mc.getHttpServletRequest().getMethod());
        }
        HawkAuthorizationScheme macAuthInfo = new HawkAuthorizationScheme(httpProps, schemeParams);
        String normalizedString = macAuthInfo.getNormalizedRequestString();
        try {
            HmacAlgorithm hmacAlgo = HmacAlgorithm.toHmacAlgorithm(macAlgo);
            byte[] serverMacData = HmacUtils.computeHmac(macKey, hmacAlgo.getJavaName(), normalizedString);

            String clientMacString = schemeParams.get(OAuthConstants.HAWK_TOKEN_SIGNATURE);
            byte[] clientMacData = Base64Utility.decode(clientMacString);
            boolean validMac = MessageDigest.isEqual(serverMacData, clientMacData);
            if (!validMac) {
                AuthorizationUtils.throwAuthorizationFailure(Collections
                    .singleton(OAuthConstants.HAWK_AUTHORIZATION_SCHEME));
            }
        } catch (Base64Exception e) {
            throw new OAuthServiceException(OAuthConstants.SERVER_ERROR, e);
        }
        validateTimestampNonce(macKey, macAuthInfo.getTimestamp(), macAuthInfo.getNonce());
        return atv;
    }

    protected abstract AccessTokenValidation getAccessTokenValidation(MessageContext mc,
                                                                      String authScheme,
                                                                      String authSchemeData,
                                                                      MultivaluedMap<String, String> extraProps,
                                                                      Map<String, String> schemeParams);

    protected static Map<String, String> getSchemeParameters(String authData) {
        String[] attributePairs = authData.split(",");
        Map<String, String> attributeMap = new HashMap<>();
        for (String pair : attributePairs) {
            String[] pairValues = pair.trim().split("=", 2);
            attributeMap.put(pairValues[0].trim(), pairValues[1].trim().replaceAll("\"", ""));
        }
        return attributeMap;
    }

    protected void validateTimestampNonce(String tokenKey, String ts, String nonce) {
        if (nonceVerifier != null) {
            nonceVerifier.verifyNonce(tokenKey, nonce, ts);
        }
    }

    public void setNonceVerifier(NonceVerifier nonceVerifier) {
        this.nonceVerifier = nonceVerifier;
    }

    public boolean isRemoteSignatureValidation() {
        return remoteSignatureValidation;
    }

    public void setRemoteSignatureValidation(boolean remoteSignatureValidation) {
        this.remoteSignatureValidation = remoteSignatureValidation;
    }
}
