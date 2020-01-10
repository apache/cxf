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
package org.apache.cxf.rs.security.oauth.data;

/**
 * Request Token representation
 */
public class RequestToken extends Token {

    private String oauthVerifier;
    private String callback;
    private String state;

    public RequestToken(Client client,
                        String tokenString,
                        String tokenSecret) {
        this(client, tokenString, tokenSecret, -1L,
             System.currentTimeMillis() / 1000);
    }

    public RequestToken(Client client, String tokenString,
                        String tokenSecret, long lifetime, long issuedAt) {
        super(client, tokenString, tokenSecret, lifetime, issuedAt);
    }

    /**
     * Sets the token verifier
     * @param verifier
     */
    public void setVerifier(String verifier) {
        this.oauthVerifier = verifier;
    }

    /**
     * Gets the token verifier
     * @return the verifier
     */
    public String getVerifier() {
        return oauthVerifier;
    }

    /**
     * Sets the callback URI
     * @param callback the callback
     */
    public void setCallback(String callback) {
        this.callback = callback;
    }

    /**
     * Gets the callback URI
     * @return the callback
     */
    public String getCallback() {
        return callback;
    }

    /**
     * Sets the state - it will be reported back to the consumer
     * after the authorization decision on this token has been made.
     * @param state
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Gets the state
     * @return the state
     */
    public String getState() {
        return state;
    }

}
