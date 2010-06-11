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

package org.apache.cxf.systest.jaxrs;

import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JAXRSMultithreadedClientTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServer.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookServer.class));
    }
    
    @Test
    public void testStatefulWebClientWithCopy() throws Exception {
        runWebClients(WebClient.create("http://localhost:" + PORT + "/bookstore"), 10, false, true);
    }
    
    @Test
    public void testStatefulWebClientThreadLocal() throws Exception {
        runWebClients(WebClient.create("http://localhost:" + PORT + "/bookstore", true), 10, true, true);
    }
    
    @Test
    public void testStatefulWebClientThreadLocalWithCopy() throws Exception {
        runWebClients(WebClient.create("http://localhost:" + PORT + "/bookstore", true), 10, false, true);
    }
    
    @Test
    public void testSimpleWebClient() throws Exception {
        WebClient client = WebClient.create("http://localhost:" + PORT + "/bookstore/booksecho");
        client.type("text/plain").accept("text/plain").header("CustomHeader", "CustomValue");
        runWebClients(client, 10, true, false);
    }
    
    @Test
    public void testSimpleProxy() throws Exception {
        BookStore proxy = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class);
        runProxies(proxy, 10, true, false);
    }
    
    @Test
    public void testThreadSafeProxy() throws Exception {
        BookStore proxy = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class,
                                                    Collections.emptyList(), true);
        runProxies(proxy, 10, true, true);
    }
    
    @Test
    public void testThreadSafeProxyWithCopy() throws Exception {
        BookStore proxy = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class,
                                                    Collections.emptyList(), true);
        runProxies(proxy, 10, false, true);
    }
    
    @Test
    public void testThreadSafeSubProxy() throws Exception {
        BookStore proxy = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class,
                                                    Collections.emptyList(), true);
        
        runProxies(proxy.echoThroughBookStoreSub(), 10, true, true);
    }
    
    private void runWebClients(WebClient client, int numberOfClients, 
        boolean threadSafe, boolean stateCanBeChanged) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS,
                                                             new ArrayBlockingQueue<Runnable>(10));
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numberOfClients);
        
        for (int i = 1; i <= numberOfClients; i++) {
            WebClient wc = !threadSafe ? WebClient.fromClient(client) : client;
            String bookName = stateCanBeChanged ? Integer.toString(i) : "TheBook";
            String bookHeader = stateCanBeChanged ? "value" + i : "CustomValue";
            
            executor.execute(new WebClientWorker(wc, bookName, bookHeader, 
                             startSignal, doneSignal, stateCanBeChanged));
        }
        startSignal.countDown();
        doneSignal.await(60, TimeUnit.SECONDS);
        executor.shutdownNow();
        assertEquals("Not all invocations have completed", 0, doneSignal.getCount());
    }
    
    private void runProxies(BookStore proxy, int numberOfClients, 
                            boolean threadSafe, boolean stateCanBeChanged) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS,
                                                            new ArrayBlockingQueue<Runnable>(10));
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(numberOfClients);
       
        for (int i = 1; i <= numberOfClients; i++) {
            // here we do a double copy : from proxy to web client and back to proxy  
            BookStore bs = !threadSafe ? JAXRSClientFactory.fromClient(
                   WebClient.fromClient(WebClient.client(proxy)), BookStore.class) : proxy;
            String bookName = stateCanBeChanged ? Integer.toString(i) : "TheBook";
            String bookHeader = stateCanBeChanged ? "value" + i : "CustomValue";
           
            executor.execute(new RootProxyWorker(bs, bookName, bookHeader, 
                            startSignal, doneSignal, stateCanBeChanged));
        }
        startSignal.countDown();
        doneSignal.await(60, TimeUnit.SECONDS);
        executor.shutdownNow();
        assertEquals("Not all invocations have completed", 0, doneSignal.getCount());
    }
    
    @Ignore
    private class WebClientWorker implements Runnable {

        private WebClient client;
        private String bookName;
        private String bookHeader;
        private CountDownLatch startSignal;
        private CountDownLatch doneSignal;
        private boolean stateCanBeChanged;
        
        public WebClientWorker(WebClient client,
                               String bookName,
                               String bookHeader,
                               CountDownLatch startSignal,
                               CountDownLatch doneSignal,
                               boolean stateCanBeChanged) {
            this.client = client;
            this.bookName = bookName;
            this.bookHeader = bookHeader;
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;
            this.stateCanBeChanged = stateCanBeChanged;
        }
        
        public void run() {
            
            try {
                startSignal.await();
                
                for (int i = 0; i < 5; i++) {
                    if (stateCanBeChanged) {
                        invoke(i);
                    } else {
                        doInvoke(bookName, bookHeader);
                    }
                }
                
                doneSignal.countDown();
            } catch (InterruptedException ex) {
                // ignore
            } catch (Exception ex) {
                ex.printStackTrace();
                Assert.fail("WebClientWorker thread failed for " + bookName + "," + bookHeader);
            } 
            
        }
        
        private void invoke(int ind) throws Exception {
            client.type("text/plain").accept("text/plain");
            
            String actualHeaderName = bookHeader + ind;
            String actualBookName = bookName + ind;
            
            MultivaluedMap<String, String> map = client.getHeaders();
            map.putSingle("CustomHeader", actualHeaderName);
            client.headers(map).path("booksecho");
                        
            doInvoke(actualBookName, actualHeaderName);
            
            // reset current path
            client.back(true);
        }
        
        private void doInvoke(String actualBookName, String actualHeaderName) throws Exception {
            Response response = client.post(actualBookName);
            
            assertEquals(actualHeaderName, 
                response.getMetadata().getFirst("CustomHeader").toString());
            String responseValue = IOUtils.readStringFromStream((InputStream)response.getEntity());
            assertEquals(actualBookName, responseValue);
        }
    }
    
    @Ignore
    private class RootProxyWorker implements Runnable {

        private BookStore proxy;
        private String bookName;
        private String bookHeader;
        private CountDownLatch startSignal;
        private CountDownLatch doneSignal;
        private boolean stateCanBeChanged;
        
        public RootProxyWorker(BookStore proxy,
                               String bookName,
                               String bookHeader,
                               CountDownLatch startSignal,
                               CountDownLatch doneSignal,
                               boolean stateCanBeChanged) {
            this.proxy = proxy;
            this.bookName = bookName;
            this.bookHeader = bookHeader;
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;
            this.stateCanBeChanged = stateCanBeChanged;
        }
        
        public void run() {
            
            try {
                startSignal.await();
                
                for (int i = 0; i < 5; i++) {
                    invoke(i);
                }
                
                doneSignal.countDown();
            } catch (InterruptedException ex) {
                // ignore
            } catch (Exception ex) {
                ex.printStackTrace();
                Assert.fail("WebClientWorker thread failed for " + bookName + "," + bookHeader);
            } 
            
        }
        
        private void invoke(int ind) throws Exception {
            
            String actualHeaderName = bookHeader + ind;
            String actualBookName = bookName + ind;
            
            if (stateCanBeChanged) {
                Client c = WebClient.client(proxy);
                MultivaluedMap<String, String> map = c.getHeaders();
                map.putSingle("CustomHeader", actualHeaderName);
                c.headers(map);
                proxy.echoBookNameAndHeader2(actualBookName);
                verifyResponse(c.getResponse(), actualBookName, actualHeaderName);
            } else {
                verifyResponse(proxy.echoBookNameAndHeader(actualHeaderName, actualBookName),
                               actualBookName, actualHeaderName);
            }
        }
        
        private void verifyResponse(Response response, String actualBookName, String actualHeaderName) 
            throws Exception { 
            assertEquals(actualHeaderName, 
                         response.getMetadata().getFirst("CustomHeader").toString());
            String responseValue = IOUtils.readStringFromStream((InputStream)response.getEntity());
            assertEquals(actualBookName, responseValue);
        }
    }
}
