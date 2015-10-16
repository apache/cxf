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
package org.apache.cxf.rs.security.jose.jwt;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;




public class JwtTokenReaderWriter extends JsonMapObjectReaderWriter {
    private static final Set<String> DATE_PROPERTIES = 
        new HashSet<String>(Arrays.asList(JwtConstants.CLAIM_EXPIRY, 
                                          JwtConstants.CLAIM_ISSUED_AT, 
                                          JwtConstants.CLAIM_NOT_BEFORE));

    public String claimsToJson(JwtClaims claims) {
        return toJson(claims);
    }

    public JwtClaims fromJsonClaims(String claimsJson) {
        JwtClaims claims = new JwtClaims();
        fromJson(claims, claimsJson);
        return claims;
        
    }
    
    @Override
    protected Object readPrimitiveValue(String name, String json, int from, int to) {
        Object value = super.readPrimitiveValue(name, json, from, to);
        if (DATE_PROPERTIES.contains(name)) {
            value = Long.valueOf(value.toString());
        }
        return value;
    }
}
