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

package minimalosgi;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {

    private final String _path = "/";
    private BundleContext _context;
    private ServiceTracker _tracker;

    public void start(BundleContext context) throws Exception {

        _context = context;

        // Use _tracker to capture when a HttpService comes and goes.
        //
        // When this bundle is started, a HttpService may not be alive. Thus, we use
        // ServiceTracker to automatically monitor when a HttpService comes alive and
        // then register this our CXF-based JAX-RS service with it.
        //
        _tracker = new ServiceTracker(
            _context,
            HttpService.class.getName(),
            new ServiceTrackerCustomizer() {
                public Object addingService(ServiceReference serviceReference) {
                    try {
                        HttpService service = (HttpService)_context.getService(serviceReference);
                        Dictionary<String, String> initParams = new Hashtable<>();
                        initParams.put("jakarta.ws.rs.Application", SampleApplication.class.getName());
                        service.registerServlet(_path, new SampleServlet(), initParams, null);
                        return service;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        throw new RuntimeException(ex);
                    }
                }

                public void modifiedService(ServiceReference serviceReference, Object o) {
                    // do nothing
                }

                public void removedService(ServiceReference serviceReference, Object o) {
                    HttpService service = (HttpService)_context.getService(serviceReference);
                    if (service != null) {
                        service.unregister(_path);
                    }
                }
            }
        );
        _tracker.open();

    }

    public void stop(BundleContext bundleContext) throws Exception {
        _tracker.remove(_tracker.getServiceReference());
    }

}
