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
package org.apache.cxf.rs.security.saml.sso;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AbstractSSOSpHandlerTest {

    @Test
    public void testCookieIsHttpOnly() {
        TestHandler handler = new TestHandler();
        handler.setStateTimeToLive(0);

        String cookie = handler.createCookie(SSOConstants.SECURITY_CONTEXT_TOKEN, "key", "/app", null);

        assertEquals(SSOConstants.SECURITY_CONTEXT_TOKEN + "=key;Path=/app;HttpOnly", cookie);
    }

    @Test
    public void testHttpOnlyCanBeDisabled() {
        TestHandler handler = new TestHandler();
        handler.setStateTimeToLive(0);
        handler.setHttpOnlyCookie(false);

        String cookie = handler.createCookie(SSOConstants.RELAY_STATE, "state", "/app", "example.com");

        assertEquals(SSOConstants.RELAY_STATE + "=state;Path=/app;Domain=example.com", cookie);
    }

    @Test
    public void testSecureCookie() {
        TestHandler handler = new TestHandler();
        handler.setStateTimeToLive(0);
        handler.setSecureCookie(true);

        String cookie = handler.createCookie(SSOConstants.SECURITY_CONTEXT_TOKEN, "key", "/app", null);

        assertEquals(SSOConstants.SECURITY_CONTEXT_TOKEN + "=key;Path=/app;HttpOnly;Secure", cookie);
    }

    @Test
    public void testIsSecureRequest() {
        TestHandler handler = new TestHandler();

        assertFalse(handler.isSecureRequest(null));

        Message m = new MessageImpl();
        assertFalse(handler.isSecureRequest(m));

        m.put(Message.REQUEST_URL, "http://sp.example.com/app/sso");
        assertFalse(handler.isSecureRequest(m));

        m.put(Message.REQUEST_URL, "https://sp.example.com/app/sso");
        assertTrue(handler.isSecureRequest(m));

        Message m2 = new MessageImpl();
        m2.put(Message.ENDPOINT_ADDRESS, "https://sp.example.com/app");
        assertTrue(handler.isSecureRequest(m2));
    }

    private static final class TestHandler extends AbstractSSOSpHandler {
        @Override
        protected String createCookie(String name, String value, String path, String domain) {
            return super.createCookie(name, value, path, domain);
        }

        @Override
        protected boolean isSecureRequest(Message m) {
            return super.isSecureRequest(m);
        }
    }
}
