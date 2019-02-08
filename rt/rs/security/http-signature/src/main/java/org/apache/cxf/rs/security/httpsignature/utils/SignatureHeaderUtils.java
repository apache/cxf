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
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.cxf.rs.security.httpsignature.exception.DigestFailureException;

public final class SignatureHeaderUtils {
    private SignatureHeaderUtils() { }

    /**
     * Add a date header at the current time using the ZoneOffset. Date format is http
     */
    public static void addDateHeader(Map<String, List<String>> messageHeaders, ZoneOffset zoneOffset) {
        String date = DateTimeFormatter.RFC_1123_DATE_TIME
                .format(LocalDateTime.now().atZone(Clock.system(zoneOffset).getZone()));
        messageHeaders.put("Date", Collections.singletonList(date));
    }

    /**
     * Maps a multimap to a normal map with comma-separated values in case of duplicate headers according to
     * the draft-cavage guidelines
     * @param multivaluedMap the multivalued map
     * @return A map with comma-separated values
     */
    public static Map<String, String> mapHeaders(Map<String, List<String>> multivaluedMap) {
        Map<String, String> mappedStrings = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : multivaluedMap.entrySet()) {
            mappedStrings.put(entry.getKey(), concatValues(entry.getValue()));
        }
        return mappedStrings;
    }

    /**
     * Get a base64 encoded digest using the Algorithm specified, typically SHA-256
     *
     * @param messageBody         The body of the message to be used to create the Digest
     * @param digestAlgorithmName The name of the algorithm used to create the digest, SHA-256 and SHA-512 are valid
     * @return A base64 encoded digest ready to be added as a header to the message
     */
    public static String createDigestHeader(String messageBody, String digestAlgorithmName) {
        MessageDigest messageDigest = createMessageDigestWithAlgorithm(digestAlgorithmName);
        messageDigest.update(messageBody.getBytes());
        return digestAlgorithmName + "=" + Base64.getEncoder().encodeToString(messageDigest.digest());
    }

    /**
     * Get a MessageDigest object based on the algorithm in the digest string
     *
     * @return a valid MessageDigest object
     */
    public static MessageDigest createMessageDigestWithAlgorithm(String algorithmName) {
        List<String> validDigestAlgorithms = Arrays.asList("SHA-256", "SHA-512");
        try {
            for (String validAlgorithm : validDigestAlgorithms) {
                if (validAlgorithm.equalsIgnoreCase(algorithmName)) {
                    return MessageDigest.getInstance(validAlgorithm);
                }
            }
            throw new NoSuchAlgorithmException("found no match in digest algorithm whitelist");
        } catch (NoSuchAlgorithmException e) {
            throw new DigestFailureException("failed to retrieve digest from digest string", e);
        }
    }

    public static void inspectMessageHeaders(Map<String, List<String>> messageHeaders) {
        Objects.requireNonNull(messageHeaders);

        if (messageHeaders.isEmpty()) {
            throw new IllegalStateException("message headers are empty");
        }
        messageHeaders.forEach((key, list) -> Objects.requireNonNull(list));
        messageHeaders.forEach((key, list) -> list.forEach(Objects::requireNonNull));
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
