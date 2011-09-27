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

import java.util.Collections;
import java.util.List;

public class Client {
    private String loginName;
    private String consumerKey;
    private String secretKey;
    private String callbackURL;
    private String applicationURI;
    private String applicationName;
    private List<String> uris = Collections.emptyList();
    private List<String> scopes = Collections.emptyList();

    public Client(String loginName,
            String consumerKey, String secretKey, String callbackURL,
            String applicationName, List<String> uris) {
        this.loginName = loginName;
        this.consumerKey = consumerKey;
        this.secretKey = secretKey;
        this.callbackURL = callbackURL;
        this.applicationName = applicationName;
        this.uris = uris;
    }
    
    public Client(String loginName, String consumerKey, String secretKey, String callbackURL,
                      String applicationName) {
        this(loginName, consumerKey, secretKey, callbackURL, applicationName, 
             Collections.<String>emptyList());
    }

    public Client(String loginName, String consumerKey, String secretKey, String callbackURL) {
        this(loginName, consumerKey, secretKey, callbackURL, null);
    }

    public Client(String loginName, String consumerKey, String secretKey) {
        this(loginName, consumerKey, secretKey, null);
    }

    public String getLoginName() {
        return loginName;
    }
    
    public List<String> getUris() {
        return uris;
    }
    
    public String getConsumerKey() {
        return consumerKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getCallbackURL() {
        return callbackURL;
    }

    public void setCallbackURL(String callbackURL) {
        this.callbackURL = callbackURL;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    
    public String getApplicationURI() {
        return applicationURI;
    }

    public void setApplicationURI(String applicationURI) {
        this.applicationURI = applicationURI;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
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

        if (applicationName != null ? !applicationName.equals(that.applicationName)
            : that.applicationName != null) {
            return false;
        }
        if (callbackURL != null ? !callbackURL.equals(that.callbackURL) : that.callbackURL != null) {
            return false;
        }
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
        result = 31 * result + (callbackURL != null ? callbackURL.hashCode() : 0);
        result = 31 * result + (applicationName != null ? applicationName.hashCode() : 0);
        return result;
    }
}
