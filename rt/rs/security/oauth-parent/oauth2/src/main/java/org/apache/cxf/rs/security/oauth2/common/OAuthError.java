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
package org.apache.cxf.rs.security.oauth2.common;

/**
 * Captures OAuth2 error properties
 */
public class OAuthError {

    private String error;
    private String errorDescription;
    private String errorUri;
    private String state;

    public OAuthError() {

    }

    public OAuthError(String error) {
        this.error = error;
    }

    public OAuthError(String error, String descr) {
        this.error = error;
        this.errorDescription = descr;
    }

    /**
     * Sets the error such as "invalid_grant", etc
     * @param error the error
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Gets the error
     * @return error
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error description
     * @param errorDescription error description
     */
    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    /**
     * Gets the error description
     * @return error description
     */
    public String getErrorDescription() {
        return errorDescription;
    }

    /**
     * Sets the optional link to the page
     * describing the error in detail
     * @param errorUri error page URI
     */
    public void setErrorUri(String errorUri) {
        this.errorUri = errorUri;
    }

    /**
     * Gets the optional link to the page
     * describing the error in detail
     */
    public String getErrorUri() {
        return errorUri;
    }

    /**
     * Sets the client state token which needs to be returned
     * to the client alongside the error information
     * if it was provided during the client request
     * @param state the client state token
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * Gets the client state token
     * @return the state
     */
    public String getState() {
        return state;
    }

    @Override
    public String toString() {
        return "OAuthError[error='" + error + '\'' + ", errorDescription='" + errorDescription + '\''
                + ", errorUri='" + errorUri + '\'' + ']';
    }
}
