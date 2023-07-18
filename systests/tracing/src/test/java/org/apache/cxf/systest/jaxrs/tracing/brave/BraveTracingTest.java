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

import static org.junit.Assert.assertTrue;

import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.tracing.brave.BraveClientFeature;
import org.apache.cxf.tracing.brave.jaxrs.BraveClientProvider;
import org.apache.cxf.tracing.brave.jaxrs.BraveFeature;
import org.junit.BeforeClass;

import brave.Tracing;

public class BraveTracingTest extends AbstractBraveTracingTest {

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly",
                   launchServer(JustBraveServer.class, true));
    }

    public static class JustBraveServer extends BraveServer {

        @Override
        Object getProvider(Tracing tracing) {
            return new BraveFeature(tracing);
        }
    }

    @Override
    protected BraveClientProvider getClientProvider(Tracing brave) {
        return new BraveClientProvider(brave);
    }

    @Override
    protected BraveClientFeature getClientFeature(Tracing brave) {
        return new BraveClientFeature(brave);
    }
}
