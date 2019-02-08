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
package org.apache.cxf.sts.common;

import java.util.List;

import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.cxf.sts.token.realm.RealmSupport;

import org.junit.Assert;

/**
 * A custom ClaimsHandler implementation for use in the tests.
 */
public class RealmSupportClaimsHandler implements ClaimsHandler, RealmSupport {

    private List<String> supportedRealms;
    private String realm;
    private List<String> supportedClaimTypes;


    public void setSupportedRealms(List<String> supportedRealms) {
        this.supportedRealms = supportedRealms;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }


    public List<String> getSupportedClaimTypes() {
        return supportedClaimTypes;
    }

    public void setSupportedClaimTypes(List<String> supportedClaimTypes) {
        this.supportedClaimTypes = supportedClaimTypes;
    }

    public ProcessedClaimCollection retrieveClaimValues(
            ClaimCollection claims, ClaimsParameters parameters) {

        if ("A".equals(realm)) {
            Assert.assertEquals("ClaimHandler in realm A. Alice username must be 'alice'",
                    "alice", parameters.getPrincipal().getName());
        }

        if ("B".equals(realm)) {
            Assert.assertEquals("ClaimHandler in realm B. Alice username must be 'ALICE'",
                    "ALICE", parameters.getPrincipal().getName());
        }

        if (supportedRealms != null && !supportedRealms.contains(parameters.getRealm())) {
            Assert.fail("ClaimHandler must not be called. Source realm '" + parameters.getRealm()
                    + "' not in supportedRealm list: " + supportedRealms);
        }

        if (claims != null && !claims.isEmpty()) {
            ProcessedClaimCollection claimCollection = new ProcessedClaimCollection();
            for (Claim requestClaim : claims) {
                if (getSupportedClaimTypes().indexOf(requestClaim.getClaimType()) != -1) {
                    ProcessedClaim claim = new ProcessedClaim();
                    claim.setClaimType(requestClaim.getClaimType());
                    claim.addValue("Value_" + requestClaim.getClaimType());
                    claimCollection.add(claim);
                }
            }
            return claimCollection;
        }

        return null;
    }

    @Override
    public List<String> getSupportedRealms() {
        return supportedRealms;
    }

    @Override
    public String getHandlerRealm() {
        return realm;
    }

}
