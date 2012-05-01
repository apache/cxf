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

package org.apache.cxf.ws.security.tokenstore;

import java.util.Collection;

/**
 * This interface defines a caching mechanism for security tokens. It is up to the underlying implementation
 * to handle token expiration (e.g. by querying the SecurityToken's expires date).
 */
public interface TokenStore {
    
    /**
     * Add the given token to the cache. The SecurityTokens getId() identifier will be used to
     * key it in the cache.
     * @param token The token to be added
     */
    void add(SecurityToken token);
    
    /**
     * Add the given token to the cache under the given identifier
     * @param identifier The identifier to use to key the SecurityToken in the cache
     * @param token The token to be added
     */
    void add(String identifier, SecurityToken token);
    
    /**
     * Remove an existing token by its identifier
     */
    void remove(String identifier);
    
    /**
     * Return the list of all valid token identifiers.
     * @return As array of (valid) token identifiers
     */
    Collection<String> getTokenIdentifiers();
    
    /**
     * Return the list of expired tokens.
     * @return An array of expired <code>Tokens</code>
     */
    Collection<SecurityToken> getExpiredTokens();
    
    /**
     * Returns the <code>Token</code> of the given identifier
     * @param identifier
     * @return The requested <code>Token</code> identified by the given identifier
     */
    SecurityToken getToken(String identifier);

}
