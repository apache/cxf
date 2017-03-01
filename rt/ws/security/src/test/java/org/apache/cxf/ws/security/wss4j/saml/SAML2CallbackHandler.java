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

package org.apache.cxf.ws.security.wss4j.saml;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.bean.KeyInfoBean;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.bean.Version;
import org.apache.wss4j.common.saml.builder.SAML2Constants;

/**
 * A Callback Handler implementation for a SAML 2 assertion. By default it creates an
 * authentication assertion using Sender Vouches.
 */
public class SAML2CallbackHandler extends AbstractSAMLCallbackHandler {

    private boolean signAssertion;

    public SAML2CallbackHandler() throws Exception {
        if (certs == null) {
            Crypto crypto = CryptoFactory.getInstance("alice.properties");
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias("alice");
            certs = crypto.getX509Certificates(cryptoType);
        }

        subjectName = "uid=alice,ou=people,ou=saml-demo,o=example.com";
        subjectQualifier = "www.example.com";
        confirmationMethod = SAML2Constants.CONF_SENDER_VOUCHES;
    }

    public SAML2CallbackHandler(boolean multivalue) throws Exception {
        this();
        this.multiValue = multivalue;
    }

    public void handle(Callback[] callbacks)
        throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof SAMLCallback) {
                SAMLCallback callback = (SAMLCallback) callbacks[i];
                callback.setIssuer("www.example.com");
                callback.setSamlVersion(Version.SAML_20);
                SubjectBean subjectBean =
                    new SubjectBean(
                        subjectName, subjectQualifier, confirmationMethod
                    );
                if (SAML2Constants.CONF_HOLDER_KEY.equals(confirmationMethod)) {
                    try {
                        KeyInfoBean keyInfo = createKeyInfo();
                        subjectBean.setKeyInfo(keyInfo);
                    } catch (Exception ex) {
                        throw new IOException("Problem creating KeyInfo: " +  ex.getMessage());
                    }
                }
                callback.setSubject(subjectBean);
                createAndSetStatement(null, callback);

                try {
                    Crypto crypto = CryptoFactory.getInstance("outsecurity.properties");
                    callback.setIssuerCrypto(crypto);
                    callback.setIssuerKeyName("myalias");
                    callback.setIssuerKeyPassword("myAliasPassword");
                    callback.setSignAssertion(signAssertion);
                } catch (WSSecurityException e) {
                    throw new IOException(e);
                }
            } else {
                throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
            }
        }
    }

    public boolean isSignAssertion() {
        return signAssertion;
    }

    public void setSignAssertion(boolean signAssertion) {
        this.signAssertion = signAssertion;
    }

}
