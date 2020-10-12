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

public class ClientRegistrationResponse extends JsonMapObject {

    public static final String CLIENT_ID = OAuthConstants.CLIENT_ID;
    public static final String CLIENT_SECRET = OAuthConstants.CLIENT_SECRET;
    public static final String REG_ACCESS_TOKEN = "registration_access_token";
    public static final String REG_CLIENT_URI = "registration_client_uri";
    public static final String CLIENT_ID_ISSUED_AT = "client_id_issued_at";
    public static final String CLIENT_SECRET_EXPIRES_AT = "client_secret_expires_at";

    private static final long serialVersionUID = 7114757825909879652L;

    public ClientRegistrationResponse() {
    }

    public ClientRegistrationResponse(Map<String, Object> props) {
        super(new LinkedHashMap<String, Object>(props));
    }

    public void setClientId(String clientId) {
        super.setProperty(CLIENT_ID, clientId);
    }
    public String getClientId() {
        return getStringProperty(CLIENT_ID);
    }
    public void setClientSecret(String clientSecret) {
        super.setProperty(CLIENT_SECRET, clientSecret);
    }
    public String getClientSecret() {
        return getStringProperty(CLIENT_SECRET);
    }
    public void setRegistrationAccessToken(String at) {
        super.setProperty(REG_ACCESS_TOKEN, at);
    }
    public String getRegistrationAccessToken() {
        return getStringProperty(REG_ACCESS_TOKEN);
    }
    public void setRegistrationClientUri(String at) {
        super.setProperty(REG_CLIENT_URI, at);
    }
    public String getRegistrationClientUri() {
        return getStringProperty(REG_CLIENT_URI);
    }
    public void setClientIdIssuedAt(Long issuedAt) {
        super.setProperty(CLIENT_ID_ISSUED_AT, issuedAt);
    }
    public Long getClientIdIssuedAt() {
        return getLongProperty(CLIENT_ID_ISSUED_AT);
    }
    public void setClientSecretExpiresAt(Long expiresAt) {
        super.setProperty(CLIENT_SECRET_EXPIRES_AT, expiresAt);
    }
    public Long getClientSecretExpiresAt() {
        return getLongProperty(CLIENT_SECRET_EXPIRES_AT);
    }
    public void setGrantTypes(List<String> grantTypes) {
        super.setProperty(ClientRegistration.GRANT_TYPES, grantTypes);
    }
    public List<String> getGrantTypes() {
        return getListStringProperty(ClientRegistration.GRANT_TYPES);
    }

}
