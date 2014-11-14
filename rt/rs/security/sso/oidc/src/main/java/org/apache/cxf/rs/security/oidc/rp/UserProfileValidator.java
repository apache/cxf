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
package org.apache.cxf.rs.security.oidc.rp;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.oidc.common.UserIdToken;
import org.apache.cxf.rs.security.oidc.common.UserProfile;

public class UserProfileValidator extends AbstractTokenValidator {
    private boolean encryptedOnly;
    
    public UserProfile getProfile(WebClient profileClient, UserIdToken idToken) {
        return getProfile(profileClient, idToken, false);
    }
    public UserProfile getProfile(WebClient profileClient, UserIdToken idToken, boolean asJwt) {
        if (asJwt) {
            String jwt = profileClient.get(String.class);
            return getProfileFromJwt(jwt, idToken);
        } else {
            UserProfile profile = profileClient.get(UserProfile.class);
            validateUserProfile(profile, idToken);
            return profile;
        }
        
    }
    public UserProfile getProfileFromJwt(String profileJwtToken, UserIdToken idToken) {
        JwtToken jwt = getProfileJwtToken(profileJwtToken, idToken);
        return getProfileFromJwt(jwt, idToken);
    }
    public UserProfile getProfileFromJwt(JwtToken jwt, UserIdToken idToken) {
        UserProfile profile = new UserProfile(jwt.getClaims().asMap());
        validateUserProfile(profile, idToken);
        return profile;
    }
    public JwtToken getProfileJwtToken(String profileJwtToken, UserIdToken idToken) {
        return getJwtToken(profileJwtToken, idToken.getAudience(), (String)idToken.getProperty("kid"), encryptedOnly);
    }
    public void validateUserProfile(UserProfile profile, UserIdToken idToken) {
        validateJwtClaims(profile, idToken.getAudience(), false);
        // validate subject
        if (!idToken.getSubject().equals(profile.getSubject())) {
            throw new SecurityException("Invalid subject");
        }
    }
    public void setEncryptedOnly(boolean encryptedOnly) {
        this.encryptedOnly = encryptedOnly;
    }
    
}
