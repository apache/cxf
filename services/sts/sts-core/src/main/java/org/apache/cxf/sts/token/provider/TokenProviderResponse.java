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

package org.apache.cxf.sts.token.provider;

import java.time.Instant;

/**
 * This class encapsulates the response from a TokenProvider instance after creating a token.
 */
public class TokenProviderResponse {

    private Object token;
    private String tokenId;
    private byte[] entropy;
    private long keySize;
    private boolean computedKey;
    private TokenReference attachedReference;
    private TokenReference unAttachedReference;
    private Instant created;
    private Instant expires;

    /**
     * Return true if the entropy represents a Computed Key.
     */
    public boolean isComputedKey() {
        return computedKey;
    }

    /**
     * Set whether the entropy represents a Computed Key or not
     */
    public void setComputedKey(boolean computedKey) {
        this.computedKey = computedKey;
    }

    /**
     * Get the KeySize that the TokenProvider set
     */
    public long getKeySize() {
        return keySize;
    }

    /**
     * Set the KeySize
     */
    public void setKeySize(long keySize) {
        this.keySize = keySize;
    }

    /**
     * Set the token
     * @param token the token to set
     */
    public void setToken(Object token) {
        this.token = token;
    }

    /**
     * Get the token
     * @return the token to set
     */
    public Object getToken() {
        return token;
    }

    /**
     * Set the token Id
     * @param tokenId the token Id
     */
    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    /**
     * Get the token Id
     * @return the token Id
     */
    public String getTokenId() {
        return tokenId;
    }

    /**
     * Set the entropy associated with the token.
     * @param entropy the entropy associated with the token.
     */
    public void setEntropy(byte[] entropy) {
        this.entropy = entropy;
    }

    /**
     * Get the entropy associated with the token.
     * @return the entropy associated with the token.
     */
    public byte[] getEntropy() {
        return entropy;
    }

    /**
     * Set the attached TokenReference
     * @param attachedReference the attached TokenReference
     */
    public void setAttachedReference(TokenReference attachedReference) {
        this.attachedReference = attachedReference;
    }

    /**
     * Get the attached TokenReference
     * @return the attached TokenReference
     */
    public TokenReference getAttachedReference() {
        return attachedReference;
    }

    /**
     * Set the unattached TokenReference
     * @param unattachedReference  Set the unattached TokenReference
     */
    public void setUnattachedReference(TokenReference unattachedReference) {
        this.unAttachedReference = unattachedReference;
    }

    /**
     * Get the unattached TokenReference
     * @return the unattached TokenReference
     */
    public TokenReference getUnAttachedReference() {
        return unAttachedReference;
    }

    /**
     * Get the Instant that this Token was Created
     * @return the Instant that this Token was Created
     */
    public Instant getCreated() {
        return created;
    }

    /**
     * Set the Instant that this Token was Created
     * @param created the Instant that this Token was Created
     */
    public void setCreated(Instant created) {
        this.created = created;
    }

    /**
     * Get the Instant that this Token expires
     * @return the Instant that this Token expires
     */
    public Instant getExpires() {
        return expires;
    }

    /**
     * Set the Instant that this Token expires
     * @param expires the Instant that this Token expires
     */
    public void setExpires(Instant expires) {
        this.expires = expires;
    }

}
