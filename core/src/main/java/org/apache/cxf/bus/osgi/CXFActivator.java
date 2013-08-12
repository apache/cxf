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

package org.apache.cxf.bus.osgi;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.cxf.bus.extension.Extension;
import org.apache.cxf.bus.extension.ExtensionRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Is called in OSGi on start and stop of the cxf bundle.
 * Manages 
 * - CXFBundleListener
 * - Attaching ManagedWorkqueues tio config admin service
 * - OsgiBusListener
 */
public class CXFActivator implements BundleActivator {
    private List<Extension> extensions;
    private ManagedWorkQueueList workQueues = new ManagedWorkQueueList();
    private ServiceTracker configAdminTracker;
    private CXFExtensionBundleListener cxfBundleListener;
    private ServiceRegistration workQueueServiceRegistration;
    private ServiceRegistration wqSingleConfigRegistratin;

    /** {@inheritDoc}*/
    @SuppressWarnings("deprecation")
    public void start(BundleContext context) throws Exception {
        cxfBundleListener = new CXFExtensionBundleListener(context.getBundle().getBundleId());
        context.addBundleListener(cxfBundleListener);
        cxfBundleListener.registerExistingBundles(context);

        configAdminTracker = new ServiceTracker(context, ConfigurationAdmin.class.getName(), null);
        configAdminTracker.open();
        workQueues.setConfigAdminTracker(configAdminTracker);
        workQueueServiceRegistration = registerManagedServiceFactory(context, ManagedServiceFactory.class, 
                                                                     workQueues,
                                                                     ManagedWorkQueueList.FACTORY_PID);
        
        WorkQueueSingleConfig wqSingleConfig = new WorkQueueSingleConfig(workQueues);
        wqSingleConfigRegistratin = registerManagedServiceFactory(context, ManagedService.class, 
                                                                  wqSingleConfig,
                                                                  WorkQueueSingleConfig.SERVICE_PID);
        
        extensions = new ArrayList<Extension>();
        extensions.add(createOsgiBusListenerExtension(context));
        extensions.add(createManagedWorkQueueListExtension(workQueues));
        ExtensionRegistry.addExtensions(extensions);
    }

    private ServiceRegistration registerManagedServiceFactory(BundleContext context,
                                                              Class<?> serviceClass,
                                                              Object service, 
                                                              String servicePid) {
        Properties props = new Properties();
        props.put(Constants.SERVICE_PID, servicePid);  
        return context.registerService(serviceClass.getName(), service, props);
    }

    private Extension createOsgiBusListenerExtension(BundleContext context) {
        Extension busListener = new Extension(OSGIBusListener.class);
        busListener.setArgs(new Object[] {context});
        return busListener;
    }

    private static Extension createManagedWorkQueueListExtension(final ManagedWorkQueueList workQueues) {
        return new Extension(ManagedWorkQueueList.class) {
            public Object getLoadedObject() {
                return workQueues;
            }

            public Extension cloneNoObject() {
                return this;
            }
        };
    }

    /** {@inheritDoc}*/
    public void stop(BundleContext context) throws Exception {
        context.removeBundleListener(cxfBundleListener);
        cxfBundleListener.shutdown();
        workQueues.shutDown();
        workQueueServiceRegistration.unregister();
        wqSingleConfigRegistratin.unregister();
        configAdminTracker.close();
        ExtensionRegistry.removeExtensions(extensions);
    }

}
