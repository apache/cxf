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

package org.apache.cxf.systest.jaxrs.security.oauth2.common;

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
import org.apache.cxf.rt.security.claims.SAMLClaim;
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
public class SamlCallbackHandler implements CallbackHandler {
    private String confirmationMethod = SAML2Constants.CONF_BEARER;
    private boolean signAssertion = true;
    private String issuer = "resourceOwner";
    private String audience;
    private boolean saml2 = true;
    private String cryptoPropertiesFile = "org/apache/cxf/systest/jaxrs/security/alice.properties";
    private String issuerKeyName = "alice";
    private String issuerKeyPassword = "password";
    private String subjectName = "alice";

    public SamlCallbackHandler(boolean signAssertion) {
        this.signAssertion = signAssertion;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        Message m = PhaseInterceptorChain.getCurrentMessage();

        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof SAMLCallback) {
                SAMLCallback callback = (SAMLCallback) callbacks[i];
                if (saml2) {
                    callback.setSamlVersion(Version.SAML_20);
                } else {
                    callback.setSamlVersion(Version.SAML_11);
                }
                callback.setIssuer(issuer);

                String subject = m != null ? (String)m.getContextualProperty("saml.subject.name") : null;
                if (subject == null) {
                    subject = subjectName;
                }
                String subjectQualifier = "www.mock-sts.com";
                SubjectBean subjectBean =
                    new SubjectBean(
                        subject, subjectQualifier, confirmationMethod
                    );
                callback.setSubject(subjectBean);

                ConditionsBean conditions = new ConditionsBean();

                AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
                audienceRestriction.setAudienceURIs(Collections.singletonList(audience));
                conditions.setAudienceRestrictions(Collections.singletonList(audienceRestriction));

                callback.setConditions(conditions);

                AuthDecisionStatementBean authDecBean = new AuthDecisionStatementBean();
                authDecBean.setDecision(Decision.INDETERMINATE);
                authDecBean.setResource("https://sp.example.com/SAML2");
                authDecBean.setSubject(subjectBean);

                ActionBean actionBean = new ActionBean();
                actionBean.setContents("Read");
                authDecBean.setActions(Collections.singletonList(actionBean));
                callback.setAuthDecisionStatementData(Collections.singletonList(authDecBean));

                AuthenticationStatementBean authBean = new AuthenticationStatementBean();
                authBean.setSubject(subjectBean);
                authBean.setAuthenticationInstant(new DateTime());
                authBean.setSessionIndex("123456");
                authBean.setSubject(subjectBean);

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
                List<AttributeBean> claims = new ArrayList<>();
                AttributeBean roleClaim = new AttributeBean();
                roleClaim.setSimpleName("subject-role");
                roleClaim.setQualifiedName(SAMLClaim.SAML_ROLE_ATTRIBUTENAME_DEFAULT);
                roleClaim.setNameFormat(SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED);
                roleClaim.setAttributeValues(new ArrayList<>(roles));
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
                authClaim.setAttributeValues(new ArrayList<>(authMethods));
                claims.add(authClaim);

                attrBean.setSamlAttributes(claims);
                callback.setAttributeStatementData(Collections.singletonList(attrBean));

                if (signAssertion) {
                    try {
                        Crypto crypto = CryptoFactory.getInstance(cryptoPropertiesFile);
                        callback.setIssuerCrypto(crypto);
                        callback.setIssuerKeyName(issuerKeyName);
                        callback.setIssuerKeyPassword(issuerKeyPassword);
                        callback.setSignAssertion(true);
                    } catch (WSSecurityException e) {
                        throw new IOException(e);
                    }
                }
            }
        }
    }

    public String getCryptoPropertiesFile() {
        return cryptoPropertiesFile;
    }

    public void setCryptoPropertiesFile(String cryptoPropertiesFile) {
        this.cryptoPropertiesFile = cryptoPropertiesFile;
    }

    public String getIssuerKeyName() {
        return issuerKeyName;
    }

    public void setIssuerKeyName(String issuerKeyName) {
        this.issuerKeyName = issuerKeyName;
    }

    public String getIssuerKeyPassword() {
        return issuerKeyPassword;
    }

    public void setIssuerKeyPassword(String issuerKeyPassword) {
        this.issuerKeyPassword = issuerKeyPassword;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public void setConfirmationMethod(String confMethod) {
        this.confirmationMethod = confMethod;
    }

    public boolean isSaml2() {
        return saml2;
    }

    public void setSaml2(boolean saml2) {
        this.saml2 = saml2;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

}
