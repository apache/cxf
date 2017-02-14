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

import java.util.List;

import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rt.security.claims.ClaimCollection;

public class SamlUserSubject extends UserSubject {
    private static final long serialVersionUID = -1135272749329239037L;
    private ClaimCollection claims;
    public SamlUserSubject(String user,
                           List<String> roles,
                           ClaimCollection claims) {
        super(user, roles);
        this.claims = claims;
    }
    public ClaimCollection getClaims() {
        return claims;
    }
}
