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
package org.apache.cxf.rs.security.oauth2.tokens.mac;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;

public class NonceVerifierImpl implements NonceVerifier {
    private NonceStore nonceStore;
    private long allowedWindow; // 2000 ms

    public void verifyNonce(String tokenKey, String clientNonceString, String clientTimestampString) 
        throws OAuthServiceException {
        
        if (StringUtils.isEmpty(clientNonceString)
            || StringUtils.isEmpty(clientTimestampString)) {
            throw new OAuthServiceException("Nonce or timestamp is not available");
        }
        
        long serverClock = System.currentTimeMillis();
        NonceHistory nonceHistory = nonceStore.getNonceHistory(tokenKey);
        boolean firstTimeRequest = false;
        if (nonceHistory == null) {
            firstTimeRequest = true;
        }
        long clientTimestamp = Long.valueOf(clientTimestampString);
        if (firstTimeRequest) {
            long requestTimeDelta = serverClock - clientTimestamp;
            Nonce nonce = new Nonce(clientNonceString, clientTimestamp);
            nonceStore.storeNonce(tokenKey, nonce, requestTimeDelta);
        } else {
            checkAdjustedRequestTime(serverClock, clientTimestamp, nonceHistory);
            checkNonceHistory(nonceHistory, clientNonceString, clientTimestamp);
        }
    }

    private static void checkNonceHistory(NonceHistory nonceHistory, final String clientNonceString,
                                          final long ts) throws OAuthServiceException {
        if (!nonceHistory.findMatchingNonces(clientNonceString, ts).isEmpty()) {
            throw new OAuthServiceException("Duplicate nonce");
        }
    }

    private void checkAdjustedRequestTime(long serverClock, long clientTimestamp, NonceHistory nonceHistory) {
        long adjustedRequestTime = clientTimestamp + nonceHistory.getRequestTimeDelta();
        long requestDelta = Math.abs(serverClock - adjustedRequestTime);
        if (requestDelta > allowedWindow) {
            throw new OAuthServiceException("Timestamp is invalid");
        }
    }

    public void setAllowedWindow(long allowedWindow) {
        this.allowedWindow = allowedWindow;
    }

    public void setNonceStore(NonceStore nonceStore) {
        this.nonceStore = nonceStore;
    }
}