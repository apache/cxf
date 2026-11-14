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

import java.lang.reflect.Proxy;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AbstractSSOSpHandlerTest {

    @Test
    public void testDefaultCookieAttributes() {
        TestHandler handler = new TestHandler();
        handler.setStateTimeToLive(0);

        assertTrue(handler.isHttpOnlyCookie());
        assertFalse(handler.isSecureCookie());
        assertEquals("Lax", handler.getSameSite());

        String cookie = handler.createCookie(SSOConstants.SECURITY_CONTEXT_TOKEN, "key", "/app", null);

        assertEquals(SSOConstants.SECURITY_CONTEXT_TOKEN + "=key;Path=/app;HttpOnly;SameSite=Lax", cookie);
    }

    @Test
    public void testHttpOnlyAndSameSiteCanBeDisabled() {
        TestHandler handler = new TestHandler();
        handler.setStateTimeToLive(0);
        handler.setHttpOnlyCookie(false);
        handler.setSameSite(null);
        assertNull(handler.getSameSite());

        String cookie = handler.createCookie(SSOConstants.RELAY_STATE, "state", "/app", "example.com");

        assertEquals(SSOConstants.RELAY_STATE + "=state;Path=/app;Domain=example.com", cookie);
    }

    @Test
    public void testSecureCookieWithCustomSameSite() {
        TestHandler handler = new TestHandler();
        handler.setStateTimeToLive(0);
        handler.setSecureCookie(true);
        handler.setSameSite("Strict");

        String cookie = handler.createCookie(SSOConstants.SECURITY_CONTEXT_TOKEN, "key", "/app", "example.com");

        assertEquals(SSOConstants.SECURITY_CONTEXT_TOKEN
            + "=key;Path=/app;Domain=example.com;HttpOnly;Secure;SameSite=Strict", cookie);
    }

    @Test
    public void testCookieWithExpires() {
        TestHandler handler = new TestHandler();

        String cookie = handler.createCookie(SSOConstants.SECURITY_CONTEXT_TOKEN, "key", "/app", null);

        String prefix = SSOConstants.SECURITY_CONTEXT_TOKEN + "=key;Path=/app;Expires=";
        assertTrue(cookie, cookie.startsWith(prefix));
        assertTrue(cookie, cookie.endsWith(";HttpOnly;SameSite=Lax"));
    }

    @Test
    public void testIsSecureRequestFromUrl() {
        TestHandler handler = new TestHandler();

        assertFalse(handler.isSecureRequest(null));

        Message m = new MessageImpl();
        assertFalse(handler.isSecureRequest(m));

        m.put(Message.REQUEST_URL, "http://sp.example.com/app/sso");
        assertFalse(handler.isSecureRequest(m));

        m.put(Message.REQUEST_URL, "https://sp.example.com/app/sso");
        assertTrue(handler.isSecureRequest(m));

        m.put(Message.REQUEST_URL, "HTTPS://sp.example.com/app/sso");
        assertTrue(handler.isSecureRequest(m));

        Message m2 = new MessageImpl();
        m2.put(Message.BASE_PATH, "https://sp.example.com/app");
        assertTrue(handler.isSecureRequest(m2));

        // the configured endpoint address says nothing about the scheme of the current request
        Message m3 = new MessageImpl();
        m3.put(Message.ENDPOINT_ADDRESS, "https://sp.example.com/app");
        assertFalse(handler.isSecureRequest(m3));
    }

    @Test
    public void testIsSecureRequestFromServletRequest() {
        TestHandler handler = new TestHandler();

        // TLS terminated at a proxy: the request URL is http but the container reports a secure request
        Message m = new MessageImpl();
        m.put(Message.REQUEST_URL, "http://sp.example.com/app/sso");
        m.put(AbstractHTTPDestination.HTTP_REQUEST, servletRequest(true));
        assertTrue(handler.isSecureRequest(m));

        Message m2 = new MessageImpl();
        m2.put(Message.REQUEST_URL, "https://sp.example.com/app/sso");
        m2.put(AbstractHTTPDestination.HTTP_REQUEST, servletRequest(false));
        assertFalse(handler.isSecureRequest(m2));
    }

    private static HttpServletRequest servletRequest(boolean secure) {
        return (HttpServletRequest) Proxy.newProxyInstance(
            AbstractSSOSpHandlerTest.class.getClassLoader(),
            new Class<?>[] {HttpServletRequest.class},
            (proxy, method, args) -> {
                if ("isSecure".equals(method.getName())) {
                    return secure;
                }
                throw new UnsupportedOperationException(method.getName());
            });
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
