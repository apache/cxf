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

package org.apache.cxf.sts.cache;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.sts.IdentityMapper;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;

class CacheIdentityMapper implements IdentityMapper {

    private final Map<String, Map<String, String>> mappingTable = new HashMap<>();

    CacheIdentityMapper() {
        Map<String, String> identities = new HashMap<>();
        identities.put("REALM_A", "user_aaa");
        identities.put("REALM_B", "user_bbb");
        identities.put("REALM_C", "user_ccc");
        identities.put("REALM_D", "user_ddd");
        identities.put("REALM_E", "user_eee");
        mappingTable.put("user_aaa@REALM_A", identities);
        mappingTable.put("user_bbb@REALM_B", identities);
        mappingTable.put("user_ccc@REALM_C", identities);
        mappingTable.put("user_ddd@REALM_D", identities);
        mappingTable.put("user_eee@REALM_E", identities);
    }

    @Override
    public Principal mapPrincipal(String sourceRealm, Principal sourcePrincipal, String targetRealm) {
        Map<String, String> identities = mappingTable.get(sourcePrincipal.getName() + '@' + sourceRealm);
        if (identities != null) {
            String targetUser = identities.get(targetRealm);
            if (targetUser != null) {
                return new CustomTokenPrincipal(targetUser);
            }
        }
        return null;
    }

}
