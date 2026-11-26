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

package org.apache.cxf.transport.http_undertow;

import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.apache.cxf.transport.https.SSLUtils;

import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class NamedGroupsSSLContextTest {
    private static final List<String> GROUPS = Arrays.asList("secp384r1", "x25519");

    @Test
    public void testEnginesGetTheNamedGroups() throws Exception {
        Assume.assumeTrue("SSLParameters.setNamedGroups() requires JDK 20+", hasNamedGroupsApi());
        SSLContext delegate = SSLContext.getDefault();
        SSLContext context = NamedGroupsSSLContext.wrap(delegate, GROUPS);
        assertEquals(delegate.getProtocol(), context.getProtocol());
        assertEquals(delegate.getProvider(), context.getProvider());
        assertArrayEquals(GROUPS.toArray(), SSLUtils.getNamedGroups(context.createSSLEngine().getSSLParameters()));
        assertArrayEquals(GROUPS.toArray(),
            SSLUtils.getNamedGroups(context.createSSLEngine("localhost", 8443).getSSLParameters()));
    }

    @Test
    public void testNoNamedGroupsKeepsTheContext() throws Exception {
        SSLContext delegate = SSLContext.getDefault();
        assertSame(delegate, NamedGroupsSSLContext.wrap(delegate, null));
        assertSame(delegate, NamedGroupsSSLContext.wrap(delegate, List.of()));
    }

    private static boolean hasNamedGroupsApi() {
        try {
            SSLParameters.class.getMethod("setNamedGroups", String[].class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
