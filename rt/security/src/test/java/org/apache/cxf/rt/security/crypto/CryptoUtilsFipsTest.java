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

import java.net.URL;
import java.net.URLClassLoader;
import java.security.Provider;
import java.security.Security;

import org.apache.cxf.helpers.JavaUtils;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class CryptoUtilsFipsTest {
    @Test
    public void testFipsWithoutExplicitProvider() throws Exception {
        assertNoBouncyCastleProvider(null, null);
    }

    @Test
    public void testFipsWithUnavailableProvider() throws Exception {
        assertNull(Security.getProvider("UnavailableFIPS"));
        assertNoBouncyCastleProvider("UnavailableFIPS",
                                    "The security provider UnavailableFIPS selected via fips.security.provider"
                                    + " system property is not available in java.security.Security.");
    }

    @Test
    public void testFipsWithAlternativeProvider() throws Exception {
        Provider provider = new Provider("AlternativeFIPS", "1.0", "Test FIPS provider") {
            private static final long serialVersionUID = 1L;
        };
        assertTrue(Security.addProvider(provider) > 0);
        try {
            assertNoBouncyCastleProvider(provider.getName(), null);
        } finally {
            Security.removeProvider(provider.getName());
        }
    }

    private void assertNoBouncyCastleProvider(String provider, String expectedError) throws Exception {
        String originalFipsEnabled = System.getProperty("fips.enabled");
        String originalFipsProvider = System.getProperty("fips.security.provider");
        Provider[] originalProviders = Security.getProviders();
        boolean[] bouncyCastleRequested = {false};
        URL[] urls = {
            CryptoUtils.class.getProtectionDomain().getCodeSource().getLocation(),
            JavaUtils.class.getProtectionDomain().getCodeSource().getLocation()
        };
        // Isolate static initialization from other tests and exclude optional providers.
        try (URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getPlatformClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("org.bouncycastle.")) {
                    bouncyCastleRequested[0] = true;
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }
        }) {
            System.setProperty("fips.enabled", "true");
            setProperty("fips.security.provider", provider);
            Class<?> javaUtils = Class.forName(JavaUtils.class.getName(), true, loader);
            assertEquals(Boolean.TRUE, javaUtils.getMethod("isFIPSEnabled").invoke(null));
            assertEquals(provider, javaUtils.getMethod("getFIPSSecurityProvider").invoke(null));

            if (expectedError != null) {
                ExceptionInInitializerError error = assertThrows(ExceptionInInitializerError.class,
                    () -> Class.forName(CryptoUtils.class.getName(), true, loader));
                assertTrue(error.getCause() instanceof IllegalStateException);
                assertTrue(error.getCause().getMessage(), error.getCause().getMessage().startsWith(expectedError));
            } else {
                Class<?> cryptoUtils = Class.forName(CryptoUtils.class.getName(), true, loader);
                byte[] random = (byte[])cryptoUtils.getMethod("generateSecureRandomBytes", int.class).invoke(null, 32);
                assertEquals(32, random.length);
            }
            assertFalse("FIPS mode must not attempt to load Bouncy Castle", bouncyCastleRequested[0]);
            assertArrayEquals(originalProviders, Security.getProviders());
        } finally {
            setProperty("fips.enabled", originalFipsEnabled);
            setProperty("fips.security.provider", originalFipsProvider);
        }
    }

    private static void setProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
