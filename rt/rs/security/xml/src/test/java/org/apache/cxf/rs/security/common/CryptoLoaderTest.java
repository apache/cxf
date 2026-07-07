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

package org.apache.cxf.rs.security.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CryptoLoaderTest {

    @Test
    public void testDisallowedSchemeRejected() throws Exception {
        URL url = new URL("ftp://example.com/crypto.properties");

        try {
            CryptoLoader.loadCryptoFromURL(url);
            fail("Expected IOException for disallowed scheme");
        } catch (IOException ex) {
            assertTrue(ex.getMessage().contains("not permitted for URIResolver"));
        }
    }

    @Test
    public void testAllowedFileSchemeLoadsCrypto() throws Exception {
        File tmp = File.createTempFile("cxf-crypto", ".properties");
        tmp.deleteOnExit();

        String props = "org.apache.wss4j.crypto.provider=org.apache.wss4j.common.crypto.Merlin\n"
            + "org.apache.wss4j.crypto.merlin.keystore.type=jks\n";

        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(props.getBytes(StandardCharsets.UTF_8));
        }

        Crypto crypto = CryptoLoader.loadCryptoFromURL(tmp.toURI().toURL());
        assertNotNull(crypto);
    }

    @Test(expected = WSSecurityException.class)
    public void testAllowedFileSchemeWithBadPropertiesFailsInCryptoFactory() throws Exception {
        File tmp = File.createTempFile("cxf-crypto-invalid", ".properties");
        tmp.deleteOnExit();

        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write("org.apache.wss4j.crypto.provider=bad.Provider\n".getBytes(StandardCharsets.UTF_8));
        }

        CryptoLoader.loadCryptoFromURL(tmp.toURI().toURL());
    }
}
