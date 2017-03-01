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

package org.apache.cxf.systest.ws.saml.client;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collections;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.bean.AttributeBean;
import org.apache.wss4j.common.saml.bean.AttributeStatementBean;
import org.apache.wss4j.common.saml.bean.ConditionsBean;
import org.apache.wss4j.common.saml.bean.KeyInfoBean;
import org.apache.wss4j.common.saml.bean.KeyInfoBean.CERT_IDENTIFIER;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.bean.Version;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.dom.WSConstants;

/**
 * A CallbackHandler instance that is used by the STS to mock up a SAML Attribute Assertion.
 */
public class SamlCallbackHandler implements CallbackHandler {
    private boolean saml2 = true;
    private String confirmationMethod = SAML2Constants.CONF_SENDER_VOUCHES;
    private CERT_IDENTIFIER keyInfoIdentifier = CERT_IDENTIFIER.X509_CERT;
    private boolean signAssertion;
    private ConditionsBean conditions;
    private String cryptoAlias = "alice";
    private String cryptoPassword = "password";
    private String cryptoPropertiesFile = "alice.properties";
    private String signatureAlgorithm = WSConstants.RSA_SHA1;
    private String digestAlgorithm = WSConstants.SHA1;

    public SamlCallbackHandler() {
        //
    }

    public SamlCallbackHandler(boolean saml2) {
        this.saml2 = saml2;
    }

    public SamlCallbackHandler(boolean saml2, boolean signAssertion) {
        this.saml2 = saml2;
        this.signAssertion = signAssertion;
    }

    public void setConfirmationMethod(String confirmationMethod) {
        this.confirmationMethod = confirmationMethod;
    }

    public void setKeyInfoIdentifier(CERT_IDENTIFIER keyInfoIdentifier) {
        this.keyInfoIdentifier = keyInfoIdentifier;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof SAMLCallback) {
                SAMLCallback callback = (SAMLCallback) callbacks[i];
                if (saml2) {
                    callback.setSamlVersion(Version.SAML_20);
                } else {
                    callback.setSamlVersion(Version.SAML_11);
                }
                if (conditions != null) {
                    callback.setConditions(conditions);
                }

                callback.setIssuer("sts");
                String subjectName = "uid=sts-client,o=mock-sts.com";
                String subjectQualifier = "www.mock-sts.com";
                if (!saml2 && SAML2Constants.CONF_SENDER_VOUCHES.equals(confirmationMethod)) {
                    confirmationMethod = SAML1Constants.CONF_SENDER_VOUCHES;
                }
                SubjectBean subjectBean =
                    new SubjectBean(
                        subjectName, subjectQualifier, confirmationMethod
                    );
                if (SAML2Constants.CONF_HOLDER_KEY.equals(confirmationMethod)
                    || SAML1Constants.CONF_HOLDER_KEY.equals(confirmationMethod)) {
                    try {
                        KeyInfoBean keyInfo = createKeyInfo();
                        subjectBean.setKeyInfo(keyInfo);
                    } catch (Exception ex) {
                        throw new IOException("Problem creating KeyInfo: " +  ex.getMessage());
                    }
                }
                callback.setSubject(subjectBean);

                AttributeStatementBean attrBean = new AttributeStatementBean();
                attrBean.setSubject(subjectBean);

                AttributeBean attributeBean = new AttributeBean();
                if (saml2) {
                    attributeBean.setQualifiedName("subject-role");
                } else {
                    attributeBean.setSimpleName("subject-role");
                    attributeBean.setQualifiedName("http://custom-ns");
                }
                attributeBean.addAttributeValue("system-user");
                attrBean.setSamlAttributes(Collections.singletonList(attributeBean));
                callback.setAttributeStatementData(Collections.singletonList(attrBean));
                callback.setSignatureAlgorithm(signatureAlgorithm);
                callback.setSignatureDigestAlgorithm(digestAlgorithm);

                try {
                    Crypto crypto = CryptoFactory.getInstance(cryptoPropertiesFile);
                    callback.setIssuerCrypto(crypto);
                    callback.setIssuerKeyName(cryptoAlias);
                    callback.setIssuerKeyPassword(cryptoPassword);
                    callback.setSignAssertion(signAssertion);
                } catch (WSSecurityException e) {
                    throw new IOException(e);
                }
            }
        }
    }

    protected KeyInfoBean createKeyInfo() throws Exception {
        Crypto crypto =
            CryptoFactory.getInstance(cryptoPropertiesFile);
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(cryptoAlias);
        X509Certificate[] certs = crypto.getX509Certificates(cryptoType);

        KeyInfoBean keyInfo = new KeyInfoBean();
        keyInfo.setCertIdentifer(keyInfoIdentifier);
        if (keyInfoIdentifier == CERT_IDENTIFIER.X509_CERT) {
            keyInfo.setCertificate(certs[0]);
        } else if (keyInfoIdentifier == CERT_IDENTIFIER.KEY_VALUE) {
            keyInfo.setPublicKey(certs[0].getPublicKey());
        }

        return keyInfo;
    }

    public boolean isSignAssertion() {
        return signAssertion;
    }

    public void setSignAssertion(boolean signAssertion) {
        this.signAssertion = signAssertion;
    }

    public ConditionsBean getConditions() {
        return conditions;
    }

    public void setConditions(ConditionsBean conditions) {
        this.conditions = conditions;
    }

    public String getCryptoAlias() {
        return cryptoAlias;
    }

    public void setCryptoAlias(String cryptoAlias) {
        this.cryptoAlias = cryptoAlias;
    }

    public String getCryptoPassword() {
        return cryptoPassword;
    }

    public void setCryptoPassword(String cryptoPassword) {
        this.cryptoPassword = cryptoPassword;
    }

    public String getCryptoPropertiesFile() {
        return cryptoPropertiesFile;
    }

    public void setCryptoPropertiesFile(String cryptoPropertiesFile) {
        this.cryptoPropertiesFile = cryptoPropertiesFile;
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

}
