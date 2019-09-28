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
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.ResourceInjector;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.resource.ObjectTypeResolver;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.ResourceResolver;
import org.apache.cxf.resource.SinglePropertyResolver;

public class ExtensionManagerImpl implements ExtensionManager, ConfiguredBeanLocator {
    public static final String EXTENSIONMANAGER_PROPERTY_NAME = "extensionManager";
    public static final String ACTIVATION_NAMESPACES_PROPERTY_NAME = "activationNamespaces";
    public static final String ACTIVATION_NAMESPACES_SETTER_METHOD_NAME = "setActivationNamespaces";
    public static final String BUS_EXTENSION_RESOURCE = "META-INF/cxf/bus-extensions.txt";

    private final ClassLoader loader;
    private ResourceManager resourceManager;
    private Map<String, Extension> all = new ConcurrentHashMap<>();
    private List<Extension> ordered = new CopyOnWriteArrayList<>();
    private final Map<Class<?>, Object> activated;
    private final Bus bus;

    public ExtensionManagerImpl(ClassLoader cl, Map<Class<?>, Object> initialExtensions,
                                ResourceManager rm, Bus b) {
        this(new String[] {BUS_EXTENSION_RESOURCE},
                 cl, initialExtensions, rm, b);
    }
    public ExtensionManagerImpl(String resource,
                                ClassLoader cl,
                                Map<Class<?>, Object> initialExtensions,
                                ResourceManager rm,
                                Bus b) {
        this(new String[] {resource}, cl, initialExtensions, rm, b);
    }
    public ExtensionManagerImpl(String[] resources,
                                ClassLoader cl,
                                Map<Class<?>, Object> initialExtensions,
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
        for (Map.Entry<String, Extension> ext : ExtensionRegistry.getRegisteredExtensions().entrySet()) {
            if (!all.containsKey(ext.getKey())) {
                all.put(ext.getKey(), ext.getValue());
                ordered.add(ext.getValue());
            }
        }
    }

    final void load(String[] resources) {
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
    }
    public void add(Extension ex) {
        all.put(ex.getName(), ex);
        ordered.add(ex);
    }

    public void initialize() {
        for (Extension e : ordered) {
            if (!e.isDeferred() && e.getLoadedObject() == null) {
                loadAndRegister(e);
            }
        }
    }

    public void removeBeansOfNames(List<String> names) {
        for (String s : names) {
            Extension ex = all.remove(s);
            if (ex != null) {
                ordered.remove(ex);
            }
        }
    }
    public void activateAll() {
        for (Extension e : ordered) {
            if (e.getLoadedObject() == null) {
                loadAndRegister(e);
            }
        }
    }
    public <T> void activateAllByType(Class<T> type) {
        for (Extension e : ordered) {
            if (e.getLoadedObject() == null) {
                Class<?> cls = e.getClassObject(loader);
                if (cls != null && type.isAssignableFrom(cls)) {
                    synchronized (e) {
                        loadAndRegister(e);
                    }
                }
            }
        }
    }

    public boolean hasBeanOfName(String name) {
        return all.containsKey(name);
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
            final URL url = urls.nextElement();
            try (InputStream is = AccessController.doPrivileged(new PrivilegedExceptionAction<InputStream>() {
                    public InputStream run() throws Exception {
                        return url.openStream();
                    }
                })) {
                List<Extension> exts = new TextExtensionFragmentParser(loader).getExtensions(is);
                for (Extension e : exts) {
                    if (loader != l) {
                        e.classloader = l;
                    }
                    if (!all.containsKey(e.getName())) {
                        all.put(e.getName(), e);
                        ordered.add(e);
                    }
                }
            } catch (PrivilegedActionException pae) {
                throw (IOException)pae.getException();
            }
        }
    }

    final void loadAndRegister(Extension e) {
        Class<?> cls = null;
        if (null != e.getInterfaceName() && !"".equals(e.getInterfaceName())) {
            cls = e.loadInterface(loader);
        }  else {
            cls = e.getClassObject(loader);
        }
        if (null != activated && null != cls && null != activated.get(cls)) {
            return;
        }

        synchronized (e) {
            Object obj = e.load(loader, bus);
            if (obj == null) {
                return;
            }

            if (null != activated) {
                Configurer configurer = (Configurer)(activated.get(Configurer.class));
                if (null != configurer) {
                    configurer.configureBean(obj);
                }
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
    }

    public <T> T getExtension(String name, Class<T> type) {
        if (name == null) {
            return null;
        }
        Extension e = all.get(name);
        if (e != null) {
            Class<?> cls = e.getClassObject(loader);
            if (cls != null && type.isAssignableFrom(cls)) {
                synchronized (e) {
                    if (e.getLoadedObject() == null) {
                        loadAndRegister(e);
                    }
                    return type.cast(e.getLoadedObject());
                }
            }
        }
        return null;
    }

    private void invokeSetterActivationNSMethod(Object target, Object value) {
        Class<?> clazz = target.getClass();
        String methodName = ACTIVATION_NAMESPACES_SETTER_METHOD_NAME;
        while (clazz != Object.class) {
            Method[] methods = clazz.getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                Class<?>[] params = method.getParameterTypes();
                if (method.getName().equals(methodName) && params.length == 1) {
                    Class<?> paramType = params[0];
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
        List<String> ret = new LinkedList<>();
        for (Extension ex : ordered) {
            Class<?> cls = ex.getClassObject(loader);
            if (cls != null && type.isAssignableFrom(cls)) {
                synchronized (ex) {
                    ret.add(ex.getName());
                }
            }
        }
        return ret;
    }
    public <T> T getBeanOfType(String name, Class<T> type) {
        if (name == null) {
            return null;
        }
        Extension ex = all.get(name);
        if (ex != null) {
            if (ex.getLoadedObject() == null) {
                loadAndRegister(ex);
            }
            return type.cast(ex.getLoadedObject());
        }
        return null;
    }
    public <T> Collection<? extends T> getBeansOfType(Class<T> type) {
        List<T> ret = new LinkedList<>();
        Extension ext = all.get(type.getName());
        if (ext != null) {
            Class<?> cls = ext.getClassObject(loader);
            if (cls != null && type.isAssignableFrom(cls)) {
                synchronized (ext) {
                    if (ext.getLoadedObject() == null) {
                        loadAndRegister(ext);
                    }
                    if (ext.getLoadedObject() != null) {
                        ret.add(type.cast(ext.getLoadedObject()));
                    }
                }
            }
        }
        for (Extension ex : ordered) {
            if (ex != ext) {
                Class<?> cls = ex.getClassObject(loader);
                if (cls != null && type.isAssignableFrom(cls)) {
                    synchronized (ex) {
                        if (ex.getLoadedObject() == null) {
                            loadAndRegister(ex);
                        }
                        if (ex.getLoadedObject() != null) {
                            ret.add(type.cast(ex.getLoadedObject()));
                        }
                    }
                }
            }
        }
        return ret;
    }
    public <T> boolean loadBeansOfType(Class<T> type, BeanLoaderListener<T> listener) {
        boolean loaded = false;
        for (Extension ex : ordered) {
            Class<?> cls = ex.getClassObject(loader);
            if (cls != null
                && type.isAssignableFrom(cls)) {
                synchronized (ex) {
                    if (listener.loadBean(ex.getName(), cls.asSubclass(type))) {
                        if (ex.getLoadedObject() == null) {
                            loadAndRegister(ex);
                        }
                        if (listener.beanLoaded(ex.getName(), type.cast(ex.getLoadedObject()))) {
                            return true;
                        }
                        loaded = true;
                    }
                }
            }
        }
        return loaded;
    }
    public boolean hasConfiguredPropertyValue(String beanName, String propertyName, String value) {
        if (beanName == null) {
            return false;
        }
        Extension ex = all.get(beanName);
        return ex != null && ex.getNamespaces() != null
            && ex.getNamespaces().contains(value);
    }
    public void destroyBeans() {
        for (Extension ex : ordered) {
            if (ex.getLoadedObject() != null) {
                ResourceInjector injector = new ResourceInjector(resourceManager);
                injector.destroy(ex.getLoadedObject());
            }
        }
    }
}
