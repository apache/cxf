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

package org.apache.cxf.rs.security.oauth.grants.code;

import java.util.List;

import org.apache.cxf.rs.security.oauth.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth.provider.OAuthServiceException;

/**
 * OAuth provider responsible for persisting the information about 
 * OAuth consumers, request and access tokens.
 */
public interface AuthorizationCodeDataProvider extends OAuthDataProvider {

    /**
     * Converts the requested scope to the list of permissions  
     * @param requestedScope
     * @return list of permissions
     */
    List<OAuthPermission> convertScopeToPermissions(List<String> requestedScope);
    
    /**
     * Creates a temporarily code grant which will capture the
     * information about the {@link Client} attempting to access or
     * modify the resource owner's resource 
     * @param reg AuthorizationCodeRegistration
     * @return new code grant
     * @see AuthorizationCodeRegistration
     * @throws OAuthServiceException
     */
    ServerAuthorizationCodeGrant createCodeGrant(AuthorizationCodeRegistration reg) 
        throws OAuthServiceException;

    /**
     * Returns the previously registered {@link ServerAuthorizationCodeGrant}
     * @param code the code grant
     * @return AuthorizationCodeGrant
     * @throws OAuthServiceException
     */
    ServerAuthorizationCodeGrant removeCodeGrant(String code) throws OAuthServiceException;
}
