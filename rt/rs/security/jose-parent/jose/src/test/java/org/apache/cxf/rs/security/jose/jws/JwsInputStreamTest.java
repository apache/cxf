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
package org.apache.cxf.rs.security.jose.jws;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;

import org.junit.Test;

import static org.junit.Assert.fail;

public class JwsInputStreamTest {

    private static final byte[] KEY = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CONTENT = "signed multipart content".getBytes(StandardCharsets.UTF_8);

    @Test
    public void testSingleByteReadVerifiesValidSignature() throws Exception {
        byte[] signatureBytes = sign(CONTENT);

        JwsVerificationSignature vSig = createVerificationSignature();
        try (JwsInputStream jwsStream =
            new JwsInputStream(new ByteArrayInputStream(CONTENT), vSig, signatureBytes, true)) {
            // Reading one byte at a time must feed the exact content bytes into the
            // signature and thus accept a valid signature at the end of the stream
            while (jwsStream.read() != -1) {
                // consume
            }
        }
    }

    @Test
    public void testTamperedContentIsRejected() throws Exception {
        byte[] signatureBytes = sign(CONTENT);

        byte[] tampered = CONTENT.clone();
        tampered[0] ^= 0x01;
        JwsVerificationSignature vSig = createVerificationSignature();
        try (JwsInputStream jwsStream =
            new JwsInputStream(new ByteArrayInputStream(tampered), vSig, signatureBytes, true)) {
            byte[] buf = new byte[8];
            try {
                while (jwsStream.read(buf, 0, buf.length) != -1) {
                    // consume
                }
                fail("Failure expected on tampered content");
            } catch (JwsException ex) {
                // expected
            }
        }
    }

    private byte[] sign(byte[] content) {
        HmacJwsSignatureProvider provider = new HmacJwsSignatureProvider(KEY, SignatureAlgorithm.HS256);
        JwsSignature sig = provider.createJwsSignature(new JwsHeaders(SignatureAlgorithm.HS256));
        sig.update(content, 0, content.length);
        return sig.sign();
    }

    private JwsVerificationSignature createVerificationSignature() {
        HmacJwsSignatureVerifier verifier = new HmacJwsSignatureVerifier(KEY, SignatureAlgorithm.HS256);
        return verifier.createJwsVerificationSignature(new JwsHeaders(SignatureAlgorithm.HS256));
    }
}
