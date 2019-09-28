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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.rs.security.httpsignature.exception.DifferentDigestsException;
import org.apache.cxf.rs.security.httpsignature.exception.DigestFailureException;
import org.apache.cxf.rs.security.httpsignature.utils.DefaultSignatureConstants;
import org.apache.cxf.rs.security.httpsignature.utils.SignatureHeaderUtils;

import org.junit.BeforeClass;
import org.junit.Test;

public class DigestVerifierTest {
    private static final String MESSAGE_BODY = "Hello";

    private static DigestVerifier digestVerifier;

    @BeforeClass
    public static void setUp() {
        digestVerifier = new DigestVerifier();
    }

    @Test
    public void validUnalteredDigest() {
        Map<String, List<String>> headers = new HashMap<>();
        createDigestHeader(MESSAGE_BODY, headers, DefaultSignatureConstants.DIGEST_ALGORITHM);
        digestVerifier.inspectDigest(MESSAGE_BODY.getBytes(), headers);
    }

    @Test(expected = DifferentDigestsException.class)
    public void differentDigestsFails() {
        Map<String, List<String>> headers = new HashMap<>();
        createDigestHeader(MESSAGE_BODY, headers, DefaultSignatureConstants.DIGEST_ALGORITHM);
        String digest = "SHA-256=HEYHEYHEYHEY";
        headers.replace("Digest", Collections.singletonList(digest));
        digestVerifier.inspectDigest(MESSAGE_BODY.getBytes(), headers);
    }

    @Test(expected = DifferentDigestsException.class)
    public void digestFailureAlteredMessageFails() {
        Map<String, List<String>> headers = new HashMap<>();
        createDigestHeader(MESSAGE_BODY, headers, DefaultSignatureConstants.DIGEST_ALGORITHM);
        digestVerifier.inspectDigest("TEST".getBytes(), headers);
    }

    @Test(expected = DigestFailureException.class)
    public void digestFailureFails() {
        Map<String, List<String>> headers = new HashMap<>();
        createDigestHeader(MESSAGE_BODY, headers, DefaultSignatureConstants.DIGEST_ALGORITHM);
        String digest = "HELLO=HEYHEYHEYHEY";
        headers.replace("Digest", Collections.singletonList(digest));
        digestVerifier.inspectDigest(MESSAGE_BODY.getBytes(), headers);
    }

    @Test(expected = DigestFailureException.class)
    public void digestFailureAlteredDigestStringFails() {
        Map<String, List<String>> headers = new HashMap<>();
        createDigestHeader(MESSAGE_BODY, headers, DefaultSignatureConstants.DIGEST_ALGORITHM);
        String digest = "BOERFKSEFK=VSJEFKSE=SRJSPAWKD";
        headers.replace("Digest", Collections.singletonList(digest));
        digestVerifier.inspectDigest(MESSAGE_BODY.getBytes(), headers);
    }

    @Test(expected = DigestFailureException.class)
    public void digestFailureEmptyDigestStringFails() {
        Map<String, List<String>> headers = new HashMap<>();
        createDigestHeader(MESSAGE_BODY, headers, DefaultSignatureConstants.DIGEST_ALGORITHM);
        String digest = "";
        headers.replace("Digest", Collections.singletonList(digest));
        digestVerifier.inspectDigest(MESSAGE_BODY.getBytes(), headers);
    }

    private static void createDigestHeader(String messageBody, Map<String, List<String>> headers,
                                           String digestAlgorithm) {
        String digest = SignatureHeaderUtils.createDigestHeader(messageBody, digestAlgorithm);
        headers.put("Digest", Collections.singletonList(digest));
    }

}
