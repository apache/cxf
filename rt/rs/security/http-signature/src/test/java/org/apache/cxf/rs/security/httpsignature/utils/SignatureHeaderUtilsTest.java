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

import org.apache.cxf.rs.security.httpsignature.exception.DigestFailureException;

import static org.junit.Assert.assertNotNull;

public class SignatureHeaderUtilsTest {

    @org.junit.Test
    public void testCreateMessageDigestSHA256() {
        assertNotNull(SignatureHeaderUtils.createMessageDigestWithAlgorithm("SHA-256"));
    }

    @org.junit.Test
    public void testCreateMessageDigestSHA512() {
        assertNotNull(SignatureHeaderUtils.createMessageDigestWithAlgorithm("SHA-512"));
    }

    @org.junit.Test(expected = DigestFailureException.class)
    public void testCreateMessageDigestSHA1() {
        SignatureHeaderUtils.createMessageDigestWithAlgorithm("SHA-1");
    }

    @org.junit.Test(expected = DigestFailureException.class)
    public void testCreateMessageDigestUnknown() {
        SignatureHeaderUtils.createMessageDigestWithAlgorithm("Unknown");
    }

    @org.junit.Test
    public void testCreateDigestHeaderSHA256() {
        assertNotNull(SignatureHeaderUtils.createDigestHeader("xyz", "SHA-256"));
    }

    @org.junit.Test
    public void testCreateDigestHeaderSHA512() {
        assertNotNull(SignatureHeaderUtils.createDigestHeader("xyz", "SHA-512"));
    }

    @org.junit.Test(expected = DigestFailureException.class)
    public void testCreateDigestHeaderSHA1() {
        SignatureHeaderUtils.createDigestHeader("xyz", "SHA-1");
    }

}
