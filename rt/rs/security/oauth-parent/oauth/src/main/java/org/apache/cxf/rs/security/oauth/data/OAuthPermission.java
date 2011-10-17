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
package org.apache.cxf.rs.security.oauth.data;

import java.util.Collections;
import java.util.List;

/**
 * Provides the complete information about a given opaque permission.
 */
public class OAuthPermission extends Permission {
    private List<String> roles = Collections.emptyList();
    private List<String> httpVerbs = Collections.emptyList();
    private String uri;
    private boolean authorizationKeyRequired = true;
    
    public OAuthPermission(String permission, String description, String role) {
        this(permission, description, Collections.singletonList(role));
    }

    public OAuthPermission(String permission, String description, List<String> roles) {
        super(permission, description);
        this.roles = roles;
    }
    
    /**
     * Returns an optional list of role names
     * @return the roles
     */
    public List<String> getRoles() {
        return roles;
    }

    /**
     * Sets an optional list of HTTP verbs 
     * @param httpVerbs the verbs
     */
    public void setHttpVerbs(List<String> httpVerbs) {
        this.httpVerbs = httpVerbs;
    }

    /**
     * Returns an optional list of HTTP verbs
     * @return the list of verbs
     */
    public List<String> getHttpVerbs() {
        return httpVerbs;
    }

    /**
     * Sets an optional URI
     * @param uri the uri
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Returns an optional URI    
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * Can be used to disable the default requirement for all
     * consumer requests to contain an access token
     * @param authorizationKeyRequired the boolean value
     */
    public void setAuthorizationKeyRequired(boolean authorizationKeyRequired) {
        this.authorizationKeyRequired = authorizationKeyRequired;
    }

    /**
     * Indicates if the access token must be present or not
     * @return the boolean value
     */
    public boolean isAuthorizationKeyRequired() {
        return authorizationKeyRequired;
    }
}
