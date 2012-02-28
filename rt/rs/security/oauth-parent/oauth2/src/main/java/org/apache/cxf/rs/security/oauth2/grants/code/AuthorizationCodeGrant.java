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
package org.apache.cxf.rs.security.oauth2.grants.code;

import java.net.URI;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrantType;



/**
 * Authorization Code Token representation
 */
public class AuthorizationCodeGrant implements AccessTokenGrant {
    private String code;
    private String redirectUri;
    
    public AuthorizationCodeGrant(String code) {
        this.code = code;
    }
    
    public AuthorizationCodeGrant(String code, URI uri) {
        this.code = code;
        redirectUri = uri.toString();
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getCode() {
        return code;
    }

    public AccessTokenGrantType getType() {
        return AccessTokenGrantType.AUTHORIZATION_CODE;
    }

    public MultivaluedMap<String, String> toMap() {
        MultivaluedMap<String, String> map = new MetadataMap<String, String>();
        map.putSingle("grant_type", AccessTokenGrantType.AUTHORIZATION_CODE.getGrantType());
        map.putSingle("code", code);
        if (redirectUri != null) {
            map.putSingle("redirect_uri", redirectUri);
        }
        return map;
    }

}
