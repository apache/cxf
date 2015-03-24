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
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.SAMLTokenPrincipal;
import org.apache.wss4j.common.principal.SAMLTokenPrincipalImpl;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.bean.AttributeBean;
import org.apache.wss4j.common.saml.bean.AttributeStatementBean;
import org.apache.wss4j.dom.WSConstants;

/**
 * A default AttributeStatementProvider implementation. It creates a default attribute with
 * value "authenticated". It also shows how to handle OnBehalfOf or ActAs elements by adding an
 * Attribute for them.
 */
public class DefaultAttributeStatementProvider implements AttributeStatementProvider {

    /**
     * Get an AttributeStatementBean using the given parameters.
     */
    public AttributeStatementBean getStatement(TokenProviderParameters providerParameters) {
        AttributeStatementBean attrBean = new AttributeStatementBean();
        List<AttributeBean> attributeList = new ArrayList<>();

        TokenRequirements tokenRequirements = providerParameters.getTokenRequirements();
        String tokenType = tokenRequirements.getTokenType();
        AttributeBean attributeBean = createDefaultAttribute(tokenType);
        attributeList.add(attributeBean);
        
        ReceivedToken actAs = tokenRequirements.getActAs();
        try {
            if (actAs != null) {
                AttributeBean parameterBean = 
                    handleAdditionalParameters(actAs.getToken(), tokenType);
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

        if (WSConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType)
            || WSConstants.SAML_NS.equals(tokenType)) {
            attributeBean.setSimpleName("token-requestor");
            attributeBean.setQualifiedName("http://cxf.apache.org/sts");
        } else {
            attributeBean.setQualifiedName("token-requestor");
            attributeBean.setNameFormat("http://cxf.apache.org/sts");
        }
        
        attributeBean.addAttributeValue("authenticated");
        
        return attributeBean;
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
        if (WSConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType) || WSConstants.SAML_NS.equals(tokenType)) {
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
        }

        return parameterBean;
    }


}
