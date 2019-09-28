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
package org.apache.cxf.sts.token.provider;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.SAMLTokenPrincipal;
import org.apache.wss4j.common.principal.SAMLTokenPrincipalImpl;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.bean.AttributeBean;
import org.apache.wss4j.common.saml.bean.AttributeStatementBean;
import org.opensaml.core.xml.XMLObject;

/**
 * An AttributeStatementProvider implementation to handle "ActAs". It adds an "ActAs "attribute" with the name of
 * the principal that this token is "acting as".
 */
public class ActAsAttributeStatementProvider implements AttributeStatementProvider {

    /**
     * Get an AttributeStatementBean using the given parameters.
     */
    public AttributeStatementBean getStatement(TokenProviderParameters providerParameters) {
        AttributeStatementBean attrBean = new AttributeStatementBean();

        TokenRequirements tokenRequirements = providerParameters.getTokenRequirements();
        ReceivedToken actAs = tokenRequirements.getActAs();
        try {
            if (actAs != null) {
                List<AttributeBean> attributeList = new ArrayList<>();
                String tokenType = tokenRequirements.getTokenType();

                AttributeBean parameterBean =
                    handleAdditionalParameters(actAs.getToken(), tokenType);
                if (!parameterBean.getAttributeValues().isEmpty()) {
                    attributeList.add(parameterBean);
                }

                attrBean.setSamlAttributes(attributeList);
            }
        } catch (WSSecurityException ex) {
            throw new STSException(ex.getMessage(), ex);
        }

        return attrBean;
    }

    /**
     * Handle an ActAs element.
     */
    private AttributeBean handleAdditionalParameters(
        Object parameter,
        String tokenType
    ) throws WSSecurityException {
        AttributeBean parameterBean = new AttributeBean();

        String claimType = "ActAs";
        if (WSS4JConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType) || WSS4JConstants.SAML_NS.equals(tokenType)) {
            parameterBean.setSimpleName(claimType);
            parameterBean.setQualifiedName("http://cxf.apache.org/sts");
        } else {
            parameterBean.setQualifiedName(claimType);
            parameterBean.setNameFormat("http://cxf.apache.org/sts");
        }
        if (parameter instanceof UsernameTokenType) {
            parameterBean.addAttributeValue(
                ((UsernameTokenType)parameter).getUsername().getValue()
            );
        } else if (parameter instanceof Element) {
            SamlAssertionWrapper wrapper = new SamlAssertionWrapper((Element)parameter);
            SAMLTokenPrincipal principal = new SAMLTokenPrincipalImpl(wrapper);
            parameterBean.addAttributeValue(principal.getName());

            // Check for other ActAs attributes here + add them in
            if (wrapper.getSaml2() != null) {
                for (org.opensaml.saml.saml2.core.AttributeStatement attributeStatement
                    : wrapper.getSaml2().getAttributeStatements()) {
                    for (org.opensaml.saml.saml2.core.Attribute attribute : attributeStatement.getAttributes()) {
                        if ("ActAs".equals(attribute.getName())) {
                            for (XMLObject attributeValue : attribute.getAttributeValues()) {
                                Element attributeValueElement = attributeValue.getDOM();
                                String text = attributeValueElement.getTextContent();
                                parameterBean.addAttributeValue(text);
                            }
                        }
                    }
                }
            } else if (wrapper.getSaml1() != null) {
                for (org.opensaml.saml.saml1.core.AttributeStatement attributeStatement
                    : wrapper.getSaml1().getAttributeStatements()) {
                    for (org.opensaml.saml.saml1.core.Attribute attribute : attributeStatement.getAttributes()) {
                        if ("ActAs".equals(attribute.getAttributeName())) {
                            for (XMLObject attributeValue : attribute.getAttributeValues()) {
                                Element attributeValueElement = attributeValue.getDOM();
                                String text = attributeValueElement.getTextContent();
                                parameterBean.addAttributeValue(text);
                            }
                        }
                    }
                }
            }
        }

        return parameterBean;
    }


}
