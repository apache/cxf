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

package org.apache.cxf.jaxrs.security;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JAASAuthenticationFilterTest {

    @Test
    public void testRFC2617() throws Exception {

        JAASAuthenticationFilter filter = new JAASAuthenticationFilter();
        filter.setRealmName("foo");

        Message m = new MessageImpl();
        Response r = filter.handleAuthenticationException(new SecurityException("Bad Auth"), m);
        assertNotNull(r);

        String result = r.getHeaderString(HttpHeaders.WWW_AUTHENTICATE);
        assertNotNull(result);

        //Test that the header conforms to RFC2617
        assertEquals("Basic realm=\"foo\"", result);
    }
}