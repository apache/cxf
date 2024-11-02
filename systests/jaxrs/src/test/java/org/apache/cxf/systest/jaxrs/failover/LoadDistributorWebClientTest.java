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

package org.apache.cxf.systest.jaxrs.failover;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.clustering.LoadDistributorFeature;
import org.apache.cxf.clustering.SequentialStrategy;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A test for the load distributor using a WebClient object
 */
public class LoadDistributorWebClientTest extends AbstractBusClientServerTestBase {
    static final String PORT1 = allocatePort(LoadDistributorServer.class);
    static final String PORT2 = allocatePort(LoadDistributorServer.class, 2);


    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(LoadDistributorServer.class, true));
        final Bus bus = createStaticBus();
        // Make sure default JSON-P/JSON-B providers are not loaded
        bus.setProperty(ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION, true);
    }

    @Test
    public void testLoadDistributor() throws Exception {
        URL busFile = LoadDistributorWebClientTest.class.getResource("cxf-client.xml");

        String address = "http://localhost:" + PORT1 + "/bookstore";

        LoadDistributorFeature feature = new LoadDistributorFeature();
        SequentialStrategy strategy = new SequentialStrategy();
        List<String> addresses = new ArrayList<>();
        addresses.add(address);
        addresses.add("http://localhost:" + PORT2 + "/bookstore");
        strategy.setAlternateAddresses(addresses);
        feature.setStrategy(strategy);

        WebClient webClient = WebClient.create(address, null,
                                               Collections.singletonList(feature),
                                               busFile.toString()).accept("application/xml");

        Book b = webClient.get(Book.class);
        assertEquals(124L, b.getId());
        assertEquals("root", b.getName());

        b = webClient.get(Book.class);
        assertEquals(124L, b.getId());
        assertEquals("root", b.getName());

    }

}