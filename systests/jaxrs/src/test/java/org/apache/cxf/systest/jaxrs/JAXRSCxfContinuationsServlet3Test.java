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

import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSCxfContinuationsServlet3Test extends AbstractBusClientServerTestBase {
    public static final String PORT = BookCxfContinuationServlet3Server.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        createStaticBus();
        assertTrue("server did not launch correctly",
                   launchServer(BookCxfContinuationServlet3Server.class));
    }

    @Test
    public void testEncodedURL() throws Exception {
        String id = "A%20B%20C"; // "A B C"
        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:" + PORT + "/bookstore/books/" + id);

        try {
            CloseableHttpResponse response = client.execute(get);
            assertEquals("Encoded path '/" + id + "' is not handled successfully",
                         200, response.getStatusLine().getStatusCode());
            assertEquals("Book description for id " + id + " is wrong",
                         "CXF in Action A B C", EntityUtils.toString(response.getEntity()));
        } finally {
            // Release current connection to the connection pool once you are done
            get.releaseConnection();
        }
    }
}
