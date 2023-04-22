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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.client.AsyncInvoker;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.ServerLauncher;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSContinuationsServlet3Test extends AbstractJAXRSContinuationsTest {

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(BookContinuationServlet3Server.class));
    }

    @Test
    public void testTimeoutAndCancelAsyncExecutor() throws Exception {
        doTestTimeoutAndCancel("/asyncexecutor/bookstore");
    }
    @Test
    public void testGetBookUnmappedFromFilter() throws Exception {
        WebClient wc =
            WebClient.create("http://localhost:" + getPort() + getBaseAddress()
                             + "/books/unmappedFromFilter");
        wc.accept("text/plain");
        Response r = wc.get();
        assertEquals(500, r.getStatus());
    }
    @Test
    public void testClientDisconnect() throws Exception {
        ServerLauncher launcher = new ServerLauncher(BookContinuationClient.class.getName());
        assertTrue("server did not launch correctly", launcher.launchServer());
        Thread.sleep(4000L);
    }

    @Test
    public void testCancelVoidOnResumedTest() throws Exception {
        String base = "http://localhost:" + getPort() + "/async/resource/";
        String expectedResponse = "Expected response";
        Future<Response> suspend = invokeRequest(base + "suspend");
        Future<Response> resume = invokeRequest(base + "resume?stage=0", expectedResponse);
        assertString(resume, AsyncResource.TRUE);
        assertString(suspend, expectedResponse);
        Future<Response> cancel = invokeRequest(base + "cancelvoid?stage=1");
        assertString(cancel, AsyncResource.FALSE);
    }

    @Test
    public void testLostThrowFromSuspendedCall() throws Exception {
        String base = "http://localhost:" + getPort() + "/async/resource/";
        Future<Response> suspend = invokeRequest(base + "suspendthrow");
        Response response = suspend.get(10, TimeUnit.SECONDS);
        assertEquals(502, response.getStatus());
    }

    @Test
    public void testSuspendSetTimeout() throws Exception {
        final String base = "http://localhost:" + getPort() + "/async/resource2/";
        Future<Response> suspend = invokeRequest(base + "suspend");

        Future<Response> timeout = invokeRequest(base + "setTimeOut");
        assertString(timeout, "true");

        assertEquals(503, suspend.get().getStatus());
    }

    private static void assertString(Future<Response> future, String check) throws Exception {
        Response response = future.get();
        String content = response.readEntity(String.class);
        assertEquals(content, Status.OK.getStatusCode(), response.getStatus());
        assertEquals(check, content);
    }

    private static <T> Future<Response> invokeRequest(String resource, T entity) {
        AsyncInvoker async = createAsyncInvoker(resource);
        return async.post(Entity.entity(entity, MediaType.TEXT_PLAIN_TYPE));
    }

    private static Future<Response> invokeRequest(String resource) {
        AsyncInvoker async = createAsyncInvoker(resource);
        return async.get();
    }

    private static AsyncInvoker createAsyncInvoker(String resource) {
        WebTarget target = ClientBuilder.newClient().target(resource);
        return target.request().async();
    }

    protected String getBaseAddress() {
        return "/async/bookstore";
    }
    protected String getBaseAddress2() {
        return "/async2/bookstore";
    }

    protected String getPort() {
        return BookContinuationServlet3Server.PORT;
    }
}
