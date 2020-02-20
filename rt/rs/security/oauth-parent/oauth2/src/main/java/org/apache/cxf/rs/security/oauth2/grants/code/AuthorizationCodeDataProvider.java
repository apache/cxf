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

import java.util.List;

import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;

/**
 * AuthorizationCodeDataProvider is the {@link OAuthDataProvider} which
 * can additionally persist the authorization code grant information
 */
public interface AuthorizationCodeDataProvider extends OAuthDataProvider {

    /**
     * Creates a temporarily code grant which will capture the
     * information about the {@link Client} requesting the access to
     * the resource owner's resources
     * @param reg information about the client code grant request
     * @return new code grant
     * @see AuthorizationCodeRegistration
     * @see ServerAuthorizationCodeGrant
     * @throws OAuthServiceException
     */
    ServerAuthorizationCodeGrant createCodeGrant(AuthorizationCodeRegistration reg)
        throws OAuthServiceException;

    /**
     * Returns the previously registered {@link ServerAuthorizationCodeGrant}
     * @param code the code grant
     * @return the grant
     * @throws OAuthServiceException if no grant with this code is available
     * @see ServerAuthorizationCodeGrant
     */
    ServerAuthorizationCodeGrant removeCodeGrant(String code) throws OAuthServiceException;

    /**
     * Return the list of code grants associated with a given client
     * @param client the client
     * @param subject the user subject, can be null
     * @return the list of grants
     * @throws OAuthServiceException
     * @see ServerAuthorizationCodeGrant
     */
    List<ServerAuthorizationCodeGrant> getCodeGrants(Client client, UserSubject subject) throws OAuthServiceException;
}
