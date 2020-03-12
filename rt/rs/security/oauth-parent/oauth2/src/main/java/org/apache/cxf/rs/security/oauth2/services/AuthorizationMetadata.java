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
package org.apache.cxf.rs.security.oauth2.services;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.json.basic.JsonMapObject;

/**
 * @see <a href="https://tools.ietf.org/html/rfc8414#section-2">Authorization Server Metadata</a>
 */
public class AuthorizationMetadata extends JsonMapObject {

    public static final String ISSUER = "issuer";
    public static final String AUTHORIZATION_ENDPOINT = "authorization_endpoint";
    public static final String TOKEN_ENDPOINT = "token_endpoint";
    public static final String JWKS_URI = "jwks_uri";
    public static final String REGISTRATION_ENDPOINT = "registration_endpoint";
    public static final String SCOPES_SUPPORTED = "scopes_supported";
    public static final String RESPONSE_TYPES_SUPPORTED = "response_types_supported";
    public static final String RESPONSE_MODES_SUPPORTED = "response_modes_supported";
    public static final String GRANT_TYPES_SUPPORTED = "grant_types_supported";
    public static final String TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED = "token_endpoint_auth_methods_supported";
    public static final String TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED =
        "token_endpoint_auth_signing_alg_values_supported";
    public static final String SERVICE_DOCUMENTATION = "service_documentation";
    public static final String UI_LOCALES_SUPPORTED = "ui_locales_supported";
    public static final String OP_POLICY_URI = "op_policy_uri";
    public static final String OP_TOS_URI = "op_tos_uri";
    public static final String REVOCATION_ENDPOINT = "revocation_endpoint";
    public static final String REVOCATION_ENDPOINT_AUTH_METHODS_SUPPORTED =
        "revocation_endpoint_auth_methods_supported";
    public static final String REVOCATION_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED =
        "revocation_endpoint_auth_signing_alg_values_supported";
    public static final String INTROSPECTION_ENDPOINT = "introspection_endpoint";
    public static final String INTROSPECTION_ENDPOINT_AUTH_METHODS_SUPPORTED =
        "introspection_endpoint_auth_methods_supported";
    public static final String INTROSPECTION_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED =
        "introspection_endpoint_auth_signing_alg_values_supported";
    public static final String CODE_CHALLENGE_METHODS_SUPPORTED = "code_challenge_methods_supported";

    public AuthorizationMetadata() {
    }

    public AuthorizationMetadata(Map<String, Object> props) {
        super(new LinkedHashMap<String, Object>(props));
    }

    public URL getIssuer() {
        return getURLProperty(ISSUER);
    }

    public void setIssuer(URL issuer) {
        setURLProperty(ISSUER, issuer);
    }

    public URL getAuthorizationEndpoint() {
        return getURLProperty(AUTHORIZATION_ENDPOINT);
    }

    public void setAuthorizationEndpoint(URL authorizationEndpoint) {
        setURLProperty(AUTHORIZATION_ENDPOINT, authorizationEndpoint);
    }

    public URL getTokenEndpoint() {
        return getURLProperty(TOKEN_ENDPOINT);
    }

    public void setTokenEndpoint(URL tokenEndpoint) {
        setURLProperty(TOKEN_ENDPOINT, tokenEndpoint);
    }

    public URL getJwksURL() {
        return getURLProperty(JWKS_URI);
    }

    public void setJwksURL(URL jwksURL) {
        setURLProperty(JWKS_URI, jwksURL);
    }

    public URL getRegistrationEndpoint() {
        return getURLProperty(REGISTRATION_ENDPOINT);
    }

    public void setRegistrationEndpoint(URL registrationEndpoint) {
        setURLProperty(REGISTRATION_ENDPOINT, registrationEndpoint);
    }

    public List<String> getScopesSupported() {
        return getListStringProperty(SCOPES_SUPPORTED);
    }

    public void setScopesSupported(List<String> scopesSupported) {
        setProperty(SCOPES_SUPPORTED, scopesSupported);
    }

    public List<String> getResponseTypesSupported() {
        return getListStringProperty(RESPONSE_TYPES_SUPPORTED);
    }

    public void setResponseTypesSupported(List<String> responseTypesSupported) {
        setProperty(RESPONSE_TYPES_SUPPORTED, responseTypesSupported);
    }

    public List<String> getResponseModesSupported() {
        return getListStringProperty(RESPONSE_MODES_SUPPORTED);
    }

    public void setResponseModesSupported(List<String> responseModesSupported) {
        setProperty(RESPONSE_MODES_SUPPORTED, responseModesSupported);
    }

    public List<String> getGrantTypesSupported() {
        return getListStringProperty(GRANT_TYPES_SUPPORTED);
    }

    public void setGrantTypesSupported(List<String> grantTypesSupported) {
        setProperty(GRANT_TYPES_SUPPORTED, grantTypesSupported);
    }

    public List<String> getTokenEndpointAuthMethodsSupported() {
        return getListStringProperty(TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED);
    }

    public void setTokenEndpointAuthMethodsSupported(List<String> tokenEndpointAuthMethodsSupported) {
        setProperty(TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED, tokenEndpointAuthMethodsSupported);
    }

    public List<String> getTokenEndpointAuthSigningAlgValuesSupported() {
        return getListStringProperty(TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED);
    }

    public void setTokenEndpointAuthSigningAlgValuesSupported(List<String> tokenEndpointAuthSigningAlgValuesSupported) {
        setProperty(TOKEN_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED, tokenEndpointAuthSigningAlgValuesSupported);
    }

    public URL getServiceDocumentation() {
        return getURLProperty(SERVICE_DOCUMENTATION);
    }

    public void setServiceDocumentation(URL serviceDocumentation) {
        setURLProperty(SERVICE_DOCUMENTATION, serviceDocumentation);
    }

    public List<String> getUiLocalesSupported() {
        return getListStringProperty(UI_LOCALES_SUPPORTED);
    }

    public void setUiLocalesSupported(List<String> uiLocalesSupported) {
        setProperty(UI_LOCALES_SUPPORTED, uiLocalesSupported);
    }

    public URL getOpPolicyURL() {
        return getURLProperty(OP_POLICY_URI);
    }

    public void setOpPolicyURL(URL opPolicyURL) {
        setURLProperty(OP_POLICY_URI, opPolicyURL);
    }

    public URL getOpTosURL() {
        return getURLProperty(OP_TOS_URI);
    }

    public void setOpTosURL(URL opTosURL) {
        setURLProperty(OP_TOS_URI, opTosURL);
    }

    //

    public URL getRevocationEndpoint() {
        return getURLProperty(REVOCATION_ENDPOINT);
    }

    public void setRevocationEndpoint(URL revocationEndpoint) {
        setURLProperty(REVOCATION_ENDPOINT, revocationEndpoint);
    }

    public List<String> getRevocationEndpointAuthMethodsSupported() {
        return getListStringProperty(REVOCATION_ENDPOINT_AUTH_METHODS_SUPPORTED);
    }

    public void setRevocationEndpointAuthMethodsSupported(List<String> revocationEndpointAuthMethodsSupported) {
        setProperty(REVOCATION_ENDPOINT_AUTH_METHODS_SUPPORTED, revocationEndpointAuthMethodsSupported);
    }

    public List<String> getRevocationEndpointAuthSigningAlgValuesSupported() {
        return getListStringProperty(REVOCATION_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED);
    }

    public void setRevocationEndpointAuthSigningAlgValuesSupported(
        List<String> revocationEndpointAuthSigningAlgValuesSupported) {
        setProperty(REVOCATION_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED,
            revocationEndpointAuthSigningAlgValuesSupported);
    }

    public URL getIntrospectionEndpoint() {
        return getURLProperty(INTROSPECTION_ENDPOINT);
    }

    public void setIntrospectionEndpoint(URL introspectionEndpoint) {
        setURLProperty(INTROSPECTION_ENDPOINT, introspectionEndpoint);
    }

    public List<String> getIntrospectionEndpointAuthMethodsSupported() {
        return getListStringProperty(INTROSPECTION_ENDPOINT_AUTH_METHODS_SUPPORTED);
    }

    public void setIntrospectionEndpointAuthMethodsSupported(List<String> introspectionEndpointAuthMethodsSupported) {
        setProperty(INTROSPECTION_ENDPOINT_AUTH_METHODS_SUPPORTED, introspectionEndpointAuthMethodsSupported);
    }

    public List<String> getIntrospectionEndpointAuthSigningAlgValuesSupported() {
        return getListStringProperty(INTROSPECTION_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED);
    }

    public void setIntrospectionEndpointAuthSigningAlgValuesSupported(
        List<String> introspectionEndpointAuthSigningAlgValuesSupported) {
        setProperty(INTROSPECTION_ENDPOINT_AUTH_SIGNING_ALG_VALUES_SUPPORTED,
            introspectionEndpointAuthSigningAlgValuesSupported);
    }

    public List<String> getCodeChallengeMethodsSupported() {
        return getListStringProperty(CODE_CHALLENGE_METHODS_SUPPORTED);
    }

    public void setCodeChallengeMethodsSupported(List<String> codeChallengeMethodsSupported) {
        setProperty(CODE_CHALLENGE_METHODS_SUPPORTED, codeChallengeMethodsSupported);
    }

    protected URL getURLProperty(String name) {
        String s = super.getStringProperty(name);
        if (null != s) {
            try {
                return new URL(s);
            } catch (MalformedURLException e) {
            }
        }
        return null;
    }

    protected void setURLProperty(String name, URL url) {
        super.setProperty(name, url != null ? url.toString() : null);
    }

}
