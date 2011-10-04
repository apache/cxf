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
package org.apache.cxf.rs.security.saml.assertion;

import java.util.List;

public class Claims {

    private List<Claim> claims;
    private String realm;
    
    public Claims(List<Claim> claims) {
        this.claims = claims;
    }
    
    public Claims(List<Claim> claims, String realm) {
        this.claims = claims;
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }
    public List<Claim> getClaims() {
        return claims;
    }
    
    public Claim findClaimByFriendlyName(String friendlyName) {
        for (Claim c : claims) {
            if (c.getFriendlyName().equals(friendlyName)) {
                return c;
            }
        }
        return null;
    }
   
    public Claim findClaimByName(String name) {
        for (Claim c : claims) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }
    
    public Claim findClaimByFormatAndName(String format, String name) {
        for (Claim c : claims) {
            if (c.getName().equals(name)
                && c.getNameFormat().equals(format)) {
                return c;
            }
        }
        return null;
    }
}
