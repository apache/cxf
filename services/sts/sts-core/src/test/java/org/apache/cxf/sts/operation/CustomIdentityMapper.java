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
package org.apache.cxf.sts.operation;

import java.security.Principal;

import org.apache.cxf.sts.IdentityMapper;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;

/**
 * A test implementation of IdentityMapper.
 */
public class CustomIdentityMapper implements IdentityMapper {

    /**
     * Map a principal in the source realm to the target realm
     * @param sourceRealm the source realm of the Principal
     * @param sourcePrincipal the principal in the source realm
     * @param targetRealm the target realm of the Principal
     * @return the principal in the target realm
     */
    public Principal mapPrincipal(String sourceRealm, Principal sourcePrincipal, String targetRealm) {
        if ("A".equals(sourceRealm)) {
            String name = sourcePrincipal.getName().toUpperCase();
            return new CustomTokenPrincipal(name);
        } else if ("B".equals(sourceRealm)) {
            String name = sourcePrincipal.getName().toLowerCase();
            return new CustomTokenPrincipal(name);
        }
        return null;
    }

}
