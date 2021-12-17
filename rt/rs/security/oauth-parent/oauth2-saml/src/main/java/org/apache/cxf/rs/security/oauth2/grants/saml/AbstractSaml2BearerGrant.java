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

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

public abstract class AbstractSaml2BearerGrant implements AccessTokenGrant {
    private static final long serialVersionUID = 5754722119855372511L;
    private String assertion;
    private String scope;
    private boolean encoded;
    private String grantType;
    protected AbstractSaml2BearerGrant(String grantType, String assertion, boolean encoded, String scope) {
        this.grantType = grantType;
        this.assertion = assertion;
        this.encoded = encoded;
        this.scope = scope;
    }

    public String getType() {
        return grantType;
    }

    protected MultivaluedMap<String, String> initMap() {
        MultivaluedMap<String, String> map = new MetadataMap<>();
        map.putSingle(OAuthConstants.GRANT_TYPE, grantType);
        return map;
    }

    protected void addScope(MultivaluedMap<String, String> map) {
        if (scope != null) {
            map.putSingle(OAuthConstants.SCOPE, scope);
        }
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
