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
package org.apache.cxf.rs.security.oauth.filters;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.rs.security.oauth.data.AccessToken;
import org.apache.cxf.rs.security.oauth.data.OAuthPermission;

/**
 * Captures the information about the current request
 */
public class OAuthInfo {
    private AccessToken token;
    private List<OAuthPermission> permissions;
    public OAuthInfo(AccessToken token,
                     List<OAuthPermission> matchedPermissions) {
        this.token = token;
        this.permissions = matchedPermissions;
    }
    public AccessToken getToken() {
        return token;
    }

    public List<String> getRoles() {
        List<String> authorities = new ArrayList<>();
        for (OAuthPermission permission : permissions) {
            authorities.addAll(permission.getRoles());
        }
        return authorities;
    }

    public List<OAuthPermission> getMatchedPermissions() {
        return permissions;
    }


}
