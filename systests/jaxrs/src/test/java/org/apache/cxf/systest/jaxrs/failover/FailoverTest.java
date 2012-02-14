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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.cxf.clustering.FailoverTargetSelector;
import org.apache.cxf.clustering.RandomStrategy;
import org.apache.cxf.clustering.RetryStrategy;
import org.apache.cxf.clustering.SequentialStrategy;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.jaxrs.client.ClientWebApplicationException;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.ServerWebApplicationException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.features.clustering.FailoverFeature;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.systest.jaxrs.Book;
import org.apache.cxf.systest.jaxrs.BookStore;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests failover within a static cluster.
 */
public class FailoverTest extends AbstractBusClientServerTestBase {
    public static final String NON_PORT = allocatePort(FailoverTest.class);

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
    public void testSequentialStrategy() throws Exception {
        FailoverFeature feature = 
            getFeature(false, false, Server.ADDRESS2, Server.ADDRESS3); 
        strategyTest(Server.ADDRESS1, feature, Server.ADDRESS2, null, false, false, false);
    }
    
    @Test    
    public void testSequentialStrategyWithCustomTargetSelector() throws Exception {
        FailoverFeature feature = 
            getFeature(true, false, Server.ADDRESS2, Server.ADDRESS3); 
        strategyTest("resolver://info", feature, Server.ADDRESS3, null, false, false, false);
    }
    
    @Test    
    public void testSequentialStrategyWithCustomTargetSelector2() throws Exception {
        FailoverFeature feature = 
            getFeature(true, false, Server.ADDRESS2, Server.ADDRESS3); 
        strategyTest("resolver://info", feature, Server.ADDRESS3, null, false, false, true);
    }
    
    @Test
    public void testSequentialStrategyWebClient() throws Exception {
        FailoverFeature feature = 
            getFeature(false, false, Server.ADDRESS2, Server.ADDRESS3); 
        strategyTestWebClient(Server.ADDRESS1, feature, Server.ADDRESS2, null, false, false);
    }
    
    @Test
    public void testRandomStrategyWebClient() throws Exception {
        FailoverFeature feature = 
            getFeature(false, true, Server.ADDRESS3, Server.ADDRESS2); 
        strategyTestWebClient(Server.ADDRESS1, feature, Server.ADDRESS3, Server.ADDRESS2, false, true);
    }
    
    @Test    
    public void testRandomStrategy() throws Exception {
        FailoverFeature feature = 
            getFeature(false, true, Server.ADDRESS2, Server.ADDRESS3); 
        strategyTest(Server.ADDRESS1, feature, Server.ADDRESS2, Server.ADDRESS3, false, true, true);
    }
    
    @Test    
    public void testRandomStrategy2() throws Exception {
        FailoverFeature feature = 
            getFeature(false, true, Server.ADDRESS2, Server.ADDRESS3); 
        strategyTest(Server.ADDRESS1, feature, Server.ADDRESS2, Server.ADDRESS3, false, true, false);
    }
    
    @Test    
    public void testSequentialStrategyWithDiffBaseAddresses() throws Exception {
        FailoverFeature feature = 
            getFeature(false, false, Server.ADDRESS3, null); 
        strategyTest(Server.ADDRESS1, feature, Server.ADDRESS3, Server.ADDRESS2, false, false, false);
    }
    
    public void testSequentialStrategyWithDiffBaseAddresses2() throws Exception {
        FailoverFeature feature = 
            getFeature(false, false, Server.ADDRESS3, null); 
        strategyTest(Server.ADDRESS1, feature, Server.ADDRESS3, Server.ADDRESS2, false, false, true);
    }
    
    @Test(expected = ServerWebApplicationException.class)
    public void testSequentialStrategyWithServerException() throws Exception {
        FailoverFeature feature = 
            getFeature(false, false, Server.ADDRESS2, Server.ADDRESS3); 
        strategyTest(Server.ADDRESS1, feature, Server.ADDRESS2, Server.ADDRESS3, true, false, false);
    }
    
    @Test(expected = ClientWebApplicationException.class)    
    public void testSequentialStrategyFailure() throws Exception {
        FailoverFeature feature = 
            getFeature(false, false, "http://localhost:" + NON_PORT + "/non-existent"); 
        strategyTest(Server.ADDRESS1, feature, null, null, false, false, false);
    }
    
    @Test
    public void testSequentialStrategyWithRetries() throws Exception {
        String address = "http://localhost:" + NON_PORT + "/non-existent";
        String address2 = "http://localhost:" + NON_PORT + "/non-existent2";
        
        FailoverFeature feature = new FailoverFeature();
        List<String> alternateAddresses = new ArrayList<String>();
        alternateAddresses.add(address);
        alternateAddresses.add(address2);
        CustomRetryStrategy strategy = new CustomRetryStrategy();
        strategy.setMaxNumberOfRetries(5);
        strategy.setAlternateAddresses(alternateAddresses);
        feature.setStrategy(strategy);
            
        BookStore store = getBookStore(address, feature);
        try {
            store.getBook("1");
            fail("Exception expected");
        } catch (ClientWebApplicationException ex) {
            assertEquals(10, strategy.getTotalCount());
            assertEquals(5, strategy.getAddressCount(address));
            assertEquals(5, strategy.getAddressCount(address2));
        }
    }
    

    private FailoverFeature getFeature(boolean custom, boolean random, String ...address) {
        FailoverFeature feature = new FailoverFeature();
        List<String> alternateAddresses = new ArrayList<String>();
        for (String s : address) {
            alternateAddresses.add(s);
        }
        if (!random) {
            SequentialStrategy strategy = new SequentialStrategy();
            strategy.setAlternateAddresses(alternateAddresses);
            feature.setStrategy(strategy);
        } else {
            RandomStrategy strategy = new RandomStrategy();
            strategy.setAlternateAddresses(alternateAddresses);
            feature.setStrategy(strategy);
        }
        if (custom) {
            FailoverTargetSelector selector = new ReplaceInitialAddressSelector(); 
            feature.setTargetSelector(selector);
        }
        
        return feature;
    }
    
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
    
    protected JAXRSClientFactoryBean createBean(String address, 
                                                FailoverFeature feature) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        List<AbstractFeature> features = new ArrayList<AbstractFeature>();
        features.add(feature);
        bean.setFeatures(features);
        
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
                bookStore = getBookStore(inactiveReplica, feature);
            }
            verifyStrategy(bookStore, expectRandom 
                              ? RandomStrategy.class
                              : SequentialStrategy.class);
            String bookId = expectServerException ? "9999" : "123";
            Exception ex = null;
            try {
                Book book = bookStore.getBook(bookId);
                assertNotNull("expected non-null response", book);
                assertEquals("unexpected id", 123L, book.getId());
            } catch (Exception error) {
                if (!expectServerException) {
                    //String currEndpoint = getCurrentEndpointAddress(bookStore);
                    //assertTrue(currEndpoint.equals(inactiveReplica));
                    throw error;
                }
                ex = error;
            }
            String currEndpoint = getCurrentEndpointAddress(bookStore);
            assertFalse(currEndpoint.equals(inactiveReplica));
            if (expectRandom) {
                assertTrue(currEndpoint.equals(activeReplica1) || currEndpoint.equals(activeReplica2));
            } else {
                assertTrue(currEndpoint.equals(activeReplica1));
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
            assertFalse(currEndpoint.equals(inactiveReplica));
            if (expectRandom) {
                assertTrue(currEndpoint.equals(activeReplica1) || currEndpoint.equals(activeReplica2));
            } else {
                assertTrue(currEndpoint.equals(activeReplica1));
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

    
    protected String getCurrentEndpointAddress(Object client) {
        return WebClient.client(client).getBaseURI().toString();
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
    
    private static class ReplaceInitialAddressSelector extends FailoverTargetSelector {
        @Override
        public synchronized void prepare(Message message) {
            EndpointInfo ei = getEndpoint().getEndpointInfo();
            ei.setAddress(Server.ADDRESS3); 
            message.put(Message.ENDPOINT_ADDRESS, Server.ADDRESS3);
            super.prepare(message);
        }
        
        @Override
        protected boolean requiresFailover(Exchange exchange) {
            return false;
        }
    }
    
    private static class CustomRetryStrategy extends RetryStrategy {
        private int totalCount;
        private Map<String, Integer> map = new HashMap<String, Integer>(); 
        @Override
        protected <T> T getNextAlternate(List<T> alternates) {
            totalCount++;
            T next = super.getNextAlternate(alternates);
            String address = (String)next;
            Integer count = map.get(address);
            if (count == null) {
                count = 0; 
            }
            count++;
            map.put(address, count);
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
