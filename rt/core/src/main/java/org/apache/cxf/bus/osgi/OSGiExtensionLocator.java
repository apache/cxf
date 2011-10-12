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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.extension.Extension;
import org.apache.cxf.bus.extension.ExtensionFragmentParser;
import org.apache.cxf.bus.extension.ExtensionRegistry;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.workqueue.AutomaticWorkQueueImpl;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * 
 */
public class OSGiExtensionLocator implements BundleActivator, SynchronousBundleListener {
    private static final Logger LOG = LogUtils.getL7dLogger(OSGiExtensionLocator.class);
    private ConcurrentMap<Long, List<Extension>> extensions 
        = new ConcurrentHashMap<Long, List<Extension>>();
    private long id;
    private Extension listener;

    static class WorkQueueList implements ManagedService {
        Map<String, OSGiAutomaticWorkQueue> queues 
            = new ConcurrentHashMap<String, OSGiAutomaticWorkQueue>();

        public void updated(Dictionary d) throws ConfigurationException {
            String s = (String)d.get("org.apache.cxf.workqueue.names");
            if (s != null) {
                String s2[] = s.split(",");
                for (String name : s2) {
                    name = name.trim();
                    if (queues.containsKey(name)) {
                        queues.get(name).updated(d);
                    } else {
                        OSGiAutomaticWorkQueue wq = new OSGiAutomaticWorkQueue(name);
                        wq.updated(d);
                        wq.setShared(true);
                        queues.put(name, wq);
                    }
                }
            }
        }
    };
    private WorkQueueList workQueues = new WorkQueueList();
    
    /** {@inheritDoc}*/
    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.RESOLVED && id != event.getBundle().getBundleId()) {
            try {
                register(event.getBundle());
            } catch (Exception ex) {
                //ignore
            }
        } else if (event.getType() == BundleEvent.UNRESOLVED || event.getType() == BundleEvent.UNINSTALLED) {
            unregister(event.getBundle().getBundleId());
        }
    }

    /** {@inheritDoc}*/
    public void start(BundleContext context) throws Exception {
        context.addBundleListener(this);
        id = context.getBundle().getBundleId();
        registerBusListener();
        for (Bundle bundle : context.getBundles()) {
            if ((bundle.getState() == Bundle.RESOLVED 
                || bundle.getState() == Bundle.STARTING 
                || bundle.getState() == Bundle.ACTIVE 
                || bundle.getState() == Bundle.STOPPING)
                && bundle.getBundleId() != context.getBundle().getBundleId()) {
                register(bundle);
            }
        }
        ServiceReference configAdminServiceRef =  
            context.getServiceReference(ConfigurationAdmin.class.getName());
        
              
        if (configAdminServiceRef != null) {  
            ConfigurationAdmin configAdmin = (ConfigurationAdmin)  
                    context.getService(configAdminServiceRef);  
              
            Configuration config = configAdmin.getConfiguration("org.apache.cxf.workqueues");
            Dictionary d = config.getProperties();
            
            Properties props = new Properties();
            props.put(Constants.SERVICE_PID, "org.apache.cxf.workqueues");  
            context.registerService(ManagedService.class.getName(),  
                                    workQueues, props); 

            
            if (d != null) {
                workQueues.updated(d);
            }
            Extension ext = new Extension(WorkQueueList.class) {
                public Object getLoadedObject() {
                    return workQueues;
                }
                public Extension cloneNoObject() {
                    return this;
                }
            };
            ExtensionRegistry.addExtensions(Collections.singletonList(ext));
        }
    }

    /** {@inheritDoc}*/
    public void stop(BundleContext context) throws Exception {
        context.removeBundleListener(this);
        unregisterBusListener();
        while (!extensions.isEmpty()) {
            unregister(extensions.keySet().iterator().next());
        }
        for (AutomaticWorkQueueImpl wq : workQueues.queues.values()) {
            wq.setShared(false);
            wq.shutdown(true);
        }
        workQueues.queues.clear();
    }
    private void registerBusListener() {
        listener = new Extension(OSGIBusListener.class);
        ExtensionRegistry.addExtensions(Collections.singletonList(listener));
    }
    private void unregisterBusListener() {
        ExtensionRegistry.removeExtensions(Collections.singletonList(listener));
        listener = null;
    }
    
    protected void register(final Bundle bundle) throws IOException {
        List<Extension> list = extensions.get(bundle.getBundleId());
        Enumeration e = bundle.findEntries("META-INF/cxf/", "bus-extensions.txt", false);
        if (e != null) {
            while (e.hasMoreElements()) {
                final URL u = (URL)e.nextElement();
                InputStream ins = u.openStream();
                List<Extension> orig = new ExtensionFragmentParser()
                    .getExtensionsFromText(ins);
                ins.close();
                LOG.info("Loading the extension from bundle " + bundle.getBundleId());
                if (orig != null && !orig.isEmpty()) {
                    if (list == null) {
                        list = new CopyOnWriteArrayList<Extension>();
                        extensions.put(bundle.getBundleId(), list);
                    }
                    for (Extension ext : orig) {
                        list.add(new OSGiExtension(ext, bundle));
                    }
                    ExtensionRegistry.addExtensions(list);
                }
            }
        }
    }
    protected void unregister(final long bundleId) {
        List<Extension> list = extensions.remove(bundleId);
        if (list != null) {
            LOG.info("Removed the extensions for bundle " + bundleId);
            ExtensionRegistry.removeExtensions(list);
        }
    }
    
    public static class OSGIBusListener implements BusLifeCycleListener {
        public static final String CONTEXT_SYMBOLIC_NAME_PROPERTY = "cxf.context.symbolicname";
        public static final String CONTEXT_VERSION_PROPERTY = "cxf.context.version";
        public static final String CONTEXT_NAME_PROPERTY = "cxf.bus.id";
        
        Bus bus;
        ServiceRegistration service;
 
        public OSGIBusListener(Bus b) {
            bus = b;
            bus.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);
        }
        private Version getBundleVersion(Bundle bundle) {
            Dictionary headers = bundle.getHeaders();
            String version = (String) headers.get(Constants.BUNDLE_VERSION);
            return (version != null) ? Version.parseVersion(version) : Version.emptyVersion;
        }
 
        public void initComplete() {
            WorkQueueManager m = bus.getExtension(WorkQueueManager.class);
            WorkQueueList l = bus.getExtension(WorkQueueList.class);
            if (l != null && m != null) {
                for (AutomaticWorkQueueImpl wq : l.queues.values()) {
                    if (m.getNamedWorkQueue(wq.getName()) == null) {
                        m.addNamedWorkQueue(wq.getName(), wq);
                    }
                }
            }
            
            BundleContext context = bus.getExtension(BundleContext.class);
            if (context != null) {
                Properties props = new Properties();
                props.put(CONTEXT_SYMBOLIC_NAME_PROPERTY, context.getBundle().getSymbolicName());
                props.put(CONTEXT_VERSION_PROPERTY, getBundleVersion(context.getBundle()));
                props.put(CONTEXT_NAME_PROPERTY, bus.getId());
    
                service = context.registerService(Bus.class.getName(), bus, props);
            }
        }
        public void preShutdown() {
        }
        public void postShutdown() {
            if (service != null) {
                service.unregister();
                service = null;
            }
        }
    }
    
    public class OSGiExtension extends Extension {
        final Bundle bundle;
        public OSGiExtension(Extension e, Bundle b) {
            super(e);
            bundle = b;
        }
        public Class<?> getClassObject(ClassLoader cl) {
            if (clazz == null) {
                try {
                    clazz = bundle.loadClass(className);
                } catch (ClassNotFoundException e) {
                    //ignore, fall to super
                }
            }
            return super.getClassObject(cl);
        }
        public Class<?> loadInterface(ClassLoader cl) {
            try {
                return bundle.loadClass(interfaceName);
            } catch (ClassNotFoundException e) {
                //ignore, fall to super
            }
            return super.loadInterface(cl);
        }
        public Extension cloneNoObject() {
            OSGiExtension ext = new OSGiExtension(this, bundle);
            ext.obj = null;
            return ext;
        }

    }

}
