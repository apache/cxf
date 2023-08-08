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
package org.apache.cxf.rs.security.oauth2.tokens.hawk;

import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NonceVerifierImplTest {

    private NonceVerifierImpl nonceVerifier;
    private NonceStore nonceStore = mock(NonceStore.class);

    @Before
    public void setUp() {
        nonceVerifier = new NonceVerifierImpl();
        nonceVerifier.setNonceStore(nonceStore);
    }

    @Test
    public void testVerifyNonce() {
        long now = System.currentTimeMillis();
        Nonce nonce1 = new Nonce("nonce1", now - 2000); // first request 2 seconds back
        Nonce nonce2 = new Nonce("nonce2", now - 1000); // second request 1 second back
        NonceHistory nonceHistory = new NonceHistory(200, nonce1); // first request time delta is 200ms
        nonceHistory.addNonce(nonce2);

        when(nonceStore.getNonceHistory("testTokenKey")).thenReturn(nonceHistory);
        nonceVerifier.setAllowedWindow(2000); // allowed window is 2 seconds
        nonceVerifier.verifyNonce("testTokenKey", "nonce3", Long.toString(now - 500));
    }

    @Test
    public void testVerifyNonceDuplicateNonce() {
        long now = System.currentTimeMillis();
        Nonce nonce1 = new Nonce("nonce1", now - 2000); // first request 2 seconds back
        Nonce nonce2 = new Nonce("nonce2", now - 1000); // second request 1 second back
        NonceHistory nonceHistory = new NonceHistory(200, nonce1); // first request time delta is 200ms
        nonceHistory.addNonce(nonce2);

        when(nonceStore.getNonceHistory("testTokenKey")).thenReturn(nonceHistory);
        nonceVerifier.setAllowedWindow(2000); // allowed window is 2 seconds
        try {
            nonceVerifier.verifyNonce("testTokenKey", "nonce2", Long.toString(now - 1000));
            fail("Exception expected");
        } catch (OAuthServiceException ex) {
            assertEquals("Duplicate nonce", ex.getMessage());
        }
    }

    @Test
    public void testVerifyNonceInvalidTimestamp() {
        long now = System.currentTimeMillis();
        Nonce nonce1 = new Nonce("nonce1", now - 2000); // first request 2 seconds back
        Nonce nonce2 = new Nonce("nonce2", now - 1000); // second request 1 second back
        NonceHistory nonceHistory = new NonceHistory(200, nonce1); // first request time delta is 200ms
        nonceHistory.addNonce(nonce2);

        when(nonceStore.getNonceHistory("testTokenKey")).thenReturn(nonceHistory);
        nonceVerifier.setAllowedWindow(2000); // allowed window is 2 seconds
        try {
            nonceVerifier.verifyNonce("testTokenKey", "nonce3", Long.toString(now - 5000)); // very old timestamp
            fail("Exception expected");
        } catch (OAuthServiceException ex) {
            assertEquals("Timestamp is invalid", ex.getMessage());
        }
    }

}