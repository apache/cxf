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
package org.apache.cxf.rs.security.oauth2.grants.clientcred;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.AbstractGrantHandler;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

/**
 * The "client_credentials" grant handler
 */
public class ClientCredentialsGrantHandler extends AbstractGrantHandler {


    public ClientCredentialsGrantHandler() {
        super(OAuthConstants.CLIENT_CREDENTIALS_GRANT);
    }

    public ServerAccessToken createAccessToken(Client client, MultivaluedMap<String, String> params)
        throws OAuthServiceException {

        if (!client.isConfidential()) {
            throw new OAuthServiceException(new OAuthError(OAuthConstants.INVALID_CLIENT));
        }
        
        ServerAccessToken at = doCreateAccessToken(client, client.getSubject(), params);
        if (at.getRefreshToken() != null) {
            LOG.warning("Client credentials grant tokens SHOULD not have refresh tokens");
        }
        return at;
    }


}
