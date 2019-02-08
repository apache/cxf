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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.SignatureProperties;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.token.realm.RealmProperties;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;

/**
 * Some abstract functionality for creating a SAML token
 */
public abstract class AbstractSAMLTokenProvider {

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractSAMLTokenProvider.class);

    protected void signToken(
        SamlAssertionWrapper assertion,
        RealmProperties samlRealm,
        STSPropertiesMBean stsProperties,
        KeyRequirements keyRequirements
    ) throws Exception {
        // Initialise signature objects with defaults of STSPropertiesMBean
        Crypto signatureCrypto = stsProperties.getSignatureCrypto();
        CallbackHandler callbackHandler = stsProperties.getCallbackHandler();
        SignatureProperties signatureProperties = stsProperties.getSignatureProperties();
        String alias = stsProperties.getSignatureUsername();

        if (samlRealm != null) {
            // If SignatureCrypto configured in realm then
            // callbackhandler and alias of STSPropertiesMBean is ignored
            if (samlRealm.getSignatureCrypto() != null) {
                LOG.fine("SAMLRealm signature keystore used");
                signatureCrypto = samlRealm.getSignatureCrypto();
                callbackHandler = samlRealm.getCallbackHandler();
                alias = samlRealm.getSignatureAlias();
            }
            // SignatureProperties can be defined independently of SignatureCrypto
            if (samlRealm.getSignatureProperties() != null) {
                signatureProperties = samlRealm.getSignatureProperties();
            }
        }

        // Get the signature algorithm to use
        String signatureAlgorithm = keyRequirements.getSignatureAlgorithm();
        if (signatureAlgorithm == null) {
            // If none then default to what is configured
            signatureAlgorithm = signatureProperties.getSignatureAlgorithm();
        } else {
            List<String> supportedAlgorithms =
                signatureProperties.getAcceptedSignatureAlgorithms();
            if (!supportedAlgorithms.contains(signatureAlgorithm)) {
                signatureAlgorithm = signatureProperties.getSignatureAlgorithm();
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("SignatureAlgorithm not supported, defaulting to: " + signatureAlgorithm);
                }
            }
        }

        // Get the c14n algorithm to use
        String c14nAlgorithm = keyRequirements.getC14nAlgorithm();
        if (c14nAlgorithm == null) {
            // If none then default to what is configured
            c14nAlgorithm = signatureProperties.getC14nAlgorithm();
        } else {
            List<String> supportedAlgorithms =
                signatureProperties.getAcceptedC14nAlgorithms();
            if (!supportedAlgorithms.contains(c14nAlgorithm)) {
                c14nAlgorithm = signatureProperties.getC14nAlgorithm();
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("C14nAlgorithm not supported, defaulting to: " + c14nAlgorithm);
                }
            }
        }

        // If alias not defined, get the default of the SignatureCrypto
        if ((alias == null || "".equals(alias)) && (signatureCrypto != null)) {
            alias = signatureCrypto.getDefaultX509Identifier();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Signature alias is null so using default alias: " + alias);
            }
        }
        // Get the password
        String password = null;
        if (callbackHandler != null) {
            WSPasswordCallback[] cb = {new WSPasswordCallback(alias, WSPasswordCallback.SIGNATURE)};
            LOG.fine("Creating SAML Token");
            callbackHandler.handle(cb);
            password = cb[0].getPassword();
        }

        LOG.fine("Signing SAML Token");
        boolean useKeyValue = signatureProperties.isUseKeyValue();
        assertion.signAssertion(
            alias, password, signatureCrypto, useKeyValue, c14nAlgorithm, signatureAlgorithm,
            signatureProperties.getDigestAlgorithm()
        );
    }


}
