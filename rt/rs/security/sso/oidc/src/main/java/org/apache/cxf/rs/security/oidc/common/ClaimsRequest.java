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
package org.apache.cxf.rs.security.oidc.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObject;

public class ClaimsRequest extends JsonMapObject {
    public static final String ID_TOKEN_CLAIMS = "id_token";
    public static final String USER_INFO_CLAIMS = "userinfo";
    private static final long serialVersionUID = -1356735897518391517L;
    

    public void setIdTokenClaims(Map<String, ClaimPreference> claims) {
        setProperty(ID_TOKEN_CLAIMS, claims);
    }
    
    public Map<String, ClaimPreference> getIdTokenClaims() {
        return getClaims(ID_TOKEN_CLAIMS);
    }
    
    public void setUserInfoClaims(Map<String, ClaimPreference> claims) {
        setProperty(USER_INFO_CLAIMS, claims);
    }
    
    private Map<String, ClaimPreference> getClaims(String propertyName) {
        Object claimsProp = getProperty(propertyName);
        if (claimsProp instanceof Map) {
            Map<String, ?> claimsMap = CastUtils.cast((Map<?, ?>)claimsProp);
            if (!claimsMap.isEmpty()) {
                if (claimsMap.values().iterator().next() instanceof ClaimPreference) {
                    return CastUtils.cast((Map<?, ?>)claimsMap);
                }
                Map<String, ClaimPreference> claims = new LinkedHashMap<String, ClaimPreference>();
                Map<String, Map<String, ?>> parsedMap = CastUtils.cast((Map<?, ?>)claimsProp);
                for (Map.Entry<String, Map<String, ?>> entry : parsedMap.entrySet()) {
                    
                    ClaimPreference pref = new ClaimPreference();
                    Object essentialProp = entry.getValue().get(ClaimPreference.ESSENTIAL_PROPERTY);
                    if (essentialProp != null) {
                        pref.setProperty(ClaimPreference.ESSENTIAL_PROPERTY, essentialProp);
                    }
                    Object valueProp = entry.getValue().get(ClaimPreference.VALUE_PROPERTY);
                    if (valueProp != null) {
                        pref.setProperty(ClaimPreference.VALUE_PROPERTY, valueProp);
                    }
                    Object valuesProp = entry.getValue().get(ClaimPreference.VALUES_PROPERTY);
                    if (valuesProp != null) {
                        pref.setProperty(ClaimPreference.VALUES_PROPERTY, valuesProp);
                    }
                }
                return claims;
            }
        } 
        return null;
        
    }
}
