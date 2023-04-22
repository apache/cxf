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

package org.apache.cxf.rs.security.oauth2.provider;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.common.OAuthRedirectionState;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;

/**
 * SessionAuthenticityTokenProvider responsible for storing and retrieving tokens
 * to validate the authenticity of request sessions
 */
public interface SessionAuthenticityTokenProvider {

    /**
     * Create a new session token and stores it
     *
     * @param mc the {@link MessageContext} of this request
     * @param params redirection-based grant request parameters
     * @param subject authenticated end user
     * @param secData
     * @return the created session token
     */
    String createSessionToken(MessageContext mc,
                              MultivaluedMap<String, String> params,
                              UserSubject subject,
                              OAuthRedirectionState secData);

    /**
     * Retrieve the stored session token
     *
     * @param mc the {@link MessageContext} of this request
     * @param params grant authorization parameters
     * @param subject authenticated end user
     * @return the stored token
     */
    String getSessionToken(MessageContext mc,
                           MultivaluedMap<String, String> params,
                           UserSubject subject);

    /**
     * Remove the stored session token
     *
     * @param mc the {@link MessageContext} of this request
     * @param params grant authorization parameters
     * @param subject authenticated end user
     */
    String removeSessionToken(MessageContext mc,
                              MultivaluedMap<String, String> params,
                              UserSubject subject);

    /**
     * Expand the session token
     *
     * @param messageContext the {@link MessageContext} of this request
     * @param sessionToken the token
     * @param subject authenticated end user
     * @return the expanded token or null
     */
    OAuthRedirectionState getSessionState(MessageContext messageContext,
                                          String sessionToken,
                                          UserSubject subject);

}
