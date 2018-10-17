package org.apache.cxf.rs.security.httpsignature;

import java.io.FileInputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

public class MockPublicKeyProvider implements PublicKeyProvider{
    @Override
    public PublicKey getKey(String keyId) {
        return getPublicKey(keyId);
    }

    private PublicKey getPublicKey(String keyId) {
        String publicKey = "-----BEGIN PUBLIC KEY-----\n" +
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzqvSq5MPAX11nuh5zQqj\n" +
                "SZBDR9ErERCF+AoXs3uRJCroNIlaAuGZ3sXOYZCVCGkt28TMbH6Pnt8z9YfrH2Fl\n" +
                "SQWn6UDa+Dk7AjCtNLeOHInE7tlWuXZu4xPR2XgiKd90ky1xIbJL5PzAjzYLjTjE\n" +
                "Wi6uqvNexi/a8/BF85DP/LJVa5pbCzD3rSlIFNjLMcohs4qhWby7ZCPSUjGh6PMf\n" +
                "mhcBQtlScrqwhSPJeCIQ2eAMcZVDFRv+MVOzCIGrNan3/X8WGQMWJQQ+CXj1mgH1\n" +
                "t3mZy1a8WGzyBqhWzblsarO/tUEoOpd1DW9iX1JtJtLZmfNmXR6R3NUoiMaZoFeN\n" +
                "qQIDAQAB\n" +
                "-----END PUBLIC KEY-----\n";

        try {
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(SignatureHeaderUtils.loadPEM(publicKey.getBytes())));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return null;
    }
}
