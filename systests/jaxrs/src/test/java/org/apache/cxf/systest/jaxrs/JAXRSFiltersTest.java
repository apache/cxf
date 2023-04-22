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

import java.util.Collections;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSFiltersTest extends AbstractClientServerTestBase {

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        assertTrue("server did not launch correctly", launchServer(AppServer.class));
    }

    public static class AppServer extends AbstractServerTestServerBase {
        public static final String PORT = allocatePort(BookServer.class);

        @Override
        protected Server createServer(Bus bus) throws Exception {
            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(ApplicationController.class);
            sf.setProviders(Collections.singletonList(new ApplicationInfoJaxrsFilter()));
            sf.setResourceProvider(ApplicationController.class,
                                   new SingletonResourceProvider(new ApplicationController()));
            sf.setAddress("http://localhost:" + PORT + "/info");
            return sf.create();
        }
    }

    @Test
    public void testPostMatchingFilterOnSubresource() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + AppServer.PORT + "/info/app/nav");
        wc.accept("text/plain");
        assertEquals("FilteredApplicationInfo", wc.get(String.class));
    }
}
