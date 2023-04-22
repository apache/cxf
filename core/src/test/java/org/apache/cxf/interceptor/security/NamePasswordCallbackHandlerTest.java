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

package org.apache.cxf.interceptor.security;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class NamePasswordCallbackHandlerTest {

    @Test
    public void testHandleCallback() throws Exception {
        NamePasswordCallbackHandler handler = new NamePasswordCallbackHandler("Barry", "dog");
        Callback[] callbacks =
            new Callback[]{new NameCallback("name"), new PasswordCallback("password", false)};
        handler.handle(callbacks);
        assertEquals("Barry", ((NameCallback)callbacks[0]).getName());
        assertEquals("dog", new String(((PasswordCallback)callbacks[1]).getPassword()));
    }

    @Test
    public void testHandleCallback2() throws Exception {
        NamePasswordCallbackHandler handler = new NamePasswordCallbackHandler("Barry", "dog");
        Callback[] callbacks =
            new Callback[]{new NameCallback("name"), new ObjectCallback()};
        handler.handle(callbacks);
        assertEquals("Barry", ((NameCallback)callbacks[0]).getName());
        Object pwobj = ((ObjectCallback)callbacks[1]).getObject();
        assertTrue(pwobj instanceof char[]);
        assertEquals("dog", new String((char[])pwobj));
    }

    @Test
    public void testHandleCallback3() throws Exception {
        NamePasswordCallbackHandler handler = new NamePasswordCallbackHandler("Barry", "dog");
        Callback[] callbacks =
            new Callback[]{new NameCallback("name"), new StringObjectCallback()};
        handler.handle(callbacks);
        assertEquals("Barry", ((NameCallback)callbacks[0]).getName());
        assertEquals("dog", ((StringObjectCallback)callbacks[1]).getObject());
    }

    @Test
    public void testHandleCallback4() throws Exception {
        NamePasswordCallbackHandler handler = new NamePasswordCallbackHandler("Barry", "dog", "setValue");
        Callback[] callbacks =
            new Callback[]{new NameCallback("name"), new CharArrayCallback()};
        handler.handle(callbacks);
        assertEquals("Barry", ((NameCallback)callbacks[0]).getName());
        assertEquals("dog", new String(((CharArrayCallback)callbacks[1]).getValue()));
    }

    @Test
    public void testHandleCallbackNullPassword() throws Exception {
        NamePasswordCallbackHandler handler = new NamePasswordCallbackHandler("Barry", null);
        Callback[] callbacks =
            new Callback[]{new NameCallback("name"), new PasswordCallback("password", false)};
        handler.handle(callbacks);
        assertEquals("Barry", ((NameCallback)callbacks[0]).getName());
        assertNull(((PasswordCallback)callbacks[1]).getPassword());
    }

    static class ObjectCallback implements Callback {
        private Object obj;

        public Object getObject() {
            return obj;
        }

        public void setObject(Object o) {
            this.obj = o;
        }
    }

    static class StringObjectCallback implements Callback {
        private String obj;

        public String getObject() {
            return obj;
        }

        public void setObject(String o) {
            this.obj = o;
        }
    }

    static class CharArrayCallback implements Callback {
        private char[] obj;

        public char[] getValue() {
            return obj;
        }

        public void setValue(char[] o) {
            this.obj = o;
        }
    }
}