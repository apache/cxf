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
import java.util.Set;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthAuthorizationData;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;


/**
 * Redirection-based Implicit Grant Service
 *
 * This resource handles the End User authorizing
 * or denying the Client embedded in the Web agent.
 *
 */
@Path("/authorize-implicit")
public class ImplicitGrantService extends AbstractImplicitGrantService {

    public ImplicitGrantService() {
        super(OAuthConstants.TOKEN_RESPONSE_TYPE, OAuthConstants.IMPLICIT_GRANT);
    }
    protected ImplicitGrantService(Set<String> responseTypes) {
        super(responseTypes, OAuthConstants.IMPLICIT_GRANT);
    }
    protected ImplicitGrantService(Set<String> supportedResponseTypes,
                                   String supportedGrantType) {
        super(supportedResponseTypes, supportedGrantType);
    }
    @Override
    protected OAuthAuthorizationData createAuthorizationData(Client client,
                                                             MultivaluedMap<String, String> params,
                                                             String redirectUri,
                                                             UserSubject subject,
                                                             List<OAuthPermission> requestedPerms,
                                                             List<OAuthPermission> alreadyAuthorizedPerms,
                                                             boolean authorizationCanBeSkipped) {
        OAuthAuthorizationData data =
            super.createAuthorizationData(client, params, redirectUri, subject,
                                          requestedPerms, alreadyAuthorizedPerms, authorizationCanBeSkipped);
        data.setImplicitFlow(true);
        return data;
    }
}


