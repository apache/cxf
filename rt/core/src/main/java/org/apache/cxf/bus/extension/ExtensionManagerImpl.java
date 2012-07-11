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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.ResourceInjector;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.resource.ObjectTypeResolver;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.ResourceResolver;
import org.apache.cxf.resource.SinglePropertyResolver;

public class ExtensionManagerImpl implements ExtensionManager, ConfiguredBeanLocator {
    public static final Logger LOG = LogUtils.getL7dLogger(ExtensionManagerImpl.class);
    
    
    public static final String EXTENSIONMANAGER_PROPERTY_NAME = "extensionManager";
    public static final String ACTIVATION_NAMESPACES_PROPERTY_NAME = "activationNamespaces";
    public static final String ACTIVATION_NAMESPACES_SETTER_METHOD_NAME = "setActivationNamespaces";
    public static final String BUS_EXTENSION_RESOURCE_XML = "META-INF/cxf/bus-extensions.xml";
    public static final String BUS_EXTENSION_RESOURCE_OLD_XML = "bus-extensions.xml";
    public static final String BUS_EXTENSION_RESOURCE = "META-INF/cxf/bus-extensions.txt";
    
    private final ClassLoader loader;
    private ResourceManager resourceManager;
    private Map<String, Extension> all = new LinkedHashMap<String, Extension>();
    private final Map<Class, Object> activated;
    private final Bus bus;

    public ExtensionManagerImpl(ClassLoader cl, Map<Class, Object> initialExtensions, 
                                ResourceManager rm, Bus b) {
        this(new String[] {BUS_EXTENSION_RESOURCE, BUS_EXTENSION_RESOURCE_XML,
                           BUS_EXTENSION_RESOURCE_OLD_XML},
                 cl, initialExtensions, rm, b);
    }
    public ExtensionManagerImpl(String resource, 
                                ClassLoader cl, 
                                Map<Class, Object> initialExtensions, 
                                ResourceManager rm,
                                Bus b) {
        this(new String[] {resource}, cl, initialExtensions, rm, b);
    }    
    public ExtensionManagerImpl(String resources[], 
                                ClassLoader cl, 
                                Map<Class, Object> initialExtensions, 
                                ResourceManager rm,
                                Bus b) {

        loader = cl;
        bus = b;
        activated = initialExtensions;
        resourceManager = rm;

        ResourceResolver extensionManagerResolver =
            new SinglePropertyResolver(EXTENSIONMANAGER_PROPERTY_NAME, this);
        resourceManager.addResourceResolver(extensionManagerResolver);
        resourceManager.addResourceResolver(new ObjectTypeResolver(this));

        load(resources);
    }
    public final synchronized void load(String resources[]) {
        if (resources == null) {
            return;
        }
        try {
            for (String resource : resources) {
                load(resource);
            }
        } catch (IOException ex) {
            throw new ExtensionException(ex);
        }
        
        for (Map.Entry<String, Extension> ext : ExtensionRegistry.getRegisteredExtensions().entrySet()) {
            if (!all.containsKey(ext.getKey())) {
                all.put(ext.getKey(), ext.getValue());
            }
        }
    }
    public synchronized void add(Extension ex) {
        all.put(ex.getName(), ex);
    }
    
    public synchronized void initialize() {
        for (Extension e : all.values()) {
            if (!e.isDeferred() && e.getLoadedObject() == null) {
                loadAndRegister(e);
            }
        }        
    }

    public synchronized void removeBeansOfNames(List<String> names) {
        for (String s : names) {
            all.remove(s);
        }
    }
    public synchronized void activateAll() {
        for (Extension e : all.values()) {
            if (e.getLoadedObject() == null) {
                loadAndRegister(e);
            }
        }        
    }
    public synchronized <T> void activateAllByType(Class<T> type) {
        for (Extension e : all.values()) {
            if (e.getLoadedObject() == null
                && type.isAssignableFrom(e.getClassObject(loader))) {
                loadAndRegister(e);
            }
        }        
    }
    final void load(String resource) throws IOException {
        if (loader != getClass().getClassLoader()) {
            load(resource, getClass().getClassLoader());
        }
        load(resource, loader);
    }
    final synchronized void load(String resource, ClassLoader l) throws IOException {
        
        Enumeration<URL> urls = l.getResources(resource);
        
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            InputStream is = url.openStream();
            try {
                List<Extension> exts;
                if (resource.endsWith("xml")) {
                    LOG.log(Level.WARNING, "DEPRECATED_EXTENSIONS", 
                            new Object[] {resource, url, BUS_EXTENSION_RESOURCE});
                    exts = new ExtensionFragmentParser().getExtensionsFromXML(is);
                } else {
                    exts = new ExtensionFragmentParser().getExtensionsFromText(is);
                }
                for (Extension e : exts) {
                    if (loader != l) {
                        e.classloader = l;
                    }
                    all.put(e.getName(), e);
                }
            } finally {
                try {
                    is.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
        }
    }

    final void loadAndRegister(Extension e) {
        Class<?> cls = null;
        if (null != e.getInterfaceName() && !"".equals(e.getInterfaceName())) {
            cls = e.loadInterface(loader);
        }

        if (null != activated && null != cls && null != activated.get(cls)) {
            return;
        }
 
        Object obj = e.load(loader, bus);
        
        Configurer configurer = (Configurer)(activated.get(Configurer.class));
        if (null != configurer) {
            configurer.configureBean(obj);
        }
        
        // let the object know for which namespaces it has been activated
        ResourceResolver namespacesResolver = null;
        if (null != e.getNamespaces()) {            
            namespacesResolver = new SinglePropertyResolver(ACTIVATION_NAMESPACES_PROPERTY_NAME, 
                                                            e.getNamespaces());
            resourceManager.addResourceResolver(namespacesResolver);
        }
        
        // Since we need to support spring2.5 by removing @Resource("activationNamespaces")
        // Now we call the setActivationNamespaces method directly here
        if (e.getNamespaces() != null && !e.getNamespaces().isEmpty()) {
            invokeSetterActivationNSMethod(obj, e.getNamespaces());
        }
        
        ResourceInjector injector = new ResourceInjector(resourceManager);
        
        try {            
            injector.inject(obj);
            injector.construct(obj);
        } finally {
            if (null != namespacesResolver) {
                resourceManager.removeResourceResolver(namespacesResolver);
            }
        }
        
        if (null != activated) {
            if (cls == null) {
                cls = obj.getClass();
            }   
            activated.put(cls, obj);
        }
    }

    public synchronized <T> T getExtension(String name, Class<T> type) {
        Extension e = all.get(name);
        if (e != null
            && type.isAssignableFrom(e.getClassObject(loader))) {
            if (e.getLoadedObject() == null) {
                loadAndRegister(e);
            }
            return type.cast(e.getLoadedObject());
        }
        return null;
    }
    
    private void invokeSetterActivationNSMethod(Object target, Object value) {
        Class clazz = target.getClass();
        String methodName = ACTIVATION_NAMESPACES_SETTER_METHOD_NAME;
        while (clazz != Object.class) {
            Method[] methods = clazz.getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                Class params[] = method.getParameterTypes();
                if (method.getName().equals(methodName) && params.length == 1) {
                    Class paramType = params[0];
                    if (paramType.isInstance(value)) {
                        try {
                            method.invoke(target, new Object[] {value});
                        } catch (Exception e) {
                            // do nothing here
                        }
                        return;
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }        
    }
    public List<String> getBeanNamesOfType(Class<?> type) {
        List<String> ret = new LinkedList<String>();
        for (Extension ex : all.values()) {
            if (type.isAssignableFrom(ex.getClassObject(loader))) {
                ret.add(ex.getName());
            }            
        }
        return ret;
    }
    public synchronized <T> Collection<? extends T> getBeansOfType(Class<T> type) {
        List<T> ret = new LinkedList<T>();
        for (Extension ex : all.values()) {
            if (type.isAssignableFrom(ex.getClassObject(loader))) {
                if (ex.getLoadedObject() == null) {
                    loadAndRegister(ex);
                }
                ret.add(type.cast(ex.getLoadedObject()));
            }            
        }
        return ret;
    }
    public synchronized <T> boolean loadBeansOfType(Class<T> type, BeanLoaderListener<T> listener) {
        boolean loaded = false;
        for (Extension ex : all.values()) {
            Class<?> cls = ex.getClassObject(loader);
            if (ex.getLoadedObject() == null 
                && type.isAssignableFrom(cls)
                && listener.loadBean(ex.getName(), cls.asSubclass(type))) {
                loadAndRegister(ex);
                if (listener.beanLoaded(ex.getName(), type.cast(ex.getLoadedObject()))) {
                    return true;
                }
                loaded = true;
            }            
        }
        return loaded;
    }
    public boolean hasConfiguredPropertyValue(String beanName, String propertyName, String value) {
        Extension ex = all.get(beanName);
        return ex != null && ex.getNamespaces() != null
            && ex.getNamespaces().contains(value);
    }
    public synchronized void destroyBeans() {
        for (Extension ex : all.values()) {
            if (ex.getLoadedObject() != null) {
                ResourceInjector injector = new ResourceInjector(resourceManager);
                injector.destroy(ex.getLoadedObject());
            }
        }        
    }
}
