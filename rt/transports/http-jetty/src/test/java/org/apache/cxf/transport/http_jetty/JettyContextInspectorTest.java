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

package org.apache.cxf.transport.http_jetty;

import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.handler.ContextHandler;

public class JettyContextInspectorTest extends Assert {
    private static final String CONTEXT_PATH = "/foo/bar";
    private ContextHandler context;
    private IMocksControl control;
    
    @Before
    public void setUp() throws Exception {
        control = EasyMock.createNiceControl();
        context = control.createMock(ContextHandler.class);
        context.getContextPath();
        EasyMock.expectLastCall().andReturn(CONTEXT_PATH);
        control.replay();
    }

    @After
    public void tearDown() {
        control.verify();
        control = null;
        context = null;
    }
    
    @Test
    public void testGetAddress() throws Exception {
        JettyContextInspector inspector = new JettyContextInspector();
        assertEquals("unexpected address",
                     CONTEXT_PATH,
                     inspector.getAddress(context));
    }
}
