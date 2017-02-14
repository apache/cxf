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
package org.apache.cxf.sts.token.validator;

import java.security.Principal;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.sts.request.ReceivedToken;

/**
 * This class encapsulates the response from a TokenValidator instance after validating a token.
 */
public class TokenValidatorResponse {

    private Principal principal;
    private Map<String, Object> additionalProperties;
    private String realm;
    private ReceivedToken token;
    private Set<Principal> roles;

    public ReceivedToken getToken() {
        return token;
    }

    public void setToken(ReceivedToken token) {
        this.token = token;
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public void setTokenRealm(String tokenRealm) {
        this.realm = tokenRealm;
    }

    public String getTokenRealm() {
        return realm;
    }

    public Set<Principal> getRoles() {
        return roles;
    }

    public void setRoles(Set<Principal> roles) {
        this.roles = roles;
    }

}
