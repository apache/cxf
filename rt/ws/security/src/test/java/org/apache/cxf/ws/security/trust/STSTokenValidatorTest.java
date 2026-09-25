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
package org.apache.cxf.ws.security.trust;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.wss4j.saml.SAML2CallbackHandler;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.saml.WSSSAMLKeyInfoProcessor;
import org.apache.wss4j.dom.validate.Credential;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for local validation of SAML Assertions in the STSTokenValidator.
 */
public class STSTokenValidatorTest {

    private static Crypto signingCrypto;
    private static Crypto trustedCrypto;
    private static Crypto untrustedCrypto;

    @BeforeClass
    public static void init() throws Exception {
        WSSConfig.init();
        signingCrypto = CryptoFactory.getInstance("outsecurity.properties");
        // Contains the signing certificate
        trustedCrypto = CryptoFactory.getInstance("outsecurity.properties");
        // Only trusts an unrelated CA
        untrustedCrypto = CryptoFactory.getInstance("cxfca.properties");
    }

    @Test
    public void testTrustedSignedAssertion() throws Exception {
        STSTokenValidator validator = new STSTokenValidator();
        RequestData data = createRequestData(trustedCrypto);
        assertTrue(validator.isValidatedLocally(createCredential(true, data), data));
    }

    @Test
    public void testUntrustedSignedAssertion() throws Exception {
        STSTokenValidator validator = new STSTokenValidator();
        RequestData data = createRequestData(untrustedCrypto);
        assertFalse(validator.isValidatedLocally(createCredential(true, data), data));
    }

    /**
     * An unsigned Assertion must not be treated as validated locally because a previous
     * signed Assertion was trusted.
     */
    @Test
    public void testUnsignedAssertionAfterTrustedAssertion() throws Exception {
        STSTokenValidator validator = new STSTokenValidator();

        RequestData trustedData = createRequestData(trustedCrypto);
        assertTrue(validator.isValidatedLocally(createCredential(true, trustedData), trustedData));

        RequestData data = createRequestData(trustedCrypto);
        assertFalse(validator.isValidatedLocally(createCredential(false, data), data));
    }

    /**
     * An untrusted Assertion must never be treated as validated locally, even when a trusted
     * Assertion is validated concurrently by the same STSTokenValidator.
     */
    @Test
    public void testConcurrentTrustedAndUntrustedAssertions() throws Exception {
        final STSTokenValidator validator = new STSTokenValidator();
        final int iterations = 200;

        final RequestData trustedData = createRequestData(trustedCrypto);
        final Credential trustedCredential = createCredential(true, trustedData);
        final RequestData untrustedData = createRequestData(untrustedCrypto);
        final Credential untrustedCredential = createCredential(true, untrustedData);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> trusted = () -> {
                boolean result = true;
                for (int i = 0; i < iterations; i++) {
                    result &= validator.isValidatedLocally(trustedCredential, trustedData);
                }
                return result;
            };
            Callable<Boolean> untrusted = () -> {
                boolean result = false;
                for (int i = 0; i < iterations; i++) {
                    result |= validator.isValidatedLocally(untrustedCredential, untrustedData);
                }
                return result;
            };

            Future<Boolean> trustedResult = executor.submit(trusted);
            Future<Boolean> untrustedResult = executor.submit(untrusted);

            assertTrue(trustedResult.get(60, TimeUnit.SECONDS));
            assertFalse(untrustedResult.get(60, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private static RequestData createRequestData(Crypto sigVerCrypto) {
        RequestData data = new RequestData();
        data.setSigVerCrypto(sigVerCrypto);
        data.setWssConfig(WSSConfig.getNewInstance());
        return data;
    }

    private static Credential createCredential(boolean signed, RequestData data) throws Exception {
        SAML2CallbackHandler callbackHandler = new SAML2CallbackHandler();
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(callbackHandler, samlCallback);
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);
        if (signed) {
            assertion.signAssertion("myalias", "myAliasPassword", signingCrypto, false);
        }

        // Round-trip the Assertion via DOM, as would happen on receipt of a message
        Document doc = DOMUtils.createDocument();
        Element element = assertion.toDOM(doc);
        // The Assertion must be attached to the Document so that the signature Reference can be resolved
        doc.appendChild(element);
        SamlAssertionWrapper receivedAssertion = new SamlAssertionWrapper(element);
        if (signed) {
            receivedAssertion.verifySignature(new WSSSAMLKeyInfoProcessor(data), data.getSigVerCrypto());
        }

        Credential credential = new Credential();
        credential.setSamlAssertion(receivedAssertion);
        return credential;
    }
}
