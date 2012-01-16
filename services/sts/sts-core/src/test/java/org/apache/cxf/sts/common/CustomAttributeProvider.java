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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;

import org.apache.cxf.sts.claims.Claim;
import org.apache.cxf.sts.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsManager;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.token.provider.AttributeStatementProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.ws.security.SAMLTokenPrincipal;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.bean.AttributeBean;
import org.apache.ws.security.saml.ext.bean.AttributeStatementBean;

/**
 * A custom AttributeStatementProvider implementation for use in the tests.
 */
public class CustomAttributeProvider implements AttributeStatementProvider {

    /**
     * Get an AttributeStatementBean using the given parameters.
     */
    public AttributeStatementBean getStatement(TokenProviderParameters providerParameters) {
        List<AttributeBean> attributeList = new ArrayList<AttributeBean>();

        TokenRequirements tokenRequirements = providerParameters.getTokenRequirements();
        String tokenType = tokenRequirements.getTokenType();
        
        // Handle Claims
        ClaimsManager claimsManager = providerParameters.getClaimsManager();
        ClaimCollection retrievedClaims = new ClaimCollection();
        if (claimsManager != null) {
            ClaimsParameters params = new ClaimsParameters();
            params.setAdditionalProperties(providerParameters.getAdditionalProperties());
            params.setAppliesToAddress(providerParameters.getAppliesToAddress());
            params.setEncryptionProperties(providerParameters.getEncryptionProperties());
            params.setKeyRequirements(providerParameters.getKeyRequirements());
            params.setPrincipal(providerParameters.getPrincipal());
            params.setRealm(providerParameters.getRealm());
            params.setStsProperties(providerParameters.getStsProperties());
            params.setTokenRequirements(providerParameters.getTokenRequirements());
            params.setTokenStore(providerParameters.getTokenStore());
            params.setWebServiceContext(providerParameters.getWebServiceContext());
            retrievedClaims = 
                claimsManager.retrieveClaimValues(
                    providerParameters.getRequestedClaims(),
                    params
                );
        }
        
        AttributeStatementBean attrBean = new AttributeStatementBean();
        Iterator<Claim> claimIterator = retrievedClaims.iterator();
        if (!claimIterator.hasNext()) {
            // If no Claims have been processed then create a default attribute
            AttributeBean attributeBean = createDefaultAttribute(tokenType);
            attributeList.add(attributeBean);
        }
        
        while (claimIterator.hasNext()) {
            Claim claim = claimIterator.next();
            AttributeBean attributeBean = createAttributeFromClaim(claim, tokenType);
            attributeList.add(attributeBean);
        }
        
        ReceivedToken onBehalfOf = tokenRequirements.getOnBehalfOf();
        ReceivedToken actAs = tokenRequirements.getActAs();
        try {
            if (onBehalfOf != null) {
                AttributeBean parameterBean = 
                    handleAdditionalParameters(false, onBehalfOf.getToken(), tokenType);
                if (!parameterBean.getAttributeValues().isEmpty()) {
                    attributeList.add(parameterBean);
                }
            }
            if (actAs != null) {
                AttributeBean parameterBean = 
                    handleAdditionalParameters(true, actAs.getToken(), tokenType);
                if (!parameterBean.getAttributeValues().isEmpty()) {
                    attributeList.add(parameterBean);
                }
            }
        } catch (WSSecurityException ex) {
            throw new STSException(ex.getMessage(), ex);
        }
        
        attrBean.setSamlAttributes(attributeList);
        
        return attrBean;
    }
    
    /**
     * Create a default attribute
     */
    private AttributeBean createDefaultAttribute(String tokenType) {
        AttributeBean attributeBean = new AttributeBean();

        if (WSConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
            || WSConstants.SAML2_NS.equals(tokenType)) {
            attributeBean.setQualifiedName("token-requestor");
            attributeBean.setNameFormat("http://cxf.apache.org/sts/custom");
        } else {
            attributeBean.setSimpleName("token-requestor");
            attributeBean.setQualifiedName("http://cxf.apache.org/sts/custom");
        }
        
        attributeBean.setAttributeValues(Collections.singletonList("authenticated"));
        
        return attributeBean;
    }

    /**
     * Handle ActAs or OnBehalfOf elements.
     */
    private AttributeBean handleAdditionalParameters(
        boolean actAs, 
        Object parameter, 
        String tokenType
    ) throws WSSecurityException {
        AttributeBean parameterBean = new AttributeBean();

        String claimType = actAs ? "CustomActAs" : "CustomOnBehalfOf";
        if (WSConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType) || WSConstants.SAML2_NS.equals(tokenType)) {
            parameterBean.setQualifiedName(claimType);
            parameterBean.setNameFormat("http://cxf.apache.org/sts/custom/" + claimType);
        } else {
            parameterBean.setSimpleName(claimType);
            parameterBean.setQualifiedName("http://cxf.apache.org/sts/custom/" + claimType);
        }
        if (parameter instanceof UsernameTokenType) {
            parameterBean.setAttributeValues(
                Collections.singletonList(((UsernameTokenType)parameter).getUsername().getValue())
            );
        } else if (parameter instanceof Element) {
            AssertionWrapper wrapper = new AssertionWrapper((Element)parameter);
            SAMLTokenPrincipal principal = new SAMLTokenPrincipal(wrapper);
            parameterBean.setAttributeValues(Collections.singletonList(principal.getName()));
        }

        return parameterBean;
    }

    /**
     * Create an Attribute from a claim.
     */
    private AttributeBean createAttributeFromClaim(Claim claim, String tokenType) {
        AttributeBean attributeBean = new AttributeBean();
        if (WSConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
            || WSConstants.SAML2_NS.equals(tokenType)) {
            attributeBean.setQualifiedName(claim.getClaimType().toString());
        } else {
            attributeBean.setSimpleName(claim.getClaimType().toString());
        }
        attributeBean.setAttributeValues(Collections.singletonList(claim.getValue()));

        return attributeBean;
    }

}
