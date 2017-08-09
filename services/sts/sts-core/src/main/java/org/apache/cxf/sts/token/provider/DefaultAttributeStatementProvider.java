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

import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.saml.bean.AttributeBean;
import org.apache.wss4j.common.saml.bean.AttributeStatementBean;

/**
 * A default AttributeStatementProvider implementation. It simply creates a default attribute with
 * value "authenticated".
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

        attrBean.setSamlAttributes(attributeList);

        return attrBean;
    }

    /**
     * Create a default attribute
     */
    private AttributeBean createDefaultAttribute(String tokenType) {
        AttributeBean attributeBean = new AttributeBean();

        if (WSS4JConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType)
            || WSS4JConstants.SAML_NS.equals(tokenType)) {
            attributeBean.setSimpleName("token-requestor");
            attributeBean.setQualifiedName("http://cxf.apache.org/sts");
        } else {
            attributeBean.setQualifiedName("token-requestor");
            attributeBean.setNameFormat("http://cxf.apache.org/sts");
        }

        attributeBean.addAttributeValue("authenticated");

        return attributeBean;
    }

}
