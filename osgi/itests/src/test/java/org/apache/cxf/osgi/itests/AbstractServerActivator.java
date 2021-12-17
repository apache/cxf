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
package org.apache.cxf.osgi.itests;

import org.apache.cxf.endpoint.Server;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

public abstract class AbstractServerActivator implements BundleActivator {

    private Server server;

    public static void awaitService(BundleContext bundleContext, String filter, long timeout)
            throws InvalidSyntaxException, InterruptedException {
        Filter serviceFilter = bundleContext.createFilter(filter);
        ServiceTracker<Object, ?> tracker = new ServiceTracker<>(bundleContext, serviceFilter, null);
        tracker.open();
        Object service = tracker.waitForService(timeout);
        tracker.close();
        if (service == null) {
            throw new IllegalStateException("Expected service with filter " + filter + " was not found");
        }
    }

    private static void awaitCxfServlet(BundleContext bundleContext)
            throws InvalidSyntaxException, InterruptedException {
        awaitService(bundleContext, "(" + Constants.OBJECTCLASS + "=jakarta.servlet.ServletContext)", 60000L);
    }

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        awaitCxfServlet(bundleContext);
        server = createServer();
    }

    protected abstract Server createServer();

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        server.destroy();
    }

}
