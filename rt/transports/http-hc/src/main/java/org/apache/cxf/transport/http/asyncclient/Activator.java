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
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

    private ServiceTracker tracker;

    @Override
    public void start(BundleContext context) throws Exception {
        tracker = new ServiceTracker(context, Bus.class.getName(), null);
        tracker.open();
        ConduitConfigurer conduitConfigurer = new ConduitConfigurer(context, tracker);
        registerManagedService(context, conduitConfigurer, "org.apache.cxf.transport.http.async");
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

    class ConduitConfigurer implements ManagedService {
        private AsyncHTTPConduitFactory conduitFactory;
        private ServiceTracker busTracker;
        private BundleContext context;
        private ServiceRegistration reg;
        
        ConduitConfigurer(BundleContext context, ServiceTracker busTracker) {
            this.context = context;
            this.busTracker = busTracker;
        }

        @SuppressWarnings({
            "rawtypes", "unchecked"
        })
        @Override
        public void updated(Dictionary properties) throws ConfigurationException {
            if (reg != null) {
                reg.unregister();
            }
            conduitFactory = new AsyncHTTPConduitFactory((Bus)this.busTracker.getService());
            conduitFactory.update(toMap(properties));
            reg = context.registerService(HTTPConduitFactory.class.getName(), conduitFactory, null);
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
        
    }
}
