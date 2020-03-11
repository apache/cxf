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
package org.apache.cxf.rs.security.oidc.idp;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.rs.security.oauth2.services.AuthorizationMetadata;

/**
 * @see <a href="https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata">OpenID Provider
 *      Metadata</a>
 */
public class OidcProviderMetadata extends AuthorizationMetadata {

    public static final String USERINFO_ENDPOINT = "userinfo_endpoint";
    public static final String ACR_VALUES_SUPPORTED = "acr_values_supported";
    public static final String SUBJECT_TYPES_SUPPORTED = "subject_types_supported";
    public static final String ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED = "id_token_signing_alg_values_supported";
    public static final String ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED = "id_token_encryption_alg_values_supported";
    public static final String ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED = "id_token_encryption_enc_values_supported";
    public static final String USERINFO_SIGNING_ALG_VALUES_SUPPORTED = "userinfo_signing_alg_values_supported";
    public static final String USERINFO_ENCRYPTION_ALG_VALUES_SUPPORTED = "userinfo_encryption_alg_values_supported";
    public static final String USERINFO_ENCRYPTION_ENC_VALUES_SUPPORTED = "userinfo_encryption_enc_values_supported";
    public static final String REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED =
        "request_object_signing_alg_values_supported";
    public static final String REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED =
        "request_object_encryption_alg_values_supported";
    public static final String REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED =
        "request_object_encryption_enc_values_supported";
    public static final String DISPLAY_VALUES_SUPPORTED = "display_values_supported";
    public static final String CLAIM_TYPES_SUPPORTED = "claim_types_supported";
    public static final String CLAIMS_SUPPORTED = "claims_supported";
    public static final String CLAIM_LOCALES_SUPPORTED = "claims_locales_supported";
    public static final String CLAIMS_PARAMETER_SUPPORTED = "claims_parameter_supported";
    public static final String REQUEST_PARAMETER_SUPPORTED = "request_parameter_supported";
    public static final String REQUEST_URI_PARAMETER_SUPPORTED = "request_uri_parameter_supported";
    public static final String REQUIRE_REQUEST_URI_REGISTRATION = "require_request_uri_registration";

    // https://openid.net/specs/openid-connect-session-1_0.html#OPMetadata
    public static final String CHECK_SESSION_IFRAME = "check_session_iframe";
    public static final String END_SESSION_ENDPOINT = "end_session_endpoint";

    public OidcProviderMetadata() {
    }

    public OidcProviderMetadata(Map<String, Object> props) {
        super(new LinkedHashMap<String, Object>(props));
    }

    public URL getUserinfoEndpoint() {
        return getURLProperty(USERINFO_ENDPOINT);
    }

    public void setUserinfoEndpoint(URL userinfoEndpoint) {
        setURLProperty(USERINFO_ENDPOINT, userinfoEndpoint);
    }

    public List<String> getAcrValuesSupported() {
        return getListStringProperty(ACR_VALUES_SUPPORTED);
    }

    public void setAcrValuesSupported(List<String> acrValuesSupported) {
        setProperty(ACR_VALUES_SUPPORTED, acrValuesSupported);
    }

    public List<String> getSubjectTypesSupported() {
        return getListStringProperty(SUBJECT_TYPES_SUPPORTED);
    }

    public void setSubjectTypesSupported(List<String> subjectTypesSupported) {
        setProperty(SUBJECT_TYPES_SUPPORTED, subjectTypesSupported);
    }

    public List<String> getIdTokenSigningAlgValuesSupported() {
        return getListStringProperty(ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED);
    }

    public void setIdTokenSigningAlgValuesSupported(List<String> idTokenSigningAlgValuesSupported) {
        setProperty(ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED, idTokenSigningAlgValuesSupported);
    }

    public List<String> getIdTokenEncryptionAlgValuesSupported() {
        return getListStringProperty(ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED);
    }

    public void setIdTokenEncryptionAlgValuesSupported(List<String> idTokenEncryptionAlgValuesSupported) {
        setProperty(ID_TOKEN_ENCRYPTION_ALG_VALUES_SUPPORTED, idTokenEncryptionAlgValuesSupported);
    }

    public List<String> getIdTokenEncryptionEncValuesSupported() {
        return getListStringProperty(ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED);
    }

    public void setIdTokenEncryptionEncValuesSupported(List<String> idTokenEncryptionEncValuesSupported) {
        setProperty(ID_TOKEN_ENCRYPTION_ENC_VALUES_SUPPORTED, idTokenEncryptionEncValuesSupported);
    }

    public List<String> getUserinfoSigningAlgValuesSupported() {
        return getListStringProperty(USERINFO_SIGNING_ALG_VALUES_SUPPORTED);
    }

    public void setUserinfoSigningAlgValuesSupported(List<String> userinfoSigningAlgValuesSupported) {
        setProperty(USERINFO_SIGNING_ALG_VALUES_SUPPORTED, userinfoSigningAlgValuesSupported);
    }

    public List<String> getUserinfoEncryptionAlgValuesSupported() {
        return getListStringProperty(USERINFO_ENCRYPTION_ALG_VALUES_SUPPORTED);
    }

    public void setUserinfoEncryptionAlgValuesSupported(List<String> userinfoEncryptionAlgValuesSupported) {
        setProperty(USERINFO_ENCRYPTION_ALG_VALUES_SUPPORTED, userinfoEncryptionAlgValuesSupported);
    }

    public List<String> getUserinfoEncryptionEncValuesSupported() {
        return getListStringProperty(USERINFO_ENCRYPTION_ENC_VALUES_SUPPORTED);
    }

    public void setUserinfoEncryptionEncValuesSupported(List<String> userinfoEncryptionEncValuesSupported) {
        setProperty(USERINFO_ENCRYPTION_ENC_VALUES_SUPPORTED, userinfoEncryptionEncValuesSupported);
    }

    public List<String> getRequestObjectSigningAlgValuesSupported() {
        return getListStringProperty(REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED);
    }

    public void setRequestObjectSigningAlgValuesSupported(List<String> requestObjectSigningAlgValuesSupported) {
        setProperty(REQUEST_OBJECT_SIGNING_ALG_VALUES_SUPPORTED, requestObjectSigningAlgValuesSupported);
    }

    public List<String> getRequestObjectEncryptionAlgValuesSupported() {
        return getListStringProperty(REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED);
    }

    public void setRequestObjectEncryptionAlgValuesSupported(List<String> requestObjectEncryptionAlgValuesSupported) {
        setProperty(REQUEST_OBJECT_ENCRYPTION_ALG_VALUES_SUPPORTED, requestObjectEncryptionAlgValuesSupported);
    }

    public List<String> getRequestObjectEncryptionEncValuesSupported() {
        return getListStringProperty(REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED);
    }

    public void setRequestObjectEncryptionEncValuesSupported(List<String> requestObjectEncryptionEncValuesSupported) {
        setProperty(REQUEST_OBJECT_ENCRYPTION_ENC_VALUES_SUPPORTED, requestObjectEncryptionEncValuesSupported);
    }

    public List<String> getDisplayValuesSupported() {
        return getListStringProperty(DISPLAY_VALUES_SUPPORTED);
    }

    public void setDisplayValuesSupported(List<String> displayValuesSupported) {
        setProperty(DISPLAY_VALUES_SUPPORTED, displayValuesSupported);
    }

    public List<String> getClaimTypesSupported() {
        return getListStringProperty(CLAIM_TYPES_SUPPORTED);
    }

    public void setClaimTypesSupported(List<String> claimTypesSupported) {
        setProperty(CLAIM_TYPES_SUPPORTED, claimTypesSupported);
    }

    public List<String> getClaimsSupported() {
        return getListStringProperty(CLAIMS_SUPPORTED);
    }

    public void setClaimsSupported(List<String> claimsSupported) {
        setProperty(CLAIMS_SUPPORTED, claimsSupported);
    }

    public List<String> getClaimsLocalesSupported() {
        return getListStringProperty(CLAIM_LOCALES_SUPPORTED);
    }

    public void setClaimsLocalesSupported(List<String> claimsLocalesSupported) {
        setProperty(CLAIM_LOCALES_SUPPORTED, claimsLocalesSupported);
    }

    public Boolean getClaimsParameterSupported() {
        return getBooleanProperty(CLAIMS_PARAMETER_SUPPORTED);
    }

    public void setClaimsParameterSupported(Boolean claimsParameterSupported) {
        setProperty(CLAIMS_PARAMETER_SUPPORTED, claimsParameterSupported);
    }

    public Boolean getRequestParameterSupported() {
        return getBooleanProperty(REQUEST_PARAMETER_SUPPORTED);
    }

    public void setRequestParameterSupported(Boolean requestParameterSupported) {
        setProperty(REQUEST_PARAMETER_SUPPORTED, requestParameterSupported);
    }

    public Boolean getRequestURIParameterSupported() {
        return getBooleanProperty(REQUEST_URI_PARAMETER_SUPPORTED);
    }

    public void setRequestURIParameterSupported(Boolean requestURIParameterSupported) {
        setProperty(REQUEST_URI_PARAMETER_SUPPORTED, requestURIParameterSupported);
    }

    public Boolean getRequireRequestURIRegistration() {
        return getBooleanProperty(REQUIRE_REQUEST_URI_REGISTRATION);
    }

    public void setRequireRequestURIRegistration(Boolean requireRequestURIRegistration) {
        setProperty(REQUIRE_REQUEST_URI_REGISTRATION, requireRequestURIRegistration);
    }

    public URL getCheckSessionIframe() {
        return getURLProperty(CHECK_SESSION_IFRAME);
    }

    public void setCheckSessionIframe(URL checkSessionIframe) {
        setURLProperty(CHECK_SESSION_IFRAME, checkSessionIframe);
    }

    public URL getEndSessionEndpoint() {
        return getURLProperty(END_SESSION_ENDPOINT);
    }

    public void setEndSessionEndpoint(URL endSessionEndpoint) {
        setURLProperty(END_SESSION_ENDPOINT, endSessionEndpoint);
    }

}
