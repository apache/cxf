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
package org.apache.cxf.systest.sts.custom;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsHandler;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.wss4j.common.util.XMLUtils;

/**
 * A custom ClaimsHandler implementation for use in the tests.
 */
public class CustomClaimsHandler implements ClaimsHandler {

    public static final String ROLE =
            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";
    public static final String GIVEN_NAME =
        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname";
    public static final String LANGUAGE =
        "http://schemas.mycompany.com/claims/language";

    public ProcessedClaimCollection retrieveClaimValues(
            ClaimCollection claims, ClaimsParameters parameters) {

        if (claims != null && !claims.isEmpty()) {
            ProcessedClaimCollection claimCollection = new ProcessedClaimCollection();
            List<Object> customContent = parameters.getTokenRequirements().getCustomContent();
            boolean foundContent = false;
            if (customContent != null) {
                for (Object customContentObj : customContent) {
                    if (!(customContentObj instanceof Element)) {
                        continue;
                    }
                    Element customContentElement = (Element)customContentObj;
                    Element realm = XMLUtils.findElement(customContentElement, "realm", "http://cxf.apache.org/custom");
                    if (realm != null) {
                        String realmStr = realm.getTextContent();
                        if ("custom-realm".equals(realmStr)) {
                            foundContent = true;
                        }
                    }
                }
            }

            for (Claim requestClaim : claims) {
                ProcessedClaim claim = new ProcessedClaim();
                claim.setClaimType(requestClaim.getClaimType());
                claim.setIssuer("Test Issuer");
                claim.setOriginalIssuer("Original Issuer");
                if (foundContent) {
                    if (ROLE.equals(requestClaim.getClaimType())) {
                        claim.addValue("admin-user");
                    } else if (GIVEN_NAME.equals(requestClaim.getClaimType())) {
                        claim.addValue(parameters.getPrincipal().getName());
                    } else if (LANGUAGE.equals(requestClaim.getClaimType())) {
                        claim.addValue(parameters.getPrincipal().getName());
                    }
                }
                claimCollection.add(claim);
            }
            return claimCollection;
        }
        return null;
    }

    public List<String> getSupportedClaimTypes() {
        List<String> list = new ArrayList<>();
        list.add(ROLE);
        list.add(GIVEN_NAME);
        list.add(LANGUAGE);
        return list;
    }

}
