package org.apache.cxf.rs.security.httpsignature;

import org.apache.cxf.rs.security.httpsignature.exception.DifferentAlgorithmsException;
import org.apache.cxf.rs.security.httpsignature.exception.DifferentDigestsException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidDataToVerifySignatureException;
import org.apache.cxf.rs.security.httpsignature.exception.MissingSignatureHeaderException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

/**
 * @author Fredrik Espedal
 */
public class MessageVerifierTest {

    private final static String path = "/evry/signature";
    private final static String method = "Get";
    private final static String messageBody = "Hello";
    private final static String digestAlgorithm = "SHA-256";
    private final static String signatureAlgorithm = "rsa-sha256";
    private final static String keyId = "testVerifier";
    private static PrivateKey privateKey;

    private static MessageVerifier messageVerifier;
    private static MessageSigner messageSigner;

    private static String privateKeyString = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDOq9Krkw8BfXWe\n" +
            "6HnNCqNJkENH0SsREIX4Cheze5EkKug0iVoC4Znexc5hkJUIaS3bxMxsfo+e3zP1\n" +
            "h+sfYWVJBafpQNr4OTsCMK00t44cicTu2Va5dm7jE9HZeCIp33STLXEhskvk/MCP\n" +
            "NguNOMRaLq6q817GL9rz8EXzkM/8slVrmlsLMPetKUgU2MsxyiGziqFZvLtkI9JS\n" +
            "MaHo8x+aFwFC2VJyurCFI8l4IhDZ4AxxlUMVG/4xU7MIgas1qff9fxYZAxYlBD4J\n" +
            "ePWaAfW3eZnLVrxYbPIGqFbNuWxqs7+1QSg6l3UNb2JfUm0m0tmZ82ZdHpHc1SiI\n" +
            "xpmgV42pAgMBAAECggEADxolMd50KUK2tp78T4xodDazg+r3/646a+6o3re32nG8\n" +
            "OCKVTkKYENxZgW2kRlIRRM7ztTXmXtCmplmBR/DBCv370CKqHZtpAXb3ITQMkW9L\n" +
            "0bxWBwVtgvBu1DpgHLk0dpDKhJDX1OrXU1+6pl4wkvp4TwursEXM9ShopCy/1hI8\n" +
            "Z7pwN7BxZYwozRdsKGbwVZc0ymjY2oZ7n2Gl0aGAUj5zHUoWASKHU1BUmKIMPL4x\n" +
            "ddrczcRB8bgzW7NyxoTBOzTcW6ve1TvWdll/L4iJm5/bYEOWhNBGWbhTF3TbRZJb\n" +
            "pT1OQudC7pj6aEeVJqa4mm0Gw6DWAJJW9YnGAqXGgQKBgQDnQ53LPZlrIn4uHoxW\n" +
            "gGvmRdOESin85JnNiN110HeOp4PThutqEo8+T+P5xPCHgG84KHFFrx/cr4e2M7Jt\n" +
            "WyB/U09/fItQefapRXUs3zLu3VGAl4jZhafVFq8OZkJ8FfdfzUE4dvNiFnJxovEW\n" +
            "arldGG7deVDcZ5+zyvvvxkf58QKBgQDkxs/t0zkWgv6UruxUM3l0i2hn+RZx+6VF\n" +
            "9NPQZ2t6gn2Ch2zL0YVjP7EJHQ4aLCQAmRffpK9v7Y5ZUK1TPN4NRipEGbPiDDMt\n" +
            "Q+Ihu2lYoYWlaRoEzL2RJpHNafrUMLn1H31RM/olkzDYga+umok/uAJOX8n8lfV+\n" +
            "lXJg6mBXOQKBgQDYMypNWuUWd0SnMP/Zzm0Q9a5sOjlOpxfyQkVnYuCiiJCBK9zh\n" +
            "aUBo7J8gXbDPvI49XfGnR7Ttx1uERohEG7Eh12y2rmQ/dAXY8Yo9zNv82wLayM+z\n" +
            "K3RfjblSKN92ycJd2bFjbDDUPk/3VHE2l8d69OCQRF4H7wgqOVWWLzQSwQKBgHhQ\n" +
            "XlWyudku9vf2rm7xyzQRhMz5YGZ9c0PBKAv397wsGBmnYv4lqEKz4kTqtNnq0NxH\n" +
            "pxiEoYb5pd0u4phd8GGGvv+ljMaap+dsReZ7i0GDYKfHCFnx2tgVMqSW0cT6AzH4\n" +
            "Z41nCmzsZcnXGi93MisCeKQDGFTwAHWb3tU9LYFxAoGBAII01I9L8ATVfeDRrGyk\n" +
            "aQTVZil9AkA45NvGc9RSk0gSn+z4gxzfGiF7BqWV7Hap9lHvNuiPtogIXG/on2I3\n" +
            "KmkLyXoj0H20pTkOj2AgpFr7KVQQO920jVV3DMbB9oPojQN/6jLkmgZPL+Xtrpwy\n" +
            "61Msz1IuZBY+VBoJ+pBKEzRe\n" +
            "-----END PRIVATE KEY-----\n";

    @BeforeClass
    public static void setup() {
        try {
            privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(SignatureHeaderUtils.loadPEM(privateKeyString.getBytes())));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        messageVerifier = new MessageVerifier(true);
        messageVerifier.setPublicKeyProvider(new MockPublicKeyProvider());
        messageVerifier.setExceptionHandler(new MockExceptionHandler());
        messageVerifier.setSecurityProvider(new MockSecurityProvider());
        messageVerifier.setAlgorithmProvider(new MockAlgorithmProvider());

        messageSigner = new MessageSigner(signatureAlgorithm, digestAlgorithm);
    }

    @Test
    public void validUnalteredRequest() {
        try {
            Map<String, List<String>> headers = createMockHeaders();
            createDigestHeader(messageBody, headers, digestAlgorithm);
            createAndAddSignature(headers);
            messageVerifier.verifyMessage(headers, messageBody);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }


    @Test(expected = MissingSignatureHeaderException.class)
    public void missingSignatureHeaderFails() {
        try {
            Map<String, List<String>> headers = createMockHeaders();
            createDigestHeader(messageBody, headers, digestAlgorithm);
            messageVerifier.verifyMessage(headers, messageBody);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = InvalidDataToVerifySignatureException.class)
    public void alteredSignatureFails() {
        try {
            Map<String, List<String>> headers = createMockHeaders();
            createDigestHeader(messageBody, headers, digestAlgorithm);
            createAndAddSignature(headers);
            String signature = headers.get("Signature").get(0);
            signature = signature.replaceFirst("signature=\"[\\w][\\w]", "signature=\"AA");
            headers.replace("Signature", Collections.singletonList(signature));
            messageVerifier.verifyMessage(headers, messageBody);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = DifferentAlgorithmsException.class)
    public void alteredAlgorithmFails() {
        try {
            Map<String, List<String>> headers = createMockHeaders();
            createDigestHeader(messageBody, headers, digestAlgorithm);
            createAndAddSignature(headers);
            String signature = headers.get("Signature").get(0);
            signature = signature.replaceFirst("algorithm=\"rsa-sha256", "algorithm=\"hmac-sha256");
            headers.replace("Signature", Collections.singletonList(signature));
            messageVerifier.verifyMessage(headers, messageBody);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Test(expected = DifferentDigestsException.class)
    public void alteredDigestFails() {
        try {
            Map<String, List<String>> headers = createMockHeaders();
            createDigestHeader(messageBody, headers, digestAlgorithm);
            createAndAddSignature(headers);
            String digest = "SHA-256=HEYHEYHEYHEY";
            headers.replace("Digest", Collections.singletonList(digest));
            messageVerifier.verifyMessage(headers, messageBody);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static void createAndAddSignature(Map<String, List<String>> headers) {
        try {
             messageSigner.sign(headers, messageBody, privateKey, keyId);
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, List<String>> createMockHeaders() {
        Map<String, List<String>> newHeader = new HashMap<>();
        newHeader.put("(request-target)", Collections.singletonList(method.toLowerCase() + " " + path));
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
