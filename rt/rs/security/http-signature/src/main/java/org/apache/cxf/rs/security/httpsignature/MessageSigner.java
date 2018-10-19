package org.apache.cxf.rs.security.httpsignature;

import org.tomitribe.auth.signatures.Signature;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MessageSigner {
    private final String digestAlgorithmName;
    private final String signatureAlgorithmName;

    /**
     * Message signer using standard digest and signing algorithm
     */
    public MessageSigner() {
        this("rsa-sha256", "SHA-256");
    }

    public MessageSigner(String signatureAlgorithmName, String digestAlgorithmName) {
        this.digestAlgorithmName = digestAlgorithmName;
        this.signatureAlgorithmName = signatureAlgorithmName;
    }

    public void sign(Map<String, List<String>> messageHeaders,
                     String messageBody,
                     PrivateKey privateKey,
                     String keyId) throws NoSuchAlgorithmException, IOException {
        if (messageBody != null) {
            messageHeaders.put("Digest",
                    Collections.singletonList(SignatureHeaderUtils
                            .createDigestHeader(messageBody, digestAlgorithmName)));
        }

        String method = SignatureHeaderUtils.getMethod(messageHeaders);
        String uri = SignatureHeaderUtils.getUri(messageHeaders);

        messageHeaders.put("Signature", Collections.singletonList(createSignature(messageHeaders,
                privateKey,
                keyId,
                uri,
                method
        )));
    }

    private String createSignature(Map<String, List<String>> messageHeaders,
                                  PrivateKey privateKey,
                                  String keyId,
                                  String uri,
                                  String method) throws IOException {
        if (messageHeaders == null) {
            throw new IllegalArgumentException("Message headers cannot be null.");
        }

        String[] headerKeysForSignature =
                messageHeaders.keySet().stream().map(String::toLowerCase).toArray(String[]::new);

        if (headerKeysForSignature.length == 0) {
            throw new IllegalArgumentException("There has to be a value in the list of header keys for signature");
        }

        if (keyId == null) {
            throw new IllegalArgumentException("Key id cannot be null.");
        }

        final Signature signature = new Signature(keyId, signatureAlgorithmName, null, headerKeysForSignature);
        final org.tomitribe.auth.signatures.Signer signer =
                new org.tomitribe.auth.signatures.Signer(privateKey, signature);
        return signer.sign(method, uri, SignatureHeaderUtils.mapHeaders(messageHeaders)).toString();
    }
}
