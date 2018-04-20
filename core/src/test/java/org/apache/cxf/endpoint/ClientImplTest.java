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

package org.apache.cxf.endpoint;

import java.util.Map;

import org.apache.cxf.Bus;

import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
public class ClientImplTest {

    private Bus bus = mock(Bus.class);
    private Endpoint endpoint = mock(Endpoint.class);

    @Before
    public void setUp() throws Exception {
        expect(bus.getExtension(ClientLifeCycleManager.class)).andReturn(null);
        expect(bus.getExtension(ClassLoader.class)).andReturn(null);
        replay(bus);
    }

    @Test
    public void requestContextIsThreadLocal() {
        Client client = new ClientImpl(bus, endpoint);
        client.setThreadLocalRequestContext(true);
        Map<String, Object> requestContext = client.getRequestContext();
        assertSame(requestContext, client.getRequestContext());
    }

    @Test
    public void requestContextIsGarbageCollected() {
        Client client = new ClientImpl(bus, endpoint);
        client.setThreadLocalRequestContext(true);
        Map<String, Object> requestContext = client.getRequestContext();
        System.gc();
        assertNotSame(requestContext, client.getRequestContext());
    }

    @Test
    public void requestContextCleanOnDemand() {
        Client client = new ClientImpl(bus, endpoint);
        client.setThreadLocalRequestContext(true);
        Map<String, Object> requestContext = client.getRequestContext();
        client.clearThreadLocalRequestContexts();
        assertNotSame(requestContext, client.getRequestContext());
    }

    @Test
    public void responseContextIsThreadLocal() {
        Client client = new ClientImpl(bus, endpoint);
        Map<String, Object> requestContext = client.getResponseContext();
        assertSame(requestContext, client.getResponseContext());
    }

    @Test
    public void responseContextIsGarbageCollected() {
        Client client = new ClientImpl(bus, endpoint);
        Map<String, Object> responseContext = client.getResponseContext();
        System.gc();
        assertNotSame(responseContext, client.getResponseContext());
    }

    @Test
    public void responseContextCleanOnDemand() {
        Client client = new ClientImpl(bus, endpoint);
        Map<String, Object> requestContext = client.getResponseContext();
        client.clearThreadLocalResponseContexts();
        assertNotSame(requestContext, client.getResponseContext());
    }
}
