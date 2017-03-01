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
package org.apache.cxf.rs.security.oauth2.grants.code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.DefaultEncryptingOAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;
import org.apache.cxf.rs.security.oauth2.utils.crypto.ModelEncryptionSupport;
import org.apache.cxf.rt.security.crypto.KeyProperties;

public class DefaultEncryptingCodeDataProvider extends DefaultEncryptingOAuthDataProvider
    implements AuthorizationCodeDataProvider {
    private long grantLifetime;
    private Set<String> grants = Collections.synchronizedSet(new HashSet<>());
    public DefaultEncryptingCodeDataProvider(String algo, int keySize) {
        super(algo, keySize);
    }
    public DefaultEncryptingCodeDataProvider(KeyProperties props) {
        super(props);
    }
    public DefaultEncryptingCodeDataProvider(SecretKey key) {
        super(key);
    }
    @Override
    public Client removeClient(String clientId) {
        Client c = super.removeClient(clientId);
        removeClientCodeGrants(c);
        return c;
    }

    protected void removeClientCodeGrants(Client c) {
        for (ServerAuthorizationCodeGrant grant : getCodeGrants(c, null)) {
            removeCodeGrant(grant.getCode());
        }
    }
    @Override
    public ServerAuthorizationCodeGrant createCodeGrant(AuthorizationCodeRegistration reg)
        throws OAuthServiceException {
        ServerAuthorizationCodeGrant grant = doCreateCodeGrant(reg);
        saveAuthorizationGrant(grant);
        return grant;
    }

    public List<ServerAuthorizationCodeGrant> getCodeGrants(Client c, UserSubject sub) {
        List<ServerAuthorizationCodeGrant> list =
            new ArrayList<>(grants.size());
        for (String key : grants) {
            ServerAuthorizationCodeGrant grant = getCodeGrant(key);
            if (c == null || grant.getClient().getClientId().equals(c.getClientId())) {
                UserSubject grantSub = grant.getSubject();
                if (sub == null || grantSub != null && grantSub.getLogin().equals(sub.getLogin())) {
                    list.add(grant);
                }
            }
        }
        return list;
    }

    @Override
    public ServerAuthorizationCodeGrant removeCodeGrant(String code) throws OAuthServiceException {
        grants.remove(code);
        return ModelEncryptionSupport.decryptCodeGrant(this, code, key);
    }
    public ServerAuthorizationCodeGrant getCodeGrant(String code) throws OAuthServiceException {

        ServerAuthorizationCodeGrant grant = ModelEncryptionSupport.decryptCodeGrant(this, code, key);
        if (grant != null) {
            grants.remove(code);
        }
        return grant;
    }

    protected ServerAuthorizationCodeGrant doCreateCodeGrant(AuthorizationCodeRegistration reg)
        throws OAuthServiceException {
        return AbstractCodeDataProvider.initCodeGrant(reg, grantLifetime);
    }

    protected List<String> getApprovedScopes(AuthorizationCodeRegistration reg) {
        return reg.getApprovedScope();
    }

    protected String getCode(AuthorizationCodeRegistration reg) {
        return OAuthUtils.generateRandomTokenKey();
    }

    public long getGrantLifetime() {
        return grantLifetime;
    }

    public void setGrantLifetime(long lifetime) {
        this.grantLifetime = lifetime;
    }

    protected long getIssuedAt() {
        return OAuthUtils.getIssuedAt();
    }

    protected void saveAuthorizationGrant(ServerAuthorizationCodeGrant grant) {
        String encrypted = ModelEncryptionSupport.encryptCodeGrant(grant, key);
        grant.setCode(encrypted);
        grants.add(encrypted);
    }
}
