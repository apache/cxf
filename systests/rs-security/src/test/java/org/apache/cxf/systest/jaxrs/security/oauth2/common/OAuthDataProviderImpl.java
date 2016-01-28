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
package org.apache.cxf.systest.jaxrs.security.oauth2.common;

import java.io.InputStream;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.grants.code.DefaultEHCacheCodeDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.saml.Constants;
import org.apache.cxf.rt.security.crypto.CryptoUtils;

/**
 * Extend the DefaultEHCacheCodeDataProvider to allow refreshing of tokens
 */
public class OAuthDataProviderImpl extends DefaultEHCacheCodeDataProvider {
    
    public OAuthDataProviderImpl(String servicePort) throws Exception {
        // filters/grants test client
        Client client = new Client("consumer-id", "this-is-a-secret", true);
        client.setRedirectUris(Collections.singletonList("http://www.blah.apache.org"));
        
        client.getAllowedGrantTypes().add("authorization_code");
        client.getAllowedGrantTypes().add("refresh_token");
        client.getAllowedGrantTypes().add("implicit");
        client.getAllowedGrantTypes().add("password");
        client.getAllowedGrantTypes().add("client_credentials");
        client.getAllowedGrantTypes().add("urn:ietf:params:oauth:grant-type:saml2-bearer");
        client.getAllowedGrantTypes().add("urn:ietf:params:oauth:grant-type:jwt-bearer");
        
        client.getRegisteredScopes().add("read_balance");
        client.getRegisteredScopes().add("create_balance");
        client.getRegisteredScopes().add("read_data");
        client.getRegisteredScopes().add("read_book");
        client.getRegisteredScopes().add("create_book");
        client.getRegisteredScopes().add("create_image");
        
        this.setClient(client);
        
        // Audience test client
        client = new Client("consumer-id-aud", "this-is-a-secret", true);
        client.setRedirectUris(Collections.singletonList("http://www.blah.apache.org"));
        
        client.getAllowedGrantTypes().add("authorization_code");
        client.getAllowedGrantTypes().add("refresh_token");
        
        client.getRegisteredAudiences().add("https://localhost:" + servicePort 
                                            + "/secured/bookstore/books");
        client.getRegisteredAudiences().add("https://127.0.0.1/test");
        
        this.setClient(client);
        
        // Audience test client 2
        client = new Client("consumer-id-aud2", "this-is-a-secret", true);
        client.setRedirectUris(Collections.singletonList("http://www.blah.apache.org"));
        
        client.getAllowedGrantTypes().add("authorization_code");
        client.getAllowedGrantTypes().add("refresh_token");
        
        client.getRegisteredAudiences().add("https://localhost:" + servicePort 
                                            + "/securedxyz/bookstore/books");
        
        this.setClient(client);
        
        // JAXRSOAuth2Test clients
        client = new Client("alice", "alice", true);
        client.getAllowedGrantTypes().add(Constants.SAML2_BEARER_GRANT);
        client.getAllowedGrantTypes().add("urn:ietf:params:oauth:grant-type:jwt-bearer");
        client.getAllowedGrantTypes().add("custom_grant");
        this.setClient(client);

        Certificate cert = loadCert();
        String encodedCert = Base64Utility.encode(cert.getEncoded());
        
        Client client2 = new Client("CN=whateverhost.com,OU=Morpit,O=ApacheTest,L=Syracuse,C=US", 
                                    null,
                                    true,
                                    null,
                                    null);
        client2.getAllowedGrantTypes().add("custom_grant");
        client2.setApplicationCertificates(Collections.singletonList(encodedCert));
        this.setClient(client2);
    }
    
    private Certificate loadCert() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/org/apache/cxf/systest/http/resources/Truststore.jks");
        return CryptoUtils.loadCertificate(is, new char[]{'p', 'a', 's', 's', 'w', 'o', 'r', 'd'}, "morpit", null);
    }
    
    @Override
    protected boolean isRefreshTokenSupported(List<String> theScopes) {
        return true;
    }
    
    @Override
    public List<OAuthPermission> convertScopeToPermissions(Client client, List<String> requestedScopes) {
        if (requestedScopes.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<OAuthPermission> permissions = new ArrayList<OAuthPermission>();
        for (String requestedScope : requestedScopes) {
            if ("read_book".equals(requestedScope)) {
                OAuthPermission permission = new OAuthPermission();
                permission.setPermission("read_book");
                permission.setHttpVerbs(Collections.singletonList("GET"));
                List<String> uris = new ArrayList<String>();
                String partnerAddress = "/secured/bookstore/books/*";
                uris.add(partnerAddress);
                permission.setUris(uris);
                
                permissions.add(permission);
            } else if ("create_book".equals(requestedScope)) {
                OAuthPermission permission = new OAuthPermission();
                permission.setPermission("create_book");
                permission.setHttpVerbs(Collections.singletonList("POST"));
                List<String> uris = new ArrayList<String>();
                String partnerAddress = "/secured/bookstore/books/*";
                uris.add(partnerAddress);
                permission.setUris(uris);
                
                permissions.add(permission);
            } else if ("create_image".equals(requestedScope)) {
                OAuthPermission permission = new OAuthPermission();
                permission.setPermission("create_image");
                permission.setHttpVerbs(Collections.singletonList("POST"));
                List<String> uris = new ArrayList<String>();
                String partnerAddress = "/secured/bookstore/image/*";
                uris.add(partnerAddress);
                permission.setUris(uris);
                
                permissions.add(permission);
            } else if ("read_balance".equals(requestedScope)) {
                OAuthPermission permission = new OAuthPermission();
                permission.setPermission("read_balance");
                permission.setHttpVerbs(Collections.singletonList("GET"));
                List<String> uris = new ArrayList<String>();
                String partnerAddress = "/partners/balance/*";
                uris.add(partnerAddress);
                permission.setUris(uris);
                
                permissions.add(permission);
            } else if ("create_balance".equals(requestedScope)) {
                OAuthPermission permission = new OAuthPermission();
                permission.setPermission("create_balance");
                permission.setHttpVerbs(Collections.singletonList("POST"));
                List<String> uris = new ArrayList<String>();
                String partnerAddress = "/partners/balance/*";
                uris.add(partnerAddress);
                permission.setUris(uris);
                
                permissions.add(permission);
            } else if ("read_data".equals(requestedScope)) {
                OAuthPermission permission = new OAuthPermission();
                permission.setPermission("read_data");
                permission.setHttpVerbs(Collections.singletonList("GET"));
                List<String> uris = new ArrayList<String>();
                String partnerAddress = "/partners/data/*";
                uris.add(partnerAddress);
                permission.setUris(uris);
                
                permissions.add(permission);
            } else {
                throw new OAuthServiceException("invalid_scope");
            }
        }
        
        return permissions;
    }
}
