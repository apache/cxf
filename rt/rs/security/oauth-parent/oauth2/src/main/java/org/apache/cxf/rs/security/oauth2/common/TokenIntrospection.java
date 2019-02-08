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
package org.apache.cxf.rs.security.oauth2.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// RFC 7622 Introspection Response
public class TokenIntrospection {
    private boolean active;
    private String scope;
    private String clientId;
    private String username;
    private String tokenType;
    private Long iat;
    private Long exp;
    private Long nbf;
    private String sub;
    private List<String> aud;
    private String iss;
    private String jti;

    private Map<String, String> extensions = new HashMap<>();

    public TokenIntrospection() {

    }

    public TokenIntrospection(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }
    public String getScope() {
        return scope;
    }
    public void setScope(String scope) {
        this.scope = scope;
    }
    public String getClientId() {
        return clientId;
    }
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getTokenType() {
        return tokenType;
    }
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    public Long getIat() {
        return iat;
    }
    public void setIat(Long iat) {
        this.iat = iat;
    }
    public Long getExp() {
        return exp;
    }
    public void setExp(Long exp) {
        this.exp = exp;
    }
    public Long getNbf() {
        return nbf;
    }
    public void setNbf(Long nbf) {
        this.nbf = nbf;
    }
    public String getSub() {
        return sub;
    }
    public void setSub(String sub) {
        this.sub = sub;
    }
    public List<String> getAud() {
        return aud;
    }
    public void setAud(List<String> aud) {
        this.aud = aud;
    }
    public String getIss() {
        return iss;
    }
    public void setIss(String iss) {
        this.iss = iss;
    }
    public String getJti() {
        return jti;
    }
    public void setJti(String jti) {
        this.jti = jti;
    }
    public Map<String, String> getExtensions() {
        return extensions;
    }
    public void setExtensions(Map<String, String> extensions) {
        this.extensions = extensions;
    }
}
