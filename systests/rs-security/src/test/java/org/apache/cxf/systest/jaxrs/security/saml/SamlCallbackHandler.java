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
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.common.CryptoLoader;
import org.apache.cxf.rs.security.common.SecurityUtils;
import org.apache.cxf.rs.security.saml.assertion.Claim;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.saml.ext.SAMLCallback;
import org.apache.ws.security.saml.ext.bean.AttributeBean;
import org.apache.ws.security.saml.ext.bean.AttributeStatementBean;
import org.apache.ws.security.saml.ext.bean.AuthDecisionStatementBean;
import org.apache.ws.security.saml.ext.bean.AuthDecisionStatementBean.Decision;
import org.apache.ws.security.saml.ext.bean.AuthenticationStatementBean;
import org.apache.ws.security.saml.ext.bean.ConditionsBean;
import org.apache.ws.security.saml.ext.bean.KeyInfoBean;
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
        Message m = PhaseInterceptorChain.getCurrentMessage();
        
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof SAMLCallback) {
                SAMLCallback callback = (SAMLCallback) callbacks[i];
                if (saml2) {
                    callback.setSamlVersion(SAMLVersion.VERSION_20);
                } else {
                    callback.setSamlVersion(SAMLVersion.VERSION_11);
                }
                callback.setIssuer("https://idp.example.org/SAML2");
                
                String subjectName = (String)m.getContextualProperty("saml.subject.name");
                if (subjectName == null) {
                    subjectName = "uid=sts-client,o=mock-sts.com";
                }
                String subjectQualifier = "www.mock-sts.com";
                if (!saml2 && SAML2Constants.CONF_SENDER_VOUCHES.equals(confirmationMethod)) {
                    confirmationMethod = SAML1Constants.CONF_SENDER_VOUCHES;
                }
                SubjectBean subjectBean = 
                    new SubjectBean(
                        subjectName, subjectQualifier, confirmationMethod
                    );
                if (SAML2Constants.CONF_HOLDER_KEY.equals(confirmationMethod)) {
                    
                    try {
                        CryptoLoader loader = new CryptoLoader();
                        Crypto crypto = loader.getCrypto(m, 
                                                         SecurityConstants.SIGNATURE_CRYPTO,
                                                         SecurityConstants.SIGNATURE_PROPERTIES);
                        X509Certificate cert = 
                            SecurityUtils.getCertificates(crypto, 
                                SecurityUtils.getUserName(m, crypto, "ws-security.signature.username"))[0];
                        
                        KeyInfoBean keyInfo = new KeyInfoBean();
                        keyInfo.setCertificate(cert);
                        subjectBean.setKeyInfo(keyInfo);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
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
                
                List<String> roles = CastUtils.cast((List<?>)m.getContextualProperty("saml.roles"));
                if (roles == null) {
                    roles = Collections.singletonList("user");
                }
                List<AttributeBean> claims = new ArrayList<AttributeBean>();
                AttributeBean roleClaim = new AttributeBean();
                roleClaim.setSimpleName("subject-role");
                roleClaim.setQualifiedName(Claim.DEFAULT_ROLE_NAME);
                roleClaim.setNameFormat(Claim.DEFAULT_NAME_FORMAT);
                roleClaim.setAttributeValues(roles);
                claims.add(roleClaim);
                
                List<String> authMethods = CastUtils.cast((List<?>)m.getContextualProperty("saml.auth"));
                if (authMethods == null) {
                    authMethods = Collections.singletonList("password");
                }
                
                AttributeBean authClaim = new AttributeBean();
                authClaim.setQualifiedName("http://claims/authentication");
                authClaim.setNameFormat("http://claims/authentication-format");
                authClaim.setAttributeValues(authMethods);
                claims.add(authClaim);
                
                attrBean.setSamlAttributes(claims);
                callback.setAttributeStatementData(Collections.singletonList(attrBean));
            }
        }
    }
    
}
