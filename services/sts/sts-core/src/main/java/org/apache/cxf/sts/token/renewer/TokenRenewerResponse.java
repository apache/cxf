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
package org.apache.cxf.sts.token.renewer;

import java.time.Instant;

import org.w3c.dom.Element;

import org.apache.cxf.sts.token.provider.TokenReference;


/**
 * This class encapsulates the response from a TokenRenewer instance after renewing a token.
 */
public class TokenRenewerResponse {

    private Element token;
    private String tokenId;
    private TokenReference attachedReference;
    private TokenReference unAttachedReference;
    private Instant created;
    private Instant expires;

    /**
     * Set the token
     * @param token the token to set
     */
    public void setToken(Element token) {
        this.token = token;
    }

    /**
     * Get the token
     * @return the token to set
     */
    public Element getToken() {
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
