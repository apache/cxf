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
    private List<String> uri = Collections.emptyList();
    private boolean authorizationKeyRequired = true;
    
    public OAuthPermission(String permission, String description, String role) {
        this(permission, description, Collections.singletonList(role));
    }
    
    public OAuthPermission(String permission, String description, List<String> roles) {
        super(permission, description);
        this.roles = roles;
    }
    
    public OAuthPermission(String permission, String description, 
                           List<String> roles, List<String> httpVerbs) {
        this(permission, description, roles);
        this.httpVerbs = httpVerbs;
    }
    
    public OAuthPermission(String permission, 
                           String description, 
                           List<String> roles, 
                           List<String> httpVerbs, 
                           List<String> uris,
                           boolean authorizeKeyRequired) {
        this(permission, description, roles, httpVerbs);
        this.uri = uris;
        this.authorizationKeyRequired = authorizeKeyRequired;
    }
    
    /**
     * Returns an optional list of role names
     * @return the roles
     */
    public List<String> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    /**
     * Returns an optional list of HTTP verbs
     * @return the list of verbs
     */
    public List<String> getHttpVerbs() {
        return Collections.unmodifiableList(httpVerbs);
    }

    /**
     * Returns an optional list of URI    
     * @return the uri
     */
    public List<String> getUris() {
        return Collections.unmodifiableList(uri);
    }

    /**
     * Indicates if the access token must be present or not
     * @return the boolean value
     */
    public boolean isAuthorizationKeyRequired() {
        return authorizationKeyRequired;
    }
}
