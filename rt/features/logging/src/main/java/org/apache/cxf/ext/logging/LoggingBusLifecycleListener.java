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

package org.apache.cxf.ext.logging;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;

/**
 * Add LoggingFeature based on system properties
 */
public class LoggingBusLifecycleListener implements BusLifeCycleListener {
    
    static final boolean FORCE_LOGGING;
    static final boolean FORCE_PRETTY;
    static {
        boolean b = false;
        boolean pretty = false;
        try {
            String prop = System.getProperty("org.apache.cxf.logging.enabled", "false");
            if ("pretty".equals(prop)) {
                b = true;
                pretty = true;
            } else {
                b = Boolean.parseBoolean(prop);
                //treat these all the same
                b |= Boolean.getBoolean("com.sun.xml.ws.transport.local.LocalTransportPipe.dump");
                b |= Boolean.getBoolean("com.sun.xml.ws.util.pipe.StandaloneTubeAssembler.dump");
                b |= Boolean.getBoolean("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump");
                b |= Boolean.getBoolean("com.sun.xml.ws.transport.http.HttpAdapter.dump");
            }
        } catch (Throwable t) {
            //ignore
        }
        FORCE_LOGGING = b;
        FORCE_PRETTY = pretty;
    }

    private final Bus bus;
    public LoggingBusLifecycleListener(Bus b) {
        bus = b;
        bus.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);
    }

    /** {@inheritDoc}*/
    @Override
    public void initComplete() {
        if (FORCE_LOGGING) {
            LoggingFeature feature = new LoggingFeature();
            feature.setPrettyLogging(FORCE_PRETTY);
            bus.getFeatures().add(feature);
            feature.initialize(bus);
        }
    }

    /** {@inheritDoc}*/
    @Override
    public void preShutdown() {
    }

    /** {@inheritDoc}*/
    @Override
    public void postShutdown() {
    }

}
