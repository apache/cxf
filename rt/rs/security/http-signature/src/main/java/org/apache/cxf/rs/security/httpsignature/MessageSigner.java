package org.apache.cxf.rs.security.httpsignature;

import org.apache.cxf.common.logging.LogUtils;
import org.tomitribe.auth.signatures.Signature;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class MessageSigner {

    protected static final Logger LOG = LogUtils.getL7dLogger(MessageSigner.class);

    private final String signatureAlgorithmName;
    private final String digestAlgorithmName;

    /**
     * Message signer using standard digest and signing algorithm
     */
    public MessageSigner() {
        this("rsa-sha256", "SHA-256");
    }

    public MessageSigner(String signatureAlgorithmName, String digestAlgorithmName) {
        this.signatureAlgorithmName = signatureAlgorithmName;
        this.digestAlgorithmName = digestAlgorithmName;
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

        LOG.info("MessageSigner: method: " + method + " uri: " + uri + " (request-target): " + messageHeaders.get("(request-target)").get(0));
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
