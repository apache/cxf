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
package org.apache.cxf.systest.jaxrs.tracing.brave;

import brave.Tracing;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.systest.brave.TestSpanHandler;
import org.apache.cxf.systest.brave.jaxrs.AbstractBraveTracingTest;
import org.apache.cxf.systest.jaxrs.tracing.BookStore;
import org.apache.cxf.systest.jaxrs.tracing.NullPointerExceptionMapper;
import org.apache.cxf.testutil.common.AbstractTestServerBase;
import org.apache.cxf.tracing.brave.BraveClientFeature;
import org.apache.cxf.tracing.brave.TraceScope;
import org.apache.cxf.tracing.brave.jaxrs.BraveClientProvider;
import org.apache.cxf.tracing.brave.jaxrs.BraveFeature;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

import org.junit.BeforeClass;

import static org.junit.Assert.assertTrue;

public class BraveTracingTest extends AbstractBraveTracingTest {
    public static final String PORT = allocatePort(BraveTracingTest.class);

    public static class BraveServer extends AbstractTestServerBase {
        private org.apache.cxf.endpoint.Server server;

        @Override
        protected void run() {
            final Tracing brave = Tracing
                    .newBuilder()
                    .addSpanHandler(new TestSpanHandler())
                    .build();

            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(BookStore.class);
            sf.setResourceProvider(BookStore.class, new SingletonResourceProvider(new BookStore<TraceScope>()));
            sf.setAddress("http://localhost:" + PORT);
            sf.setProvider(new JacksonJsonProvider());
            sf.setProvider(new BraveFeature(brave));
            sf.setProvider(new NullPointerExceptionMapper());
            server = sf.create();
        }

        @Override
        public void tearDown() throws Exception {
            server.destroy();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly",
                   launchServer(BraveServer.class, true));
    }

    @Override
    protected BraveClientProvider getClientProvider(Tracing brave) {
        return new BraveClientProvider(brave);
    }

    @Override
    protected BraveClientFeature getClientFeature(Tracing brave) {
        return new BraveClientFeature(brave);
    }
    
    @Override
    protected int getPort() {
        return Integer.parseInt(PORT);
    }
}
