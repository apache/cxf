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
package org.apache.cxf.rt.security.crypto;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import javax.crypto.SecretKey;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertArrayEquals;

@RunWith(Parameterized.class)
public class CryptoUtilsTest {
    private final boolean compression;

    public CryptoUtilsTest(boolean compression) {
        this.compression = compression;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> compression() {
        return Arrays.asList(new Object[][] {{true}, {false}});
    }

    @Test
    public void testCompression() {
        byte[] bytes = "testString".getBytes(StandardCharsets.UTF_8);
        KeyProperties keyProps = new KeyProperties("AES");
        keyProps.setCompressionSupported(compression);
        SecretKey secretKey = CryptoUtils.getSecretKey(keyProps);
        byte[] encryptedBytes = CryptoUtils.encryptBytes(bytes, secretKey, keyProps);
        byte[] decryptedBytes = CryptoUtils.decryptBytes(encryptedBytes, secretKey, keyProps);
        assertArrayEquals(bytes, decryptedBytes);
    }
}
