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

import org.apache.cxf.sts.claims.ClaimsMapper;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;

/**
 * A test implementation of ClaimsMapper.
 */
public class CustomClaimsMapper implements ClaimsMapper {

    /**
     * transforms the claim values to upper-case
     */
    public ProcessedClaimCollection mapClaims(String sourceRealm,
            ProcessedClaimCollection sourceClaims, String targetRealm,
            ClaimsParameters parameters) {

        ProcessedClaimCollection targetClaims = new ProcessedClaimCollection();

        for (ProcessedClaim c : sourceClaims) {
            ProcessedClaim nc = new ProcessedClaim();
            nc.setClaimType(c.getClaimType());
            nc.setIssuer(c.getIssuer());
            nc.setOriginalIssuer(c.getOriginalIssuer());
            nc.setPrincipal(c.getPrincipal());
            for (Object s : c.getValues()) {
                if (s instanceof String) {
                    nc.addValue(((String)s).toUpperCase());
                }
            }
            targetClaims.add(nc);
        }

        return targetClaims;
    }

}
