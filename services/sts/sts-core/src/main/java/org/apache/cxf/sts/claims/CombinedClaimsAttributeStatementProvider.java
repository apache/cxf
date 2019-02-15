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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.sts.token.provider.AttributeStatementProvider;
import org.apache.cxf.sts.token.provider.TokenProviderParameters;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.saml.bean.AttributeBean;
import org.apache.wss4j.common.saml.bean.AttributeStatementBean;
import org.apache.wss4j.common.saml.builder.SAML2Constants;

/**
 * This class differs from the ClaimsAttributeStatementProvider in that it combines claims that have the same name.
 */
public class CombinedClaimsAttributeStatementProvider implements AttributeStatementProvider {

    private String nameFormat = SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED;

    public AttributeStatementBean getStatement(TokenProviderParameters providerParameters) {
        // Handle Claims
        ProcessedClaimCollection retrievedClaims = ClaimsUtils.processClaims(providerParameters);
        if (retrievedClaims == null) {
            return null;
        }

        Iterator<ProcessedClaim> claimIterator = retrievedClaims.iterator();
        if (!claimIterator.hasNext()) {
            return null;
        }

        Map<AttributeKey, AttributeBean> attributeMap = new LinkedHashMap<>();

        String tokenType = providerParameters.getTokenRequirements().getTokenType();
        boolean saml2 = WSS4JConstants.WSS_SAML2_TOKEN_TYPE.equals(tokenType)
            || WSS4JConstants.SAML2_NS.equals(tokenType);

        while (claimIterator.hasNext()) {
            ProcessedClaim claim = claimIterator.next();
            AttributeKey attributeKey = createAttributeKey(claim, saml2);

            attributeMap.merge(
                attributeKey,
                createAttributeBean(attributeKey, claim.getValues()),
                (v1, v2) -> {
                    v1.getAttributeValues().addAll(claim.getValues());
                    return v1;
                });
        }

        AttributeStatementBean attrBean = new AttributeStatementBean();
        attrBean.setSamlAttributes(new ArrayList<>(attributeMap.values()));

        return attrBean;
    }

    private AttributeBean createAttributeBean(AttributeKey attributeKey, List<Object> claimValues) {
        AttributeBean attributeBean =
            new AttributeBean(attributeKey.getSimpleName(), attributeKey.getQualifiedName(), claimValues);
        attributeBean.setNameFormat(attributeKey.getNameFormat());
        return attributeBean;
    }

    private AttributeKey createAttributeKey(ProcessedClaim claim, boolean saml2) {

        String claimType = claim.getClaimType();
        if (saml2) {
            return new AttributeKey(claimType, nameFormat, null);
        } else {
            String uri = claimType;
            int lastSlash = uri.lastIndexOf('/');
            if (lastSlash == (uri.length() - 1)) {
                uri = uri.substring(0, lastSlash);
                lastSlash = uri.lastIndexOf('/');
            }

            String namespace = uri.substring(0, lastSlash);
            String name = uri.substring(lastSlash + 1, uri.length());

            return new AttributeKey(namespace, null, name);
        }
    }

    public String getNameFormat() {
        return nameFormat;
    }

    public void setNameFormat(String nameFormat) {
        this.nameFormat = nameFormat;
    }

    private static class AttributeKey {
        private final String qualifiedName;
        private final String simpleName;
        private final String nameFormat;

        // SAML 2.0 constructor
        AttributeKey(String qualifiedName, String nameFormat, String simpleName) {
            this.qualifiedName = qualifiedName;
            this.nameFormat = nameFormat;
            this.simpleName = simpleName;
        }

        public String getQualifiedName() {
            return qualifiedName;
        }

        public String getSimpleName() {
            return simpleName;
        }

        public String getNameFormat() {
            return nameFormat;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AttributeKey)) {
                return false;
            }

            AttributeKey that = (AttributeKey) o;

            if (qualifiedName == null && that.qualifiedName != null
                || qualifiedName != null && !qualifiedName.equals(that.qualifiedName)) {
                return false;
            }

            if (simpleName == null && that.simpleName != null
                || simpleName != null && !simpleName.equals(that.simpleName)) {
                return false;
            }

            return !(nameFormat == null && that.nameFormat != null
                || nameFormat != null && !nameFormat.equals(that.nameFormat));
        }

        @Override
        public int hashCode() {
            int result = 0;
            if (qualifiedName != null) {
                result = 31 * result + qualifiedName.hashCode();
            }
            if (simpleName != null) {
                result = 31 * result + simpleName.hashCode();
            }
            if (nameFormat != null) {
                result = 31 * result + nameFormat.hashCode();
            }

            return result;
        }
    }
}

