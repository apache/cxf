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

import jakarta.ws.rs.Path;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;


/**
 * Redirection-based Implicit Grant Service which returns tokens for the confidential clients
 * directly to a human user.
 */
@Path("/implicit-confidential")
public class ImplicitConfidentialGrantService extends AbstractImplicitGrantService {

    public ImplicitConfidentialGrantService() {
        super(OAuthConstants.TOKEN_RESPONSE_TYPE, OAuthConstants.IMPLICIT_CONFIDENTIAL_GRANT);
    }

    @Override
    protected void processRefreshToken(StringBuilder sb, String refreshToken) {
        sb.append('&').append(OAuthConstants.REFRESH_TOKEN).append('=').append(refreshToken);
    }
    @Override
    protected boolean canSupportPublicClient(Client c) {
        return false;
    }


}


