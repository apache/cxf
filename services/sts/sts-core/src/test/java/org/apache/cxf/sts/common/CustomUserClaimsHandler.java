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

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimTypes;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;

/**
 * A custom ClaimsHandler implementation for use in the tests.
 */
public class CustomUserClaimsHandler implements ClaimsHandler {

    private static List<String> knownURIs = new ArrayList<>();

    static {
        knownURIs.add(ClaimTypes.FIRSTNAME.toString());
    }

    public List<String> getSupportedClaimTypes() {
        return knownURIs;
    }

    public ProcessedClaimCollection retrieveClaimValues(
            ClaimCollection claims, ClaimsParameters parameters) {

        if (claims != null && !claims.isEmpty()) {
            ProcessedClaimCollection claimCollection = new ProcessedClaimCollection();
            for (Claim requestClaim : claims) {
                ProcessedClaim claim = new ProcessedClaim();
                claim.setClaimType(requestClaim.getClaimType());
                if (ClaimTypes.FIRSTNAME.toString().equals(requestClaim.getClaimType())) {

                    if (parameters.getPrincipal().getName().equalsIgnoreCase("alice")) {
                        claim.addValue("aliceClaim");
                    } else if (parameters.getPrincipal().getName().equalsIgnoreCase("bob")) {
                        claim.addValue("bobClaim");
                    }
                }
                claimCollection.add(claim);
            }
            return claimCollection;
        }


        return null;
    }



}
