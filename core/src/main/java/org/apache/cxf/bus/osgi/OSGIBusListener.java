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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.ExtensionManagerImpl;
import org.apache.cxf.buslifecycle.BusCreationListener;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.util.CollectionUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.endpoint.ClientLifeCycleListener;
import org.apache.cxf.endpoint.ClientLifeCycleManager;
import org.apache.cxf.endpoint.ServerLifeCycleListener;
import org.apache.cxf.endpoint.ServerLifeCycleManager;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

public class OSGIBusListener implements BusLifeCycleListener {
    public static final String CONTEXT_SYMBOLIC_NAME_PROPERTY = "cxf.context.symbolicname";
    public static final String CONTEXT_VERSION_PROPERTY = "cxf.context.version";
    public static final String CONTEXT_NAME_PROPERTY = "cxf.bus.id";

    private static final String SERVICE_PROPERTY_PRIVATE = "org.apache.cxf.bus.private.extension";
    private static final String SERVICE_PROPERTY_RESTRICTED = "org.apache.cxf.bus.restricted.extension";
    private static final String BUS_EXTENSION_BUNDLES_EXCLUDES = "bus.extension.bundles.excludes";
    Bus bus;
    ServiceRegistration<?> service;
    BundleContext defaultContext;
    private Pattern extensionBundlesExcludesPattern;

    public OSGIBusListener(Bus b) {
        this(b, null);
    }
    public OSGIBusListener(Bus b, Object[] args) {
        bus = b;
        if (args != null && args.length > 0
            && args[0] instanceof BundleContext) {
            defaultContext = (BundleContext)args[0];
        }
        String extExcludes = (String)bus.getProperty(BUS_EXTENSION_BUNDLES_EXCLUDES);
        if (!StringUtils.isEmpty(extExcludes)) {
            try {
                extensionBundlesExcludesPattern = Pattern.compile(extExcludes);
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }
        BusLifeCycleManager manager = bus.getExtension(BusLifeCycleManager.class);
        manager.registerLifeCycleListener(this);
        registerConfiguredBeanLocator();
        registerClientLifeCycleListeners();
        registerServerLifecycleListeners();
        registerBusFeatures();
        sendBusCreatedToBusCreationListeners();

    }
    private void registerConfiguredBeanLocator() {
        final ConfiguredBeanLocator cbl = bus.getExtension(ConfiguredBeanLocator.class);
        if (cbl instanceof ExtensionManagerImpl) {
            // wire in the OSGi things
            bus.setExtension(new OSGiBeanLocator(cbl, defaultContext),
                             ConfiguredBeanLocator.class);
        }
    }

    public void initComplete() {
        ManagedWorkQueueList wqList = bus.getExtension(ManagedWorkQueueList.class);
        if (wqList != null) {
            WorkQueueManager manager = bus.getExtension(WorkQueueManager.class);
            wqList.addAllToWorkQueueManager(manager);
        }
        registerBusAsService();
    }


    public void preShutdown() {
    }

    public void postShutdown() {
        if (service != null) {
            service.unregister();
            service = null;
        }
    }

    private static ServiceReference<?>[] getServiceReferences(BundleContext context, Class<?> serviceClass) {
        ServiceReference<?>[] refs = null;
        try {
            refs = context.getServiceReferences(serviceClass.getName(), null);
        } catch (InvalidSyntaxException e) {
            // ignore
        }
        if (refs == null) {
            refs = new ServiceReference<?>[]{};
        }
        return refs;
    }

    private void sendBusCreatedToBusCreationListeners() {
        ServiceReference<?>[] refs = getServiceReferences(defaultContext, BusCreationListener.class);
        for (ServiceReference<?> ref : refs) {
            if (!isPrivate(ref) && !isExcluded(ref)) {
                BusCreationListener listener = (BusCreationListener)defaultContext.getService(ref);
                listener.busCreated(bus);
            }
        }
    }

    private void registerServerLifecycleListeners() {
        ServiceReference<?>[] refs = getServiceReferences(defaultContext, ServerLifeCycleListener.class);
        ServerLifeCycleManager clcm = bus.getExtension(ServerLifeCycleManager.class);
        for (ServiceReference<?> ref : refs) {
            if (!isPrivate(ref) && !isExcluded(ref)) {
                ServerLifeCycleListener listener = (ServerLifeCycleListener)defaultContext.getService(ref);
                clcm.registerListener(listener);
            }
        }
    }
    private void registerClientLifeCycleListeners() {
        ServiceReference<?>[] refs = getServiceReferences(defaultContext, ClientLifeCycleListener.class);
        ClientLifeCycleManager clcm = bus.getExtension(ClientLifeCycleManager.class);
        for (ServiceReference<?> ref : refs) {
            if (!isPrivate(ref) && !isExcluded(ref)) {
                ClientLifeCycleListener listener = (ClientLifeCycleListener)defaultContext.getService(ref);
                clcm.registerListener(listener);
            }
        }
    }

    private void registerBusFeatures() {
        ServiceReference<?>[] refs = getServiceReferences(defaultContext, Feature.class);
        for (ServiceReference<?> ref : refs) {
            if (!isPrivate(ref) && !isExcluded(ref)) {
                Feature feature = (Feature)defaultContext.getService(ref);
                bus.getFeatures().add(feature);
            }
        }
    }

    private boolean isPrivate(ServiceReference<?> ref) {
        Object o = ref.getProperty(SERVICE_PROPERTY_PRIVATE);
        
        if (o == null) {
            return false;
        }
        
        Boolean pvt = Boolean.FALSE;
        if (o instanceof String) {
            pvt = Boolean.parseBoolean((String)o);
        } else if (o instanceof Boolean) {
            pvt = (Boolean)o;
        }
        return pvt.booleanValue();
    }

    private boolean isExcluded(ServiceReference<?> ref) {
        String o = (String)ref.getProperty(SERVICE_PROPERTY_RESTRICTED);
        if (!StringUtils.isEmpty(o)) {
            // if the service's restricted-regex is set, the service is excluded when the app not matching that regex
            BundleContext app = bus.getExtension(BundleContext.class);
            try {
                if (app != null && !app.getBundle().getSymbolicName().matches(o)) {
                    return true;
                }
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }
        // if the excludes-regex is set, the service is excluded when matching that regex.
        return extensionBundlesExcludesPattern != null
            && extensionBundlesExcludesPattern.matcher(ref.getBundle().getSymbolicName()).matches();
    }

    private Version getBundleVersion(Bundle bundle) {
        Dictionary<?, ?> headers = bundle.getHeaders();
        String version = (String) headers.get(Constants.BUNDLE_VERSION);
        return (version != null) ? Version.parseVersion(version) : Version.emptyVersion;
    }



    private void registerBusAsService() {
        BundleContext context = bus.getExtension(BundleContext.class);
        if (context != null) {
            Map<String, Object> props = new HashMap<>();
            props.put(CONTEXT_SYMBOLIC_NAME_PROPERTY, context.getBundle().getSymbolicName());
            props.put(CONTEXT_VERSION_PROPERTY, getBundleVersion(context.getBundle()));
            props.put(CONTEXT_NAME_PROPERTY, bus.getId());

            service = context.registerService(Bus.class.getName(), bus, CollectionUtils.toDictionary(props));
        }
    }

}