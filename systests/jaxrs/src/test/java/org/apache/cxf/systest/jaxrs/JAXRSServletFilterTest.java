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

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSServletFilterTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookServerServletFilter.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly",
                   launchServer(BookServerServletFilter.class, true));
        createStaticBus();
    }


    @Test
    public void testServletConfigInitParam() throws Exception {

        String endpointAddress =
            "http://localhost:" + PORT + "/webapp/filter/resources/servlet/config/query?name=a";
        WebClient wc = WebClient.create(endpointAddress);
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        wc.accept("text/plain");

        assertEquals("avalue", wc.get(String.class));
    }
}
