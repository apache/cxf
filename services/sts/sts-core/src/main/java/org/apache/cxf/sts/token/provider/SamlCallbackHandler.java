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

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.bean.AttributeStatementBean;
import org.apache.wss4j.common.saml.bean.AuthDecisionStatementBean;
import org.apache.wss4j.common.saml.bean.AuthenticationStatementBean;
import org.apache.wss4j.common.saml.bean.ConditionsBean;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.bean.Version;

/**
 * This CallbackHandler implementation is populated with SAML Beans by the SAMLTokenProvider, and is tasked
 * with setting them on a SAMLCallback object, which will be parsed (by WSS4J) into a SAML Assertion.
 */
public class SamlCallbackHandler implements CallbackHandler {
    private TokenProviderParameters tokenParameters;
    private List<AttributeStatementBean> attributeBeans;
    private List<AuthenticationStatementBean> authBeans;
    private List<AuthDecisionStatementBean> authDecisionBeans;
    private ConditionsBean conditionsBean;
    private SubjectBean subjectBean;
    private String issuer;

    /**
     * Set the list of AttributeStatementBeans.
     */
    public void setAttributeBeans(List<AttributeStatementBean> attributeBeanList) {
        this.attributeBeans = attributeBeanList;
    }

    /**
     * Set the list of AuthenticationStatementBeans.
     */
    public void setAuthenticationBeans(List<AuthenticationStatementBean> authBeanList) {
        this.authBeans = authBeanList;
    }

    /**
     * Set the list of AuthDecisionStatementBeans.
     */
    public void setAuthDecisionStatementBeans(List<AuthDecisionStatementBean> authDecisionBeanList) {
        this.authDecisionBeans = authDecisionBeanList;
    }

    /**
     * Set the SubjectBean
     */
    public void setSubjectBean(SubjectBean subjectBean) {
        this.subjectBean = subjectBean;
    }

    /**
     * Set the ConditionsBean
     */
    public void setConditionsBean(ConditionsBean conditionsBean) {
        this.conditionsBean = conditionsBean;
    }

    /**
     * Set the TokenProviderParameters.
     */
    public void setTokenProviderParameters(TokenProviderParameters tokenProviderParameters) {
        this.tokenParameters = tokenProviderParameters;
    }

    /**
     * Set the issuer name
     */
    public void setIssuer(String issuerName) {
        this.issuer = issuerName;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof SAMLCallback) {
                SAMLCallback samlCallback = (SAMLCallback) callback;

                // Set the Subject
                if (subjectBean != null) {
                    samlCallback.setSubject(subjectBean);
                }

                // Set the token Type.
                TokenRequirements tokenRequirements = tokenParameters.getTokenRequirements();
                String tokenType = tokenRequirements.getTokenType();
                boolean saml1 = false;
                if (WSS4JConstants.WSS_SAML_TOKEN_TYPE.equals(tokenType)
                    || WSS4JConstants.SAML_NS.equals(tokenType)) {
                    samlCallback.setSamlVersion(Version.SAML_11);
                    saml1 = true;
                    setSubjectOnBeans();
                } else {
                    samlCallback.setSamlVersion(Version.SAML_20);
                }

                // Set the issuer
                if (issuer == null) {
                    STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
                    samlCallback.setIssuer(stsProperties.getIssuer());
                } else {
                    samlCallback.setIssuer(issuer);
                }

                // Set the statements
                boolean statementAdded = false;
                if (attributeBeans != null && !attributeBeans.isEmpty()) {
                    samlCallback.setAttributeStatementData(attributeBeans);
                    statementAdded = true;
                }
                if (authBeans != null && !authBeans.isEmpty()) {
                    samlCallback.setAuthenticationStatementData(authBeans);
                    statementAdded = true;
                }
                if (authDecisionBeans != null && !authDecisionBeans.isEmpty()) {
                    samlCallback.setAuthDecisionStatementData(authDecisionBeans);
                    statementAdded = true;
                }

                // If SAML 1.1 we *must* add a Statement
                if (saml1 && !statementAdded) {
                    AttributeStatementBean defaultStatement =
                        new DefaultAttributeStatementProvider().getStatement(tokenParameters);
                    defaultStatement.setSubject(subjectBean);
                    samlCallback.setAttributeStatementData(Collections.singletonList(defaultStatement));
                }

                // Set the conditions
                samlCallback.setConditions(conditionsBean);
            }
        }
    }

    /**
     * For SAML 1.1 default to setting the SubjectBean on the statements if they
     * don't already have a Subject defined.
     */
    private void setSubjectOnBeans() {
        if (attributeBeans != null) {
            for (AttributeStatementBean attributeBean : attributeBeans) {
                if (attributeBean.getSubject() == null) {
                    attributeBean.setSubject(subjectBean);
                }
            }
        }
        if (authBeans != null) {
            for (AuthenticationStatementBean authBean : authBeans) {
                if (authBean.getSubject() == null) {
                    authBean.setSubject(subjectBean);
                }
            }
        }
        if (authDecisionBeans != null) {
            for (AuthDecisionStatementBean authDecisionBean : authDecisionBeans) {
                if (authDecisionBean.getSubject() == null) {
                    authDecisionBean.setSubject(subjectBean);
                }
            }
        }

    }


}
