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
 * 
 */
public interface TokenStore {
    /**
     * Add the given token to the list.
     * @param token The token to be added
     * @throws TokenStoreException
     */
    void add(SecurityToken token);
    
    /**
     * Update an existing token.
     * @param token
     */
    void update(SecurityToken token);
    
    /**
     * Remove an existing token.
     */
    void remove(SecurityToken token);
    
    /**
     * Return the list of all token identifiers.
     * @return As array of token identifiers
     */
    Collection<String> getTokenIdentifiers();
    
    /**
     * Return the list of <code>EXPIRED</code> tokens.
     * If there are no <code>EXPIRED</code> tokens <code>null</code> will be 
     * returned
     * @return An array of expired <code>Tokens</code>
     */
    Collection<SecurityToken> getExpiredTokens();
    
    /**
     * Return the list of ISSUED and RENEWED tokens.
     * @return An array of ISSUED and RENEWED <code>Tokens</code>.
     */
    Collection<SecurityToken> getValidTokens();
    
    /**
     * Return the list of RENEWED tokens.
     * @return An array of RENEWED <code>Tokens</code>
     */
    Collection<SecurityToken> getRenewedTokens();
    
    /**
     * Return the list of CANCELLED tokens
     * @return An array of CANCELLED <code>Tokens</code>
     */
    Collection<SecurityToken> getCancelledTokens();
    
    /**
     * Returns the <code>Token</code> of the given id
     * @param id
     * @return The requested <code>Token</code> identified by the give id
     */
    SecurityToken getToken(String id);
    
    
    
    /**
     * Removes all expired tokens.  
     */
    void removeExpiredTokens();
    
    /**
     * Removes all cancelled tokens.
     */
    void removeCancelledTokens();
    
    /**
     * Controls whether the store will automatically remove cancelled and expired 
     * tokens.  If true, calls to getCancelledTokens() and getExpiredTokens() 
     * will never return value;
     * @param auto
     */
    void setAutoRemoveTokens(boolean auto);
    
}
