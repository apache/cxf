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

package org.apache.cxf.systest.wssec.examples.saml;

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
import org.apache.wss4j.common.saml.bean.KeyInfoBean;
import org.apache.wss4j.common.saml.bean.KeyInfoBean.CERT_IDENTIFIER;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.bean.Version;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;

/**
 * A CallbackHandler instance that is used by the STS to mock up a SAML Attribute Assertion.
 */
public class SamlCallbackHandler implements CallbackHandler {
    private String confirmationMethod;
    private boolean saml2;
    private boolean signed;

    public SamlCallbackHandler() {
        //
    }

    public void setConfirmationMethod(String confirmationMethod) {
        this.confirmationMethod = confirmationMethod;
    }

    public void setSaml2(boolean isSaml2) {
        saml2 = isSaml2;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof SAMLCallback) {
                SAMLCallback callback = (SAMLCallback) callbacks[i];
                if (!saml2) {
                    callback.setSamlVersion(Version.SAML_11);
                }
                callback.setIssuer("sts");
                String subjectName = "uid=sts-client,o=mock-sts.com";
                String subjectQualifier = "www.mock-sts.com";

                String subjectConfMethod = confirmationMethod;
                if (subjectConfMethod == null && !saml2) {
                    subjectConfMethod = SAML1Constants.CONF_BEARER;
                } else if (subjectConfMethod == null && saml2) {
                    subjectConfMethod = SAML2Constants.CONF_BEARER;
                }

                SubjectBean subjectBean =
                    new SubjectBean(
                        subjectName, subjectQualifier, subjectConfMethod
                    );
                if (SAML2Constants.CONF_HOLDER_KEY.equals(subjectConfMethod)
                    || SAML1Constants.CONF_HOLDER_KEY.equals(subjectConfMethod)) {
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

                try {
                    String file = "alice.properties";
                    Crypto crypto = CryptoFactory.getInstance(file);
                    callback.setIssuerCrypto(crypto);
                    callback.setIssuerKeyName("alice");
                    callback.setIssuerKeyPassword("password");
                    callback.setSignAssertion(signed);
                } catch (WSSecurityException e) {
                    throw new IOException(e);
                }
            }
        }
    }

    protected KeyInfoBean createKeyInfo() throws Exception {
        Crypto crypto =
            CryptoFactory.getInstance("alice.properties");
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias("alice");
        X509Certificate[] certs = crypto.getX509Certificates(cryptoType);

        KeyInfoBean keyInfo = new KeyInfoBean();
        keyInfo.setCertificate(certs[0]);
        keyInfo.setCertIdentifer(CERT_IDENTIFIER.X509_CERT);

        return keyInfo;
    }

    public boolean isSigned() {
        return signed;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
    }

}
