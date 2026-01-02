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

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeGrant;
import org.apache.cxf.transport.http.auth.HttpAuthSupplier;

public class CodeAuthSupplier implements HttpAuthSupplier {
    private volatile String code;
    private BearerAuthSupplier tokenSupplier = new BearerAuthSupplier();
    public CodeAuthSupplier() {
    }

    public boolean requiresRequestCaching() {
        return true;
    }

    public String getAuthorization(AuthorizationPolicy authPolicy,
                                   URI currentURI,
                                   Message message,
                                   String fullHeader) {
        synchronized (tokenSupplier) {
            if (code != null && tokenSupplier.getClientAccessToken().getTokenKey() == null) {
                WebClient wc = tokenSupplier.createAccessTokenServiceClient();
                ClientAccessToken at = OAuthClientUtils.getAccessToken(wc,
                                                tokenSupplier.getConsumer(),
                                                new AuthorizationCodeGrant(code));
                code = null;
                tokenSupplier.setClientAccessToken(at);
            }
        }
        return tokenSupplier.getAuthorization(authPolicy, currentURI, message, fullHeader);
    }

    public void setCode(String code) {
        this.code = code;
    }
    public void setRefreshEarly(boolean refreshEarly) {
        tokenSupplier.setRefreshEarly(refreshEarly);
    }
    public void setAccessTokenServiceUri(String uri) {
        tokenSupplier.setAccessTokenServiceUri(uri);
    }
    public void setConsumer(Consumer consumer) {
        tokenSupplier.setConsumer(consumer);
    }
}
