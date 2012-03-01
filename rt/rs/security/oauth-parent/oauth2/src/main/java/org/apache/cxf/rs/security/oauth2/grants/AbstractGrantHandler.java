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

package org.apache.cxf.rs.security.oauth2.grants;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.cxf.rs.security.oauth2.provider.AccessTokenGrantHandler;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.MD5SequenceGenerator;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;



public abstract class AbstractGrantHandler implements AccessTokenGrantHandler {
    
    private static final long DEFAULT_TOKEN_LIFETIME = 3600L;
    
    private long tokenLifetime = DEFAULT_TOKEN_LIFETIME;
    private List<String> supportedGrants;
    private OAuthDataProvider dataProvider;
        
    protected AbstractGrantHandler(String grant) {
        supportedGrants = Collections.singletonList(grant);
    }
    
    public void setDataProvider(OAuthDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }
    public OAuthDataProvider getDataProvider() {
        return dataProvider;
    }
    
    public List<String> getSupportedGrantTypes() {
        return supportedGrants;
    }
    
    protected static String generateRandomTokenKey() throws OAuthServiceException {
        try {
            byte[] bytes = UUID.randomUUID().toString().getBytes("UTF-8");
            return new MD5SequenceGenerator().generate(bytes);
        } catch (Exception ex) {
            throw new OAuthServiceException(OAuthConstants.SERVER_ERROR, ex);
        }
    }

    public void setTokenLifetime(long tokenLifetime) {
        this.tokenLifetime = tokenLifetime;
    }

    public long getTokenLifetime() {
        return tokenLifetime;
    }
    
    
}
