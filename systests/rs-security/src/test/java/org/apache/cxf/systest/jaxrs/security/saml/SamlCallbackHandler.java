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
import java.time.Instant;
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
import org.apache.cxf.rs.security.common.RSSecurityUtils;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.rt.security.claims.SAMLClaim;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.bean.ActionBean;
import org.apache.wss4j.common.saml.bean.AttributeBean;
import org.apache.wss4j.common.saml.bean.AttributeStatementBean;
import org.apache.wss4j.common.saml.bean.AudienceRestrictionBean;
import org.apache.wss4j.common.saml.bean.AuthDecisionStatementBean;
import org.apache.wss4j.common.saml.bean.AuthDecisionStatementBean.Decision;
import org.apache.wss4j.common.saml.bean.AuthenticationStatementBean;
import org.apache.wss4j.common.saml.bean.ConditionsBean;
import org.apache.wss4j.common.saml.bean.KeyInfoBean;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.bean.Version;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;

/**
 * A CallbackHandler instance that is used by the STS to mock up a SAML Attribute Assertion.
 */
public class SamlCallbackHandler implements CallbackHandler {
    private boolean saml2 = true;
    private String confirmationMethod = SAML2Constants.CONF_SENDER_VOUCHES;
    private String signatureAlgorithm;
    private String digestAlgorithm;
    private boolean signAssertion;

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
                    callback.setSamlVersion(Version.SAML_20);
                } else {
                    callback.setSamlVersion(Version.SAML_11);
                }
                callback.setIssuer("https://idp.example.org/SAML2");

                String subjectName = null;
                if (m != null) {
                    subjectName = (String)m.getContextualProperty("saml.subject.name");
                }
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
                            RSSecurityUtils.getCertificates(crypto,
                                RSSecurityUtils.getUserName(m, crypto, SecurityConstants.SIGNATURE_USERNAME))[0];

                        KeyInfoBean keyInfo = new KeyInfoBean();
                        keyInfo.setCertificate(cert);
                        subjectBean.setKeyInfo(keyInfo);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
                callback.setSubject(subjectBean);

                ConditionsBean conditions = new ConditionsBean();

                AudienceRestrictionBean audienceRestriction = new AudienceRestrictionBean();
                audienceRestriction.setAudienceURIs(Collections.singletonList("https://sp.example.com/SAML2"));
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
                authBean.setAuthenticationInstant(Instant.now());
                authBean.setSessionIndex("123456");
                // AuthnContextClassRef is not set
                authBean.setAuthenticationMethod(
                        "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport");
                callback.setAuthenticationStatementData(
                    Collections.singletonList(authBean));

                AttributeStatementBean attrBean = new AttributeStatementBean();
                attrBean.setSubject(subjectBean);

                List<String> roles = null;
                if (m != null) {
                    roles = CastUtils.cast((List<?>)m.getContextualProperty("saml.roles"));
                }
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

                List<String> authMethods = null;
                if (m != null) {
                    authMethods = CastUtils.cast((List<?>)m.getContextualProperty("saml.auth"));
                }
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

                callback.setSignatureAlgorithm(signatureAlgorithm);
                callback.setSignatureDigestAlgorithm(digestAlgorithm);

                callback.setSignAssertion(signAssertion);
            }
        }
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public void setDigestAlgorithm(String digestAlgorithm) {
        this.digestAlgorithm = digestAlgorithm;
    }

    public boolean isSignAssertion() {
        return signAssertion;
    }

    public void setSignAssertion(boolean signAssertion) {
        this.signAssertion = signAssertion;
    }

}
