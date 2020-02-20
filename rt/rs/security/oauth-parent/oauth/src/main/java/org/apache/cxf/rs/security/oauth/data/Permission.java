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

/**
 * Base permission description which is visible to
 * authorization handlers
 * @see OAuthAuthorizationData
 */
public class Permission {
    private String permission;
    private String description;
    private boolean isDefault;

    public Permission() {

    }

    public Permission(String permission, String description) {
        this.description = description;
        this.permission = permission;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    /**
     * Indicates that this permission has been allocated by default.
     * Authorization View handlers may use this property in order to restrict
     * the list of scopes which may be refused to non-default scopes only
     * @param value
     */
    public void setDefault(boolean value) {
        this.isDefault = value;
    }

    public boolean isDefault() {
        return isDefault;
    }
}
