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
package org.apache.cxf.rs.security.oidc.rp;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.rs.security.oauth2.client.ClientCodeRequestFilter;
import org.apache.cxf.rs.security.oauth2.client.ClientTokenContext;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oidc.common.ClaimsRequest;
import org.apache.cxf.rs.security.oidc.common.IdToken;
import org.apache.cxf.rs.security.oidc.utils.OidcUtils;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

public class OidcClientCodeRequestFilter extends ClientCodeRequestFilter {

    private static final String ACR_PARAMETER = "acr_values";
    private static final String LOGIN_HINT_PARAMETER = "login_hint";
    private static final String MAX_AGE_PARAMETER = "max_age";
    private static final String PROMPT_PARAMETER = "prompt";
    private static final List<String> PROMPTS = Arrays.asList("none", "consent", "login", "select_account");
    private IdTokenReader idTokenReader;
    private UserInfoClient userInfoClient;
    private List<String> authenticationContextRef;
    private String promptLogin;
    private Long maxAgeOffset;
    private String claims;
    private String claimsLocales;
    private String roleClaim;
    /**
     * The OAuth 2.0 / OIDC response_type to request from the Authorization Endpoint.
     * Defaults to {@code code} (Authorization Code Flow).
     * Set to an Implicit or Hybrid value (e.g. {@code id_token}, {@code code id_token})
     * to enable those flows; a nonce will be auto-generated and enforced for any
     * response type that contains {@code id_token}, per OIDC Core §3.2.2.1 and §3.3.2.1.
     */
    private String responseType;

    public OidcClientCodeRequestFilter() {
        super();
        setScopes("openid");
    }

    public void setAuthenticationContextRef(String acr) {
        this.authenticationContextRef = Arrays.asList(acr.split(" "));
    }

    /**
     * Set the {@code response_type} sent to the Authorization Endpoint.
     * When set to a value that contains {@code id_token} (Implicit or Hybrid flows),
     * the filter will automatically generate a nonce for each authorization request
     * and enforce its presence in the returned ID Token.
     * @param responseType e.g. {@code id_token}, {@code code id_token}, {@code id_token token}
     */
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    /**
     * Returns {@code true} when the configured response type causes an ID Token to be
     * delivered directly from the Authorization Endpoint (Implicit or Hybrid flows),
     * meaning a nonce is REQUIRED per OIDC Core §3.2.2.1 and §3.3.2.1.
     */
    private boolean isNonceRequired() {
        return responseType != null
            && responseType.contains(OidcUtils.ID_TOKEN_RESPONSE_TYPE);
    }

    @Override
    protected ClientTokenContext createTokenContext(ContainerRequestContext rc,
                                                    ClientAccessToken at,
                                                    MultivaluedMap<String, String> requestParams,
                                                    MultivaluedMap<String, String> state) {
        if (rc.getSecurityContext() instanceof OidcSecurityContext) {
            return ((OidcSecurityContext)rc.getSecurityContext()).getOidcContext();
        }
        OidcClientTokenContextImpl ctx = new OidcClientTokenContextImpl();
        if (at != null) {
            if (idTokenReader == null) {
                throw new OAuthServiceException(OAuthConstants.SERVER_ERROR);
            }
            IdToken idToken = idTokenReader.getIdToken(at,
                                  requestParams.getFirst(OAuthConstants.AUTHORIZATION_CODE_VALUE),
                                  getConsumer());
            // Hybrid flow can be detected from context: when the Authorization Endpoint
            // returns an id_token directly in the callback parameters (response_type
            // contains "id_token" together with "code"), a nonce is REQUIRED even if
            // the caller has not explicitly configured a responseType on this filter.
            boolean hybridFlowDetected = requestParams.containsKey(OidcUtils.ID_TOKEN);
            // Validate the properties set up at the redirection time.
            validateIdToken(idToken, state, hybridFlowDetected);

            ctx.setIdToken(idToken);
            if (userInfoClient != null) {
                ctx.setUserInfo(userInfoClient.getUserInfo(at,
                                                           ctx.getIdToken(),
                                                           getConsumer()));
            }
            OidcSecurityContext oidcSecCtx = new OidcSecurityContext(ctx);
            oidcSecCtx.setRoleClaim(roleClaim);
            rc.setSecurityContext(oidcSecCtx);
        }

        return ctx;
    }

    @Override
    protected MultivaluedMap<String, String> toCodeRequestState(ContainerRequestContext rc, UriInfo ui) {
        MultivaluedMap<String, String> state = super.toCodeRequestState(rc, ui);
        if (maxAgeOffset != null) {
            state.putSingle(MAX_AGE_PARAMETER, Long.toString(System.currentTimeMillis() + maxAgeOffset));
        }
        // Per OIDC Core §3.2.2.1 and §3.3.2.1, a nonce is REQUIRED for Implicit and Hybrid flows
        // (any response_type containing "id_token"). Auto-generate one if the caller has not
        // already supplied it, so replay protection is always active for these flows.
        if (isNonceRequired() && state.getFirst(IdToken.NONCE_CLAIM) == null) {
            state.putSingle(IdToken.NONCE_CLAIM,
                Base64UrlUtility.encode(CryptoUtils.generateSecureRandomBytes(16)));
        }
        return state;
    }

    private void validateIdToken(IdToken idToken, MultivaluedMap<String, String> state,
                                    boolean hybridFlowDetected) {

        String nonce = state != null ? state.getFirst(IdToken.NONCE_CLAIM) : null;
        // A nonce is REQUIRED (OIDC Core §3.2.2.1 / §3.3.2.1) when:
        //  (a) the configured responseType contains "id_token" (Implicit or Hybrid), OR
        //  (b) an id_token was observed in the authorization callback parameters, which
        //      indicates a Hybrid flow even without explicit responseType configuration.
        // In either case, reject the response if no nonce was round-tripped — this means
        // the authorization request was sent without one, removing replay protection.
        if ((isNonceRequired() || hybridFlowDetected) && nonce == null) {
            throw new OAuthServiceException(OAuthConstants.INVALID_REQUEST);
        }
        String tokenNonce = idToken.getNonce();
        if (nonce != null && (tokenNonce == null || !nonce.equals(tokenNonce))) {
            throw new OAuthServiceException(OAuthConstants.INVALID_REQUEST);
        }
        if (maxAgeOffset != null) {
            long authTime = Long.parseLong(state.getFirst(MAX_AGE_PARAMETER));
            Long tokenAuthTime = idToken.getAuthenticationTime();
            if (tokenAuthTime > authTime) {
                throw new OAuthServiceException(OAuthConstants.INVALID_REQUEST);
            }
        }

        String acr = idToken.getAuthenticationContextRef();
        // Skip the check if the acr is not set given it is a voluntary claim
        if (acr != null && authenticationContextRef != null && !authenticationContextRef.contains(acr)) {
            throw new OAuthServiceException(OAuthConstants.INVALID_REQUEST);
        }

    }
    public void setIdTokenReader(IdTokenReader idTokenReader) {
        this.idTokenReader = idTokenReader;
    }

    public void setUserInfoClient(UserInfoClient userInfoClient) {
        this.userInfoClient = userInfoClient;
    }

    @Override
    protected void checkSecurityContextStart(ContainerRequestContext rc) {
        SecurityContext sc = rc.getSecurityContext();
        if (!(sc instanceof OidcSecurityContext) && sc.getUserPrincipal() != null) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
    }

    @Override
    protected void setAdditionalCodeRequestParams(UriBuilder ub,
                                                  MultivaluedMap<String, String> redirectState,
                                                  MultivaluedMap<String, String> codeRequestState) {
        // Prefer the nonce from redirectState (managed by the ClientCodeStateManager).
        // Fall back to codeRequestState to cover the auto-generated nonce path used when the
        // state manager does not copy the nonce into its redirect map.
        String nonce = redirectState != null ? redirectState.getFirst(IdToken.NONCE_CLAIM) : null;
        if (nonce == null && codeRequestState != null) {
            nonce = codeRequestState.getFirst(IdToken.NONCE_CLAIM);
        }
        if (nonce != null) {
            ub.queryParam(IdToken.NONCE_CLAIM, nonce);
        }
        if (redirectState != null && redirectState.getFirst(MAX_AGE_PARAMETER) != null) {
            ub.queryParam(MAX_AGE_PARAMETER, redirectState.getFirst(MAX_AGE_PARAMETER));
        }
        if (codeRequestState != null && codeRequestState.getFirst(LOGIN_HINT_PARAMETER) != null) {
            ub.queryParam(LOGIN_HINT_PARAMETER, codeRequestState.getFirst(LOGIN_HINT_PARAMETER));
        }
        if (claims != null) {
            ub.queryParam("claims", claims);
        }
        if (claimsLocales != null) {
            ub.queryParam("claims_locales", claimsLocales);
        }
        if (authenticationContextRef != null) {
            ub.queryParam(ACR_PARAMETER, authenticationContextRef);
        }
        if (promptLogin != null) {
            ub.queryParam(PROMPT_PARAMETER, promptLogin);
        }
        // Override the response_type set by the base filter (which defaults to "code").
        // This is required to support Implicit (id_token) and Hybrid (code id_token, etc.) flows.
        if (responseType != null && !OAuthConstants.CODE_RESPONSE_TYPE.equals(responseType)) {
            ub.replaceQueryParam(OAuthConstants.RESPONSE_TYPE, responseType);
        }

    }

    public void setPromptLogin(String promptLogin) {
        if (PROMPTS.contains(promptLogin)) {
            this.promptLogin = promptLogin;
        } else {
            throw new IllegalArgumentException("Illegal prompt value");
        }
    }

    public void setMaxAgeOffset(Long maxAgeOffset) {
        this.maxAgeOffset = maxAgeOffset;
    }

    public void setClaimsRequest(ClaimsRequest claimsRequest) {
        setClaims(new JsonMapObjectReaderWriter().toJson(claimsRequest));
    }

    public void setClaims(String claims) {
        this.claims = claims;
    }

    public void setClaimsLocales(String claimsLocales) {
        this.claimsLocales = claimsLocales;
    }

    public void setRoleClaim(String roleClaim) {
        this.roleClaim = roleClaim;
    }
}
