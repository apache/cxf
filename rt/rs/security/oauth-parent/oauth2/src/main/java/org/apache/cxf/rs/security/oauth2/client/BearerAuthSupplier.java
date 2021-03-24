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

import java.net.URI;
import java.util.Collections;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.provider.OAuthJSONProvider;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.transport.http.auth.HttpAuthSupplier;

public class BearerAuthSupplier extends AbstractAuthSupplier implements HttpAuthSupplier {
    private Consumer consumer;
    private String accessTokenServiceUri;
    private boolean refreshEarly;
    public BearerAuthSupplier() {
        super(OAuthConstants.BEARER_AUTHORIZATION_SCHEME);
    }

    public boolean requiresRequestCaching() {
        return true;
    }

    public String getAuthorization(AuthorizationPolicy authPolicy,
                                   URI currentURI,
                                   Message message,
                                   String fullHeader) {
        if (getClientAccessToken().getTokenKey() == null) {
            return null;
        }


        if (fullHeader == null) {
            // regular authorization
            if (refreshEarly) {
                refreshAccessTokenIfExpired(authPolicy);
            }
            return createAuthorizationHeader();
        }
        // the last call resulted in 401, trying to refresh the token(s)
        if (refreshAccessToken(authPolicy)) {
            return createAuthorizationHeader();
        }
        return null;
    }
    private void refreshAccessTokenIfExpired(AuthorizationPolicy authPolicy) {
        ClientAccessToken at = getClientAccessToken();
        if (OAuthUtils.isExpired(at.getIssuedAt(),
                                 at.getExpiresIn())) {
            refreshAccessToken(authPolicy);
        }

    }


    private boolean refreshAccessToken(AuthorizationPolicy authPolicy) {
        ClientAccessToken at = getClientAccessToken();
        if (at.getRefreshToken() == null) {
            return false;
        }
        // Client id and secret are needed to refresh the tokens
        // AuthorizationPolicy can hold them by default, Consumer can also be injected into this supplier
        // and checked if the policy is null.
        // Client TLS authentication is also fine as an alternative authentication mechanism,
        // how can we check here that a 2-way TLS has been set up ?
        Consumer theConsumer = consumer;
        if (theConsumer == null
            && authPolicy != null && authPolicy.getUserName() != null && authPolicy.getPassword() != null) {
            theConsumer = new Consumer(authPolicy.getUserName(), authPolicy.getPassword());
        }
        if (theConsumer == null) {
            return false;
        }
        // Can WebCient be safely constructed at HttpConduit initialization time ?
        // If yes then createAccessTokenServiceClient() can be called inside
        // setAccessTokenServiceUri, though given that the token refreshment would
        // not be done on every request the current approach is quite reasonable

        WebClient accessTokenService = createAccessTokenServiceClient();
        setClientAccessToken(OAuthClientUtils.refreshAccessToken(accessTokenService, theConsumer, at));
        return true;
    }

    WebClient createAccessTokenServiceClient() {
        return WebClient.create(accessTokenServiceUri, Collections.singletonList(new OAuthJSONProvider()));
    }

    public void setRefreshToken(String refreshToken) {
        getClientAccessToken().setRefreshToken(refreshToken);
    }

    public void setAccessTokenServiceUri(String uri) {
        this.accessTokenServiceUri = uri;
    }

    public Consumer getConsumer() {
        return consumer;
    }
    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

    public void setRefreshEarly(boolean refreshEarly) {
        this.refreshEarly = refreshEarly;
    }

}
