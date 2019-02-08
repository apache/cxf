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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Captures the information associated with the access token request.
 */
public class AccessTokenRegistration {
    private Client client;
    private List<String> requestedScope = new LinkedList<>();
    private List<String> approvedScope = new LinkedList<>();
    private String grantType;
    private UserSubject subject;
    private List<String> audiences = new LinkedList<>();
    private String nonce;
    private String clientCodeVerifier;
    private String responseType;
    private String grantCode;
    private Map<String, String> extraProperties = new LinkedHashMap<>();

    /**
     * Sets the {@link Client} instance
     * @param client the client
     */
    public void setClient(Client client) {
        this.client = client;
    }

    /**
     * Returns the {@link Client} instance
     * @return the client.
     */
    public Client getClient() {
        return client;
    }

    /**
     * Sets the requested scope
     * @param requestedScope the scope
     */
    public void setRequestedScope(List<String> requestedScope) {
        this.requestedScope = requestedScope;
    }

    /**
     * Gets the requested scope
     * @return the scope
     */
    public List<String> getRequestedScope() {
        return requestedScope;
    }

    /**
     * Sets the scope explicitly approved by the end user
     * @param approvedScope the approved scope
     */
    public void setApprovedScope(List<String> approvedScope) {
        this.approvedScope = approvedScope;
    }

    /**
     * Gets the scope explicitly approved by the end user
     * @return the scope
     */
    public List<String> getApprovedScope() {
        return approvedScope;
    }

    /**
     * Sets the {@link UserSubject) instance capturing
     * the information about the end user
     * @param subject the end user subject
     */
    public void setSubject(UserSubject subject) {
        this.subject = subject;
    }

    /**
     * Gets the {@link UserSubject) instance capturing
     * the information about the end user
     * @return the subject
     */
    public UserSubject getSubject() {
        return subject;
    }

    /**
     * Sets the type of grant which is exchanged for this token
     * @param grantType the grant type
     */
    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }
    /**
     * Gets the type of grant which is exchanged for this token
     * @return the grant type
     */
    public String getGrantType() {
        return grantType;
    }

    public List<String> getAudiences() {
        return audiences;
    }

    public void setAudiences(List<String> audiences) {
        this.audiences = audiences;
    }

    public String getClientCodeVerifier() {
        return clientCodeVerifier;
    }

    public void setClientCodeVerifier(String clientCodeVerifier) {
        this.clientCodeVerifier = clientCodeVerifier;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    /**
     * Set the response type
     * @param responseType the response type
     */
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    /**
     * Get the response type
     * @return the response type, null if no redirection flow was used
     */
    public String getResponseType() {
        return responseType;
    }

    /**
     * Set the grant code which was used to request the token
     * @param grantCode the grant code
     */
    public void setGrantCode(String grantCode) {
        this.grantCode = grantCode;
    }

    /**
     * Get the grant code
     * @return the grant code, null if no authorization code grant was used
     */
    public String getGrantCode() {
        return grantCode;
    }

    public Map<String, String> getExtraProperties() {
        return extraProperties;
    }

    public void setExtraProperties(Map<String, String> extraProperties) {
        this.extraProperties = extraProperties;
    }
}
