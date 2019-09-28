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
package org.apache.cxf.bus.extension;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.bus.managers.BindingFactoryManagerImpl;
import org.apache.cxf.bus.managers.ConduitInitiatorManagerImpl;
import org.apache.cxf.bus.managers.DestinationFactoryManagerImpl;
import org.apache.cxf.buslifecycle.BusCreationListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.configuration.NullConfigurer;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.AbstractBasicInterceptorProvider;
import org.apache.cxf.resource.DefaultResourceManager;
import org.apache.cxf.resource.ObjectTypeResolver;
import org.apache.cxf.resource.PropertiesResolver;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.ResourceResolver;
import org.apache.cxf.resource.SinglePropertyResolver;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;

/**
 * This bus uses CXF's built in extension manager to load components
 * (as opposed to using the Spring bus implementation). While this is faster
 * to load it doesn't allow extensive configuration and customization like
 * the Spring bus does.
 */
public class ExtensionManagerBus extends AbstractBasicInterceptorProvider implements Bus {
    public static final String BUS_PROPERTY_NAME = "bus";

    private static final String BUS_ID_PROPERTY_NAME = "org.apache.cxf.bus.id";

    protected final Map<Class<?>, Object> extensions;
    protected final Set<Class<?>> missingExtensions;
    protected String id;
    private BusState state;
    private final Collection<Feature> features = new CopyOnWriteArrayList<>();
    private final Map<String, Object> properties = new ConcurrentHashMap<>(16, 0.75f, 4);


    private final ExtensionManagerImpl extensionManager;

    public ExtensionManagerBus(Map<Class<?>, Object> extensions, Map<String, Object> props,
          ClassLoader extensionClassLoader) {
        this.extensions = extensions == null ? new ConcurrentHashMap<>(16, 0.75f, 4)
                : new ConcurrentHashMap<>(extensions);
        this.missingExtensions = new CopyOnWriteArraySet<>();


        state = BusState.INITIAL;

        BusFactory.possiblySetDefaultBus(this);
        if (null != props) {
            properties.putAll(props);
        }

        Configurer configurer = (Configurer)this.extensions.get(Configurer.class);
        if (null == configurer) {
            configurer = new NullConfigurer();
            this.extensions.put(Configurer.class, configurer);
        }

        id = getBusId(properties);

        ResourceManager resourceManager = new DefaultResourceManager();

        properties.put(BUS_ID_PROPERTY_NAME, BUS_PROPERTY_NAME);
        properties.put(BUS_PROPERTY_NAME, this);
        properties.put(DEFAULT_BUS_ID, this);

        ResourceResolver propertiesResolver = new PropertiesResolver(properties);
        resourceManager.addResourceResolver(propertiesResolver);

        ResourceResolver busResolver = new SinglePropertyResolver(BUS_PROPERTY_NAME, this);
        resourceManager.addResourceResolver(busResolver);
        resourceManager.addResourceResolver(new ObjectTypeResolver(this));

        busResolver = new SinglePropertyResolver(DEFAULT_BUS_ID, this);
        resourceManager.addResourceResolver(busResolver);
        resourceManager.addResourceResolver(new ObjectTypeResolver(this));
        resourceManager.addResourceResolver(new ResourceResolver() {
            public <T> T resolve(String resourceName, Class<T> resourceType) {
                if (extensionManager != null) {
                    return extensionManager.getExtension(resourceName, resourceType);
                }
                return null;
            }
            public InputStream getAsStream(String name) {
                return null;
            }
        });

        this.extensions.put(ResourceManager.class, resourceManager);

        extensionManager = new ExtensionManagerImpl(new String[0],
                                                    extensionClassLoader,
                                                    this.extensions,
                                                    resourceManager,
                                                    this);

        setState(BusState.INITIAL);

        if (null == this.getExtension(DestinationFactoryManager.class)) {
            new DestinationFactoryManagerImpl(this);
        }

        if (null == this.getExtension(ConduitInitiatorManager.class)) {
            new ConduitInitiatorManagerImpl(this);
        }

        if (null == this.getExtension(BindingFactoryManager.class)) {
            new BindingFactoryManagerImpl(this);
        }
        extensionManager.load(new String[] {ExtensionManagerImpl.BUS_EXTENSION_RESOURCE});
        extensionManager.activateAllByType(ResourceResolver.class);

        this.extensions.put(ExtensionManager.class, extensionManager);
    }

    public ExtensionManagerBus(Map<Class<?>, Object> e, Map<String, Object> properties) {
        this(e, properties, Thread.currentThread().getContextClassLoader());
    }
    public ExtensionManagerBus(Map<Class<?>, Object> e) {
        this(e, null, Thread.currentThread().getContextClassLoader());
    }
    public ExtensionManagerBus() {
        this(null, null, Thread.currentThread().getContextClassLoader());
    }


    protected final void setState(BusState state) {
        this.state = state;
    }

    public void setId(String i) {
        id = i;
    }

    public final <T> T getExtension(Class<T> extensionType) {
        Object obj = extensions.get(extensionType);
        if (obj == null) {
            if (missingExtensions.contains(extensionType)) {
                //already know we cannot find it
                return null;
            }
            ConfiguredBeanLocator loc = (ConfiguredBeanLocator)extensions.get(ConfiguredBeanLocator.class);
            if (loc == null) {
                loc = createConfiguredBeanLocator();
            }
            if (loc != null) {
                obj = loc.getBeanOfType(extensionType.getName(), extensionType);
                if (obj != null) {
                    extensions.put(extensionType, obj);
                } else {
                    //force loading
                    Collection<?> objs = loc.getBeansOfType(extensionType);
                    if (objs != null && !objs.isEmpty()) {
                        extensions.put(extensionType, objs.iterator().next());
                    }
                    obj = extensions.get(extensionType);
                }
            }
        }
        if (null != obj) {
            return extensionType.cast(obj);
        }
        //record that it couldn't be found to avoid expensive searches again in the future
        missingExtensions.add(extensionType);
        return null;
    }

    public boolean hasExtensionByName(String name) {
        for (Class<?> c : extensions.keySet()) {
            if (name.equals(c.getName())) {
                return true;
            }
        }
        ConfiguredBeanLocator loc = (ConfiguredBeanLocator)extensions.get(ConfiguredBeanLocator.class);
        if (loc == null) {
            loc = createConfiguredBeanLocator();
        }
        if (loc != null) {
            return loc.hasBeanOfName(name);
        }
        return false;
    }

    protected final synchronized ConfiguredBeanLocator createConfiguredBeanLocator() {
        ConfiguredBeanLocator loc = (ConfiguredBeanLocator)extensions.get(ConfiguredBeanLocator.class);
        if (loc == null) {
            loc = extensionManager;
            this.setExtension(loc, ConfiguredBeanLocator.class);
        }
        return loc;
    }

    public final <T> void setExtension(T extension, Class<T> extensionType) {
        if (extension == null) {
            extensions.remove(extensionType);
            missingExtensions.add(extensionType);
        } else {
            extensions.put(extensionType, extension);
            missingExtensions.remove(extensionType);
        }
    }

    public String getId() {
        return null == id ? DEFAULT_BUS_ID + Integer.toString(Math.abs(this.hashCode())) : id;
    }

    public void initialize() {
        setState(BusState.INITIALIZING);

        Collection<? extends BusCreationListener> ls = getExtension(ConfiguredBeanLocator.class)
            .getBeansOfType(BusCreationListener.class);
        for (BusCreationListener l : ls) {
            l.busCreated(this);
        }

        doInitializeInternal();

        BusLifeCycleManager lifeCycleManager = this.getExtension(BusLifeCycleManager.class);
        if (null != lifeCycleManager) {
            lifeCycleManager.initComplete();
        }
        setState(BusState.RUNNING);
    }

    protected void doInitializeInternal() {
        extensionManager.initialize();
        initializeFeatures();
    }

    protected void loadAdditionalFeatures() {

    }

    protected void initializeFeatures() {
        loadAdditionalFeatures();
        for (Feature f : features) {
            f.initialize(this);
        }
    }

    public void shutdown() {
        shutdown(true);
    }

    protected void destroyBeans() {
        extensionManager.destroyBeans();
    }

    public void shutdown(boolean wait) {
        if (state == BusState.SHUTTING_DOWN) {
            return;
        }
        BusLifeCycleManager lifeCycleManager = this.getExtension(BusLifeCycleManager.class);
        if (null != lifeCycleManager) {
            lifeCycleManager.preShutdown();
        }
        synchronized (this) {
            state = BusState.SHUTTING_DOWN;
        }
        destroyBeans();
        synchronized (this) {
            state = BusState.SHUTDOWN;
            notifyAll();
        }
        if (null != lifeCycleManager) {
            lifeCycleManager.postShutdown();
        }

        if (BusFactory.getDefaultBus(false) == this) {
            BusFactory.setDefaultBus(null);
        }
        BusFactory.clearDefaultBusForAnyThread(this);
    }

    public BusState getState() {
        return state;
    }

    public Collection<Feature> getFeatures() {
        return features;
    }

    public synchronized void setFeatures(Collection<? extends Feature> features) {
        this.features.clear();
        this.features.addAll(features);
        if (state == BusState.RUNNING) {
            initializeFeatures();
        }
    }

    public interface ExtensionFinder {
        <T> T findExtension(Class<T> cls);
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> map) {
        properties.clear();
        properties.putAll(map);
    }

    public Object getProperty(String s) {
        return properties.get(s);
    }

    public void setProperty(String s, Object o) {
        if (o == null) {
            properties.remove(s);
        } else {
            properties.put(s, o);
        }
    }



    private static String getBusId(Map<String, Object> properties) {

        String busId;

        // first check properties
        if (null != properties) {
            busId = (String)properties.get(BUS_ID_PROPERTY_NAME);
            if (null != busId && !busId.isEmpty()) {
                return busId;
            }
        }

        // next check system properties
        busId = SystemPropertyAction.getPropertyOrNull(BUS_ID_PROPERTY_NAME);
        if (null != busId && !busId.isEmpty()) {
            return busId;
        }

        // otherwise use null so the default will be used
        return null;
    }
}
