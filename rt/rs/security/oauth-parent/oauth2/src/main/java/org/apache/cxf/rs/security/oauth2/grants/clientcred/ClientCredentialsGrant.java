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
import org.apache.cxf.rs.security.oauth2.grants.AbstractGrant;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

public class ClientCredentialsGrant extends AbstractGrant {

    private static final long serialVersionUID = 5586488165697954347L;
    private String clientId;
    private String clientSecret;
    public ClientCredentialsGrant() {
        this(null);
    }

    public ClientCredentialsGrant(String scope) {
        this(scope, null);
    }

    public ClientCredentialsGrant(String scope, String audience) {
        super(OAuthConstants.CLIENT_CREDENTIALS_GRANT, scope, audience);
    }

    public MultivaluedMap<String, String> toMap() {
        MultivaluedMap<String, String> map = super.toMap();
        if (clientId != null) {
            map.putSingle(OAuthConstants.CLIENT_ID, clientId);
            if (clientSecret != null) {
                map.putSingle(OAuthConstants.CLIENT_SECRET, clientSecret);

            }
        }
        return map;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
