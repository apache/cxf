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

package org.apache.servicemix.cxf.transport.http_osgi;

import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.Bus;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.MessageObserver;

import org.easymock.classextension.IMocksControl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.classextension.EasyMock.*;

public class OsgiDestinationTest extends Assert {

    private static final String ADDRESS = "http://bar/snafu";
    private IMocksControl control; 
    private Bus bus;
    private ConduitInitiator ci;
    private OsgiDestinationRegistryIntf registry;
    private MessageObserver observer;
    private EndpointInfo endpoint;

    @Before
    public void setUp() {
        control = createNiceControl();
        bus = control.createMock(Bus.class);
        ci = control.createMock(ConduitInitiator.class);
        registry = control.createMock(OsgiDestinationRegistryIntf.class);
        observer = control.createMock(MessageObserver.class);
        endpoint = new EndpointInfo();
        endpoint.setAddress(ADDRESS);
    }

    @After
    public void tearDown() {
        bus = null;
        ci = null;
        registry = null;
        observer = null;
    }

    @Test
    public void testCtor() throws Exception {
        OsgiDestination destination = 
            new OsgiDestination(bus, ci, endpoint, registry, "snafu");

        assertNull(destination.getMessageObserver());
        assertNotNull(destination.getAddress());
        assertNotNull(destination.getAddress().getAddress());
        assertEquals(ADDRESS, 
                     destination.getAddress().getAddress().getValue());
    }

    @Test
    public void testMessage() throws Exception {
        MessageImpl message = setUpMessage();

        control.replay();

        OsgiDestination destination = 
            new OsgiDestination(bus, ci, endpoint, registry, "snafu");
        destination.setMessageObserver(observer);

        destination.doMessage(message);

        control.verify();
    }

    @Test
    public void testShutdown() throws Exception {
        registry.removeDestination("snafu");
        expectLastCall();
        control.replay();

        OsgiDestination destination = 
            new OsgiDestination(bus, ci, endpoint, registry, "snafu");

        destination.shutdown();
         
        control.verify();
    }

    private MessageImpl setUpMessage() {
        MessageImpl message = control.createMock(MessageImpl.class);
        HttpServletRequest request =
            control.createMock(HttpServletRequest.class);
        message.get("HTTP.REQUEST");
        expectLastCall().andReturn(request);
        request.getHeaderNames();
        expectLastCall().andReturn(new StringTokenizer("content-type content-length"));
        request.getHeaders("content-type");
        expectLastCall().andReturn(new StringTokenizer("text/xml"));
        request.getHeaders("content-length");
        expectLastCall().andReturn(new StringTokenizer("1234"));
        observer.onMessage(message);
        expectLastCall();
        return message;
    }
}
