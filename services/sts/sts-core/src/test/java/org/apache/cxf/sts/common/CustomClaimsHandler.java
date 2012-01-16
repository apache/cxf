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

import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.xml.ws.WebServiceContext;

import org.apache.cxf.sts.claims.Claim;
import org.apache.cxf.sts.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimTypes;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.RequestClaim;
import org.apache.cxf.sts.claims.RequestClaimCollection;

/**
 * A custom ClaimsHandler implementation for use in the tests.
 */
public class CustomClaimsHandler implements ClaimsHandler {
    
    private static List<URI> knownURIs = new ArrayList<URI>();
    
    static {
        knownURIs.add(ClaimTypes.FIRSTNAME);
        knownURIs.add(ClaimTypes.LASTNAME);
        knownURIs.add(ClaimTypes.EMAILADDRESS);
    }

    public List<URI> getSupportedClaimTypes() {
        return knownURIs;
    }

    @Deprecated
    public ClaimCollection retrieveClaimValues(
            Principal principal, RequestClaimCollection claims, WebServiceContext context, String realm) {
        
        ClaimsParameters params = new ClaimsParameters();
        params.setPrincipal(principal);
        params.setWebServiceContext(context);
        params.setRealm(realm);
        
        return retrieveClaimValues(claims, params);
    }
    
    
    public ClaimCollection retrieveClaimValues(
            RequestClaimCollection claims, ClaimsParameters parameters) {
      
        if (claims != null && claims.size() > 0) {
            ClaimCollection claimCollection = new ClaimCollection();
            for (RequestClaim requestClaim : claims) {
                Claim claim = new Claim();
                claim.setClaimType(requestClaim.getClaimType());
                if (ClaimTypes.FIRSTNAME.equals(requestClaim.getClaimType())) {
                    claim.setValue("alice");
                } else if (ClaimTypes.LASTNAME.equals(requestClaim.getClaimType())) {
                    claim.setValue("doe");
                } else if (ClaimTypes.EMAILADDRESS.equals(requestClaim.getClaimType())) {
                    claim.setValue("alice@cxf.apache.org");
                }
                claimCollection.add(claim);
            }
            return claimCollection;
        }
        
        return null;
    }

        
}
