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
package org.apache.cxf.rs.security.httpsignature;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.rs.security.httpsignature.exception.DifferentAlgorithmsException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidDataToVerifySignatureException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidSignatureException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.exception.MissingSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.exception.MultipleSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.provider.MockAlgorithmProvider;
import org.apache.cxf.rs.security.httpsignature.provider.MockPublicKeyProvider;
import org.apache.cxf.rs.security.httpsignature.provider.MockSecurityProvider;
import org.apache.cxf.rs.security.httpsignature.utils.SignatureHeaderUtils;

import org.junit.BeforeClass;
import org.junit.Test;

public class MessageVerifierTest {
    private static final String KEY_ID = "testVerifier";
    private static final String MESSAGE_BODY = "Hello";
    private static final String METHOD = "GET";
    private static final String URI = "/test/signature";
    private static final String KEY_PAIR_GENERATOR_ALGORITHM = "RSA";

    private static MessageSigner messageSigner;
    private static MessageVerifier messageVerifier;

    @BeforeClass
    public static void setup() {

        try {
            final KeyPair keyPair = KeyPairGenerator.getInstance(KEY_PAIR_GENERATOR_ALGORITHM)
                    .generateKeyPair();

            messageVerifier = new MessageVerifier(new MockPublicKeyProvider(keyPair.getPublic()));
            messageVerifier.setSecurityProvider(new MockSecurityProvider());
            messageVerifier.setAlgorithmProvider(new MockAlgorithmProvider());

            messageSigner = new MessageSigner(keyPair.getPrivate(), KEY_ID);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void validUnalteredRequest() {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers, MESSAGE_BODY);
        messageVerifier.verifyMessage(headers, METHOD, URI);
    }

    @Test
    public void validUnalteredRequestWithoutBody() {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers);
        messageVerifier.verifyMessage(headers, METHOD, URI);
    }

    @Test
    public void validUnalteredRequestWithExtraHeaders() {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers, MESSAGE_BODY);
        headers.put("Test", Collections.singletonList("value"));
        headers.put("Test2", Collections.singletonList("value2"));
        messageVerifier.verifyMessage(headers, METHOD, URI);
    }

    @Test(expected = DifferentAlgorithmsException.class)
    public void differentAlgorithmsFails() {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers, MESSAGE_BODY);
        String signature = headers.get("Signature").get(0);
        signature = signature.replaceFirst("algorithm=\"rsa-sha256", "algorithm=\"hmac-sha256");
        headers.replace("Signature", Collections.singletonList(signature));
        messageVerifier.verifyMessage(headers, METHOD, URI);
    }

    @Test(expected = InvalidDataToVerifySignatureException.class)
    public void invalidDataToVerifySignatureFails() {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers, MESSAGE_BODY);
        headers.remove("Content-Length");
        messageVerifier.verifyMessage(headers, METHOD, URI);
    }

    @Test(expected = InvalidSignatureException.class)
    public void invalidSignatureFails() {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers, MESSAGE_BODY);
        String signature = headers.get("Signature").get(0);
        signature = signature.replaceFirst("signature=\"[\\w][\\w]", "signature=\"AA");
        headers.replace("Signature", Collections.singletonList(signature));
        messageVerifier.verifyMessage(headers, METHOD, URI);
    }

    @Test(expected = InvalidSignatureHeaderException.class)
    public void invalidSignatureHeaderFails() {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers, MESSAGE_BODY);
        String signature = headers.get("Signature").get(0);
        signature = signature.replaceFirst(",signature", "signature");
        headers.replace("Signature", Collections.singletonList(signature));
        messageVerifier.verifyMessage(headers, METHOD, URI);
    }

    @Test(expected = MissingSignatureHeaderException.class)
    public void missingSignatureHeaderFails() {
        Map<String, List<String>> headers = createMockHeaders();
        messageVerifier.verifyMessage(headers, METHOD, URI);
    }

    @Test(expected = MultipleSignatureHeaderException.class)
    public void multipleSignatureHeaderFails() {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers, MESSAGE_BODY);
        String signature = headers.get("Signature").get(0);
        List<String> signatureList = new ArrayList<>(headers.get("Signature"));
        signatureList.add(signature);
        headers.put("Signature", signatureList);
        messageVerifier.verifyMessage(headers, METHOD, URI);
    }

    private static void createAndAddSignature(Map<String, List<String>> headers, String messageBody) {
        try {
            messageSigner.sign(headers, URI, METHOD, messageBody);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createAndAddSignature(Map<String, List<String>> headers) {
        try {
            messageSigner.sign(headers, URI, METHOD);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, List<String>> createMockHeaders() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Host", Collections.singletonList("example.org"));
        headers.put("Accept", Collections.singletonList(""));
        headers.put("Content-Length", Collections.singletonList("18"));
        SignatureHeaderUtils.addDateHeader(headers, ZoneOffset.UTC);
        return headers;
    }

}
