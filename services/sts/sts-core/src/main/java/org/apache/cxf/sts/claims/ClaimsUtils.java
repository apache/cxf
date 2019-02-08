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

import org.apache.cxf.sts.token.provider.TokenProviderParameters;

/**
 * Some common utility methods for claims
 */
public final class ClaimsUtils {

    private ClaimsUtils() {
        // complete
    }

    public static ProcessedClaimCollection processClaims(TokenProviderParameters providerParameters) {
        // Handle Claims
        ClaimsManager claimsManager = providerParameters.getClaimsManager();
        ProcessedClaimCollection retrievedClaims = new ProcessedClaimCollection();
        if (claimsManager != null) {
            ClaimsParameters params = new ClaimsParameters();
            params.setAdditionalProperties(providerParameters.getAdditionalProperties());
            params.setAppliesToAddress(providerParameters.getAppliesToAddress());
            params.setEncryptionProperties(providerParameters.getEncryptionProperties());
            params.setKeyRequirements(providerParameters.getKeyRequirements());
            if (providerParameters.getTokenRequirements().getOnBehalfOf() != null) {
                params.setPrincipal(providerParameters.getTokenRequirements().getOnBehalfOf().getPrincipal());
                params.setRoles(providerParameters.getTokenRequirements().getOnBehalfOf().getRoles());
            } else if (providerParameters.getTokenRequirements().getActAs() != null) {
                params.setPrincipal(providerParameters.getTokenRequirements().getActAs().getPrincipal());
                params.setRoles(providerParameters.getTokenRequirements().getActAs().getRoles());
            } else {
                params.setPrincipal(providerParameters.getPrincipal());
            }
            params.setRealm(providerParameters.getRealm());
            params.setStsProperties(providerParameters.getStsProperties());
            params.setTokenRequirements(providerParameters.getTokenRequirements());
            params.setTokenStore(providerParameters.getTokenStore());
            params.setMessageContext(providerParameters.getMessageContext());
            retrievedClaims =
                claimsManager.retrieveClaimValues(
                    providerParameters.getRequestedPrimaryClaims(),
                    providerParameters.getRequestedSecondaryClaims(),
                    params
                );
        }
        return retrievedClaims;
    }

}

