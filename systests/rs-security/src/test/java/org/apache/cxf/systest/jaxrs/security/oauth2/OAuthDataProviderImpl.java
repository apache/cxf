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
package org.apache.cxf.systest.jaxrs.security.oauth2;

import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenRegistration;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.saml.Constants;
import org.apache.cxf.rs.security.oauth2.tokens.bearer.BearerAccessToken;
import org.apache.cxf.rs.security.oauth2.tokens.refresh.RefreshToken;
import org.apache.cxf.rt.security.crypto.CryptoUtils;


public class OAuthDataProviderImpl implements OAuthDataProvider {

    private Map<String, Client> clients = new HashMap<String, Client>();
    
    public OAuthDataProviderImpl() throws Exception {
        Client client = new Client("alice", "alice", true);
        client.getAllowedGrantTypes().add(Constants.SAML2_BEARER_GRANT);
        client.getAllowedGrantTypes().add("urn:ietf:params:oauth:grant-type:jwt-bearer");
        client.getAllowedGrantTypes().add("custom_grant");
        clients.put(client.getClientId(), client);

        
        Certificate cert = loadCert();
        String encodedCert = Base64Utility.encode(cert.getEncoded());
        
        Client client2 = new Client("CN=whateverhost.com,OU=Morpit,O=ApacheTest,L=Syracuse,C=US", 
                                    null,
                                    true,
                                    null,
                                    null);
        client2.getAllowedGrantTypes().add("custom_grant");
        client2.setApplicationCertificates(Collections.singletonList(encodedCert));
        clients.put(client2.getClientId(), client2);
    }

    private Certificate loadCert() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/org/apache/cxf/systest/http/resources/Truststore.jks");
        return CryptoUtils.loadCertificate(is, new char[]{'p', 'a', 's', 's', 'w', 'o', 'r', 'd'}, "morpit", null);
    }

    @Override
    public Client getClient(String clientId) throws OAuthServiceException {
        return clients.get(clientId);
    }

    @Override
    public ServerAccessToken createAccessToken(AccessTokenRegistration accessToken)
        throws OAuthServiceException {
        return new BearerAccessToken(accessToken.getClient(), 3600);
    }

    @Override
    public ServerAccessToken getAccessToken(String accessToken) throws OAuthServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServerAccessToken getPreauthorizedToken(Client client, List<String> requestedScopes,
                                                   UserSubject subject, String grantType)
        throws OAuthServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServerAccessToken refreshAccessToken(Client client, String refreshToken,
                                                List<String> requestedScopes) throws OAuthServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<OAuthPermission> convertScopeToPermissions(Client client, List<String> requestedScope) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void revokeToken(Client client, String token, String tokenTypeHint) throws OAuthServiceException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<ServerAccessToken> getAccessTokens(Client client) throws OAuthServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<RefreshToken> getRefreshTokens(Client client) throws OAuthServiceException {
        // TODO Auto-generated method stub
        return null;
    }

}
