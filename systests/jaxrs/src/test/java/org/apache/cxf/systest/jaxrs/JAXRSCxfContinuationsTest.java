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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSCxfContinuationsTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookCxfContinuationServer.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        createStaticBus();
        assertTrue("server did not launch correctly",
                   launchServer(BookCxfContinuationServer.class));
    }

    @Test
    public void testContinuation() throws Exception {

        doTestContinuation("books");
    }

    @Test
    public void testContinuationSubresource() throws Exception {

        doTestContinuation("books/subresources");
    }

    private void doTestContinuation(String pathSegment) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
                                                             new ArrayBlockingQueue<Runnable>(10));
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(1);

        executor.execute(new BookWorker("http://localhost:" + PORT + "/bookstore/" + pathSegment + "/1",
                                        "1",
                                        "CXF in Action1", startSignal, doneSignal));
        startSignal.countDown();
        doneSignal.await(60, TimeUnit.SECONDS);
        executor.shutdownNow();
        assertEquals("Not all invocations have completed", 0, doneSignal.getCount());
    }

    private void checkBook(String address, String id, String expected) throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(address);

        try {
            CloseableHttpResponse response = client.execute(get);
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals("Book description for id " + id + " is wrong",
                         expected, EntityUtils.toString(response.getEntity()));
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

        public void run() {

            try {
                startSignal.await();
                checkBook(address, id, expected);
                doneSignal.countDown();
            } catch (InterruptedException ex) {
                // ignore
            } catch (Exception ex) {
                ex.printStackTrace();
                Assert.fail("Book thread failed for : " + id);
            }

        }

    }

    @Test
    public void testEncodedURL() throws Exception {
        String id = "A%20B%20C"; // "A B C"
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + PORT + "/bookstore/books/" + id);

        try {
            CloseableHttpResponse response = client.execute(get);
            assertEquals("Encoded path '/" + id + "' is not handled successfully",
                         200,  response.getStatusLine().getStatusCode());
            assertEquals("Book description for id " + id + " is wrong",
                         "CXF in Action A B C", EntityUtils.toString(response.getEntity()));
        } finally {
            // Release current connection to the connection pool once you are done
            get.releaseConnection();
        }
    }
}
