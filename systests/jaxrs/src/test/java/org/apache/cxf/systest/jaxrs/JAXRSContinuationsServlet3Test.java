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

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.ServerLauncher;
import org.junit.BeforeClass;
import org.junit.Test;


public class JAXRSContinuationsServlet3Test extends AbstractJAXRSContinuationsTest {
    public static final String PORT = BookContinuationServlet3Server.PORT;
   
    
    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        createStaticBus();
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
        Thread.sleep(4000);
    }

    
    protected String getBaseAddress() {
        return "/async/bookstore";
    }
    
    protected String getPort() {
        return PORT;
    }
}
