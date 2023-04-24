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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;

public class StaticEndpointClaimsHandler implements ClaimsHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(StaticEndpointClaimsHandler.class);

    private Map<String, Map<String, String>> endpointClaimsMap;
    private List<String> supportedClaims;

    public void setEndpointClaims(Map<String, Map<String, String>> userClaims) {
        this.endpointClaimsMap = userClaims;
    }

    public Map<String, Map<String, String>> getEndpointClaims() {
        return endpointClaimsMap;
    }

    public void setSupportedClaims(List<String> supportedClaims) {
        this.supportedClaims = supportedClaims;
    }

    public List<String> getSupportedClaimTypes() {
        return Collections.unmodifiableList(this.supportedClaims);
    }

    public ProcessedClaimCollection retrieveClaimValues(
            ClaimCollection claims, ClaimsParameters parameters) {

        ProcessedClaimCollection claimsColl = new ProcessedClaimCollection();
        String appliesTo = parameters.getAppliesToAddress();
        if (appliesTo == null) {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer("AppliesTo not provided in RST. " + StaticEndpointClaimsHandler.class.getName() + " ignored");
            }
            return claimsColl;
        }
        Map<String, String> endpointClaims = this.getEndpointClaims().get(appliesTo);
        if (endpointClaims == null) {
            if (LOG.isLoggable(Level.FINER)) {
                LOG.finer(StaticEndpointClaimsHandler.class.getName()
                        + " doesn't define claims for endpoint '" + appliesTo + "'");
            }
            return claimsColl;
        }
        for (Claim claim : claims) {
            if (endpointClaims.keySet().contains(claim.getClaimType())) {
                ProcessedClaim c = new ProcessedClaim();
                c.setClaimType(claim.getClaimType());
                c.setPrincipal(parameters.getPrincipal());
                c.addValue(endpointClaims.get(claim.getClaimType()));
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

