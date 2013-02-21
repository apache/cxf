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
package org.apache.cxf.rs.security.oauth2.grants.saml;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.saml.Base64UrlUtility;
import org.apache.cxf.rs.security.oauth2.saml.Constants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

public class Saml2BearerGrant implements AccessTokenGrant {
    private String assertion;
    private String scope;
    private boolean encoded; 
    
    public Saml2BearerGrant(String assertion) {
        this(assertion, false);
    }
    
    public Saml2BearerGrant(String assertion, boolean encoded) {
        this(assertion, false, null);
    }
    
    public Saml2BearerGrant(String assertion, String scope) {
        this(assertion, false, scope);
    }
    
    public Saml2BearerGrant(String assertion, boolean encoded, String scope) {
        this.assertion = assertion;
        this.encoded = encoded;
        this.scope = scope;
    }
    
    public String getType() {
        return Constants.SAML2_BEARER_GRANT;
    }

    public MultivaluedMap<String, String> toMap() {
        MultivaluedMap<String, String> map = new MetadataMap<String, String>();
        map.putSingle(OAuthConstants.GRANT_TYPE, Constants.SAML2_BEARER_GRANT);
        map.putSingle(Constants.CLIENT_GRANT_ASSERTION_PARAM, encodeAssertion());
        if (scope != null) {
            map.putSingle(OAuthConstants.SCOPE, scope);
        }
        return map;
    }

    protected String encodeAssertion() {
        if (encoded) {
            return assertion;
        }
        
        try {
            return Base64UrlUtility.encode(assertion);
        } catch (Exception ex) {
            throw new OAuthServiceException(ex.getMessage(), ex);
        }
    }
}
