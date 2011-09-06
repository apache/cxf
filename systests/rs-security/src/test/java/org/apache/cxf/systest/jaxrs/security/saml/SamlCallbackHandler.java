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

package org.apache.cxf.systest.jaxrs.security.saml;

import java.io.IOException;
import java.util.Collections;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.saml.ext.SAMLCallback;
import org.apache.ws.security.saml.ext.bean.AttributeBean;
import org.apache.ws.security.saml.ext.bean.AttributeStatementBean;
import org.apache.ws.security.saml.ext.bean.AuthDecisionStatementBean;
import org.apache.ws.security.saml.ext.bean.AuthDecisionStatementBean.Decision;
import org.apache.ws.security.saml.ext.bean.AuthenticationStatementBean;
import org.apache.ws.security.saml.ext.bean.ConditionsBean;
import org.apache.ws.security.saml.ext.bean.SubjectBean;
import org.apache.ws.security.saml.ext.builder.SAML1Constants;
import org.apache.ws.security.saml.ext.builder.SAML2Constants;
import org.joda.time.DateTime;
import org.opensaml.common.SAMLVersion;

/**
 * A CallbackHandler instance that is used by the STS to mock up a SAML Attribute Assertion.
 */
public class SamlCallbackHandler implements CallbackHandler {
    private boolean saml2 = true;
    private String confirmationMethod = SAML2Constants.CONF_SENDER_VOUCHES;
    
    public SamlCallbackHandler() {
        //
    }
    
    public SamlCallbackHandler(boolean saml2) {
        this.saml2 = saml2;
    }
    
    public void setConfirmationMethod(String confirmationMethod) {
        this.confirmationMethod = confirmationMethod;
    }
    
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof SAMLCallback) {
                SAMLCallback callback = (SAMLCallback) callbacks[i];
                if (saml2) {
                    callback.setSamlVersion(SAMLVersion.VERSION_20);
                } else {
                    callback.setSamlVersion(SAMLVersion.VERSION_11);
                }
                callback.setIssuer("https://idp.example.org/SAML2");
                
                String subjectName = "uid=sts-client,o=mock-sts.com";
                String subjectQualifier = "www.mock-sts.com";
                if (!saml2 && SAML2Constants.CONF_SENDER_VOUCHES.equals(confirmationMethod)) {
                    confirmationMethod = SAML1Constants.CONF_SENDER_VOUCHES;
                }
                SubjectBean subjectBean = 
                    new SubjectBean(
                        subjectName, subjectQualifier, confirmationMethod
                    );
                // SubjectConfirmationData - not possible to set yet
                callback.setSubject(subjectBean);
                
                ConditionsBean conditions = new ConditionsBean();
                conditions.setAudienceURI("https://sp.example.com/SAML2");
                callback.setConditions(conditions);
                
                AuthDecisionStatementBean authDecBean = new AuthDecisionStatementBean();
                authDecBean.setDecision(Decision.INDETERMINATE);
                callback.setAuthDecisionStatementData(Collections.singletonList(authDecBean));
                
                AuthenticationStatementBean authBean = new AuthenticationStatementBean();
                authBean.setSubject(subjectBean);
                authBean.setAuthenticationInstant(new DateTime());
                authBean.setSessionIndex("123456");
                // AuthnContextClassRef is not set
                authBean.setAuthenticationMethod(
                        "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");
                callback.setAuthenticationStatementData(
                    Collections.singletonList(authBean));
                
                AttributeStatementBean attrBean = new AttributeStatementBean();
                attrBean.setSubject(subjectBean);
                
                AttributeBean attributeBean = new AttributeBean();
                attributeBean.setSimpleName("subject-role");
                attributeBean.setQualifiedName("urn:oid:1.3.6.1.4.1.5923.1.1.1.1");
                attributeBean.setAttributeValues(Collections.singletonList("system-user"));
                attrBean.setSamlAttributes(Collections.singletonList(attributeBean));
                callback.setAttributeStatementData(Collections.singletonList(attrBean));
            }
        }
    }
    
}
