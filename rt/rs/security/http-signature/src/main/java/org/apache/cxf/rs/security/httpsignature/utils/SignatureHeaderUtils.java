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
package org.apache.cxf.rs.security.httpsignature.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class SignatureHeaderUtils {

    private SignatureHeaderUtils() { }
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
     * Get a base64 encoded digest using the Algorithm specified, typically SHA-256
     *
     * @param messageBody         The body of the message to be used to create the Digest
     * @param digestAlgorithmName The name of the algorithm used to create the digest, SHA-256 and SHA-512 are valid
     * @return A base64 encoded digest ready to be added as a header to the message
     * @throws NoSuchAlgorithmException If the user gives an unexpected digestAlgorithmName
     */
    public static String createDigestHeader(String messageBody, String digestAlgorithmName)
            throws NoSuchAlgorithmException {
        MessageDigest messageDigest = getDigestAlgorithm(digestAlgorithmName);
        messageDigest.update(messageBody.getBytes());
        return digestAlgorithmName + "=" + Base64.getEncoder().encodeToString(messageDigest.digest());
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
}
