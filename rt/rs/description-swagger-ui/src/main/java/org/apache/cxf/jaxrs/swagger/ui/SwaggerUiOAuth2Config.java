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

package org.apache.cxf.jaxrs.swagger.ui;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Swagger UI OAuth2 configuration parameters, to be injected into swagger initialization JS code (call to initOAuth()).
 * @author Gaetan Pitteloud
 * @see org.apache.cxf.jaxrs.swagger.ui.SwaggerUiService
 */
public class SwaggerUiOAuth2Config {

    private String clientId;
    private String clientSecret;
    private String realm;
    private String appName;
    private List<String> scopes;
    private Map<String, String> additionalQueryStringParams;
    private Boolean useBasicAuthenticationWithAccessCodeGrant;
    private Boolean usePkceWithAuthorizationCodeGrant;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public SwaggerUiOAuth2Config clientId(String cid) {
        setClientId(cid);
        return this;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public SwaggerUiOAuth2Config clientSecret(String secret) {
        setClientSecret(secret);
        return this;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public SwaggerUiOAuth2Config realm(String re) {
        setRealm(re);
        return this;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public SwaggerUiOAuth2Config appName(String name) {
        setAppName(name);
        return this;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public SwaggerUiOAuth2Config scopes(List<String> scopesList) {
        setScopes(scopesList);
        return this;
    }

    public Map<String, String> getAdditionalQueryStringParams() {
        return additionalQueryStringParams;
    }

    public void setAdditionalQueryStringParams(Map<String, String> additionalQueryStringParams) {
        this.additionalQueryStringParams = additionalQueryStringParams;
    }

    public SwaggerUiOAuth2Config additionalQueryStringParams(Map<String, String> additionalParams) {
        setAdditionalQueryStringParams(additionalParams);
        return this;
    }

    public Boolean getUseBasicAuthenticationWithAccessCodeGrant() {
        return useBasicAuthenticationWithAccessCodeGrant;
    }

    public void setUseBasicAuthenticationWithAccessCodeGrant(Boolean useBasicAuthenticationWithAccessCodeGrant) {
        this.useBasicAuthenticationWithAccessCodeGrant = useBasicAuthenticationWithAccessCodeGrant;
    }

    public SwaggerUiOAuth2Config useBasicAuthenticationWithAccessCodeGrant(Boolean basicAuth) {
        setUseBasicAuthenticationWithAccessCodeGrant(basicAuth);
        return this;
    }

    public Boolean getUsePkceWithAuthorizationCodeGrant() {
        return usePkceWithAuthorizationCodeGrant;
    }

    public void setUsePkceWithAuthorizationCodeGrant(Boolean usePkceWithAuthorizationCodeGrant) {
        this.usePkceWithAuthorizationCodeGrant = usePkceWithAuthorizationCodeGrant;
    }

    public SwaggerUiOAuth2Config usePkceWithAuthorizationCodeGrant(Boolean pkce) {
        setUsePkceWithAuthorizationCodeGrant(pkce);
        return this;
    }

    /**
     * Print this object as json, so that it can be injected into JavaScript code
     * @return JSON for this object
     */
    public String toJsonString() {
        // don't add a json dependency
        final StringBuilder json = new StringBuilder("{");
        addStringField(json, "clientId", clientId);
        addStringField(json, "clientSecret", clientSecret);
        addStringField(json, "realm", realm);
        addStringField(json, "appName", appName);
        addListField(json, "scopes", scopes);
        addMapField(json, "additionalQueryStringParams", additionalQueryStringParams);
        addBooleanField(json, "useBasicAuthenticationWithAccessCodeGrant", useBasicAuthenticationWithAccessCodeGrant);
        addBooleanField(json, "usePkceWithAuthorizationCodeGrant", usePkceWithAuthorizationCodeGrant);
        // remove last printed ","
        if (json.toString().endsWith(",")) {
            json.delete(json.length() - 1, json.length());
        }
        return json.append('}').toString();
    }

    private void addStringField(StringBuilder json, String name, String value) {
        if (value != null) {
            json.append(quote(name)).append(':').append(quote(value)).append(',');
        }
    }

    private void addBooleanField(StringBuilder json, String name, Boolean value) {
        if (value != null) {
            json.append(quote(name)).append(':').append(value).append(',');
        }
    }

    private void addListField(StringBuilder json, String name, List<String> value) {
        if (value != null) {
            json.append(quote(name)).append(':').append(
                    value.stream().map(this::quote)
                            .collect(Collectors.joining(",", "[", "]"))
            ).append(',');
        }
    }

    private void addMapField(StringBuilder json, String name, Map<String, String> value) {
        if (value != null) {
            json.append(quote(name)).append(':').append(
                    value.entrySet().stream().map(this::entryToString)
                            .collect(Collectors.joining(",", "{", "}"))
            ).append(',');
        }
    }

    private String quote(String s) {
        return '"' + s + '"';
    }

    private String entryToString(Map.Entry<String, String> entry) {
        return quote(entry.getKey()) + ':' + quote(entry.getValue());
    }
}
