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

package org.apache.cxf.rs.security.oauth2.client;

import java.io.IOException;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

public class AccessTokenClientFilter extends AbstractAuthSupplier implements ClientRequestFilter {

    public AccessTokenClientFilter() {
        super(OAuthConstants.BEARER_AUTHORIZATION_SCHEME);
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION,
                                              createAuthorizationHeader());

    }
    protected ClientAccessToken getClientAccessToken() {
        ClientAccessToken at = super.getClientAccessToken();
        if (at.getTokenKey() == null) {
            ClientTokenContext ctx = StaticClientTokenContext.getClientTokenContext();
            if (ctx != null) {
                at = ctx.getToken();
            }
        }
        return at;
    }
}
