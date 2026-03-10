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

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.clustering.FailoverFeature;
import org.apache.cxf.clustering.FailoverTargetSelector;
import org.apache.cxf.clustering.LoadDistributorFeature;
import org.apache.cxf.clustering.LoadDistributorTargetSelector;
import org.apache.cxf.clustering.SequentialStrategy;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.systest.jaxrs.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests failover within a static cluster.
 */
public class LoadDistributorTest extends AbstractBusClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(Server.class, true));
        boolean activeReplica1Started = false;
        boolean activeReplica2Started = false;
        for (int i = 0; i < 60; i++) {
            if (!activeReplica1Started) {
                activeReplica1Started = checkReplica(Server.ADDRESS2);
            }
            if (!activeReplica2Started) {
                activeReplica2Started = checkReplica(Server.ADDRESS3);
            }
            if (activeReplica1Started && activeReplica2Started) {
                break;
            }
            Thread.sleep(1000);
        }
    }
    private static boolean checkReplica(String address) {
        try {
            Response r = WebClient.create(address).query("_wadl").get();
            return r.getStatus() == 200;
        } catch (Exception ex) {
            return false;
        }
    }

    @Test
    public void testMultipleAltAddresses() throws Exception {
        FailoverFeature feature = getFeature(Server.ADDRESS2, Server.ADDRESS3);
        strategyTest(Server.ADDRESS1, feature);
    }

    @Test
    public void testSingleAltAddress() throws Exception {
        LoadDistributorFeature feature = new LoadDistributorFeature();
        List<String> alternateAddresses = new ArrayList<>();
        alternateAddresses.add(Server.ADDRESS2);
        SequentialStrategy strategy = new SequentialStrategy();
        strategy.setAlternateAddresses(alternateAddresses);
        feature.setStrategy(strategy);

        BookStore bookStore = getBookStore(Server.ADDRESS1, feature);
        Book book = bookStore.getBook("123");
        assertEquals("unexpected id", 123L, book.getId());

        book = bookStore.getBook("123");
        assertEquals("unexpected id", 123L, book.getId());
    }


    private FailoverFeature getFeature(String ...address) {
        FailoverFeature feature = new FailoverFeature();
        List<String> alternateAddresses = new ArrayList<>();
        for (String s : address) {
            alternateAddresses.add(s);
        }
        SequentialStrategy strategy = new SequentialStrategy();
        strategy.setAlternateAddresses(alternateAddresses);
        feature.setStrategy(strategy);

        LoadDistributorTargetSelector selector = new LoadDistributorTargetSelector();
        selector.setFailover(false);

        feature.setTargetSelector(selector);

        return feature;
    }

    protected BookStore getBookStore(String address,
                                     FailoverFeature feature) throws Exception {
        JAXRSClientFactoryBean bean = createBean(address, feature);
        bean.setServiceClass(BookStore.class);
        return bean.create(BookStore.class);
    }

    protected JAXRSClientFactoryBean createBean(String address,
                                                FailoverFeature feature) {
        final Bus bus = new ExtensionManagerBus();
        // Make sure default JSON-P/JSON-B providers are not loaded
        bus.setProperty(ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION, true);

        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        List<AbstractFeature> features = new ArrayList<>();
        features.add(feature);
        bean.setFeatures(features);
        bean.setBus(bus);
   
        return bean;
    }

    protected void strategyTest(String initialAddress,
                                FailoverFeature feature) throws Exception {
        assertEquals(Server.ADDRESS1, initialAddress);
        int address2Count = 0;
        int address3Count = 0;
        for (int i = 0; i < 20; i++) {
            BookStore bookStore = getBookStore(initialAddress, feature);
            verifyStrategy(bookStore, SequentialStrategy.class);
            String bookId = "123";

            Book book = bookStore.getBook(bookId);
            assertNotNull("expected non-null response", book);
            assertEquals("unexpected id", 123L, book.getId());

            String address = getCurrentEndpointAddress(bookStore);
            if (Server.ADDRESS2.equals(address)) {
                address2Count++;
            } else if (Server.ADDRESS3.equals(address)) {
                address3Count++;
            }
        }
        assertEquals(10, address2Count);
        assertEquals(10, address3Count);
    }

    protected String getCurrentEndpointAddress(Object client) {
        return WebClient.getConfig(client).getConduitSelector()
            .getEndpoint().getEndpointInfo().getAddress();
    }

    protected void verifyStrategy(Object proxy, Class<?> clz) {
        ConduitSelector conduitSelector =
            WebClient.getConfig(proxy).getConduitSelector();
        if (conduitSelector instanceof FailoverTargetSelector) {
            Object strategy =
                ((FailoverTargetSelector)conduitSelector).getStrategy();
            assertTrue("unexpected strategy", clz.isInstance(strategy));
        } else {
            fail("unexpected conduit selector: " + conduitSelector);
        }
    }

}
