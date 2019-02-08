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

package org.apache.cxf.ws.security.wss4j;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.xml.security.utils.XMLUtils;

public class TokenStoreCallbackHandler implements CallbackHandler {
    private CallbackHandler internal;
    private TokenStore store;
    public TokenStoreCallbackHandler(CallbackHandler in, TokenStore st) {
        internal = in;
        store = st;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof WSPasswordCallback) {
                WSPasswordCallback pc = (WSPasswordCallback)callback;

                String id = pc.getIdentifier();
                SecurityToken tok = store.getToken(id);
                if (tok != null && !tok.isExpired()) {
                    if (tok.getSHA1() == null && pc.getKey() != null) {
                        tok.setSHA1(getSHA1(pc.getKey()));
                        // Create another cache entry with the SHA1 Identifier as the key for easy retrieval
                        store.add(tok.getSHA1(), tok);
                    }
                    pc.setKey(tok.getSecret());
                    pc.setKey(tok.getKey());
                    pc.setCustomToken(tok.getToken());
                    return;
                }
            }
        }
        if (internal != null) {
            internal.handle(callbacks);
        }
    }

    private static String getSHA1(byte[] input) {
        try {
            byte[] digestBytes = KeyUtils.generateDigest(input);
            return XMLUtils.encodeToString(digestBytes);
        } catch (WSSecurityException e) {
            //REVISIT
        }
        return null;
    }
}