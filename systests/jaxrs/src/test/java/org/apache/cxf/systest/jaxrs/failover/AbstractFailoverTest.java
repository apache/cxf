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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.clustering.FailoverFeature;
import org.apache.cxf.clustering.FailoverTargetSelector;
import org.apache.cxf.clustering.RandomStrategy;
import org.apache.cxf.clustering.RetryStrategy;
import org.apache.cxf.clustering.SequentialStrategy;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.systest.jaxrs.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests failover within a static cluster.
 */
public abstract class AbstractFailoverTest extends AbstractBusClientServerTestBase {
    public static final String NON_PORT = allocatePort(AbstractFailoverTest.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(Server.class, true));
        boolean activeReplica1Started = false;
        boolean activeReplica2Started = false;
        for (int i = 0; i < 10; i++) {
            if (!activeReplica1Started) {
                activeReplica1Started = checkReplica(Server.ADDRESS2);
            }
            if (!activeReplica2Started) {
                activeReplica2Started = checkReplica(Server.ADDRESS3);
            }
            if (activeReplica1Started && activeReplica2Started) {
                break;
            }
            Thread.sleep(100L);
        }
    }

    private static boolean checkReplica(String address) {
        try {
            return WebClient.create(address).query("_wadl").get().getStatus() == 200;
        } catch (Exception ex) {
            return false;
        }
    }

    @Test
    public void testSequentialStrategy() throws Exception {
        FailoverFeature feature =
            getFeature(false, Server.ADDRESS2, Server.ADDRESS3);
        strategyTest(Server.ADDRESS1, feature, Server.ADDRESS2, null, false, false, false);
    }

    @Test
    public void testSequentialStrategyWebClient() throws Exception {
        FailoverFeature feature =
            getFeature(false, Server.ADDRESS2, Server.ADDRESS3);
        strategyTestWebClient(Server.ADDRESS1, feature, Server.ADDRESS2, null, false, false);
    }

    @Test
    public void testSequentialStrategyWith404() throws Exception {
        FailoverFeature feature = getFeature(false, Server.ADDRESS3);
        feature.getTargetSelector().setSupportNotAvailableErrorsOnly(true);
        strategyTestWebClient(Server.ADDRESS2 + "/new", feature, Server.ADDRESS3, null, false, false);
    }

    @Test
    public void testSequentialStrategyWith406() throws Exception {
        FailoverFeature feature = getFeature(false, Server.ADDRESS3);
        feature.getTargetSelector().setSupportNotAvailableErrorsOnly(false);
        strategyTestWebClientHttpError(Server.ADDRESS2, feature, Server.ADDRESS3, false);
    }

    @Test
    public void testSequentialStrategyWith406NoFailover() throws Exception {
        FailoverFeature feature = getFeature(false, Server.ADDRESS3);
        strategyTestWebClientHttpError(Server.ADDRESS2, feature, Server.ADDRESS3, true);
    }

    @Test
    public void testRandomStrategyWebClient() throws Exception {
        FailoverFeature feature =
            getFeature(true, Server.ADDRESS3, Server.ADDRESS2);
        strategyTestWebClient(Server.ADDRESS1, feature, Server.ADDRESS3, Server.ADDRESS2, false, true);
    }

    @Test
    public void testRandomStrategy() throws Exception {
        FailoverFeature feature =
            getFeature(true, Server.ADDRESS2, Server.ADDRESS3);
        strategyTest(Server.ADDRESS1, feature, Server.ADDRESS2, Server.ADDRESS3, false, true, true);
    }

    @Test
    public void testRandomStrategy2() throws Exception {
        FailoverFeature feature =
            getFeature(true, Server.ADDRESS2, Server.ADDRESS3);
        strategyTest(Server.ADDRESS1, feature, Server.ADDRESS2, Server.ADDRESS3, false, true, false);
    }

    @Test
    public void testSequentialStrategyWithDiffBaseAddresses() throws Exception {
        FailoverFeature feature =
            getFeature(false, Server.ADDRESS3, null);
        strategyTest(Server.ADDRESS1, feature, Server.ADDRESS3, Server.ADDRESS2, false, false, false);
    }
    @Test
    public void testSequentialStrategyWithDiffBaseAddresses2() throws Exception {
        FailoverFeature feature =
            getFeature(false, Server.ADDRESS3, null);
        strategyTest(Server.ADDRESS1, feature, Server.ADDRESS3, Server.ADDRESS2, false, false, true);
    }

    @Test(expected = InternalServerErrorException.class)
    public void testSequentialStrategyWithServerException() throws Exception {
        FailoverFeature feature =
            getFeature(false, Server.ADDRESS2, Server.ADDRESS3);
        strategyTest(Server.ADDRESS1, feature, Server.ADDRESS2, Server.ADDRESS3, true, false, false);
    }

    @Test(expected = ProcessingException.class)
    public void testSequentialStrategyFailure() throws Exception {
        FailoverFeature feature =
            getFeature(false, "http://localhost:" + NON_PORT + "/non-existent");
        strategyTest(Server.ADDRESS1, feature, null, null, false, false, false);
    }

    @Test
    public void testSequentialStrategyWithRetries() throws Exception {
        String address = "http://localhost:" + NON_PORT + "/non-existent";
        String address2 = "http://localhost:" + NON_PORT + "/non-existent2";

        CustomRetryStrategy strategy = new CustomRetryStrategy();
        strategy.setMaxNumberOfRetries(5);
        strategy.setAlternateAddresses(Arrays.asList(address, address2));

        FailoverFeature feature = new FailoverFeature();
        feature.setStrategy(strategy);

        BookStore store = getBookStore(address, feature);
        try {
            store.getBook("1");
            fail("Exception expected");
        } catch (ProcessingException ex) {
            assertEquals(10, strategy.getTotalCount());
            assertEquals(5, strategy.getAddressCount(address));
            assertEquals(5, strategy.getAddressCount(address2));
        }
    }

    protected abstract FailoverFeature getFeature(boolean random, String ...address);



    protected BookStore getBookStore(String address,
                                     FailoverFeature feature) throws Exception {
        JAXRSClientFactoryBean bean = createBean(address, feature);
        bean.setServiceClass(BookStore.class);
        return bean.create(BookStore.class);
    }

    protected WebClient getWebClient(String address,
                                     FailoverFeature feature) throws Exception {
        JAXRSClientFactoryBean bean = createBean(address, feature);
        return bean.createWebClient();
    }

    private static JAXRSClientFactoryBean createBean(String address,
                                                FailoverFeature feature) {
        final Bus bus = new ExtensionManagerBus();
        // Make sure default JSON-P/JSON-B providers are not loaded
        bus.setProperty(ProviderFactory.SKIP_JAKARTA_JSON_PROVIDERS_REGISTRATION, true);

        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        bean.setFeatures(Arrays.asList(feature));
        bean.setBus(bus);
        return bean;
    }

    protected void strategyTest(String inactiveReplica,
                                FailoverFeature feature,
                                String activeReplica1,
                                String activeReplica2,
                                boolean expectServerException,
                                boolean expectRandom,
                                boolean singleProxy) throws Exception {
        boolean randomized = false;
        String prevEndpoint = null;
        BookStore bookStore = null;

        if (singleProxy) {
            bookStore = getBookStore(inactiveReplica, feature);
        }

        for (int i = 0; i < 20; i++) {
            if (!singleProxy) {
                feature.getTargetSelector().close();
                bookStore = getBookStore(inactiveReplica, feature);
            }
            verifyStrategy(bookStore, expectRandom
                              ? RandomStrategy.class
                              : SequentialStrategy.class);
            Exception ex = null;
            try {
                if (expectServerException) {
                    bookStore.getBook("9999");
                    fail("Exception expected");
                } else {
                    final long bookId = 123L;
                    Book book = bookStore.echoBookElementJson(new Book("CXF", bookId));
                    assertNotNull("expected non-null response", book);
                    assertEquals("unexpected id", bookId, book.getId());
                }
            } catch (Exception error) {
                if (!expectServerException) {
                    //String currEndpoint = getCurrentEndpointAddress(bookStore);
                    //assertEquals(currEndpoint, inactiveReplica);
                    throw error;
                }
                ex = error;
            }
            String currEndpoint = getCurrentEndpointAddress(bookStore);
            assertNotEquals(currEndpoint, inactiveReplica);
            if (expectRandom) {
                assertTrue(currEndpoint.equals(activeReplica1) || currEndpoint.equals(activeReplica2));
            } else {
                assertEquals(activeReplica1, currEndpoint);
            }
            if (expectServerException) {
                assertNotNull(ex);
                throw ex;
            }

            if (!(prevEndpoint == null || currEndpoint.equals(prevEndpoint))) {
                randomized = true;
            }
            prevEndpoint = currEndpoint;
        }
        if (!singleProxy) {
            assertEquals("unexpected random/sequential distribution of failovers",
                         expectRandom,
                         randomized);
        }
    }

    protected void strategyTestWebClient(String inactiveReplica,
                                FailoverFeature feature,
                                String activeReplica1,
                                String activeReplica2,
                                boolean expectServerException,
                                boolean expectRandom) throws Exception {
        boolean randomized = false;
        String prevEndpoint = null;
        for (int i = 0; i < 20; i++) {
            feature.getTargetSelector().close();
            WebClient bookStore = getWebClient(inactiveReplica, feature);
            verifyStrategy(bookStore, expectRandom
                              ? RandomStrategy.class
                              : SequentialStrategy.class);
            String bookId = expectServerException ? "9999" : "123";
            bookStore.path("bookstore/books").path(bookId);
            Exception ex = null;
            try {
                Book book = bookStore.get(Book.class);
                assertNotNull("expected non-null response", book);
                assertEquals("unexpected id", 123L, book.getId());
            } catch (Exception error) {
                if (!expectServerException) {
                    throw error;
                }
                ex = error;
            }
            String currEndpoint = getCurrentEndpointAddress(bookStore);
            assertNotEquals(currEndpoint, inactiveReplica);
            if (expectRandom) {
                assertTrue(currEndpoint.equals(activeReplica1) || currEndpoint.equals(activeReplica2));
            } else {
                assertEquals(currEndpoint, activeReplica1);
            }
            if (expectServerException) {
                assertNotNull(ex);
                throw ex;
            }

            if (!(prevEndpoint == null || currEndpoint.equals(prevEndpoint))) {
                randomized = true;
            }
            prevEndpoint = currEndpoint;
        }
        assertEquals("unexpected random/sequential distribution of failovers",
                     expectRandom,
                     randomized);
    }

    protected void strategyTestWebClientHttpError(String currentReplica,
                                 FailoverFeature feature,
                                 String newReplica,
                                 boolean notAvailableOnly) throws Exception {
        WebClient bookStore = getWebClient(currentReplica, feature);
        verifyStrategy(bookStore, SequentialStrategy.class);
        bookStore.path("bookstore/webappexceptionXML");
        Response r = bookStore.get();
        assertEquals(406, r.getStatus());
        String currEndpoint = getCurrentEndpointAddress(bookStore);
        if (notAvailableOnly) {
            assertEquals(currEndpoint, currentReplica);
        } else {
            assertEquals(currEndpoint, newReplica);
        }
    }


    protected String getCurrentEndpointAddress(Object client) {
        String currentBaseURI = WebClient.client(client).getBaseURI().toString();
        String currentURI = WebClient.client(client).getCurrentURI().toString();
        assertTrue(currentURI.startsWith(currentBaseURI));
        return currentBaseURI;
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

    private static final class CustomRetryStrategy extends RetryStrategy {
        private int totalCount;
        private Map<String, Integer> map = new HashMap<>();
        @Override
        protected <T> T getNextAlternate(List<T> alternates) {
            totalCount++;
            T next = super.getNextAlternate(alternates);
            if (next != null) {
                String address = (String)next;
                Integer count = map.get(address);
                if (count == null) {
                    count = map.isEmpty() ? 1 /* count first call */ : 0;
                }
                count++;
                map.put(address, count);
            }
            return next;
        }

        public int getTotalCount() {
            return totalCount - 2;
        }

        public int getAddressCount(String address) {
            return map.get(address) - 1;
        }
    }
}
