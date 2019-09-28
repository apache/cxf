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

import org.apache.cxf.rs.security.oauth2.saml.Constants;

public class Saml2BearerGrant extends AbstractSaml2BearerGrant {
    private static final long serialVersionUID = -7296527609343431294L;

    public Saml2BearerGrant(String assertion) {
        this(assertion, false);
    }

    public Saml2BearerGrant(String assertion, boolean encoded) {
        this(assertion, encoded, null);
    }

    public Saml2BearerGrant(String assertion, String scope) {
        this(assertion, false, scope);
    }

    public Saml2BearerGrant(String assertion, boolean encoded, String scope) {
        super(Constants.SAML2_BEARER_GRANT, assertion, encoded, scope);
    }

    public MultivaluedMap<String, String> toMap() {
        MultivaluedMap<String, String> map = initMap();
        map.putSingle(Constants.CLIENT_GRANT_ASSERTION_PARAM, encodeAssertion());
        addScope(map);
        return map;
    }
}
