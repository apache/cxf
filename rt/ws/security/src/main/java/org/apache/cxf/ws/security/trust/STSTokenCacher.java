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

package org.apache.cxf.ws.security.trust;

import org.w3c.dom.Element;

import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;

/**
 * This interface allows you to plug in some custom logic when storing/retrieving STS tokens in/from the cache
 */
public interface STSTokenCacher {

    /**
     * Retrieve a cached STS token. The retrieveTokenFromEndpoint boolean lets us known whether we want to retrieve the
     * token from the endpoint or not.
     */
    SecurityToken retrieveToken(Message message, boolean retrieveTokenFromEndpoint) throws TokenStoreException;

    /**
     * Retrieve a cached STS token for a given delegation token Element
     */
    SecurityToken retrieveToken(Message message, Element delegationToken, String cacheKey) throws TokenStoreException;

    /**
     * Store a token in the cache. The storeTokenInEndpoint boolean lets us know whether we want to store the token
     * in the endpoint or not.
     */
    void storeToken(Message message, SecurityToken securityToken, boolean storeTokenInEndpoint)
            throws TokenStoreException;

    /**
     * Store a given delegation token in the cache (or update it if it's already there), with a reference to the
     * security token obtained from the STS.
     */
    void storeToken(Message message, Element delegationToken, String secTokenId, String cacheKey)
            throws TokenStoreException;

    /**
     * Remove a cached STS token
     */
    void removeToken(Message message, SecurityToken securityToken) throws TokenStoreException;

}

