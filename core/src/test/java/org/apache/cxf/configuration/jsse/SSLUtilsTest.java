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

import java.util.Arrays;

import javax.net.ssl.SSLContext;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.security.FiltersType;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class SSLUtilsTest {

    @Test
    public void testDefaultCipherSuitesFilterExcluded() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, new java.security.SecureRandom());

        FiltersType filtersType = new FiltersType();
        filtersType.getInclude().add(".*_AES_.*");
        String[] supportedCipherSuites = sslContext.getSocketFactory().getSupportedCipherSuites();
        String[] filteredCipherSuites = SSLUtils.getFilteredCiphersuites(filtersType, supportedCipherSuites,
                                         LogUtils.getL7dLogger(SSLUtilsTest.class), false);

        assertTrue(filteredCipherSuites.length > 0);
        // Check we have no anon/EXPORT/NULL/etc ciphersuites
        assertFalse(Arrays.stream(
            filteredCipherSuites).anyMatch(c -> c.matches(".*NULL|anon|EXPORT|DES|MD5|CBC|RC4.*")));
    }

    @Test
    public void testExclusionFilter() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, new java.security.SecureRandom());

        FiltersType filtersType = new FiltersType();
        filtersType.getInclude().add(".*_SHA384");
        filtersType.getExclude().add(".*_SHA256");
        String[] supportedCipherSuites = sslContext.getSocketFactory().getSupportedCipherSuites();
        String[] filteredCipherSuites = SSLUtils.getFilteredCiphersuites(filtersType, supportedCipherSuites,
                                         LogUtils.getL7dLogger(SSLUtilsTest.class), false);

        assertTrue(filteredCipherSuites.length > 0);
        // Check we have no SHA-256 ciphersuites
        assertFalse(Arrays.stream(
            filteredCipherSuites).anyMatch(c -> c.matches(".*_SHA256")));
    }

    @Test
    public void testInclusionFilter() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, new java.security.SecureRandom());

        FiltersType filtersType = new FiltersType();
        filtersType.getInclude().add(".*_SHA256");
        String[] supportedCipherSuites = sslContext.getSocketFactory().getSupportedCipherSuites();
        String[] filteredCipherSuites = SSLUtils.getFilteredCiphersuites(filtersType, supportedCipherSuites,
                                         LogUtils.getL7dLogger(SSLUtilsTest.class), false);

        assertTrue(filteredCipherSuites.length > 0);
        // Check we have SHA-256 ciphersuites
        assertTrue(Arrays.stream(
            filteredCipherSuites).anyMatch(c -> c.matches(".*_SHA256")));
    }


}