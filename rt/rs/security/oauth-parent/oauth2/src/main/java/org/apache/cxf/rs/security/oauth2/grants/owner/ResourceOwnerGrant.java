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
package org.apache.cxf.rs.security.oauth2.grants.owner;

import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.rs.security.oauth2.grants.AbstractGrant;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

public class ResourceOwnerGrant extends AbstractGrant {
    private static final long serialVersionUID = -1673025972824906386L;
    private String ownerName;
    private String ownerPassword;

    public ResourceOwnerGrant(String name, String password) {
        this(name, password, null);
    }

    public ResourceOwnerGrant(String name, String password, String scope) {
        this(name, password, scope, null);
    }

    public ResourceOwnerGrant(String name, String password,
                              String scope, String audience) {
        super(OAuthConstants.RESOURCE_OWNER_GRANT, scope, audience);
        this.ownerName = name;
        this.ownerPassword = password;
    }

    public MultivaluedMap<String, String> toMap() {
        MultivaluedMap<String, String> map = super.toMap();
        map.putSingle(OAuthConstants.RESOURCE_OWNER_NAME, ownerName);
        map.putSingle(OAuthConstants.RESOURCE_OWNER_PASSWORD, ownerPassword);

        return map;
    }

}
