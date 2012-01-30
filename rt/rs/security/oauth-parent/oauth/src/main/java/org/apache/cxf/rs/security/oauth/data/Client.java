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
 * Represents a registered third-party consumer
 */
public class Client {
    private String consumerKey;
    private String secretKey;
    private String applicationURI;
    private String applicationName;
    
    private String loginName;
        
    private AccessToken preAuthorizedToken;

    public Client(String consumerId, 
                  String secretKey,
                  String applicationName,
                  String applicationURI) {
        this.consumerKey = consumerId;
        this.secretKey = secretKey;
        this.applicationURI = applicationURI;
        this.applicationName = applicationName;
    }
    
    public Client(String consumerId, String secretKey) {
        this(consumerId, secretKey, null, null);
    }

    /**
     * Gets the consumer registration id
     * @return the consumer key
     */
    public String getConsumerKey() {
        return consumerKey;
    }

    /**
     * Gets the secret key
     * @return the secret key
     */
    public String getSecretKey() {
        return secretKey;
    }

    /**
     * Gets the name of the third-party application
     * this client represents
     * @return the application name
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * Sets the name of the third-party application
     * this client represents
     * @param applicationName the name
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Gets the public URI of the third-party application.
     * For example, this property can be used to validate 
     * request token callbacks
     * @return the application URI
     */
    public String getApplicationURI() {
        return applicationURI;
    }

    /**
     * Sets the public URI of the third-party application.
     */
    public void setApplicationURI(String applicationURI) {
        this.applicationURI = applicationURI;
    }

    /**
     * Gets the optional login name; can be used 
     * for enforcing the RBAC rules 
     * @return the login name
     */
    public String getLoginName() {
        return loginName == null ? consumerKey : loginName;
    }
    
    /**
     * Sets the optional login name
     * @param name the login name
     */
    public void setLoginName(String name) {
        this.loginName = name;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Client that = (Client)o;

        if (!consumerKey.equals(that.consumerKey)) {
            return false;
        }
        if (!secretKey.equals(that.secretKey)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = consumerKey.hashCode();
        result = 31 * result + secretKey.hashCode();
        return result;
    }

    public void setPreAuthorizedToken(AccessToken preAuthorizedToken) {
        this.preAuthorizedToken = preAuthorizedToken;
    }

    public AccessToken getPreAuthorizedToken() {
        return preAuthorizedToken;
    }
}
