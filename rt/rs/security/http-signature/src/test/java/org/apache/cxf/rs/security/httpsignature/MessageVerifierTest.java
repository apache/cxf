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

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rs.security.httpsignature.exception.DifferentAlgorithmsException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidDataToVerifySignatureException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidSignatureException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.exception.MissingSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.exception.MultipleSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.provider.MockAlgorithmProvider;
import org.apache.cxf.rs.security.httpsignature.provider.MockSecurityProvider;
import org.apache.cxf.rs.security.httpsignature.utils.SignatureHeaderUtils;

import org.junit.BeforeClass;
import org.junit.Test;

public class MessageVerifierTest {
    private static final String KEY_ID = "testVerifier";
    private static final String METHOD = "GET";
    private static final String DELETE_METHOD = "DELETE";
    private static final String URI = "/test/signature";
    private static final String KEY_PAIR_GENERATOR_ALGORITHM = "RSA";
    private static final String MESSAGE_BODY = "Hello";

    private static MessageSigner messageSigner;
    private static MessageVerifier messageVerifier;
    private static KeyPair keyPair;

    @BeforeClass
    public static void setUp() {

        try {
            keyPair = KeyPairGenerator.getInstance(KEY_PAIR_GENERATOR_ALGORITHM).generateKeyPair();

            messageVerifier = new MessageVerifier(keyId -> keyPair.getPublic());
            messageVerifier.setSecurityProvider(new MockSecurityProvider());
            messageVerifier.setAlgorithmProvider(new MockAlgorithmProvider());
            messageVerifier.setAddDefaultRequiredHeaders(false);

            messageSigner = new MessageSigner(keyId -> keyPair.getPrivate(), KEY_ID);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void validUnalteredRequest() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers);
        messageVerifier.verifyMessage(headers, METHOD, URI, new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test
    public void nullBodyRequest() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers);
        messageVerifier.verifyMessage(headers, DELETE_METHOD, URI, new MessageImpl(), null);
    }

    @Test
    public void validUnalteredRequestWithoutBody() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers);
        messageVerifier.verifyMessage(headers, METHOD, URI, new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test
    public void validUnalteredRequestWithExtraHeaders() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers);
        headers.put("Test", Collections.singletonList("value"));
        headers.put("Test2", Collections.singletonList("value2"));
        messageVerifier.verifyMessage(headers, METHOD, URI, new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test(expected = DifferentAlgorithmsException.class)
    public void differentAlgorithmsFails() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers);
        String signature = headers.get("Signature").get(0);
        signature = signature.replaceFirst("algorithm=\"rsa-sha256", "algorithm=\"hmac-sha256");
        headers.replace("Signature", Collections.singletonList(signature));
        messageVerifier.verifyMessage(headers, METHOD, URI, new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test(expected = InvalidDataToVerifySignatureException.class)
    public void invalidDataToVerifySignatureFails() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers);
        headers.remove("Content-Length");
        messageVerifier.verifyMessage(headers, METHOD, URI, new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test(expected = InvalidSignatureException.class)
    public void invalidSignatureFails() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers);
        String signature = headers.get("Signature").get(0);
        signature = signature.replaceFirst("signature=\".{10}", "signature=\"AAAAAAAAAA");
        headers.replace("Signature", Collections.singletonList(signature));
        messageVerifier.verifyMessage(headers, METHOD, URI, new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test(expected = InvalidSignatureHeaderException.class)
    public void invalidSignatureHeaderFails() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers);
        String signature = headers.get("Signature").get(0);
        signature = signature.replaceFirst(",signature", ",signature2");
        headers.replace("Signature", Collections.singletonList(signature));
        messageVerifier.verifyMessage(headers, METHOD, URI, new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test(expected = MissingSignatureHeaderException.class)
    public void missingSignatureHeaderFails() {
        Map<String, List<String>> headers = createMockHeaders();
        messageVerifier.verifyMessage(headers, METHOD, URI, new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test(expected = MultipleSignatureHeaderException.class)
    public void multipleSignatureHeaderFails() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers);
        String signature = headers.get("Signature").get(0);
        List<String> signatureList = new ArrayList<>(headers.get("Signature"));
        signatureList.add(signature);
        headers.put("Signature", signatureList);
        messageVerifier.verifyMessage(headers, METHOD, URI, new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test
    public void symmetricSignature() throws IOException, NoSuchAlgorithmException {
        Map<String, List<String>> headers = createMockHeaders();

        KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
        SecretKey secretKey = keyGenerator.generateKey();

        MessageSigner hmacMessageSigner =
            new MessageSigner("hmac-sha256", keyId -> secretKey, KEY_ID);
        hmacMessageSigner.sign(headers, URI, METHOD);

        MessageVerifier hmacMessageVerifier =
            new MessageVerifier(keyId -> secretKey, null, keyId -> "hmac-sha256", Collections.emptyList());
        hmacMessageVerifier.setAddDefaultRequiredHeaders(false);
        hmacMessageVerifier.verifyMessage(headers, METHOD, URI, new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test
    public void requiredHeaderPresent() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();
        headers.put("Test", Collections.singletonList("value"));
        createAndAddSignature(headers);

        MessageVerifier headerVerifier = new MessageVerifier(keyId -> keyPair.getPublic(),
                                                             Collections.singletonList("test"));
        headerVerifier.setAddDefaultRequiredHeaders(false);
        headerVerifier.setSecurityProvider(new MockSecurityProvider());
        headerVerifier.setAlgorithmProvider(new MockAlgorithmProvider());
        headerVerifier.verifyMessage(headers, METHOD, URI, new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test(expected = InvalidDataToVerifySignatureException.class)
    public void requiredHeaderPresentButNotSigned() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers);
        headers.put("Test", Collections.singletonList("value"));

        MessageVerifier headerVerifier = new MessageVerifier(keyId -> keyPair.getPublic(),
                                                             Collections.singletonList("test"));
        headerVerifier.setAddDefaultRequiredHeaders(false);
        headerVerifier.setSecurityProvider(new MockSecurityProvider());
        headerVerifier.setAlgorithmProvider(new MockAlgorithmProvider());
        headerVerifier.verifyMessage(headers, METHOD, URI, new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test(expected = InvalidDataToVerifySignatureException.class)
    public void requiredHeaderNotPresent() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers);

        MessageVerifier headerVerifier = new MessageVerifier(keyId -> keyPair.getPublic(),
                                                             Collections.singletonList("test"));
        headerVerifier.setAddDefaultRequiredHeaders(false);
        headerVerifier.setSecurityProvider(new MockSecurityProvider());
        headerVerifier.setAlgorithmProvider(new MockAlgorithmProvider());
        headerVerifier.verifyMessage(headers, METHOD, URI, new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test
    public void defaultRequiredHeaderPresent() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();
        headers.put("Test", Collections.singletonList("value"));
        headers.put(HTTPSignatureConstants.REQUEST_TARGET, Collections.singletonList("12345"));
        createAndAddSignature(headers);

        MessageVerifier headerVerifier = new MessageVerifier(keyId -> keyPair.getPublic(),
                                                             Collections.singletonList("test"));
        headerVerifier.setSecurityProvider(new MockSecurityProvider());
        headerVerifier.setAlgorithmProvider(new MockAlgorithmProvider());
        headerVerifier.verifyMessage(headers, METHOD, URI, new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test
    public void defaultRequiredHeaderPresentTestNotRequired() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();
        headers.put("Test", Collections.singletonList("value"));
        headers.put(HTTPSignatureConstants.REQUEST_TARGET, Collections.singletonList("12345"));
        createAndAddSignature(headers);

        MessageVerifier headerVerifier = new MessageVerifier(keyId -> keyPair.getPublic());
        headerVerifier.setSecurityProvider(new MockSecurityProvider());
        headerVerifier.setAlgorithmProvider(new MockAlgorithmProvider());
        headerVerifier.verifyMessage(headers, METHOD, URI, new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test
    public void nullBodyDelMethodShouldNotThrowInvalidDataToVerifySignatureException() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();
        headers.put("Test", Collections.singletonList("value"));
        headers.put(HTTPSignatureConstants.REQUEST_TARGET, Collections.singletonList("12345"));
        createAndAddSignatureDeleteMethod(headers);

        MessageVerifier headerVerifier = new MessageVerifier(keyId -> keyPair.getPublic());
        headerVerifier.setAddDefaultRequiredHeaders(true);
        headerVerifier.setSecurityProvider(new MockSecurityProvider());
        headerVerifier.setAlgorithmProvider(new MockAlgorithmProvider());
        headerVerifier.verifyMessage(headers, DELETE_METHOD, URI, new MessageImpl(), null);
    }

    @Test(expected = InvalidDataToVerifySignatureException.class)
    public void defaultRequiredHeaderNotPresent() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();
        headers.put("Test", Collections.singletonList("value"));
        createAndAddSignature(headers);

        MessageVerifier headerVerifier = new MessageVerifier(keyId -> keyPair.getPublic(),
                                                             Collections.singletonList("test"));
        headerVerifier.setSecurityProvider(new MockSecurityProvider());
        headerVerifier.setAlgorithmProvider(new MockAlgorithmProvider());
        headerVerifier.verifyMessage(headers, METHOD, URI, new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    @Test(expected = InvalidDataToVerifySignatureException.class)
    public void defaultRequiredHeaderNotPresentTestNotRequired() throws IOException {
        Map<String, List<String>> headers = createMockHeaders();
        headers.put("Test", Collections.singletonList("value"));
        createAndAddSignature(headers);

        MessageVerifier headerVerifier = new MessageVerifier(keyId -> keyPair.getPublic());
        headerVerifier.setSecurityProvider(new MockSecurityProvider());
        headerVerifier.setAlgorithmProvider(new MockAlgorithmProvider());
        headerVerifier.verifyMessage(headers, METHOD, URI, new MessageImpl(), MESSAGE_BODY.getBytes());
    }

    private static void createAndAddSignature(Map<String, List<String>> headers) throws IOException {
        messageSigner.sign(headers, URI, METHOD);
    }

    private static void createAndAddSignatureDeleteMethod(Map<String, List<String>> headers) throws IOException {
        messageSigner.sign(headers, URI, DELETE_METHOD);
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
