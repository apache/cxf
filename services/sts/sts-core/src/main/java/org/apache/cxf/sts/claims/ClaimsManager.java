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

import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.ws.WebServiceContext;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.ws.security.sts.provider.STSException;

/**
 * This class holds various ClaimsHandler implementations.
 */
public class ClaimsManager {

    private static final Logger LOG = LogUtils.getL7dLogger(ClaimsManager.class);

    private List<ClaimsHandler> claimHandlers;
    private List<URI> supportedClaimTypes = new ArrayList<URI>();

    public List<URI> getSupportedClaimTypes() {
        return supportedClaimTypes;
    }

    public List<ClaimsHandler> getClaimHandlers() {
        return claimHandlers;
    }

    public void setClaimHandlers(List<ClaimsHandler> claimHandlers) {
        this.claimHandlers = claimHandlers;
        if (claimHandlers == null) {
            supportedClaimTypes.clear();
        } else {
            for (ClaimsHandler handler : claimHandlers) {
                supportedClaimTypes.addAll(handler.getSupportedClaimTypes());
            }
        }
    }

    @Deprecated
    public ClaimCollection retrieveClaimValues(
            Principal principal, RequestClaimCollection claims, WebServiceContext context, String realm) {
        if (claimHandlers != null && claimHandlers.size() > 0 && claims != null && claims.size() > 0) {
            ClaimCollection returnCollection = new ClaimCollection();
            for (ClaimsHandler handler : claimHandlers) {
                ClaimCollection claimCollection = handler.retrieveClaimValues(
                        principal, claims, context, realm);
                if (claimCollection != null && claimCollection.size() != 0) {
                    returnCollection.addAll(claimCollection);
                }
            }
            validateClaimValues(claims, returnCollection);
            return returnCollection;
        }
        return null;
    }
    
    public ClaimCollection retrieveClaimValues(RequestClaimCollection claims, ClaimsParameters parameters) {
        if (claimHandlers != null && claimHandlers.size() > 0 && claims != null && claims.size() > 0) {
            ClaimCollection returnCollection = new ClaimCollection();
            for (ClaimsHandler handler : claimHandlers) {
                ClaimCollection claimCollection = handler.retrieveClaimValues(claims, parameters);
                if (claimCollection != null && claimCollection.size() != 0) {
                    returnCollection.addAll(claimCollection);
                }
            }
            validateClaimValues(claims, returnCollection);
            return returnCollection;
        }
        return null;
    }
    
    private boolean validateClaimValues(RequestClaimCollection requestedClaims, ClaimCollection claims) {
        for (RequestClaim claim : requestedClaims) {
            URI claimType = claim.getClaimType();
            boolean found = false;
            if (!claim.isOptional()) {
                for (Claim c : claims) {
                    if (c.getClaimType().equals(claimType)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    LOG.warning("Mandatory claim not found: " + claim.getClaimType());
                    throw new STSException("Mandatory claim '" + claim.getClaimType() + "' not found");
                }
            }
        }
        return true;
        
    }
    
    

}
 