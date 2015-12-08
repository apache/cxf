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
package org.apache.cxf.systest.jaxrs.security.oauth2.grants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.grants.code.DefaultEHCacheCodeDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;

/**
 * Extend the DefaultEHCacheCodeDataProvider to allow refreshing of tokens
 */
public class OAuthDataProviderImpl extends DefaultEHCacheCodeDataProvider {
    
    public OAuthDataProviderImpl() {
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
        
        this.setClient(client);
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
        
        List<OAuthPermission> permissions = new ArrayList<>();
        for (String requestedScope : requestedScopes) {
            if ("read_balance".equals(requestedScope)) {
                OAuthPermission permission = new OAuthPermission();
                permission.setHttpVerbs(Collections.singletonList("GET"));
                List<String> uris = new ArrayList<>();
                String partnerAddress = "/partners/balance/*";
                uris.add(partnerAddress);
                permission.setUris(uris);
                
                permissions.add(permission);
            } else if ("create_balance".equals(requestedScope)) {
                OAuthPermission permission = new OAuthPermission();
                permission.setHttpVerbs(Collections.singletonList("POST"));
                List<String> uris = new ArrayList<>();
                String partnerAddress = "/partners/balance/*";
                uris.add(partnerAddress);
                permission.setUris(uris);
                
                permissions.add(permission);
            } else if ("read_data".equals(requestedScope)) {
                OAuthPermission permission = new OAuthPermission();
                permission.setHttpVerbs(Collections.singletonList("GET"));
                List<String> uris = new ArrayList<>();
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