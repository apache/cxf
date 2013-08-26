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

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This bean represents a resource owner authorization challenge.
 * Typically, an HTML view will be returned to a resource owner who
 * will authorize or deny the third-party client
 */
@XmlRootElement(name = "authorizationData", 
                namespace = "http://org.apache.cxf.rs.security.oauth")
public class OAuthAuthorizationData implements Serializable {
    private static final long serialVersionUID = -7755998413495017637L;
    
    private String clientId;
    private String endUserName;
    private String redirectUri;
    private String state;
    private String proposedScope;
    
    private String authenticityToken;
    private String replyTo;
    
    private String applicationName;
    private String applicationWebUri;
    private String applicationDescription;
    private String applicationLogoUri;
    private List<Property> extraApplicationProperties = new LinkedList<Property>();
    
    private List<? extends Permission> permissions;
    private String audience;
    
    public OAuthAuthorizationData() {
    }

    /**
     * Sets the client application name
     * @return application name
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Sets the client application name
     * @param applicationName application name
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Gets the list of scopes translated to {@link Permission} instances
     * requested by the client application
     * @return the list of scopes
     */
    public List<? extends Permission> getPermissions() {
        return permissions;
    }

    /**
     * Gets the list of scopes translated to {@link Permission} instances
     * @return the list of scopses
     **/
    public void setPermissions(List<? extends Permission> permissions) {
        this.permissions = permissions;
    }

    /**
     * Sets the authenticity token linking the authorization 
     * challenge to the current end user session
     * 
     * @param authenticityToken the session authenticity token 
     */
    public void setAuthenticityToken(String authenticityToken) {
        this.authenticityToken = authenticityToken;
    }

    /**
     * Gets the authenticity token linking the authorization 
     * challenge to the current end user session
     * @return the session authenticity token
     */
    public String getAuthenticityToken() {
        return authenticityToken;
    }

    /**
     * Sets the application description
     * @param applicationDescription the description
     */
    public void setApplicationDescription(String applicationDescription) {
        this.applicationDescription = applicationDescription;
    }

    /**
     * Gets the application description
     * @return the description
     */
    public String getApplicationDescription() {
        return applicationDescription;
    }

    /**
     * Sets the client id which needs to be retained in a hidden form field
     * @param clientId the client id
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Gets the client id which needs to be retained in a hidden form field
     * @return the client id
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Sets the redirect uri which needs to be retained in a hidden form field
     * @param redirectUri the redirect uri
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /**
     * Gets the redirect uri which needs to be retained in a hidden form field
     * @return the redirect uri
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Sets the client state token which needs to be retained in a hidden form field
     * @param state the state
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Gets the client state token which needs to be retained in a hidden form field
     * @return
     */
    public String getState() {
        return state;
    }

    /**
     * Sets the application web URI
     * @param applicationWebUri the application URI
     */
    public void setApplicationWebUri(String applicationWebUri) {
        this.applicationWebUri = applicationWebUri;
    }

    /**
     * Gets the application web URI
     * @return the application URI
     */
    public String getApplicationWebUri() {
        return applicationWebUri;
    }

    /**
     * Sets the application logo URI
     * @param applicationLogoUri the logo URI
     */
    public void setApplicationLogoUri(String applicationLogoUri) {
        this.applicationLogoUri = applicationLogoUri;
    }

    /**
     * Gets the application logo URI
     * @return the logo URI
     */
    public String getApplicationLogoUri() {
        return applicationLogoUri;
    }

    /**
     * Sets the requested scope which needs to be retained in a hidden form field
     * @param proposedScope the scope
     */
    public void setProposedScope(String proposedScope) {
        this.proposedScope = proposedScope;
    }

    /**
     * Gets the requested scope which needs to be retained in a hidden form field
     * @return the scope
     */
    public String getProposedScope() {
        return proposedScope;
    }

    /**
     * Sets the absolute URI where the authorization decision data 
     * will need to be sent to
     * @param replyTo authorization decision handler URI
     */
    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    /**
     * Gets the absolute URI where the authorization decision data 
     * will need to be sent to
     * @return authorization decision handler URI
     */
    public String getReplyTo() {
        return replyTo;
    }

    public List<Property> getExtraApplicationProperties() {
        return extraApplicationProperties;
    }

    public void setExtraApplicationProperties(List<Property> extraApplicationProperties) {
        this.extraApplicationProperties = extraApplicationProperties;
    }

    public String getEndUserName() {
        return endUserName;
    }

    public void setEndUserName(String endUserName) {
        this.endUserName = endUserName;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

}
