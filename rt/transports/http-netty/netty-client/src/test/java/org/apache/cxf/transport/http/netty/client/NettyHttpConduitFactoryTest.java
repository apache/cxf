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
package org.apache.cxf.transport.http.netty.client;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.http.HTTPConduitFactory;

import io.netty.channel.EventLoopGroup;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;




public class NettyHttpConduitFactoryTest {
    Bus bus;

    @BeforeClass
    public static void classUp() {
        // Get rid of any notion of a default bus set by other
        // rogue tests.
        BusFactory.setDefaultBus(null);
    }

    @AfterClass
    public static void classDown() {
        // Clean up.
        BusFactory.setDefaultBus(null);
    }

    @After
    public void tearDown() {
        if (bus != null) {
            bus.shutdown(false);
            bus = null;
        }
    }

    @Test
    public void testLoadingHttpConduitFactory() throws Exception {
        bus = BusFactory.getDefaultBus(true);

        assertNotNull("Cannot get bus", bus);

        // Make sure we got the Transport Factory.
        HTTPConduitFactory factory =
                bus.getExtension(HTTPConduitFactory.class);
        assertNotNull("Cannot get HTTPConduitFactory", factory);

        assertTrue(NettyHttpConduitFactory.class.isInstance(factory));


    }

    @Test
    public void testShutdownEventLoopGroup() throws Exception {
        bus = BusFactory.getDefaultBus(true);

        assertNotNull("Cannot get bus", bus);

        // Make sure we got the Transport Factory.
        NettyHttpTransportFactory factory =
                bus.getExtension(NettyHttpTransportFactory.class);
        assertNotNull("Cannot get NettyHttpTransportFactory", factory);

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setName(new QName("bla", "Service"));
        EndpointInfo ei = new EndpointInfo(serviceInfo, "");
        ei.setName(new QName("bla", "Port"));
        ei.setAddress("netty://foo");

        // The EventLoopGroup is put into bus when create a new netty http conduit
        factory.getConduit(ei, null, bus);

        bus.shutdown(true);

        EventLoopGroup eventLoopGroup = bus.getExtension(EventLoopGroup.class);
        assertNotNull("We should find the EventLoopGroup here.", eventLoopGroup);
        assertTrue("The eventLoopGroup should be shutdown.", eventLoopGroup.isShutdown());

    }
}