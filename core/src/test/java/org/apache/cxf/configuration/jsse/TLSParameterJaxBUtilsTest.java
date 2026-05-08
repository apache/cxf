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
package org.apache.cxf.configuration.jsse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import org.apache.cxf.configuration.security.KeyStoreType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Unit tests for TLSParameterJaxBUtils keystore loading.
 *
 * <p>CXF-SSRF-001 — {@link TLSParameterJaxBUtils#getKeyStore(KeyStoreType, boolean)} passes the
 * configured {@code url} attribute directly to {@code new URL(kst.getUrl()).openStream()} without
 * validating the scheme.  This allows any protocol understood by the JDK URL handler (including
 * {@code ftp:}, {@code jar:}, {@code jndi:}, …) to be used as the keystore source, creating a
 * Server-Side Request Forgery (SSRF) surface.</p>
 */
public class TLSParameterJaxBUtilsTest {

    @Test
    public void testFileBasedJksKeystoreIsLoaded() throws Exception {
        final String password = "changeit";
        final Path tempJks = Files.createTempFile("cxf-filestore-", ".jks");

        try {
            KeyStore source = KeyStore.getInstance("JKS");
            source.load(null, password.toCharArray());
            try (OutputStream os = Files.newOutputStream(tempJks)) {
                source.store(os, password.toCharArray());
            }

            KeyStoreType kst = new KeyStoreType();
            kst.setType("JKS");
            kst.setPassword(password);
            kst.setFile(tempJks.toString());

            KeyStore loaded = TLSParameterJaxBUtils.getKeyStore(kst, false);
            assertNotNull(loaded);
            assertEquals(0, loaded.size());
        } finally {
            Files.deleteIfExists(tempJks);
        }
    }

    /**
     * Verifies that {@code getKeyStore()} rejects an {@code ftp://} URL with an
     * {@link IllegalArgumentException}.  Prior to the fix, the URL was passed directly
     * to {@code new URL(…).openStream()} without scheme validation, allowing any JDK-supported
     * protocol (ftp, jar, jndi, …) to be used as a keystore source (SSRF).
     */
    @Test
    public void testFtpUrlAllowedErroneouslyInKeystoreUrl() throws Exception {
        KeyStoreType kst = new KeyStoreType();
        kst.setUrl("ftp://127.0.0.1:12345/keystore.jks");

        try {
            TLSParameterJaxBUtils.getKeyStore(kst, false);
            fail("Expected IllegalArgumentException for ftp:// keystore URL — scheme not in allowlist");
        } catch (IllegalArgumentException e) {
            // expected — ftp:// is correctly rejected
        } catch (IOException e) {
            fail("Expected IllegalArgumentException but got IOException, "
                    + "meaning ftp:// was accepted and an outbound connection was attempted: " + e);
        }
    }

}
