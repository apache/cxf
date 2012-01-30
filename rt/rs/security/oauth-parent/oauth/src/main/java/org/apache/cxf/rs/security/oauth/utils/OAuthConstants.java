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

package org.apache.cxf.rs.security.oauth.utils;

/**
 * Miscellaneous constants 
 */
public final class OAuthConstants {
    
    public static final String OAUTH_DATA_PROVIDER_CLASS = "oauth.data.provider-class";
    public static final String OAUTH_DATA_VALIDATOR_CLASS = "oauth.data.validator-class";
    public static final String OAUTH_DATA_PROVIDER_INSTANCE_KEY = "oauth.data.provider-instance.key";

    public static final String VERIFIER_INVALID = "verifier_invalid";

    public static final String AUTHENTICITY_TOKEN = "session_authenticity_token";
    
    public static final String AUTHORIZATION_DECISION_KEY = "oauthDecision";
    public static final String AUTHORIZATION_DECISION_ALLOW = "allow";
    public static final String AUTHORIZATION_DECISION_DENY = "deny";

    public static final String X_OAUTH_SCOPE = "x_oauth_scope";
    public static final String OAUTH_CONSUMER_SECRET = "oauth_consumer_secret";
    
    private OAuthConstants() {
        
    }

}
