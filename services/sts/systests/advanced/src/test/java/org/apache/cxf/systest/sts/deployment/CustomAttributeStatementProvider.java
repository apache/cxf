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
package org.apache.cxf.systest.sts.deployment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cxf.sts.claims.ClaimsManager;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.cxf.sts.token.provider.AttributeStatementProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.saml.bean.AttributeBean;
import org.apache.wss4j.common.saml.bean.AttributeStatementBean;
import org.apache.wss4j.common.saml.builder.SAML2Constants;

public class CustomAttributeStatementProvider implements AttributeStatementProvider {

    private String nameFormat = SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED;

    public AttributeStatementBean getStatement(TokenProviderParameters providerParameters) {

        // Handle Claims
        ClaimsManager claimsManager = providerParameters.getClaimsManager();
        ProcessedClaimCollection retrievedClaims = new ProcessedClaimCollection();
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
            params.setMessageContext(providerParameters.getMessageContext());
            retrievedClaims =
                claimsManager.retrieveClaimValues(
                    providerParameters.getRequestedPrimaryClaims(),
                    providerParameters.getRequestedSecondaryClaims(),
                    params
                );
        }
        if (retrievedClaims == null) {
            return null;
        }

        Iterator<ProcessedClaim> claimIterator = retrievedClaims.iterator();
        if (!claimIterator.hasNext()) {
            return null;
        }

        List<AttributeBean> attributeList = new ArrayList<>();
        String tokenType = providerParameters.getTokenRequirements().getTokenType();

        AttributeStatementBean attrBean = new AttributeStatementBean();
        while (claimIterator.hasNext()) {
            ProcessedClaim claim = claimIterator.next();
            AttributeBean attributeBean = new AttributeBean();

            String claimType = claim.getClaimType();
            if (WSS4JConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
                || WSS4JConstants.SAML2_NS.equals(tokenType)) {
                attributeBean.setQualifiedName(claimType);
                attributeBean.setNameFormat(nameFormat);
            } else {
                String uri = claimType;
                int lastSlash = uri.lastIndexOf('/');
                if (lastSlash == (uri.length() - 1)) {
                    uri = uri.substring(0, lastSlash);
                    lastSlash = uri.lastIndexOf('/');
                }

                String namespace = uri.substring(0, lastSlash);
                String name = uri.substring(lastSlash + 1, uri.length());

                attributeBean.setSimpleName(name);
                attributeBean.setQualifiedName(namespace);
            }
            attributeBean.setAttributeValues(claim.getValues());
            attributeList.add(attributeBean);
        }
        attrBean.setSamlAttributes(attributeList);

        return attrBean;
    }

    public String getNameFormat() {
        return nameFormat;
    }

    public void setNameFormat(String nameFormat) {
        this.nameFormat = nameFormat;
    }

}
