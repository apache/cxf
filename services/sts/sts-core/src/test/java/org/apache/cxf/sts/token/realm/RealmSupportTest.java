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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.sts.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsManager;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.RequestClaim;
import org.apache.cxf.sts.claims.RequestClaimCollection;
import org.apache.cxf.sts.common.RealmSupportClaimsHandler;
import org.apache.cxf.sts.operation.CustomIdentityMapper;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.junit.Assert;

public class RealmSupportTest extends org.junit.Assert {

    
    @org.junit.Test
    public void testIdentityMappingRealmA2B() throws Exception {
        
        ClaimsManager claimsManager = new ClaimsManager();
        
        claimsManager.setIdentityMapper(new CustomIdentityMapper());
        
        RealmSupportClaimsHandler realmAHandler = new RealmSupportClaimsHandler();
        realmAHandler.setRealm("A");
        realmAHandler.setSupportedClaimTypes(Collections.singletonList(URI.create("Claim-A")));
        
        RealmSupportClaimsHandler realmBHandler = new RealmSupportClaimsHandler();
        realmBHandler.setRealm("B");
        realmBHandler.setSupportedClaimTypes(Collections.singletonList(URI.create("Claim-B")));
        
        RealmSupportClaimsHandler realmCHandler = new RealmSupportClaimsHandler();
        realmCHandler.setRealm("B");
        realmCHandler.setSupportedClaimTypes(Collections.singletonList(URI.create("Claim-C")));
        
        List<ClaimsHandler> claimHandlers = new ArrayList<ClaimsHandler>();
        claimHandlers.add(realmAHandler);
        claimHandlers.add(realmBHandler);
        claimHandlers.add(realmCHandler);
        claimsManager.setClaimHandlers(Collections.unmodifiableList(claimHandlers));
        
        RequestClaimCollection requestedClaims = createRequestClaimCollection();
        
        ClaimsParameters parameters = new ClaimsParameters();
        parameters.setRealm("A");
        parameters.setPrincipal(new CustomTokenPrincipal("alice"));
        ClaimCollection claims = claimsManager.retrieveClaimValues(requestedClaims, parameters);
        Assert.assertEquals("Number of claims incorrect", 3, claims.size());
    }
    
    @org.junit.Test
    public void testIdentityMappingRealmB2A() throws Exception {
        
        ClaimsManager claimsManager = new ClaimsManager();
        
        claimsManager.setIdentityMapper(new CustomIdentityMapper());
        
        RealmSupportClaimsHandler realmAHandler = new RealmSupportClaimsHandler();
        realmAHandler.setRealm("A");
        realmAHandler.setSupportedClaimTypes(Collections.singletonList(URI.create("Claim-A")));
        
        RealmSupportClaimsHandler realmBHandler = new RealmSupportClaimsHandler();
        realmBHandler.setRealm("B");
        realmBHandler.setSupportedClaimTypes(Collections.singletonList(URI.create("Claim-B")));
        
        RealmSupportClaimsHandler realmCHandler = new RealmSupportClaimsHandler();
        realmCHandler.setRealm("B");
        realmCHandler.setSupportedClaimTypes(Collections.singletonList(URI.create("Claim-C")));
        
        List<ClaimsHandler> claimHandlers = new ArrayList<ClaimsHandler>();
        claimHandlers.add(realmAHandler);
        claimHandlers.add(realmBHandler);
        claimHandlers.add(realmCHandler);
        claimsManager.setClaimHandlers(Collections.unmodifiableList(claimHandlers));
        
        RequestClaimCollection requestedClaims = createRequestClaimCollection();
        
        ClaimsParameters parameters = new ClaimsParameters();
        parameters.setRealm("B");
        parameters.setPrincipal(new CustomTokenPrincipal("ALICE"));
        ClaimCollection claims = claimsManager.retrieveClaimValues(requestedClaims, parameters);
        Assert.assertEquals("Number of claims incorrect", 3, claims.size());
    }

    @org.junit.Test
    public void testFilteredRealmAIdentityMapping() throws Exception {
        
        ClaimsManager claimsManager = new ClaimsManager();
        
        claimsManager.setIdentityMapper(new CustomIdentityMapper());
        
        RealmSupportClaimsHandler realmAHandler = new RealmSupportClaimsHandler();
        realmAHandler.setRealm("A");
        realmAHandler.setSupportedClaimTypes(Collections.singletonList(URI.create("Claim-A")));
        
        RealmSupportClaimsHandler realmBHandler = new RealmSupportClaimsHandler();
        realmBHandler.setRealm("B");
        realmBHandler.setSupportedClaimTypes(Collections.singletonList(URI.create("Claim-B")));
        
        RealmSupportClaimsHandler realmCHandler = new RealmSupportClaimsHandler();
        realmCHandler.setRealm("A");
        realmCHandler.setSupportedRealms(Collections.singletonList("A"));
        realmCHandler.setSupportedClaimTypes(Collections.singletonList(URI.create("Claim-C")));
        
        List<ClaimsHandler> claimHandlers = new ArrayList<ClaimsHandler>();
        claimHandlers.add(realmAHandler);
        claimHandlers.add(realmBHandler);
        claimHandlers.add(realmCHandler);
        claimsManager.setClaimHandlers(Collections.unmodifiableList(claimHandlers));
        
        RequestClaimCollection requestedClaims = createRequestClaimCollection();
        
        ClaimsParameters parameters = new ClaimsParameters();
        parameters.setRealm("A");
        parameters.setPrincipal(new CustomTokenPrincipal("alice"));
        ClaimCollection claims = claimsManager.retrieveClaimValues(requestedClaims, parameters);
        Assert.assertEquals("Number of claims incorrect", 3, claims.size());
        
        //Asserts in RealmSupportClaimsHandler must succeed
        
    }
    
    
    @org.junit.Test
    public void testFilteredRealmBIdentityMapping() throws Exception {
        
        ClaimsManager claimsManager = new ClaimsManager();
        
        claimsManager.setIdentityMapper(new CustomIdentityMapper());
        
        RealmSupportClaimsHandler realmAHandler = new RealmSupportClaimsHandler();
        realmAHandler.setRealm("A");
        realmAHandler.setSupportedClaimTypes(Collections.singletonList(URI.create("Claim-A")));
        
        RealmSupportClaimsHandler realmBHandler = new RealmSupportClaimsHandler();
        realmBHandler.setRealm("B");
        realmBHandler.setSupportedClaimTypes(Collections.singletonList(URI.create("Claim-B")));
        
        RealmSupportClaimsHandler realmCHandler = new RealmSupportClaimsHandler();
        realmCHandler.setRealm("A");
        realmCHandler.setSupportedRealms(Collections.singletonList("A"));
        realmCHandler.setSupportedClaimTypes(Collections.singletonList(URI.create("Claim-C")));
        
        List<ClaimsHandler> claimHandlers = new ArrayList<ClaimsHandler>();
        claimHandlers.add(realmAHandler);
        claimHandlers.add(realmBHandler);
        claimHandlers.add(realmCHandler);
        claimsManager.setClaimHandlers(Collections.unmodifiableList(claimHandlers));
        
        RequestClaimCollection requestedClaims = createRequestClaimCollection();
        
        ClaimsParameters parameters = new ClaimsParameters();
        parameters.setRealm("B");
        parameters.setPrincipal(new CustomTokenPrincipal("ALICE"));
        ClaimCollection claims = claimsManager.retrieveClaimValues(requestedClaims, parameters);
        Assert.assertEquals("Number of claims incorrect", 2, claims.size());
        
        //Asserts in RealmSupportClaimsHandler must succeed
        
    }
    
    private RequestClaimCollection createRequestClaimCollection() {
        RequestClaimCollection requestedClaims = new RequestClaimCollection();
        RequestClaim requestClaimA = new RequestClaim();
        requestClaimA.setClaimType(URI.create("Claim-A"));
        requestClaimA.setOptional(false);
        requestedClaims.add(requestClaimA);
        RequestClaim requestClaimB = new RequestClaim();
        requestClaimB.setClaimType(URI.create("Claim-B"));
        requestClaimB.setOptional(false);
        requestedClaims.add(requestClaimB);
        RequestClaim requestClaimC = new RequestClaim();
        requestClaimC.setClaimType(URI.create("Claim-C"));
        requestClaimC.setOptional(true);
        requestedClaims.add(requestClaimC);
        return requestedClaims;
    }
    
}


