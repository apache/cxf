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

package org.apache.cxf.rs.security.oauth.provider;

import java.util.List;


import org.apache.cxf.rs.security.oauth.data.AccessToken;
import org.apache.cxf.rs.security.oauth.data.Client;
import org.apache.cxf.rs.security.oauth.data.OAuthPermission;
import org.apache.cxf.rs.security.oauth.data.RequestToken;
import org.apache.cxf.rs.security.oauth.data.RequestTokenRegistration;


public interface OAuthDataProvider {

    Client getClient(String clientId) throws OAuthServiceException;

    RequestToken createRequestToken(RequestTokenRegistration reg) throws OAuthServiceException;

    RequestToken getRequestToken(String requestToken) throws OAuthServiceException;

    String createRequestTokenVerifier(RequestToken requestToken) throws OAuthServiceException;
    
    AccessToken createAccessToken(RequestToken requestToken) throws OAuthServiceException;

    AccessToken getAccessToken(String accessToken) throws OAuthServiceException;

    void removeTokens(String clientId) throws OAuthServiceException;;

    List<OAuthPermission> getPermissionsInfo(List<String> requestPermissions);
}
