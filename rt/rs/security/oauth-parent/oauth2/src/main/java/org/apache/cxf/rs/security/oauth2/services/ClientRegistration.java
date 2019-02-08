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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

public class ClientRegistration extends JsonMapObject {
    public static final String REDIRECT_URIS = "redirect_uris";
    public static final String RESPONSE_TYPES = "response_types";
    public static final String GRANT_TYPES = "grant_types";
    public static final String APPLICATION_TYPE = "application_type";
    public static final String CONTACTS = "contacts";
    public static final String CLIENT_NAME = "client_name";
    public static final String LOGO_URI = "logo_uri";
    public static final String CLIENT_URI = "client_uri";
    public static final String POLICY_URI = "policy_uri";
    public static final String TOS_URI = "tos_uri";
    public static final String TOKEN_ENDPOINT_AUTH_METHOD = "token_endpoint_auth_method";
    public static final String SCOPE = OAuthConstants.SCOPE;
    // Extension - an array of resource audiences, the name is based on combining
    // a property 'resource' (the resource indicators draft) and "_uris", similar to
    // a "redirect_uris" property. This property name may change in the future.
    public static final String RESOURCE_URIS = "resource_uris";


    private static final long serialVersionUID = 7903976943604132150L;

    public ClientRegistration() {
    }

    public ClientRegistration(Map<String, Object> props) {
        super(new LinkedHashMap<String, Object>(props));
    }

    public void setRedirectUris(List<String> redirectUris) {
        super.setProperty(REDIRECT_URIS, redirectUris);
    }
    public List<String> getRedirectUris() {
        return getListStringProperty(REDIRECT_URIS);
    }

    public void setResourceUris(List<String> redirectUris) {
        super.setProperty(RESOURCE_URIS, redirectUris);
    }
    public List<String> getResourceUris() {
        return getListStringProperty(RESOURCE_URIS);
    }

    public void setResponseTypes(List<String> responseTypes) {
        super.setProperty(RESPONSE_TYPES, responseTypes);
    }
    public List<String> getResponseTypes() {
        return getListStringProperty(RESPONSE_TYPES);
    }
    public void setGrantTypes(List<String> grantTypes) {
        super.setProperty(GRANT_TYPES, grantTypes);
    }
    public List<String> getGrantTypes() {
        return getListStringProperty(GRANT_TYPES);
    }
    public void setApplicationType(String applicationType) {
        super.setProperty(APPLICATION_TYPE, applicationType);
    }
    public String getApplicationType() {
        return getStringProperty(APPLICATION_TYPE);
    }
    public void setContacts(List<String> contacts) {
        super.setProperty(CONTACTS, contacts);
    }
    public List<String> getContacts() {
        return getListStringProperty(CONTACTS);
    }
    public void setClientName(String clientName) {
        super.setProperty(CLIENT_NAME, clientName);
    }
    public String getClientName() {
        return getStringProperty(CLIENT_NAME);
    }
    public void setLogoUri(String logoUri) {
        super.setProperty(LOGO_URI, logoUri);
    }
    public String getLogoUri() {
        return getStringProperty(LOGO_URI);
    }
    public void setClientUri(String clientUri) {
        super.setProperty(CLIENT_URI, clientUri);
    }
    public String getClientUri() {
        return getStringProperty(CLIENT_URI);
    }
    public void setPolicyUri(String policyUri) {
        super.setProperty(POLICY_URI, policyUri);
    }
    public String getPolicyUri() {
        return getStringProperty(POLICY_URI);
    }
    public void setTosUri(String tosUri) {
        super.setProperty(TOS_URI, tosUri);
    }
    public String getTosUri() {
        return getStringProperty(TOS_URI);
    }
    public void setTokenEndpointAuthMethod(String method) {
        super.setProperty(TOKEN_ENDPOINT_AUTH_METHOD, method);
    }
    public String getTokenEndpointAuthMethod() {
        return getStringProperty(TOKEN_ENDPOINT_AUTH_METHOD);
    }
    public void setScope(String scope) {
        super.setProperty(SCOPE, scope);
    }
    public String getScope() {
        return getStringProperty(SCOPE);
    }

}
