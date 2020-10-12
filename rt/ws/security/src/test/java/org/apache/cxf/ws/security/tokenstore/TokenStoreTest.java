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
package org.apache.cxf.ws.security.tokenstore;

import java.util.Arrays;
import java.util.Collection;

import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.xml.security.utils.ClassLoaderUtils;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(value = org.junit.runners.Parameterized.class)
public class TokenStoreTest {

    private TokenStore store;

    public TokenStoreTest(TokenStore store) {
        this.store = store;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<TokenStore> data() throws TokenStoreException {
        Message message = new MessageImpl();
        message.put(
                SecurityConstants.CACHE_CONFIG_FILE,
                ClassLoaderUtils.getResource("cxf-ehcache.xml", TokenStoreTest.class)
        );
        message.setExchange(new ExchangeImpl());
        return Arrays.asList(
                new MemoryTokenStoreFactory().newTokenStore(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, message),
                new EHCacheTokenStoreFactory().newTokenStore(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, message)
        );
    }

    // tests TokenStore apis for storing in the cache.
    @org.junit.Test
    public void testTokenAdd() throws Exception {
        String key = "key";
        SecurityToken token = new SecurityToken(key);
        store.add(token);
        assertEquals(token, store.getToken(key));
        store.remove(token.getId());
        assertNull(store.getToken(key));

        String newKey = "xyz";
        store.add(newKey, token);
        assertNull(store.getToken(key));
        assertEquals(token, store.getToken(newKey));
        store.remove(newKey);
        assertNull(store.getToken(newKey));
    }

    // tests TokenStore apis for removing from the cache.
    @org.junit.Test
    public void testTokenRemove() {
        SecurityToken token1 = new SecurityToken("token1");
        SecurityToken token2 = new SecurityToken("token2");
        SecurityToken token3 = new SecurityToken("token3");
        store.add(token1);
        store.add(token2);
        store.add(token3);
        assertTrue(store.getTokenIdentifiers().size() == 3);
        store.remove(token3.getId());
        assertNull(store.getToken("test3"));
        store.remove(token1.getId());
        store.remove(token2.getId());
        assertTrue(store.getTokenIdentifiers().isEmpty());
    }

}
