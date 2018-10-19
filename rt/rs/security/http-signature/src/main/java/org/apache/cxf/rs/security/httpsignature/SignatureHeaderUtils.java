package org.apache.cxf.rs.security.httpsignature;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

public final class SignatureHeaderUtils {
    /**
     * Maps a multimap to a normal map with comma-separated values in case of duplicate headers
     * @param multivaluedMap the multivalued map
     * @return A map with comma-separated values
     */
    public static Map<String, String> mapHeaders(Map<String, List<String>> multivaluedMap) {
        Map<String, String> mappedStrings = new HashMap<>();
        for (String key : multivaluedMap.keySet()) {
            mappedStrings.put(key, concatValues(multivaluedMap.get(key)));
        }
        return mappedStrings;
    }

    /**
     * Loads and decodes a PEM key to be ready to create a KeySpec
     * @param resource The raw bytes from a PEM file
     * @return decoded bytes which can be input to a KeySpec
     */
    public static byte[] loadPEM(byte[] resource) {
        String pem = new String(resource, ISO_8859_1);
        Pattern parse = Pattern.compile("(?m)(?s)^---*BEGIN.*---*$(.*)^---*END.*---*$.*");
        String encoded = parse.matcher(pem).replaceFirst("$1");
        return Base64.getMimeDecoder().decode(encoded);
    }

    /**
     * Get a base64 encoded digest using the Algorithm specified, typically SHA-256
     *
     * @param messageBody         The body of the message to be used to create the Digest
     * @param digestAlgorithmName The name of the algorithm used to create the digest, SHA-256 and SHA-512 are valid
     * @return A base64 encoded digest ready to be added as a header to the message
     * @throws NoSuchAlgorithmException If the user gives an unexpected digestAlgorithmName
     */
    public static  String createDigestHeader(String messageBody, String digestAlgorithmName) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = getDigestAlgorithm(digestAlgorithmName);
        messageDigest.update(messageBody.getBytes());
        return digestAlgorithmName + "=" + new String(Base64.getEncoder().encode(messageDigest.digest()));
    }

    /**
     * Get digest algorithm based on digestAlgorithmName
     *
     * @param digestAlgorithmName The name of the algorithm used to create the digest, SHA-256 and SHA-512 are valid
     * @return The digest algorithm
     * @throws NoSuchAlgorithmException If the user gives an unexpected digestAlgorithmName
     */
    public static MessageDigest getDigestAlgorithm(String digestAlgorithmName) throws NoSuchAlgorithmException {
        String temporaryString = digestAlgorithmName.toUpperCase();
        if (temporaryString.startsWith("SHA-256")) {
            return MessageDigest.getInstance("SHA-256");
        } else if (temporaryString.startsWith("SHA-512")) {
            return MessageDigest.getInstance("SHA-512");
        } else {
            throw new NoSuchAlgorithmException("Found no valid algorithm in Digest");
        }
    }

    public static String getMethod(Map<String, List<String>> responseHeaders) {
        if (responseHeaders.containsKey("(request-target)")) {
            return getRequestTargetSubString(responseHeaders.get("(request-target)").get(0), 0);
        }
        return "";
    }

    public static String getUri(Map<String, List<String>> responseHeaders) {
        if (responseHeaders.containsKey("(request-target)")) {
            return getRequestTargetSubString(responseHeaders.get("(request-target)").get(0), 1);
        }
        return "";
    }

    private static String concatValues(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < values.size(); x++) {
            sb.append(values.get(x));
            if (x != values.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private static String getRequestTargetSubString(String requestTarget, int index) {
        List<String> subStrings = Arrays.asList(requestTarget.split(" "));
        return subStrings.get(index);
    }
}
