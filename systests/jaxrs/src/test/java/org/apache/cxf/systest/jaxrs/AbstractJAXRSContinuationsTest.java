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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.Ignore;
import org.junit.Test;


public abstract class AbstractJAXRSContinuationsTest extends AbstractBusClientServerTestBase {
    
    @Test
    public void testDefaultTimeout() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + getPort() + getBaseAddress() + "/books/defaulttimeout");
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        Response r = wc.get();
        assertEquals(503, r.getStatus());
    }
    
    @Test
    public void testImmediateResume() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + getPort() + getBaseAddress() + "/books/resume");
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        wc.accept("text/plain");
        String str = wc.get(String.class);
        assertEquals("immediateResume", str);
    }
    
    @Test
    public void testUnmappedAfterTimeout() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + getPort() + getBaseAddress() + "/books/suspend/unmapped");
        Response r = wc.get();
        assertEquals(500, r.getStatus());
    }
    
    @Test
    public void testImmediateResumeSubresource() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + getPort() 
                                        + getBaseAddress() + "/books/subresources/books/resume");
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        wc.accept("text/plain");
        String str = wc.get(String.class);
        assertEquals("immediateResume", str);
    }
    
    @Test
    public void testGetBookNotFound() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + getPort() + getBaseAddress() + "/books/notfound");
        wc.accept("text/plain");
        Response r = wc.get();
        assertEquals(404, r.getStatus());
    }
    
    @Test
    public void testGetBookNotFoundUnmapped() throws Exception {
        WebClient wc = 
            WebClient.create("http://localhost:" + getPort() + getBaseAddress() + "/books/notfound/unmapped");
        wc.accept("text/plain");
        Response r = wc.get();
        assertEquals(500, r.getStatus());
    }
    
    @Test
    public void testTimeoutAndCancel() throws Exception {
        doTestTimeoutAndCancel(getBaseAddress());
    }
    
    protected void doTestTimeoutAndCancel(String baseAddress) throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + getPort() + baseAddress + "/books/cancel");
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        Response r = wc.get();
        assertEquals(503, r.getStatus());
        String retryAfter = r.getHeaderString(HttpHeaders.RETRY_AFTER);
        assertNotNull(retryAfter);
        assertEquals("10", retryAfter);
    }
    
    @Test
    public void testContinuationWithTimeHandler() throws Exception {
        
        doTestContinuation("/books/timeouthandler");
    }
    
    @Test
    public void testContinuationWithTimeHandlerResumeOnly() throws Exception {
        
        doTestContinuation("/books/timeouthandlerresume");
    }
    
    @Test
    public void testContinuation() throws Exception {
        
        doTestContinuation("/books");
    }
    
    @Test
    public void testContinuationSubresource() throws Exception {
        
        doTestContinuation("/books/subresources");
    }
    
    protected void doTestContinuation(String pathSegment) throws Exception {
        final String port = getPort();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS,
                                                             new ArrayBlockingQueue<Runnable>(10));
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(1);
        List<BookWorker> workers = new ArrayList<BookWorker>(5);
        for (int x = 1; x < 6; x++) {
            workers.add(new BookWorker("http://localhost:" + port + getBaseAddress() + pathSegment + "/" + x, 
                                       Integer.toString(x), 
                                       "CXF in Action" + x, startSignal, doneSignal));
        }
        for (BookWorker w : workers) {
            executor.execute(w);
        }
        
        startSignal.countDown();
        doneSignal.await(60, TimeUnit.SECONDS);
        executor.shutdownNow();       
        assertEquals("Not all invocations have completed", 0, doneSignal.getCount());
        for (BookWorker w : workers) {
            w.checkError();
        }
    }
    
    private void checkBook(String address, String id, String expected) throws Exception {
        GetMethod get = new GetMethod(address);
        HttpClient httpclient = new HttpClient();
        try {
            int result = httpclient.executeMethod(get);
            assertEquals(200, result);
            assertEquals("Book description for id " + id + " is wrong",
                         expected, IOUtils.toString(get.getResponseBodyAsStream()));
        } finally {
            // Release current connection to the connection pool once you are done
            get.releaseConnection();
        }
    }
    
    @Ignore
    private class BookWorker implements Runnable {

        private String address;
        private String id;
        private String expected;
        private CountDownLatch startSignal;
        private CountDownLatch doneSignal;
        private Exception error;
        BookWorker(String address,
                          String id,
                          String expected,
                           CountDownLatch startSignal,
                           CountDownLatch doneSignal) {
            this.address = address;
            this.id = id;
            this.expected = expected;
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;
        }
        
        public void checkError() throws Exception {
            if (error != null) {
                throw error;
            }
        }

        public void run() {
            
            try {
                startSignal.await();
                checkBook(address, id, expected);
                doneSignal.countDown();
            } catch (InterruptedException ex) {
                // ignore
            } catch (Exception ex) {
                ex.fillInStackTrace();
                error = ex;
            } 
            
        }
        
    }
    
    protected String getBaseAddress() {
        return "/bookstore";
    }
    
    protected abstract String getPort();
    
}
