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
package org.apache.cxf.rs.security.oidc.utils;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jws.JwsException;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthRedirectionState;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oidc.common.AbstractUserInfo;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.idp.OidcProviderMetadata;
import org.apache.cxf.rt.security.crypto.MessageDigestUtils;

public final class OidcUtils {

    public static final String ID_TOKEN_RESPONSE_TYPE = "id_token";
    public static final String ID_TOKEN_AT_RESPONSE_TYPE = "id_token token";
    public static final String CODE_AT_RESPONSE_TYPE = "code token";
    public static final String CODE_ID_TOKEN_RESPONSE_TYPE = "code id_token";
    public static final String CODE_ID_TOKEN_AT_RESPONSE_TYPE = "code id_token token";

    public static final String ID_TOKEN = "id_token";
    public static final String OPENID_SCOPE = "openid";
    public static final String PROFILE_SCOPE = "profile";
    public static final String EMAIL_SCOPE = "email";
    public static final String ADDRESS_SCOPE = "address";
    public static final String PHONE_SCOPE = "phone";

    public static final List<String> PROFILE_CLAIMS =
        Collections.unmodifiableList(Arrays.asList(AbstractUserInfo.NAME_CLAIM,
                                                   AbstractUserInfo.FAMILY_NAME_CLAIM,
                                                   AbstractUserInfo.GIVEN_NAME_CLAIM,
                                                   AbstractUserInfo.MIDDLE_NAME_CLAIM,
                                                   AbstractUserInfo.NICKNAME_CLAIM,
                                                   AbstractUserInfo.PREFERRED_USERNAME_CLAIM,
                                                   AbstractUserInfo.PROFILE_CLAIM,
                                                   AbstractUserInfo.PICTURE_CLAIM,
                                                   AbstractUserInfo.WEBSITE_CLAIM,
                                                   AbstractUserInfo.GENDER_CLAIM,
                                                   AbstractUserInfo.BIRTHDATE_CLAIM,
                                                   AbstractUserInfo.ZONEINFO_CLAIM,
                                                   AbstractUserInfo.LOCALE_CLAIM,
                                                   AbstractUserInfo.UPDATED_AT_CLAIM));
    public static final List<String> EMAIL_CLAIMS =
        Collections.unmodifiableList(Arrays.asList(AbstractUserInfo.EMAIL_CLAIM,
                                                   AbstractUserInfo.EMAIL_VERIFIED_CLAIM));
    public static final List<String> ADDRESS_CLAIMS =
        Collections.unmodifiableList(Arrays.asList(AbstractUserInfo.ADDRESS_CLAIM));
    public static final List<String> PHONE_CLAIMS =
        Collections.unmodifiableList(Arrays.asList(AbstractUserInfo.PHONE_CLAIM));

    public static final String CLAIMS_PARAM = "claims";
    public static final String CLAIM_NAMES_PROPERTY = "_claim_names";
    public static final String CLAIM_SOURCES_PROPERTY = "_claim_sources";
    public static final String JWT_CLAIM_SOURCE_PROPERTY = "JWT";
    public static final String ENDPOINT_CLAIM_SOURCE_PROPERTY = "endpoint";
    public static final String TOKEN_CLAIM_SOURCE_PROPERTY = "access_token";

    public static final String PROMPT_PARAMETER = "prompt";
    public static final String PROMPT_NONE_VALUE = "none";
    public static final String PROMPT_CONSENT_VALUE = "consent";
    public static final String CONSENT_REQUIRED_ERROR = "consent_required";

    private static final Map<String, List<String>> SCOPES_MAP;
    static {
        SCOPES_MAP = new HashMap<>();
        SCOPES_MAP.put(PHONE_SCOPE, PHONE_CLAIMS);
        SCOPES_MAP.put(EMAIL_SCOPE, EMAIL_CLAIMS);
        SCOPES_MAP.put(ADDRESS_SCOPE, ADDRESS_CLAIMS);
        SCOPES_MAP.put(PROFILE_SCOPE, PROFILE_CLAIMS);
    }

    private OidcUtils() {

    }
    public static List<String> getPromptValues(MultivaluedMap<String, String> params) {
        String prompt = params.getFirst(PROMPT_PARAMETER);
        if (prompt != null) {
            return Arrays.asList(prompt.trim().split(" "));
        }
        return Collections.emptyList();
    }

    public static String getOpenIdScope() {
        return OPENID_SCOPE;
    }
    public static String getProfileScope() {
        return getScope(OPENID_SCOPE, PROFILE_SCOPE);
    }
    public static String getEmailScope() {
        return getScope(OPENID_SCOPE, EMAIL_SCOPE);
    }
    public static String getAddressScope() {
        return getScope(OPENID_SCOPE, ADDRESS_SCOPE);
    }
    public static String getPhoneScope() {
        return getScope(OPENID_SCOPE, PHONE_SCOPE);
    }
    public static String getAllScopes() {
        return getScope(OPENID_SCOPE, PROFILE_SCOPE, EMAIL_SCOPE, ADDRESS_SCOPE, PHONE_SCOPE);
    }

    public static List<String> getScopeClaims(String... scope) {
        List<String> claims = new ArrayList<>();
        if (scope != null) {
            for (String s : scope) {
                if (SCOPES_MAP.containsKey(s)) {
                    claims.addAll(SCOPES_MAP.get(s));
                }
            }
        }
        return claims;
    }

    private static String getScope(String... scopes) {
        return String.join(" ", scopes);
    }
    public static void validateAccessTokenHash(ClientAccessToken at, JwtToken jwt) {
        validateAccessTokenHash(at, jwt, true);
    }
    public static void validateAccessTokenHash(ClientAccessToken at, JwtToken jwt, boolean required) {
        validateAccessTokenHash(at.getTokenKey(), jwt, required);
    }
    public static void validateAccessTokenHash(String accessToken, JwtToken jwt, boolean required) {
        String hashClaim = (String)jwt.getClaims().getClaim(IdToken.ACCESS_TOKEN_HASH_CLAIM);
        if (hashClaim == null && required) {
            throw new OAuthServiceException("Invalid hash");
        }
        if (hashClaim != null) {
            validateHash(accessToken,
                         (String)jwt.getClaims().getClaim(IdToken.ACCESS_TOKEN_HASH_CLAIM),
                         jwt.getJwsHeaders().getSignatureAlgorithm());
        }
    }
    public static void validateCodeHash(String code, JwtToken jwt) {
        validateCodeHash(code, jwt, true);
    }
    public static void validateCodeHash(String code, JwtToken jwt, boolean required) {
        String hashClaim = (String)jwt.getClaims().getClaim(IdToken.AUTH_CODE_HASH_CLAIM);
        if (hashClaim == null && required) {
            throw new OAuthServiceException("Invalid hash");
        }
        if (hashClaim != null) {
            validateHash(code,
                         (String)jwt.getClaims().getClaim(IdToken.AUTH_CODE_HASH_CLAIM),
                         jwt.getJwsHeaders().getSignatureAlgorithm());
        }
    }
    private static void validateHash(String value, String theHash, SignatureAlgorithm joseAlgo) {
        String hash = calculateHash(value, joseAlgo);
        if (!hash.equals(theHash)) {
            throw new OAuthServiceException("Invalid hash");
        }
    }
    public static String calculateAccessTokenHash(String value, SignatureAlgorithm sigAlgo) {
        return calculateHash(value, sigAlgo);
    }
    public static String calculateAuthorizationCodeHash(String value, SignatureAlgorithm sigAlgo) {
        return calculateHash(value, sigAlgo);
    }
    private static String calculateHash(String value, SignatureAlgorithm sigAlgo) {
        if (sigAlgo == SignatureAlgorithm.NONE) {
            throw new JwsException(JwsException.Error.INVALID_ALGORITHM);
        }
        String algoShaSizeString = sigAlgo.getJwaName().substring(2);
        String javaShaAlgo = "SHA-" + algoShaSizeString;
        int algoShaSize = Integer.parseInt(algoShaSizeString);
        int valueHashSize = (algoShaSize / 8) / 2;
        try {
            byte[] atBytes = StringUtils.toBytesASCII(value);
            byte[] digest = MessageDigestUtils.createDigest(atBytes,  javaShaAlgo);
            return Base64UrlUtility.encodeChunk(digest, 0, valueHashSize);
        } catch (NoSuchAlgorithmException ex) {
            throw new OAuthServiceException(ex);
        }
    }
    public static void setStateClaimsProperty(OAuthRedirectionState state,
                                              MultivaluedMap<String, String> params) {
        String claims = params.getFirst(OidcUtils.CLAIMS_PARAM);
        if (claims != null) {
            state.getExtraProperties().put(OidcUtils.CLAIMS_PARAM, claims);
        }
    }

    public static OidcProviderMetadata getOidcProviderMetadata(String issuerURL) {
        Response response = WebClient.create(issuerURL).path("/.well-known/openid-configuration")
            .accept(MediaType.APPLICATION_JSON).get();
        if (Status.OK.getStatusCode() != response.getStatus()) {
            throw ExceptionUtils.toWebApplicationException(response);
        }
        return new OidcProviderMetadata(new JsonMapObjectReaderWriter().fromJson(response.readEntity(String.class)));
    }

}
