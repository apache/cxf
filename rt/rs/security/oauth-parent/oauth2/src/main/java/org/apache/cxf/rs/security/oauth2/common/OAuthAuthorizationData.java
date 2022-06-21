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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

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

    private String applicationName;
    private String applicationWebUri;
    private String applicationDescription;
    private String applicationLogoUri;
    private List<String> applicationCertificates = new LinkedList<>();
    private Map<String, String> extraApplicationProperties = new HashMap<>();
    private boolean implicitFlow;

    private List<OAuthPermission> permissions;
    private List<OAuthPermission> alreadyAuthorizedPermissions;
    private String preauthorizedTokenKey;
    private boolean hidePreauthorizedScopesInForm;
    private boolean applicationRegisteredDynamically;
    private boolean supportSinglePageApplications;

    public OAuthAuthorizationData() {
    }

    /**
     * Get the client application name
     * @return application name
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Set the client application name
     * @param applicationName application name
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Get the list of scopes translated to {@link Permission} instances
     * requested by the client application
     * @return the list of scopes
     */
    public List<OAuthPermission> getPermissions() {
        return permissions;
    }

    /**
     * Set the list of scopes translated to {@link OAuthPermission} instances
     * @return the list of scopes
     **/
    public void setPermissions(List<OAuthPermission> permissions) {
        this.permissions = permissions;
    }

    /**
     * Get the list of scopes already approved by a user
     * @return the list of approved scopes
     */
    public List<OAuthPermission> getAlreadyAuthorizedPermissions() {
        return alreadyAuthorizedPermissions;
    }

    /**
     * Set the list of scopes already approved by a user
     * @param perms the list of approved scopes
     */
    public void setAlreadyAuthorizedPermissions(List<OAuthPermission> perms) {
        this.alreadyAuthorizedPermissions = perms;
    }

    /**
     * Set the authenticity token linking the authorization
     * challenge to the current end user session
     *
     * @param authenticityToken the session authenticity token
     */
    public void setAuthenticityToken(String authenticityToken) {
        this.authenticityToken = authenticityToken;
    }

    /**
     * Get the authenticity token linking the authorization
     * challenge to the current end user session
     * @return the session authenticity token
     */
    public String getAuthenticityToken() {
        return authenticityToken;
    }

    /**
     * Set the application description
     * @param applicationDescription the description
     */
    public void setApplicationDescription(String applicationDescription) {
        this.applicationDescription = applicationDescription;
    }

    /**
     * Get the application description
     * @return the description
     */
    public String getApplicationDescription() {
        return applicationDescription;
    }

    /**
     * Set the application web URI
     * @param applicationWebUri the application URI
     */
    public void setApplicationWebUri(String applicationWebUri) {
        this.applicationWebUri = applicationWebUri;
    }

    /**
     * Get the application web URI
     * @return the application URI
     */
    public String getApplicationWebUri() {
        return applicationWebUri;
    }

    /**
     * Set the application logo URI
     * @param applicationLogoUri the logo URI
     */
    public void setApplicationLogoUri(String applicationLogoUri) {
        this.applicationLogoUri = applicationLogoUri;
    }

    /**
     * Get the application logo URI
     * @return the logo URI
     */
    public String getApplicationLogoUri() {
        return applicationLogoUri;
    }

    /**
     * Set the absolute URI where the authorization decision data
     * will need to be sent to
     * @param replyTo authorization decision handler URI
     */
    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    /**
     * Get the absolute URI where the authorization decision data
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

    public boolean isHidePreauthorizedScopesInForm() {
        return hidePreauthorizedScopesInForm;
    }

    public void setHidePreauthorizedScopesInForm(boolean hidePreauthorizedScopesInForm) {
        this.hidePreauthorizedScopesInForm = hidePreauthorizedScopesInForm;
    }
    public List<String> getPermissionsAsStrings() {
        return permissions != null ? OAuthUtils.convertPermissionsToScopeList(permissions)
            : Collections.emptyList();
    }
    public List<String> getAlreadyAuthorizedPermissionsAsStrings() {
        return alreadyAuthorizedPermissions != null
            ? OAuthUtils.convertPermissionsToScopeList(alreadyAuthorizedPermissions)
            : Collections.emptyList();
    }
    public List<OAuthPermission> getAllPermissions() {
        List<OAuthPermission> allPerms = new LinkedList<>();
        if (alreadyAuthorizedPermissions != null) {
            allPerms.addAll(alreadyAuthorizedPermissions);
            if (permissions != null) {
                List<String> list = getAlreadyAuthorizedPermissionsAsStrings();
                for (OAuthPermission perm : permissions) {
                    if (!list.contains(perm.getPermission())) {
                        allPerms.add(perm);
                    }
                }
            }
        } else if (permissions != null) {
            allPerms.addAll(permissions);
        }
        return allPerms;
    }

    public boolean isApplicationRegisteredDynamically() {
        return applicationRegisteredDynamically;
    }

    public void setApplicationRegisteredDynamically(boolean applicationRegisteredDynamically) {
        this.applicationRegisteredDynamically = applicationRegisteredDynamically;
    }

    public boolean isSupportSinglePageApplications() {
        return supportSinglePageApplications;
    }

    public void setSupportSinglePageApplications(boolean supportSinglePageApplications) {
        this.supportSinglePageApplications = supportSinglePageApplications;
    }

    public void setPreauthorizedTokenKey(String preauthorizedTokenKey) {
        this.preauthorizedTokenKey = preauthorizedTokenKey;
    }

    public String getPreauthorizedTokenKey() {
        return this.preauthorizedTokenKey;
    }

}
