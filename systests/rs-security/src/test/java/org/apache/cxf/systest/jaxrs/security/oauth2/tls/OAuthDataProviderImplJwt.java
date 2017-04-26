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
package org.apache.cxf.systest.jaxrs.security.oauth2.tls;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.grants.code.DefaultEHCacheCodeDataProvider;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

/**
 * Extend the DefaultEHCacheCodeDataProvider to allow refreshing of tokens
 */
public class OAuthDataProviderImplJwt extends DefaultEHCacheCodeDataProvider {
    public OAuthDataProviderImplJwt() throws Exception {

        Client client = new Client("boundJwt",
                                   null,
                                   true,
                                   null,
                                   null);
        client.getProperties().put(OAuthConstants.TLS_CLIENT_AUTH_SUBJECT_DN, 
                                    "CN=whateverhost.com,OU=Morpit,O=ApacheTest,L=Syracuse,C=US");
        client.getAllowedGrantTypes().add("custom_grant");
        this.setClient(client);
        this.setUseJwtFormatForAccessTokens(true);
        this.setStoreJwtTokenKeyOnly(true);
    }

}