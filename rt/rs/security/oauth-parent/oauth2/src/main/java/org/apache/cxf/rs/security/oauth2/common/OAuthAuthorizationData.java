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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * This bean represents a resource owner authorization challenge.
 * Typically, an HTML view will be returned to a resource owner who
 * will authorize or deny the third-party client
 */
@XmlRootElement(name = "authorizationData", 
                namespace = "http://org.apache.cxf.rs.security.oauth")
public class OAuthAuthorizationData extends OAuthRedirectionState implements Serializable {
    private static final long serialVersionUID = -7755998413495017637L;
    
    private String endUserName;
    private String authenticityToken;
    private String replyTo;
    private String responseType;
    
    private String applicationName;
    private String applicationWebUri;
    private String applicationDescription;
    private String applicationLogoUri;
    private List<String> applicationCertificates = new LinkedList<String>();
    private Map<String, String> extraApplicationProperties = new HashMap<String, String>();
    private boolean implicitFlow;
    
    private List<? extends Permission> permissions;
    
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

    public Map<String, String> getExtraApplicationProperties() {
        return extraApplicationProperties;
    }

    public void setExtraApplicationProperties(Map<String, String> extraApplicationProperties) {
        this.extraApplicationProperties = extraApplicationProperties;
    }

    public String getEndUserName() {
        return endUserName;
    }

    public void setEndUserName(String endUserName) {
        this.endUserName = endUserName;
    }
    public List<String> getApplicationCertificates() {
        return applicationCertificates;
    }

    public void setApplicationCertificates(List<String> applicationCertificates) {
        this.applicationCertificates = applicationCertificates;
    }

    public boolean isImplicitFlow() {
        return implicitFlow;
    }

    public void setImplicitFlow(boolean implicitFlow) {
        this.implicitFlow = implicitFlow;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }
}
