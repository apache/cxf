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

import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.rs.security.httpsignature.exception.DifferentDigestsException;
import org.apache.cxf.rs.security.httpsignature.exception.DigestFailureException;
import org.apache.cxf.rs.security.httpsignature.exception.MissingDigestException;
import org.apache.cxf.rs.security.httpsignature.utils.SignatureHeaderUtils;

public class DigestVerifier {
    private static final Logger LOG = LogUtils.getL7dLogger(DigestVerifier.class);

    public void inspectDigest(byte[] messageBody, Map<String, List<String>> responseHeaders) {
        LOG.fine("Starting digest verification");
        if (responseHeaders.containsKey("Digest")) {

            MessageDigest messageDigest = SignatureHeaderUtils
                    .createMessageDigestWithAlgorithm(splitDigestHeader(responseHeaders.get("Digest").get(0)).get(0));
            messageDigest.update(messageBody);
            byte[] generatedDigest = messageDigest.digest();
            byte[] headerDigest = Base64.getDecoder()
                    .decode(splitDigestHeader(responseHeaders.get("Digest").get(0)).get(1));

            if (!Arrays.equals(generatedDigest, headerDigest)) {
                throw new DifferentDigestsException("the digest does not match the body of the message");
            }
        } else {
            throw new MissingDigestException("found no digest header");
        }
        LOG.fine("Finished digest verification");
    }

    private List<String> splitDigestHeader(String digestHeader) {
        List<String> items = Arrays.asList(digestHeader.split("=", 2));
        if (items.size() != 2) {
            throw new DigestFailureException("invalid digest header format");
        }
        return items;
    }

}
