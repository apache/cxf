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
package org.apache.cxf.rs.security.oauth2.grants.refresh;

import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.AccessTokenGrantHandler;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

public class RefreshTokenGrantHandler implements AccessTokenGrantHandler {

    private OAuthDataProvider dataProvider;
    private boolean partialMatchScopeValidation;
    private boolean useAllClientScopes;

    public void setDataProvider(OAuthDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    public List<String> getSupportedGrantTypes() {
        return Collections.singletonList(OAuthConstants.REFRESH_TOKEN_GRANT);
    }

    public ServerAccessToken createAccessToken(Client client, MultivaluedMap<String, String> params)
        throws OAuthServiceException {
        String refreshToken = params.getFirst(OAuthConstants.REFRESH_TOKEN);
        List<String> requestedScopes = OAuthUtils.getRequestedScopes(client,
                                            params.getFirst(OAuthConstants.SCOPE),
                                            useAllClientScopes,
                                            partialMatchScopeValidation, false);
        final ServerAccessToken st = dataProvider.refreshAccessToken(client, refreshToken, requestedScopes);
        st.setGrantType(OAuthConstants.REFRESH_TOKEN_GRANT);
        return st;
    }

    public void setPartialMatchScopeValidation(boolean partialMatchScopeValidation) {
        this.partialMatchScopeValidation = partialMatchScopeValidation;
    }

    public void setUseAllClientScopes(boolean useAllClientScopes) {
        this.useAllClientScopes = useAllClientScopes;
    }
}
