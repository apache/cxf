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
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This bean represents a resource owner authorization challenge.
 * Typically, an HTML view will be returned to a resource owner who
 * will authorize or deny the third-party consumer
 */
@XmlRootElement(name = "authorizationData", 
                namespace = "http://org.apache.cxf.rs.security.oauth")
public class OAuthAuthorizationData implements Serializable {
    private static final long serialVersionUID = -7755998413495017637L;
    
    private String clientId;
    private String redirectUri;
    private String state;
    private String proposedScope;
    
    private String authenticityToken;
    private String replyTo;
    
    private String applicationName;
    private String applicationWebUri;
    private String applicationDescription;
    private String applicationLogoUri;
    private List<? extends Permission> permissions;
    
    public OAuthAuthorizationData() {
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public List<? extends Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<? extends Permission> permissions) {
        this.permissions = permissions;
    }

    public void setAuthenticityToken(String authenticityToken) {
        this.authenticityToken = authenticityToken;
    }

    public String getAuthenticityToken() {
        return authenticityToken;
    }

    public void setApplicationDescription(String applicationDescription) {
        this.applicationDescription = applicationDescription;
    }

    public String getApplicationDescription() {
        return applicationDescription;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public void setApplicationWebUri(String applicationWebUri) {
        this.applicationWebUri = applicationWebUri;
    }

    public String getApplicationWebUri() {
        return applicationWebUri;
    }

    public void setApplicationLogoUri(String applicationLogoUri) {
        this.applicationLogoUri = applicationLogoUri;
    }

    public String getApplicationLogoUri() {
        return applicationLogoUri;
    }

    public void setProposedScope(String proposedScope) {
        this.proposedScope = proposedScope;
    }

    public String getProposedScope() {
        return proposedScope;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getReplyTo() {
        return replyTo;
    }

}
