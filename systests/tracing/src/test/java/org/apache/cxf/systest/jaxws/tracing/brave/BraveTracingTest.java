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
package org.apache.cxf.systest.jaxws.tracing.brave;


import brave.Tracing;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.systest.brave.TestSpanHandler;
import org.apache.cxf.systest.brave.jaxws.AbstractBraveTracingTest;
import org.apache.cxf.testutil.common.AbstractTestServerBase;
import org.apache.cxf.tracing.brave.BraveClientFeature;
import org.apache.cxf.tracing.brave.BraveFeature;

import org.junit.After;
import org.junit.BeforeClass;

import static org.junit.Assert.assertTrue;

public class BraveTracingTest extends AbstractBraveTracingTest {
    public static final String PORT = allocatePort(BraveTracingTest.class);

    public static class Server extends AbstractTestServerBase {

        private org.apache.cxf.endpoint.Server server;

        @Override
        protected void run() {
            final Tracing brave = Tracing.newBuilder()
                .localServiceName("book-store")
                .addSpanHandler(new TestSpanHandler())
                .build();

            final JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
            sf.setServiceClass(BookStore.class);
            sf.setAddress("http://localhost:" + PORT);
            sf.getFeatures().add(new BraveFeature(brave));
            server = sf.create();
        }

        @Override
        public void tearDown() throws Exception {
            server.destroy();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @After
    public void tearDown() {
        TestSpanHandler.clear();
    }

    @Override
    protected int getPort() {
        return Integer.parseInt(PORT);
    }

    @Override
    protected Feature getClientFeature(Tracing tracing) {
        return new BraveClientFeature(tracing);
    }
}
