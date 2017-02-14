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

package org.apache.cxf.sts.token.realm;


import org.apache.cxf.sts.IdentityMapper;
import org.apache.cxf.sts.claims.ClaimsMapper;


/**
 * This class holds the parameters that will be required to define
 * a one-way relationship between a source and target realm.
 * Two types of relationships are supported: FederatedIdentity and FederatedClaims
 * If the realm of received token in the RST differs with the target realm either
 * the configured IdentityMapper of ClaimsMapper are called depending on the type of relationship.
 */
public class Relationship {

    public static final String FED_TYPE_IDENTITY = "FederatedIdentity";
    public static final String FED_TYPE_CLAIMS = "FederatedClaims";


    private String sourceRealm;
    private String targetRealm;
    private IdentityMapper identityMapper;
    private ClaimsMapper claimsMapper;
    private String type;


    public void setSourceRealm(String sourceRealm) {
        this.sourceRealm = sourceRealm;
    }

    public String getSourceRealm() {
        return sourceRealm;
    }

    public void setTargetRealm(String targetRealm) {
        this.targetRealm = targetRealm;
    }

    public String getTargetRealm() {
        return targetRealm;
    }

    public void setIdentityMapper(IdentityMapper identityMapper) {
        this.identityMapper = identityMapper;
    }

    public IdentityMapper getIdentityMapper() {
        return identityMapper;
    }

    public void setClaimsMapper(ClaimsMapper claimsMapper) {
        this.claimsMapper = claimsMapper;
    }

    public ClaimsMapper getClaimsMapper() {
        return claimsMapper;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

}

