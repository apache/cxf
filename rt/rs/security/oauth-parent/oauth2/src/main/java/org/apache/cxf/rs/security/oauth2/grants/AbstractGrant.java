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

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.common.AccessTokenGrant;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;


/**
 * Abstract access token grant
 */
public abstract class AbstractGrant implements AccessTokenGrant {

    private static final long serialVersionUID = 3586571576928674560L;
    private String grantType;
    private String scope;
    private String audience;

    protected AbstractGrant(String grantType) {
        this(grantType, null);
    }

    protected AbstractGrant(String grantType, String scope) {
        this(grantType, scope, null);
    }

    protected AbstractGrant(String grantType, String scope, String audience) {
        this.grantType = grantType;
        this.scope = scope;
        this.audience = audience;
    }

    public String getType() {
        return grantType;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public MultivaluedMap<String, String> toMap() {
        MultivaluedMap<String, String> map = new MetadataMap<>();
        map.putSingle(OAuthConstants.GRANT_TYPE, getType());
        if (scope != null) {
            map.putSingle(OAuthConstants.SCOPE, scope);
        }
        if (audience != null) {
            map.putSingle(OAuthConstants.CLIENT_AUDIENCE, audience);
        }
        return map;
    }
}
