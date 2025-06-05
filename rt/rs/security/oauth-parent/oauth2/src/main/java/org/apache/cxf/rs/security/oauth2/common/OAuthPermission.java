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
import java.util.LinkedList;
import java.util.List;

import jakarta.persistence.Cacheable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.xml.bind.annotation.XmlRootElement;


/**
 * Provides the complete information about a given opaque permission.
 * For example, a scope parameter such as "read_calendar" will be
 * translated into the instance of this class in order to provide
 * the human readable description and optionally restrict it to
 * a limited set of HTTP verbs and request URIs
 */
@XmlRootElement
@Entity
@Cacheable
public class OAuthPermission implements Serializable {
    private static final long serialVersionUID = -6486616235830491290L;
    private List<String> httpVerbs = new LinkedList<>();
    private List<String> uris = new LinkedList<>();
    private String permission;
    private String description;
    private boolean isDefaultPermission;
    private boolean invisibleToClient;
    public OAuthPermission() {

    }

    public OAuthPermission(String permission) {
        this.permission = permission;
    }

    public OAuthPermission(String permission, String description) {
        this.description = description;
        this.permission = permission;
    }

    /**
     * Gets the optional list of HTTP verbs
     * @return the list of HTTP verbs
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn
    public List<String> getHttpVerbs() {
        return httpVerbs;
    }

    /**
     * Sets the optional list of HTTP verbs, example,
     * "GET" and "POST", etc
     * @param httpVerbs the list of HTTP verbs
     */
    public void setHttpVerbs(List<String> httpVerbs) {
        this.httpVerbs = httpVerbs;
    }

    /**
     * Gets the optional list of relative request URIs
     * @return the list of URIs
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn
    public List<String> getUris() {
        return uris;
    }

    /**
     * Sets the optional list of relative request URIs
     * @param uri the list of URIs
     */
    public void setUris(List<String> uri) {
        this.uris = uri;
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
    @Id
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
     * @param value true if the permission has been allocated by default
     */
    public void setDefaultPermission(boolean value) {
        this.isDefaultPermission = value;
    }

    public boolean isDefaultPermission() {
        return isDefaultPermission;
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
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof OAuthPermission)) {
            return false;
        }

        if (object == this) {
            return true;
        }

        OAuthPermission that = (OAuthPermission)object;
        if (getHttpVerbs() != null && that.getHttpVerbs() == null
            || getHttpVerbs() == null && that.getHttpVerbs() != null
            || getHttpVerbs() != null && !getHttpVerbs().equals(that.getHttpVerbs())) {
            return false;
        }
        if (getUris() != null && that.getUris() == null
            || getUris() == null && that.getUris() != null
            || getUris() != null && !getUris().equals(that.getUris())) {
            return false;
        }
        if (!getPermission().equals(that.getPermission())) {
            return false;
        }
        if (getDescription() != null && that.getDescription() == null
            || getDescription() == null && that.getDescription() != null
            || getDescription() != null && !getDescription().equals(that.getDescription())) {
            return false;
        }
        if (isInvisibleToClient() != that.isInvisibleToClient()
            || isDefaultPermission() != that.isDefaultPermission()) {
            return false;
        }

        return true;
    }
    @Override
    public int hashCode() {
        int hashCode = 17;
        if (getHttpVerbs() != null) {
            hashCode = 31 * hashCode + getHttpVerbs().hashCode();
        }
        if (getUris() != null) {
            hashCode = 31 * hashCode + getUris().hashCode();
        }
        hashCode = 31 * hashCode + getPermission().hashCode();
        if (getDescription() != null) {
            hashCode = 31 * hashCode + getDescription().hashCode();
        }
        hashCode = 31 * hashCode + Boolean.hashCode(isInvisibleToClient());
        hashCode = 31 * hashCode + Boolean.hashCode(isDefaultPermission());

        return hashCode;
    }
}
