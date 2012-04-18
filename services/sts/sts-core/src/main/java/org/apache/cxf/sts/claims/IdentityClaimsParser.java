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
import java.net.URISyntaxException;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.logging.LogUtils;

public class IdentityClaimsParser implements ClaimsParser {
    
    public static final String IDENTITY_CLAIMS_DIALECT = 
        "http://schemas.xmlsoap.org/ws/2005/05/identity";

    private static final Logger LOG = LogUtils.getL7dLogger(IdentityClaimsParser.class);

    public RequestClaim parse(Element claim) {
        return parseClaimType(claim);
    }

    public static RequestClaim parseClaimType(Element claimType) {
        String claimLocalName = claimType.getLocalName();
        String claimNS = claimType.getNamespaceURI();
        if ("ClaimType".equals(claimLocalName)) {
            String claimTypeUri = claimType.getAttribute("Uri");
            String claimTypeOptional = claimType.getAttribute("Optional");
            RequestClaim requestClaim = new RequestClaim();
            try {
                requestClaim.setClaimType(new URI(claimTypeUri));
            } catch (URISyntaxException e) {
                LOG.log(
                    Level.WARNING, 
                    "Cannot create URI from the given ClaimType attribute value " + claimTypeUri,
                    e
                );
            }
            requestClaim.setOptional(Boolean.parseBoolean(claimTypeOptional));
            return requestClaim;
        } else if ("ClaimValue".equals(claimLocalName)) {
            String claimTypeUri = claimType.getAttribute("Uri");
            String claimTypeOptional = claimType.getAttribute("Optional");
            RequestClaim requestClaim = new RequestClaim();
            try {
                requestClaim.setClaimType(new URI(claimTypeUri));
            } catch (URISyntaxException e) {
                LOG.log(
                    Level.WARNING, 
                    "Cannot create URI from the given ClaimTye attribute value " + claimTypeUri,
                    e
                );
            }
            
            Node valueNode = claimType.getFirstChild();
            if (valueNode != null) {
                if ("Value".equals(valueNode.getLocalName())) {
                    requestClaim.setClaimValue(valueNode.getTextContent().trim());
                } else {
                    LOG.warning("Unsupported child element of ClaimValue element "
                            + valueNode.getLocalName());
                    return null;
                }
            } else {
                LOG.warning("No child element of ClaimValue element available");
                return null;
            }
             
            requestClaim.setOptional(Boolean.parseBoolean(claimTypeOptional));
            
            return requestClaim;
        }
        
        LOG.fine("Found unknown element: " + claimLocalName + " " + claimNS);
        return null;
    }

    /**
     * Return the supported dialect of this class
     */
    public String getSupportedDialect() {
        return IDENTITY_CLAIMS_DIALECT;
    }
}
