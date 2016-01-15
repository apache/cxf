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

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Provides the complete information about a given opaque permission.
 * For example, a scope parameter such as "read_calendar" will be
 * translated into the instance of this class in order to provide
 * the human readable description and optionally restrict it to
 * a limited set of HTTP verbs and request URIs
 */
@XmlRootElement
public class OAuthPermission extends Permission {
    private static final long serialVersionUID = -6486616235830491290L;
    private List<String> httpVerbs = new LinkedList<String>();
    private List<String> uris = new LinkedList<String>();
    
    public OAuthPermission() {
        
    }
    
    public OAuthPermission(String permission) {
        this(permission, null);
    }
    
    public OAuthPermission(String permission, String description) {
        super(permission, description);
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
     * Gets the optional list of HTTP verbs
     * @return the list of HTTP verbs
     */
    public List<String> getHttpVerbs() {
        return httpVerbs;
    }

    /**
     * Sets the optional list of relative request URIs
     * @param uri the list of URIs
     */
    public void setUris(List<String> uri) {
        this.uris = uri;
    }

    /**
     * Gets the optional list of relative request URIs
     * @return the list of URIs
     */
    public List<String> getUris() {
        return uris;
    }
    
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof OAuthPermission) || !super.equals(object)) {
            return false;
        }
        
        OAuthPermission that = (OAuthPermission)object;
        if (this.httpVerbs != null && that.httpVerbs == null
            || this.httpVerbs == null && that.httpVerbs != null
            || this.httpVerbs != null && !this.httpVerbs.equals(that.httpVerbs)) {
            return false;
        }
        if (this.uris != null && that.uris == null || this.uris == null && that.uris != null //NOPMD
            || this.uris != null && !this.uris.equals(that.uris)) { //NOPMD
            return false;
        }
        
        return true;
    }
    
    @Override
    public int hashCode() {
        int hashCode = super.hashCode();
        if (httpVerbs != null) {
            hashCode = 31 * hashCode + httpVerbs.hashCode();
        }
        if (uris != null) {
            hashCode = 31 * hashCode + uris.hashCode();
        }
        
        return hashCode;
    }
}
