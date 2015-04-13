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

package org.apache.cxf.systest.jaxrs.security.oauth2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rt.security.saml.claims.SAMLClaim;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.bean.ActionBean;
import org.apache.wss4j.common.saml.bean.AttributeBean;
import org.apache.wss4j.common.saml.bean.AttributeStatementBean;
import org.apache.wss4j.common.saml.bean.AudienceRestrictionBean;
import org.apache.wss4j.common.saml.bean.AuthDecisionStatementBean;
import org.apache.wss4j.common.saml.bean.AuthDecisionStatementBean.Decision;
import org.apache.wss4j.common.saml.bean.AuthenticationStatementBean;
import org.apache.wss4j.common.saml.bean.ConditionsBean;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.bean.Version;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.joda.time.DateTime;

/**
 * A CallbackHandler instance that is used by the STS to mock up a SAML Attribute Assertion.
 */
public class SamlCallbackHandler2 implements CallbackHandler {
    public static final String PORT = BookServerOAuth2.PORT;
    private String confirmationMethod = SAML2Constants.CONF_BEARER;
    
    public SamlCallbackHandler2() {
    }
    
    public void setConfirmationMethod(String confirmationMethod) {
        this.confirmationMethod = confirmationMethod;
    }
    
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof SAMLCallback) {
                SAMLCallback callback = (SAMLCallback) callbacks[i];
                callback.setSamlVersion(Version.SAML_20);
                callback.setIssuer("alice");
                
                String subjectName = m != null ? (String)m.getContextualProperty("saml.subject.name") : null;
                if (subjectName == null) {
                    subjectName = "alice";
                }
                String subjectQualifier = "www.mock-sts.com";
                SubjectBean subjectBean = 
                    new SubjectBean(
                        subjectName, subjectQualifier, confirmationMethod
                    );
                callback.setSubject(subjectBean);
                
                ConditionsBean conditions = new ConditionsBean();
                AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
                String audienceURI = "https://localhost:" + PORT + "/oauth2-auth/token";
                audienceRestriction.setAudienceURIs(Collections.singletonList(audienceURI));
                conditions.setAudienceRestrictions(Collections.singletonList(audienceRestriction));
                
                callback.setConditions(conditions);
                
                AuthDecisionStatementBean authDecBean = new AuthDecisionStatementBean();
                authDecBean.setDecision(Decision.INDETERMINATE);
                authDecBean.setResource("https://sp.example.com/SAML2");
                ActionBean actionBean = new ActionBean();
                actionBean.setContents("Read");
                authDecBean.setActions(Collections.singletonList(actionBean));
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
                
                List<String> roles = m != null 
                    ? CastUtils.<String>cast((List<?>)m.getContextualProperty("saml.roles")) : null;
                if (roles == null) {
                    roles = Collections.singletonList("user");
                }
                List<AttributeBean> claims = new ArrayList<AttributeBean>();
                AttributeBean roleClaim = new AttributeBean();
                roleClaim.setSimpleName("subject-role");
                roleClaim.setQualifiedName(SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT);
                roleClaim.setNameFormat(SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED);
                roleClaim.setAttributeValues(new ArrayList<Object>(roles));
                claims.add(roleClaim);
                
                List<String> authMethods = 
                    m != null ? CastUtils.<String>cast((List<?>)m.getContextualProperty("saml.auth")) : null;
                if (authMethods == null) {
                    authMethods = Collections.singletonList("password");
                }
                
                AttributeBean authClaim = new AttributeBean();
                authClaim.setSimpleName("http://claims/authentication");
                authClaim.setQualifiedName("http://claims/authentication");
                authClaim.setNameFormat("http://claims/authentication-format");
                authClaim.setAttributeValues(new ArrayList<Object>(authMethods));
                claims.add(authClaim);
                
                attrBean.setSamlAttributes(claims);
                callback.setAttributeStatementData(Collections.singletonList(attrBean));
                
                try {
                    Crypto crypto = 
                        CryptoFactory.getInstance("org/apache/cxf/systest/jaxrs/security/alice.properties");
                    callback.setIssuerCrypto(crypto);
                    callback.setIssuerKeyName("alice");
                    callback.setIssuerKeyPassword("password");
                    callback.setSignAssertion(true);
                } catch (WSSecurityException e) {
                    throw new IOException(e);
                }
            }
        }
    }
    
}
