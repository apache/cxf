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
public class SamlRoleCallbackHandler implements CallbackHandler {
    private static final String ROLE_URI =
        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";
    private boolean saml2 = true;
    private String confirmationMethod = SAML2Constants.CONF_BEARER;
    private CERT_IDENTIFIER keyInfoIdentifier = CERT_IDENTIFIER.X509_CERT;
    private String roleName;
    private boolean signAssertion;
    private String cryptoAlias = "alice";
    private String cryptoPassword = "password";
    private String cryptoPropertiesFile = "alice.properties";

    public SamlRoleCallbackHandler() {
        //
    }

    public SamlRoleCallbackHandler(boolean saml2) {
        this.saml2 = saml2;
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
                attributeBean.setNameFormat(SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED);
                if (saml2) {
                    attributeBean.setQualifiedName(ROLE_URI);
                    attributeBean.setNameFormat(SAML2Constants.ATTRNAME_FORMAT_UNSPECIFIED);
                } else {
                    String uri = ROLE_URI;
                    int lastSlash = uri.lastIndexOf('/');
                    if (lastSlash == (uri.length() - 1)) {
                        uri = uri.substring(0, lastSlash);
                        lastSlash = uri.lastIndexOf('/');
                    }

                    String namespace = uri.substring(0, lastSlash);
                    String name = uri.substring(lastSlash + 1, uri.length());

                    attributeBean.setSimpleName(name);
                    attributeBean.setQualifiedName(namespace);
                }
                attributeBean.addAttributeValue(roleName);
                attrBean.setSamlAttributes(Collections.singletonList(attributeBean));
                callback.setAttributeStatementData(Collections.singletonList(attrBean));

                try {
                    Crypto crypto = CryptoFactory.getInstance(cryptoPropertiesFile);
                    callback.setIssuerCrypto(crypto);
                    callback.setIssuerKeyName(cryptoAlias);
                    callback.setIssuerKeyPassword(cryptoPassword);
                    callback.setSignAssertion(signAssertion);
                } catch (Exception ex) {
                    throw new IOException("Problem creating KeyInfo: " +  ex.getMessage());
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
        keyInfo.setCertIdentifer(keyInfoIdentifier);
        if (keyInfoIdentifier == CERT_IDENTIFIER.X509_CERT) {
            keyInfo.setCertificate(certs[0]);
        } else if (keyInfoIdentifier == CERT_IDENTIFIER.KEY_VALUE) {
            keyInfo.setPublicKey(certs[0].getPublicKey());
        }

        return keyInfo;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public void setSignAssertion(boolean signAssertion) {
        this.signAssertion = signAssertion;
    }
}
