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

package org.apache.cxf.bus;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.buslifecycle.BusCreationListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.interceptor.AbstractBasicInterceptorProvider;

@NoJSR250Annotations
public class CXFBusImpl extends AbstractBasicInterceptorProvider implements Bus {
    static final boolean FORCE_LOGGING;
    static {
        boolean b = false;
        try {
            b = Boolean.getBoolean("org.apache.cxf.logging.enabled");
            //treat these all the same
            b |= Boolean.getBoolean("com.sun.xml.ws.transport.local.LocalTransportPipe.dump");
            b |= Boolean.getBoolean("com.sun.xml.ws.util.pipe.StandaloneTubeAssembler.dump");
            b |= Boolean.getBoolean("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump");
            b |= Boolean.getBoolean("com.sun.xml.ws.transport.http.HttpAdapter.dump");
        } catch (Throwable t) {
            //ignore
        }
        FORCE_LOGGING = b;
    }
    
    protected final Map<Class, Object> extensions;
    protected String id;
    private BusState state;      
    private final Collection<AbstractFeature> features = new CopyOnWriteArrayList<AbstractFeature>();
    private final Map<String, Object> properties = new ConcurrentHashMap<String, Object>();
    
    public CXFBusImpl() {
        this(null);
    }

    public CXFBusImpl(Map<Class, Object> extensions) {
        if (extensions == null) {
            extensions = new ConcurrentHashMap<Class, Object>();
        } else {
            extensions = new ConcurrentHashMap<Class, Object>(extensions);
        }
        this.extensions = extensions;
        
        state = BusState.INITIAL;
        
        CXFBusFactory.possiblySetDefaultBus(this);
        if (FORCE_LOGGING) {
            features.add(new LoggingFeature());
        }
    }

    protected void setState(BusState state) {
        this.state = state;
    }
    
    public void setId(String i) {
        id = i;
    }

    public final <T> T getExtension(Class<T> extensionType) {
        Object obj = extensions.get(extensionType);
        if (obj == null) {
            ConfiguredBeanLocator loc = (ConfiguredBeanLocator)extensions.get(ConfiguredBeanLocator.class);
            if (loc == null) {
                loc = createConfiguredBeanLocator();
            }
            if (loc != null) {
                //force loading
                Collection<?> objs = loc.getBeansOfType(extensionType);
                if (objs != null) {
                    for (Object o : objs) {
                        extensions.put(extensionType, o);
                    }
                }
                obj = extensions.get(extensionType);
            }
        }
        if (null != obj) {
            return extensionType.cast(obj);
        }
        return null;
    }
    
    protected synchronized ConfiguredBeanLocator createConfiguredBeanLocator() {
        ConfiguredBeanLocator loc = (ConfiguredBeanLocator)extensions.get(ConfiguredBeanLocator.class);
        if (loc == null) {
            loc = new ConfiguredBeanLocator() {
                public List<String> getBeanNamesOfType(Class<?> type) {
                    return null;
                }
                public <T> Collection<? extends T> getBeansOfType(Class<T> type) {
                    return null;
                }
                public <T> boolean loadBeansOfType(Class<T> type, BeanLoaderListener<T> listener) {
                    return false;
                }
                public boolean hasConfiguredPropertyValue(String beanName, 
                                                          String propertyName,
                                                          String value) {
                    return false;
                }
                public <T> T getBeanOfType(String name, Class<T> type) {
                    return null;
                }
            };
            this.setExtension(loc, ConfiguredBeanLocator.class);
        }
        return loc;
    }

    public <T> void setExtension(T extension, Class<T> extensionType) {
        extensions.put(extensionType, extension);
    }
     
    public String getId() {        
        return null == id ? DEFAULT_BUS_ID + Integer.toString(this.hashCode()) : id;
    }

    public void run() {
        synchronized (this) {
            state = BusState.RUNNING;
            while (state == BusState.RUNNING) {

                try {
                    wait();
                } catch (InterruptedException ex) {
                    // ignore;
                }
            }
        }
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
        initializeFeatures();
    }

    protected void loadAdditionalFeatures() {
        
    }
    
    protected void initializeFeatures() {
        loadAdditionalFeatures();
        if (features != null) {
            for (AbstractFeature f : features) {
                f.initialize(this);
            }
        }
    }

    public void shutdown() {
        shutdown(true);
    }

    protected void destroyBeans() {
        
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

    public Collection<AbstractFeature> getFeatures() {
        return features;
    }

    public synchronized void setFeatures(Collection<AbstractFeature> features) {
        this.features.clear();
        this.features.addAll(features);
        if (FORCE_LOGGING) {
            this.features.add(new LoggingFeature());
        }
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
}
