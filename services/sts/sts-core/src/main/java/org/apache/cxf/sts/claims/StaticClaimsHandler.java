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

package org.apache.cxf.sts.claims;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;

public class StaticClaimsHandler implements ClaimsHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(StaticClaimsHandler.class);

    private Map<String, String> globalClaims;

    public void setGlobalClaims(Map<String, String> globalClaims) {
        this.globalClaims = globalClaims;
    }

    public Map<String, String> getGlobalClaims() {
        return globalClaims;
    }


    public List<String> getSupportedClaimTypes() {
        return new ArrayList<>(getGlobalClaims().keySet());
    }

    public ProcessedClaimCollection retrieveClaimValues(
            ClaimCollection claims, ClaimsParameters parameters) {

        ProcessedClaimCollection claimsColl = new ProcessedClaimCollection();
        for (Claim claim : claims) {
            if (getGlobalClaims().keySet().contains(claim.getClaimType().toString())) {
                ProcessedClaim c = new ProcessedClaim();
                c.setClaimType(claim.getClaimType());
                c.setPrincipal(parameters.getPrincipal());
                c.addValue(getGlobalClaims().get(claim.getClaimType().toString()));
                claimsColl.add(c);
            } else {
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.finer("Unsupported claim: " + claim.getClaimType());
                }
            }
        }
        return claimsColl;

    }

}

