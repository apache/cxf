package org.apache.cxf.rs.security.httpsignature;

import org.apache.cxf.common.logging.LogUtils;
import org.tomitribe.auth.signatures.Signature;
import org.tomitribe.auth.signatures.Verifier;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.*;
import java.util.logging.Logger;

public class MessageVerifier {
    private ExceptionHandler exceptionHandler;

    protected static final Logger LOG = LogUtils.getL7dLogger(MessageVerifier.class);

    public MessageVerifier(boolean verifyMessageBody) {
        setExceptionHandler(null);
        setSecurityProvider(null);
        setAlgorithmProvider(null);
        this.verifyMessageBody = verifyMessageBody;
    }

    public MessageVerifier(PublicKeyProvider publicKeyProvider,
                           ExceptionHandler exceptionHandler,
                           SecurityProvider securityProvider,
                           AlgorithmProvider algorithmProvider,
                           boolean verifyMessageBody)
    {
        Objects.requireNonNull(publicKeyProvider, "Public key provider cannot be null");
        this.publicKeyProvider = publicKeyProvider;
        this.exceptionHandler = exceptionHandler;
        this.securityProvider = securityProvider;
        this.algorithmProvider = algorithmProvider;
        this.verifyMessageBody = verifyMessageBody;
    }

    private PublicKeyProvider publicKeyProvider;

    private SecurityProvider securityProvider;

    private AlgorithmProvider algorithmProvider;

    private boolean verifyMessageBody;

    public void setSecurityProvider(SecurityProvider securityProvider) {
        if (securityProvider != null) {
            this.securityProvider = securityProvider;
        } else {
            this.securityProvider = (keyId) -> Security.getProvider("SunRsaSign");
        }
    }

    public void setPublicKeyProvider(PublicKeyProvider publicKeyProvider) {
        this.publicKeyProvider = publicKeyProvider;
    }

    public void setAlgorithmProvider(AlgorithmProvider algorithmProvider) {
        if (algorithmProvider != null) {
            this.algorithmProvider = algorithmProvider;
        } else {
            this.algorithmProvider = (keyId) -> "rsa-sha256";
        }
    }

    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        if (exceptionHandler != null) {
            this.exceptionHandler = exceptionHandler;
        } else {
            this.exceptionHandler = (exception, type) -> new SignatureException("exception of type: " + type + " occurred");
        }
    }

    public void verifyMessage(Map<String, List<String>> messageHeaders, String messageBody) {
        if (verifyMessageBody) {
            inspectDigest(messageBody, messageHeaders);
        }
        verifyMessage(messageHeaders);
    }

    public void verifyMessage(Map<String, List<String>> messageHeaders) {
        inspectIllegalState();

        inspectMissingSignatureHeader(messageHeaders);

        inspectMultipleSignatureHeaders(messageHeaders);

        Signature signature = extractSignature(messageHeaders.get("Signature").get(0));

        String providedAlgorithm = algorithmProvider.getAlgorithmName(signature.getKeyId());
        Objects.requireNonNull(providedAlgorithm, "provided algorithm is null");

        String signatureAlgorithm = signature.getAlgorithm().toString();
        if (!providedAlgorithm.equals(signatureAlgorithm)) {
            throw exceptionHandler.handle(new SignatureException("algorithm from header and provided are different"),
                    SignatureExceptionType.DIFFERENT_ALGORITHMS);
        }

        // Replace the algorithm provided by the headers with the algorithm given by the algorithm provider
        Signature newSignature =
                Signature.fromString(replaceAlgorithm(signature.toString(), signatureAlgorithm, providedAlgorithm));

        Key key = publicKeyProvider.getKey(signature.getKeyId());
        Objects.requireNonNull(key, "provided public key is null");

        runVerifier(messageHeaders, key, newSignature);
    }

    private String replaceAlgorithm(String signatureString, String oldAlgorithm, String newAlgorithm) {
        return signatureString.replaceFirst("algorithm=\"" + oldAlgorithm, "algorithm=\"" + newAlgorithm);
    }

    private void inspectIllegalState() {
        if (publicKeyProvider == null) {
            throw new IllegalStateException("public key provider is not set");
        }
        if (securityProvider == null) {
            throw new IllegalStateException("security provider is not set");
        }
        if (algorithmProvider == null) {
            throw new IllegalStateException("algorithm provider is not set");
        }
    }

    private void inspectDigest(String messageBody, Map<String, List<String>> responseHeaders) {
        LOG.info("Starting digest verification");
        if (responseHeaders.containsKey("Digest")) {
            String headerDigest = responseHeaders.get("Digest").get(0);

            MessageDigest messageDigest = getDigestAlgorithm(headerDigest);
            messageDigest.update(messageBody.getBytes(StandardCharsets.UTF_8));
            String generatedDigest = new String(messageDigest.digest());

            headerDigest = new String(Base64.getDecoder().decode(trimAlgorithmName(headerDigest)));

            if (!generatedDigest.equals(headerDigest)) {
                throw exceptionHandler.handle(new SignatureException("the digest does not match the body of the message"),
                        SignatureExceptionType.DIFFERENT_DIGESTS);
            }
        } else {
            throw exceptionHandler.handle(new SignatureException("failed to validate the digest"),
                    SignatureExceptionType.MISSING_DIGEST);
        }
        LOG.info("Finished digest verification");
    }

    private MessageDigest getDigestAlgorithm(String digestString) {
        try {
            return SignatureHeaderUtils.getDigestAlgorithm(digestString);
        } catch (NoSuchAlgorithmException e) {
            throw exceptionHandler.handle(new SignatureException("failed to validate the digest"),
                    SignatureExceptionType.DIGEST_FAILURE);
        }
    }

    private String trimAlgorithmName(String digest) {
        int startingIndex = digest.indexOf("=");
        return digest.substring(startingIndex + 1, digest.length());
    }

    private void inspectMultipleSignatureHeaders(Map<String, List<String>> responseHeaders) {
        if (responseHeaders.get("Signature").size() > 1) {
            throw exceptionHandler.handle(new SignatureException("there are multiple signature headers in request"),
                    SignatureExceptionType.MULTIPLE_SIGNATURE_HEADERS);
        }
    }

    private void inspectMissingSignatureHeader(Map<String, List<String>> responseHeaders) {
        if (!responseHeaders.containsKey("Signature")) {
            throw exceptionHandler.handle(new SignatureException("there is no signature header in request"),
                    SignatureExceptionType.MISSING_SIGNATURE_HEADER);
        }
    }

    private Signature extractSignature(String signatureString) {
        try {
            return Signature.fromString(signatureString);
        } catch (Exception e) {
            throw exceptionHandler.handle(new SignatureException("failed to parse signature from signature header"),
                    SignatureExceptionType.INVALID_SIGNATURE_HEADER);
        }
    }

    private void runVerifier(Map<String, List<String>> messageHeaders, Key key, Signature signature) {
        java.security.Provider provider = securityProvider.getProvider(signature.getKeyId());
        Objects.requireNonNull(provider, "provided provider is null");

        String method = SignatureHeaderUtils.getMethod(messageHeaders);
        String uri = SignatureHeaderUtils.getUri(messageHeaders);

        Verifier verifier = new Verifier(key, signature, provider);
        LOG.info("Starting signature verification");
        try {
            boolean success = verifier.verify(method, uri,
                    SignatureHeaderUtils.mapHeaders(messageHeaders));
            if (!success) {
                throw exceptionHandler.handle(new SignatureException("signature is not valid"),
                        SignatureExceptionType.FAILED_TO_VERIFY_SIGNATURE);
            }
        } catch (Exception e) {
            throw exceptionHandler.handle(new SignatureException(e.getMessage()),
                    SignatureExceptionType.INVALID_DATA_TO_VERIFY_SIGNATURE);
        }
    }
}
