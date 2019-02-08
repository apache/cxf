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
package org.apache.cxf.rs.security.oauth2.utils.crypto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeDataProvider;
import org.apache.cxf.rs.security.oauth2.grants.code.AuthorizationCodeRegistration;
import org.apache.cxf.rs.security.oauth2.grants.code.ServerAuthorizationCodeGrant;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;

public class CodeGrantEncryptingDataProvider extends EncryptingDataProvider
    implements AuthorizationCodeDataProvider {

    private Set<String> grants = new HashSet<>();

    public CodeGrantEncryptingDataProvider() throws Exception {
        super();
    }

    @Override
    public ServerAuthorizationCodeGrant createCodeGrant(AuthorizationCodeRegistration reg)
        throws OAuthServiceException {
        ServerAuthorizationCodeGrant grant =
            new ServerAuthorizationCodeGrant(reg.getClient(), 123);
        grant.setAudience(reg.getAudience());
        String encrypted = ModelEncryptionSupport.encryptCodeGrant(grant, key);
        grant.setCode(encrypted);
        grants.add(encrypted);
        return grant;
    }

    @Override
    public ServerAuthorizationCodeGrant removeCodeGrant(String code) throws OAuthServiceException {
        grants.remove(code);
        return ModelEncryptionSupport.decryptCodeGrant(this, code, key);
    }

    @Override
    public List<ServerAuthorizationCodeGrant> getCodeGrants(Client c, UserSubject sub) throws OAuthServiceException {
        return null;
    }
}
