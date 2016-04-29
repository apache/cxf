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
package org.apache.cxf.transport.http.asyncclient;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.transport.http.HTTPConduitFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {

    private static final String ASYNC_CLIENT_PID = "org.apache.cxf.transport.http.async";

    private ServiceTracker tracker;

    private BundleContext bundleContext;
    private AsyncHTTPConduitFactory conduitFactory;
    private ServiceRegistration reg;

    @Override
    public void start(BundleContext context) throws Exception {
        this.bundleContext = context;
        tracker = new ServiceTracker(context, Bus.class.getName(), new BusTracker());
        tracker.open();
        ConduitConfigurer conduitConfigurer = new ConduitConfigurer();
        registerManagedService(context, conduitConfigurer, ASYNC_CLIENT_PID);
    }

    private void registerManagedService(BundleContext context, ConduitConfigurer conduitConfigurer, String servicePid) {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_PID, servicePid);
        context.registerService(ManagedService.class.getName(), conduitConfigurer, properties);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        tracker.close();
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private void registerConduitFactory(Bus bus, Dictionary properties) {
        unregisterConduitFactory();
        if (bus != null) {
            conduitFactory = new AsyncHTTPConduitFactory(bus);
            conduitFactory.update(toMap(properties));
            reg = bundleContext.registerService(HTTPConduitFactory.class.getName(), conduitFactory, null);
        }
    }
    
    private void unregisterConduitFactory() {
        if (reg != null) {
            reg.unregister();
            reg = null;
        }
    }
    
    private Map<String, Object> toMap(Dictionary<String, ?> properties) {
        Map<String, Object> props = new HashMap<String, Object>();
        if (properties == null) {
            return props;
        }
        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            props.put(key, properties.get(key));
        }
        return props;
    }
    
    class BusTracker implements ServiceTrackerCustomizer {

        @Override
        public Object addingService(ServiceReference reference) {
            Bus bus = (Bus)bundleContext.getService(reference);
            registerConduitFactory(bus, getConfig());
            return bus;
        }
        
        private Dictionary<?, ?> getConfig() {
            ServiceReference configurationAdminReference = 
                    bundleContext.getServiceReference(ConfigurationAdmin.class.getName());

            if (configurationAdminReference != null) {
                ConfigurationAdmin confAdmin =
                        (ConfigurationAdmin) bundleContext.getService(configurationAdminReference);

                Configuration configuration;
                try {
                    configuration = confAdmin.getConfiguration(ASYNC_CLIENT_PID, null);
                } catch (IOException e) {
                    throw new IllegalStateException("Couldn't retreive configuration for PID " + ASYNC_CLIENT_PID, e);
                }
                if (configuration != null) {
                    return configuration.getProperties();
                }
            }
            return null;
        }

        @Override
        public void modifiedService(ServiceReference reference, Object service) { }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            unregisterConduitFactory();
            bundleContext.ungetService(reference);
        }

    }

    class ConduitConfigurer implements ManagedService {

        @Override
        public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
            Bus bus = (Bus)tracker.getService();
            registerConduitFactory(bus, properties);
        }

    }
}
