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
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.BindingFactoryManagerImpl;
import org.apache.cxf.bus.BusState;
import org.apache.cxf.bus.CXFBusImpl;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.configuration.NullConfigurer;
import org.apache.cxf.resource.DefaultResourceManager;
import org.apache.cxf.resource.ObjectTypeResolver;
import org.apache.cxf.resource.PropertiesResolver;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.ResourceResolver;
import org.apache.cxf.resource.SinglePropertyResolver;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.ConduitInitiatorManagerImpl;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.DestinationFactoryManagerImpl;

/**
 * This bus uses CXF's built in extension manager to load components
 * (as opposed to using the Spring bus implementation). While this is faster
 * to load it doesn't allow extensive configuration and customization like
 * the Spring bus does.
 */
public class ExtensionManagerBus extends CXFBusImpl {
    
    public static final String BUS_PROPERTY_NAME = "bus";
    private static final String BUS_ID_PROPERTY_NAME = "org.apache.cxf.bus.id";
    private final ExtensionManagerImpl extensionManager;
    
    public ExtensionManagerBus(Map<Class, Object> e, Map<String, Object> properties,
          ClassLoader extensionClassLoader) {
        super(e);

        if (null == properties) {
            properties = new HashMap<String, Object>();
        }
        
        Configurer configurer = (Configurer)extensions.get(Configurer.class);
        if (null == configurer) {
            configurer = new NullConfigurer();
            extensions.put(Configurer.class, configurer);
        }
 
        setId(getBusId(properties));
        
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
        
        extensions.put(ResourceManager.class, resourceManager);

        extensionManager = new ExtensionManagerImpl(new String[0],
                                                    extensionClassLoader,
                                                    extensions,
                                                    resourceManager, 
                                                    this);
                                  
        setState(BusState.INITIAL);
        

        DestinationFactoryManager dfm = this.getExtension(DestinationFactoryManager.class);
        if (null == dfm) {
            dfm = new DestinationFactoryManagerImpl(this);
        }

        ConduitInitiatorManager cfm = this.getExtension(ConduitInitiatorManager.class);
        if (null == cfm) {
            cfm = new ConduitInitiatorManagerImpl(this);
        }
        
        BindingFactoryManager bfm = this.getExtension(BindingFactoryManager.class);
        if (null == bfm) {
            bfm = new BindingFactoryManagerImpl(this);
            extensions.put(BindingFactoryManager.class, bfm);
        }
        extensionManager.load(new String[] {
            ExtensionManagerImpl.BUS_EXTENSION_RESOURCE,
            ExtensionManagerImpl.BUS_EXTENSION_RESOURCE_XML,
            ExtensionManagerImpl.BUS_EXTENSION_RESOURCE_OLD_XML});
        
        extensionManager.activateAllByType(ResourceResolver.class);
        
        
        this.setExtension(extensionManager, ExtensionManager.class);
        
    }
    
    public ExtensionManagerBus(Map<Class, Object> e, Map<String, Object> properties) {
       this(e, properties, Thread.currentThread().getContextClassLoader());
    }

    public ExtensionManagerBus() {
        this(null, null, Thread.currentThread().getContextClassLoader());
    }
    @Override
    public void doInitializeInternal() {
        extensionManager.initialize();
        super.doInitializeInternal();
    }
    protected void destroyBeans() {
        extensionManager.destroyBeans();
    }

    protected synchronized ConfiguredBeanLocator createConfiguredBeanLocator() {
        ConfiguredBeanLocator loc = (ConfiguredBeanLocator)extensions.get(ConfiguredBeanLocator.class);
        if (loc == null) {
            loc = extensionManager; 
            this.setExtension(loc, ConfiguredBeanLocator.class);
        }
        return loc;
    }

    private String getBusId(Map<String, Object> properties) {

        String busId = null;

        // first check properties
        if (null != properties) {
            busId = (String)properties.get(BUS_ID_PROPERTY_NAME);
            if (null != busId && !"".equals(busId)) {
                return busId;
            }
        }

        // next check system properties
        busId = SystemPropertyAction.getPropertyOrNull(BUS_ID_PROPERTY_NAME);
        if (null != busId && !"".equals(busId)) {
            return busId;
        }

        // otherwise use null so the default will be used
        return null;
    }
}
