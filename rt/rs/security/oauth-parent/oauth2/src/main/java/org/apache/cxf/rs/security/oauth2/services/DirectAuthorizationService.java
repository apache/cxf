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

package org.apache.cxf.rs.security.oauth2.services;

import java.util.List;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.provider.SubjectCreator;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.security.SecurityContext;


@Path("/authorize-direct")
public class DirectAuthorizationService extends AbstractOAuthService {
    private SubjectCreator subjectCreator;
    private boolean partialMatchScopeValidation;
    private boolean useAllClientScopes;
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Produces("text/html")
    public Response authorize(MultivaluedMap<String, String> params) {
        SecurityContext sc = getAndValidateSecurityContext(params);
        Client client = getClient(params);
        // Create a UserSubject representing the end user
        UserSubject userSubject = createUserSubject(sc, params);


        AccessTokenRegistration reg = new AccessTokenRegistration();
        reg.setClient(client);
        reg.setGrantType(OAuthConstants.DIRECT_TOKEN_GRANT);
        reg.setSubject(userSubject);

        String providedScope = params.getFirst(OAuthConstants.SCOPE);
        List<String> requestedScope = OAuthUtils.getRequestedScopes(client,
                                                           providedScope,
                                                           useAllClientScopes,
                                                           partialMatchScopeValidation);

        reg.setRequestedScope(requestedScope);
        reg.setApprovedScope(requestedScope);
        ServerAccessToken token = getDataProvider().createAccessToken(reg);
        ClientAccessToken clientToken = OAuthUtils.toClientAccessToken(token, isWriteOptionalParameters());
        return Response.ok(clientToken).build();
    }

    protected SecurityContext getAndValidateSecurityContext(MultivaluedMap<String, String> params) {
        SecurityContext securityContext =
            (SecurityContext)getMessageContext().get(SecurityContext.class.getName());
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
        checkTransportSecurity();
        return securityContext;
    }
    protected UserSubject createUserSubject(SecurityContext securityContext,
                                            MultivaluedMap<String, String> params) {
        UserSubject subject;
        if (subjectCreator != null) {
            subject = subjectCreator.createUserSubject(getMessageContext(), params);
            if (subject != null) {
                return subject;
            }
        }

        subject = getMessageContext().getContent(UserSubject.class);
        if (subject != null) {
            return subject;
        }
        return OAuthUtils.createSubject(securityContext);
    }

    public SubjectCreator getSubjectCreator() {
        return subjectCreator;
    }

    public void setSubjectCreator(SubjectCreator subjectCreator) {
        this.subjectCreator = subjectCreator;
    }
    protected Client getClient(MultivaluedMap<String, String> params) {
        Client client = null;

        try {
            client = getValidClient(params.getFirst(OAuthConstants.CLIENT_ID), params);
        } catch (OAuthServiceException ex) {
            if (ex.getError() != null) {
                reportInvalidRequestError(ex.getError(), null);
            }
        }

        if (client == null) {
            reportInvalidRequestError("Client ID is invalid", null);
        }
        return client;

    }

    public boolean isPartialMatchScopeValidation() {
        return partialMatchScopeValidation;
    }

    public void setPartialMatchScopeValidation(boolean partialMatchScopeValidation) {
        this.partialMatchScopeValidation = partialMatchScopeValidation;
    }

    public void setUseAllClientScopes(boolean useAllClientScopes) {
        this.useAllClientScopes = useAllClientScopes;
    }
}


