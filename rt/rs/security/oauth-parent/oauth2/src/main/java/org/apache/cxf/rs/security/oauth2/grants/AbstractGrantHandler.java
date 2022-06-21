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

package org.apache.cxf.rs.security.oauth2.grants;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenGrantHandler;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;


/**
 * Abstract access token grant handler
 */
public abstract class AbstractGrantHandler implements AccessTokenGrantHandler {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractGrantHandler.class);

    private List<String> supportedGrants;
    private OAuthDataProvider dataProvider;
    private boolean partialMatchScopeValidation;
    private boolean canSupportPublicClients;
    protected AbstractGrantHandler(String grant) {
        supportedGrants = Collections.singletonList(grant);
    }

    protected AbstractGrantHandler(List<String> grants) {
        if (grants.isEmpty()) {
            throw new IllegalArgumentException("The list of grant types can not be empty");
        }
        supportedGrants = grants;
    }

    public void setDataProvider(OAuthDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }
    public OAuthDataProvider getDataProvider() {
        return dataProvider;
    }

    public List<String> getSupportedGrantTypes() {
        return Collections.unmodifiableList(supportedGrants);
    }

    protected String getSingleGrantType() {
        if (supportedGrants.size() > 1) {
            String errorMessage = "Request grant type must be specified";
            LOG.warning(errorMessage);
            throw new WebApplicationException(500);
        }
        return supportedGrants.get(0);
    }

    protected ServerAccessToken doCreateAccessToken(Client client,
                                                    UserSubject subject,
                                                    MultivaluedMap<String, String> params) {

        return doCreateAccessToken(client,
                                   subject,
                                   OAuthUtils.parseScope(params.getFirst(OAuthConstants.SCOPE)),
                                   getAudiences(client, params.getFirst(OAuthConstants.CLIENT_AUDIENCE)));
    }

    protected ServerAccessToken doCreateAccessToken(Client client,
                                                    UserSubject subject,
                                                    List<String> requestedScopes) {

        return doCreateAccessToken(client, subject, getSingleGrantType(), requestedScopes);
    }

    protected ServerAccessToken doCreateAccessToken(Client client,
                                                    UserSubject subject,
                                                    List<String> requestedScopes,
                                                    List<String> audiences) {

        return doCreateAccessToken(client, subject, getSingleGrantType(), requestedScopes,
                                   audiences);
    }

    protected ServerAccessToken doCreateAccessToken(Client client,
                                                    UserSubject subject,
                                                    String requestedGrant,
                                                    List<String> requestedScopes) {
        return doCreateAccessToken(client, subject, requestedGrant, requestedScopes, Collections.emptyList());
    }

    protected ServerAccessToken doCreateAccessToken(Client client,
                                                    UserSubject subject,
                                                    String requestedGrant,
                                                    List<String> requestedScopes,
                                                    List<String> audiences) {
        ServerAccessToken token = getPreAuthorizedToken(client, subject, requestedGrant,
                                                        requestedScopes, audiences);
        if (token != null) {
            return token;
        }

        // Delegate to the data provider to create the one
        AccessTokenRegistration reg = new AccessTokenRegistration();
        reg.setClient(client);
        reg.setGrantType(requestedGrant);
        reg.setSubject(subject);
        reg.setRequestedScope(requestedScopes);
        reg.setApprovedScope(getApprovedScopes(client, subject, requestedScopes));
        reg.setAudiences(audiences);
        return dataProvider.createAccessToken(reg);
    }

    protected List<String> getApprovedScopes(Client client, UserSubject subject, List<String> requestedScopes) {
        // This method can be overridden if the down-scoping is required
        return Collections.emptyList();
    }

    protected ServerAccessToken getPreAuthorizedToken(Client client,
                                                      UserSubject subject,
                                                      String requestedGrant,
                                                      List<String> requestedScopes,
                                                      List<String> audiences) {
        if (!OAuthUtils.validateScopes(requestedScopes, client.getRegisteredScopes(),
                                       partialMatchScopeValidation)) {
            throw new OAuthServiceException(new OAuthError(OAuthConstants.INVALID_SCOPE));
        }
        if (!OAuthUtils.validateAudiences(audiences, client.getRegisteredAudiences())) {
            throw new OAuthServiceException(new OAuthError(OAuthConstants.INVALID_GRANT));
        }

        // Get a pre-authorized token if available
        return dataProvider.getPreauthorizedToken(
                                     client, requestedScopes, subject, requestedGrant);

    }

    public boolean isPartialMatchScopeValidation() {
        return partialMatchScopeValidation;
    }

    public void setPartialMatchScopeValidation(boolean partialMatchScopeValidation) {
        this.partialMatchScopeValidation = partialMatchScopeValidation;
    }

    public void setCanSupportPublicClients(boolean support) {
        canSupportPublicClients = support;
    }

    public boolean isCanSupportPublicClients() {
        return canSupportPublicClients;
    }
    protected List<String> getAudiences(Client client, String clientAudience) {
        if (client.getRegisteredAudiences().isEmpty() && clientAudience == null) {
            return Collections.emptyList();
        }
        if (clientAudience != null) {
            List<String> audiences = Collections.singletonList(clientAudience);
            if (!OAuthUtils.validateAudiences(audiences, client.getRegisteredAudiences())) {
                throw new OAuthServiceException(OAuthConstants.INVALID_GRANT);
            }
            return audiences;
        }
        return client.getRegisteredAudiences();
    }
}
