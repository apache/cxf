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

import java.io.Serializable;

/**
 * Base permission description
 * @see OAuthAuthorizationData
 */
public class Permission implements Serializable {
    private static final long serialVersionUID = 8988574955042726083L;
    private String permission;
    private String description;
    private boolean isDefault;
    private boolean invisibleToClient;
    
    public Permission() {
        
    }
    
    public Permission(String permission, String description) {
        this.description = description;
        this.permission = permission;
    }
   
    /**
     * Gets the permission description
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the permission description
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the permission value such as "read_calendar"
     * @return the value
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Sets the permission value such as "read_calendar"
     * @param permission the permission value
     */
    public void setPermission(String permission) {
        this.permission = permission;
    }

    /**
     * Indicates if this permission has been allocated by default or not.
     * Authorization View handlers may use this property to optimize the way the user selects the
     * scopes.
     * For example, assume that read', 'add' and 'update' scopes are supported and the 
     * 'read' scope is always allocated. This can be presented at the UI level as follows:
     * the read-only check-box control will represent a 'read' scope and a user will be able to
     * optionally select 'add' and/or 'update' scopes, in addition to the default 'read' one. 
     * @param isDefault true if the permission has been allocated by default
     */
    public void setDefault(boolean value) {
        this.isDefault = value;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isInvisibleToClient() {
        return invisibleToClient;
    }

    /**
     * Set the visibility status; by default all the scopes approved by a user can 
     * be optionally reported to the client in access token responses. Some scopes may need
     * to stay 'invisible' to client.
     * @param invisibleToClient
     */
    public void setInvisibleToClient(boolean invisibleToClient) {
        this.invisibleToClient = invisibleToClient;
    }
}
