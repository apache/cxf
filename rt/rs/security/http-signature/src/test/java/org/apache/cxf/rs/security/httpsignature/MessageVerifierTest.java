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
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

import org.apache.cxf.rs.security.httpsignature.exception.DifferentAlgorithmsException;
import org.apache.cxf.rs.security.httpsignature.exception.DifferentDigestsException;
import org.apache.cxf.rs.security.httpsignature.exception.DigestFailureException;
import org.apache.cxf.rs.security.httpsignature.exception.FailedToVerifySignatureException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidDataToVerifySignatureException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.exception.MissingDigestException;
import org.apache.cxf.rs.security.httpsignature.exception.MissingSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.exception.MultipleSignatureHeaderException;

import org.junit.BeforeClass;
import org.junit.Test;

public class MessageVerifierTest {
    private static final String KEY_ID = "testVerifier";
    private static final String MESSAGE_BODY = "Hello";
    private static final String METHOD = "Get";
    private static final String URI = "/test/signature";
    private static PrivateKey privateKey;

    private static MessageSigner messageSigner;
    private static MessageVerifier messageVerifier;

    private static String privateKeyString = "-----BEGIN PRIVATE KEY-----\n"
            + "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDOq9Krkw8BfXWe\n"
            + "6HnNCqNJkENH0SsREIX4Cheze5EkKug0iVoC4Znexc5hkJUIaS3bxMxsfo+e3zP1\n"
            + "h+sfYWVJBafpQNr4OTsCMK00t44cicTu2Va5dm7jE9HZeCIp33STLXEhskvk/MCP\n"
            + "NguNOMRaLq6q817GL9rz8EXzkM/8slVrmlsLMPetKUgU2MsxyiGziqFZvLtkI9JS\n"
            + "MaHo8x+aFwFC2VJyurCFI8l4IhDZ4AxxlUMVG/4xU7MIgas1qff9fxYZAxYlBD4J\n"
            + "ePWaAfW3eZnLVrxYbPIGqFbNuWxqs7+1QSg6l3UNb2JfUm0m0tmZ82ZdHpHc1SiI\n"
            + "xpmgV42pAgMBAAECggEADxolMd50KUK2tp78T4xodDazg+r3/646a+6o3re32nG8\n"
            + "OCKVTkKYENxZgW2kRlIRRM7ztTXmXtCmplmBR/DBCv370CKqHZtpAXb3ITQMkW9L\n"
            + "0bxWBwVtgvBu1DpgHLk0dpDKhJDX1OrXU1+6pl4wkvp4TwursEXM9ShopCy/1hI8\n"
            + "Z7pwN7BxZYwozRdsKGbwVZc0ymjY2oZ7n2Gl0aGAUj5zHUoWASKHU1BUmKIMPL4x\n"
            + "ddrczcRB8bgzW7NyxoTBOzTcW6ve1TvWdll/L4iJm5/bYEOWhNBGWbhTF3TbRZJb\n"
            + "pT1OQudC7pj6aEeVJqa4mm0Gw6DWAJJW9YnGAqXGgQKBgQDnQ53LPZlrIn4uHoxW\n"
            + "gGvmRdOESin85JnNiN110HeOp4PThutqEo8+T+P5xPCHgG84KHFFrx/cr4e2M7Jt\n"
            + "WyB/U09/fItQefapRXUs3zLu3VGAl4jZhafVFq8OZkJ8FfdfzUE4dvNiFnJxovEW\n"
            + "arldGG7deVDcZ5+zyvvvxkf58QKBgQDkxs/t0zkWgv6UruxUM3l0i2hn+RZx+6VF\n"
            + "9NPQZ2t6gn2Ch2zL0YVjP7EJHQ4aLCQAmRffpK9v7Y5ZUK1TPN4NRipEGbPiDDMt\n"
            + "Q+Ihu2lYoYWlaRoEzL2RJpHNafrUMLn1H31RM/olkzDYga+umok/uAJOX8n8lfV+\n"
            + "lXJg6mBXOQKBgQDYMypNWuUWd0SnMP/Zzm0Q9a5sOjlOpxfyQkVnYuCiiJCBK9zh\n"
            + "aUBo7J8gXbDPvI49XfGnR7Ttx1uERohEG7Eh12y2rmQ/dAXY8Yo9zNv82wLayM+z\n"
            + "K3RfjblSKN92ycJd2bFjbDDUPk/3VHE2l8d69OCQRF4H7wgqOVWWLzQSwQKBgHhQ\n"
            + "XlWyudku9vf2rm7xyzQRhMz5YGZ9c0PBKAv397wsGBmnYv4lqEKz4kTqtNnq0NxH\n"
            + "pxiEoYb5pd0u4phd8GGGvv+ljMaap+dsReZ7i0GDYKfHCFnx2tgVMqSW0cT6AzH4\n"
            + "Z41nCmzsZcnXGi93MisCeKQDGFTwAHWb3tU9LYFxAoGBAII01I9L8ATVfeDRrGyk\n"
            + "aQTVZil9AkA45NvGc9RSk0gSn+z4gxzfGiF7BqWV7Hap9lHvNuiPtogIXG/on2I3\n"
            + "KmkLyXoj0H20pTkOj2AgpFr7KVQQO920jVV3DMbB9oPojQN/6jLkmgZPL+Xtrpwy\n"
            + "61Msz1IuZBY+VBoJ+pBKEzRe\n"
            + "-----END PRIVATE KEY-----\n";

    @BeforeClass
    public static void setup() {
        try {
            privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(
                            SignatureHeaderUtils.loadPEM(privateKeyString.getBytes())));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        messageVerifier = new MessageVerifier(true);
        messageVerifier.setPublicKeyProvider(new MockPublicKeyProvider());
        messageVerifier.setExceptionHandler(new MockExceptionHandler());
        messageVerifier.setSecurityProvider(new MockSecurityProvider());
        messageVerifier.setAlgorithmProvider(new MockAlgorithmProvider());

        messageSigner = new MessageSigner(DefaultSignatureConstants.SIGNING_ALGORITHM,
                DefaultSignatureConstants.DIGEST_ALGORITHM);
    }

    @Test
    public void validUnalteredRequest() {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers, MESSAGE_BODY);
        messageVerifier.verifyMessage(headers, MESSAGE_BODY);
    }


    @Test(expected = DifferentAlgorithmsException.class)
    public void differentAlgorithmsFails() {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers, MESSAGE_BODY);
        String signature = headers.get("Signature").get(0);
        signature = signature.replaceFirst("algorithm=\"rsa-sha256", "algorithm=\"hmac-sha256");
        headers.replace("Signature", Collections.singletonList(signature));
        messageVerifier.verifyMessage(headers, MESSAGE_BODY);
    }

    @Test(expected = DifferentDigestsException.class)
    public void differentDigestsFails() {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers, MESSAGE_BODY);
        String digest = "SHA-256=HEYHEYHEYHEY";
        headers.replace("Digest", Collections.singletonList(digest));
        messageVerifier.verifyMessage(headers, MESSAGE_BODY);
    }

    @Test(expected = DigestFailureException.class)
    public void digestFailureFails() {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers, MESSAGE_BODY);
        String digest = "HELLO=HEYHEYHEYHEY";
        headers.replace("Digest", Collections.singletonList(digest));
        messageVerifier.verifyMessage(headers, MESSAGE_BODY);
    }

    @Test(expected = FailedToVerifySignatureException.class)
    public void failedToVerifySignatureFails() {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers, MESSAGE_BODY);
        String signature = headers.get("Signature").get(0);
        signature = signature.replaceFirst("signature=\"[\\w][\\w]", "signature=\"AA");
        headers.replace("Signature", Collections.singletonList(signature));
        messageVerifier.verifyMessage(headers, MESSAGE_BODY);
    }

    @Test(expected = InvalidDataToVerifySignatureException.class)
    public void invalidDataToVerifySignatureFails() {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers, MESSAGE_BODY);
        headers.remove("Content-Length");
        messageVerifier.verifyMessage(headers, MESSAGE_BODY);
    }

    @Test(expected = InvalidSignatureHeaderException.class)
    public void invalidSignatureHeaderFails() {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers, MESSAGE_BODY);
        String signature = headers.get("Signature").get(0);
        signature = signature.replaceFirst(",signature", "signature");
        headers.replace("Signature", Collections.singletonList(signature));
        messageVerifier.verifyMessage(headers, MESSAGE_BODY);
    }

    @Test(expected = MissingDigestException.class)
    public void missingDigestFails() {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers, null);
        messageVerifier.verifyMessage(headers, MESSAGE_BODY);
    }

    @Test(expected = MissingSignatureHeaderException.class)
    public void missingSignatureHeaderFails() {
        try {
            Map<String, List<String>> headers = createMockHeaders();
            createDigestHeader(MESSAGE_BODY, headers, DefaultSignatureConstants.DIGEST_ALGORITHM);
            messageVerifier.verifyMessage(headers, MESSAGE_BODY);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = MultipleSignatureHeaderException.class)
    public void multipleSignatureHeaderFails() {
        Map<String, List<String>> headers = createMockHeaders();
        createAndAddSignature(headers, MESSAGE_BODY);
        String signature = headers.get("Signature").get(0);
        List<String> signatureList = new ArrayList<>(headers.get("Signature"));
        signatureList.add(signature);
        headers.put("Signature", signatureList);
        messageVerifier.verifyMessage(headers, MESSAGE_BODY);
    }

    private static void createAndAddSignature(Map<String, List<String>> headers, String messageBody) {
        try {
            messageSigner.sign(headers, messageBody, privateKey, KEY_ID);
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, List<String>> createMockHeaders() {
        Map<String, List<String>> newHeader = new HashMap<>();
        newHeader.put("(request-target)", Collections.singletonList(METHOD.toLowerCase() + " " + URI));
        newHeader.put("Host", Collections.singletonList("example.org"));
        newHeader.put("Date", Collections.singletonList("Tue, 07 Jun 2014 20:51:35 GMT"));
        newHeader.put("Accept", Collections.singletonList(""));
        newHeader.put("Content-Length", Collections.singletonList("18"));
        return newHeader;
    }

    private static void createDigestHeader(String messageBody, Map<String, List<String>> headers,
                                           String digestAlgorithm) throws NoSuchAlgorithmException {
        String digest = SignatureHeaderUtils.createDigestHeader(messageBody, digestAlgorithm);
        headers.put("Digest", Collections.singletonList(digest));
    }
}
