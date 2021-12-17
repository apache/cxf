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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManagerFactory;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.grants.code.JPACodeDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.saml.Constants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

/**
 * Extend the JPACodeDataProvider to allow refreshing of tokens
 */
public class JPAOAuthDataProviderImpl extends JPACodeDataProvider {
    private Set<String> externalClients = new HashSet<>();

    public JPAOAuthDataProviderImpl(String servicePort, EntityManagerFactory emf) throws Exception {
        this(servicePort, null, emf);
    }

    public JPAOAuthDataProviderImpl(String servicePort, String partnerPort, EntityManagerFactory emf) throws Exception {
        super();

        super.setEntityManagerFactory(emf);

        // filters/grants test client
        Client client = new Client("consumer-id", "this-is-a-secret", true);
        List<String> redirectUris = new ArrayList<>();
        redirectUris.add("http://www.blah.apache.org");
        if (partnerPort != null) {
            redirectUris.add("https://localhost:" + partnerPort + "/partnerservice/bookstore/books");
        }
        client.setRedirectUris(redirectUris);

        client.getAllowedGrantTypes().add("authorization_code");
        client.getAllowedGrantTypes().add("refresh_token");
        client.getAllowedGrantTypes().add("implicit");
        client.getAllowedGrantTypes().add("hybrid");
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
        client.getRegisteredScopes().add("openid");

        this.setClient(client);

        // OIDC filters test client
        client = new Client("consumer-id-oidc", "this-is-a-secret", true);
        client.setRedirectUris(Arrays.asList(
            "https://localhost:" + servicePort + "/secured/bookstore/books",
            "http://www.blah.apache.org"));

        client.getAllowedGrantTypes().add("authorization_code");
        client.getAllowedGrantTypes().add("refresh_token");

        client.getRegisteredScopes().add("openid");
        client.getRegisteredScopes().add(OAuthConstants.REFRESH_TOKEN_SCOPE);

        this.setClient(client);

        // Audience test client
        client = new Client("consumer-id-aud", "this-is-a-secret", true);
        client.setRedirectUris(Collections.singletonList("http://www.blah.apache.org"));

        client.getAllowedGrantTypes().add("authorization_code");
        client.getAllowedGrantTypes().add("refresh_token");

        client.getRegisteredAudiences().add("https://localhost:" + servicePort
                                            + "/secured/bookstore/books");
        client.getRegisteredAudiences().add("https://127.0.0.1/test");
        client.getRegisteredScopes().add("openid");

        this.setClient(client);

        // Audience test client 2
        client = new Client("consumer-id-aud2", "this-is-a-secret", true);
        client.setRedirectUris(Collections.singletonList("http://www.blah.apache.org"));

        client.getAllowedGrantTypes().add("authorization_code");
        client.getAllowedGrantTypes().add("refresh_token");

        client.getRegisteredAudiences().add("https://localhost:" + servicePort
                                            + "/securedxyz/bookstore/books");
        client.getRegisteredScopes().add("openid");

        this.setClient(client);

        // JAXRSOAuth2Test clients
        client = new Client("alice", "alice", true);
        client.getAllowedGrantTypes().add(Constants.SAML2_BEARER_GRANT);
        client.getAllowedGrantTypes().add("urn:ietf:params:oauth:grant-type:jwt-bearer");
        client.getAllowedGrantTypes().add("custom_grant");
        this.setClient(client);

        client = new Client("fredNoPassword", null, true);
        client.getAllowedGrantTypes().add("custom_grant");
        this.setClient(client);

        client = new Client("fredPublic", null, false);
        client.getAllowedGrantTypes().add("custom_grant");
        this.setClient(client);

        client = new Client("fred", "password", true);
        client.getAllowedGrantTypes().add("custom_grant");
        this.setClient(client);

        // external clients (in LDAP/etc) which can be used for client cred
        externalClients.add("bob:bobPassword");

    }

    @Override
    protected ServerAccessToken createNewAccessToken(Client client, UserSubject userSub) {
        ServerAccessToken token = super.createNewAccessToken(client, userSub);
        token.setNotBefore((System.currentTimeMillis() / 1000L) - 5L);
        return token;
    }

    @Override
    public Client getClient(String clientId) {
        Client c = super.getClient(clientId);
        if (c == null) {
            String clientSecret = super.getCurrentClientSecret();
            if (externalClients.contains(clientId + ":" + clientSecret)) {
                c = new Client(clientId, clientSecret, true);
                c.setTokenEndpointAuthMethod(OAuthConstants.TOKEN_ENDPOINT_AUTH_BASIC);
            }
        }
        return c;

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
            final OAuthPermission permission;
            if ("read_book".equals(requestedScope)) {
                permission = new OAuthPermission("read_book");
                permission.setHttpVerbs(Collections.singletonList("GET"));
                permission.setUris(Collections.singletonList("/secured/bookstore/books/*"));
            } else if ("create_book".equals(requestedScope)) {
                permission = new OAuthPermission("create_book");
                permission.setHttpVerbs(Collections.singletonList("POST"));
                permission.setUris(Collections.singletonList("/secured/bookstore/books/*"));
            } else if ("create_image".equals(requestedScope)) {
                permission = new OAuthPermission("create_image");
                permission.setHttpVerbs(Collections.singletonList("POST"));
                permission.setUris(Collections.singletonList("/secured/bookstore/image/*"));
            } else if ("read_balance".equals(requestedScope)) {
                permission = new OAuthPermission("read_balance");
                permission.setHttpVerbs(Collections.singletonList("GET"));
                permission.setUris(Collections.singletonList("/partners/balance/*"));
            } else if ("create_balance".equals(requestedScope)) {
                permission = new OAuthPermission("create_balance");
                permission.setHttpVerbs(Collections.singletonList("POST"));
                permission.setUris(Collections.singletonList("/partners/balance/*"));
            } else if ("read_data".equals(requestedScope)) {
                permission = new OAuthPermission("read_data");
                permission.setHttpVerbs(Collections.singletonList("GET"));
                permission.setUris(Collections.singletonList("/partners/data/*"));
            } else if ("openid".equals(requestedScope)) {
                permission = new OAuthPermission("openid", "Authenticate user");
            } else if (OAuthConstants.REFRESH_TOKEN_SCOPE.equals(requestedScope)) {
                permission = new OAuthPermission(requestedScope);
            } else {
                throw new OAuthServiceException("invalid_scope");
            }
            permissions.add(permission);
        }

        return permissions;
    }
}