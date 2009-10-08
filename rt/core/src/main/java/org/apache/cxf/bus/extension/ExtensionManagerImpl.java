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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.common.injection.ResourceInjector;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.resource.ObjectTypeResolver;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.ResourceResolver;
import org.apache.cxf.resource.SinglePropertyResolver;

public class ExtensionManagerImpl implements ExtensionManager {

    public static final String EXTENSIONMANAGER_PROPERTY_NAME = "extensionManager";
    public static final String ACTIVATION_NAMESPACES_PROPERTY_NAME = "activationNamespaces";
    public static final String ACTIVATION_NAMESPACES_SETTER_METHOD_NAME = "setActivationNamespaces";
    public static final String BUS_EXTENSION_RESOURCE_COMPAT = "META-INF/bus-extensions.xml";
    public static final String BUS_EXTENSION_RESOURCE = "META-INF/cxf/bus-extensions.xml";
    
    private final ClassLoader loader;
    private ResourceManager resourceManager;
    private Map<String, Collection<Extension>> deferred;
    private final Map<Class, Object> activated;
    private final Map<String, Collection<Object>> namespaced = 
        new ConcurrentHashMap<String, Collection<Object>>();

    public ExtensionManagerImpl(ClassLoader cl, Map<Class, Object> initialExtensions, 
                                ResourceManager rm) {
        this(new String[] {BUS_EXTENSION_RESOURCE, BUS_EXTENSION_RESOURCE_COMPAT}, cl, initialExtensions, rm);
    }
    public ExtensionManagerImpl(String resource, 
                                ClassLoader cl, 
                                Map<Class, Object> initialExtensions, 
                                ResourceManager rm) {
        this(new String[] {resource}, cl, initialExtensions, rm);
    }    
    public ExtensionManagerImpl(String resources[], 
                                ClassLoader cl, 
                                Map<Class, Object> initialExtensions, 
                                ResourceManager rm) {

        loader = cl;
        activated = initialExtensions;
        resourceManager = rm;

        ResourceResolver extensionManagerResolver =
            new SinglePropertyResolver(EXTENSIONMANAGER_PROPERTY_NAME, this);
        resourceManager.addResourceResolver(extensionManagerResolver);
        resourceManager.addResourceResolver(new ObjectTypeResolver(this));

        deferred = new ConcurrentHashMap<String, Collection<Extension>>();

        try {
            for (String resource : resources) {
                load(resource);
            }
        } catch (IOException ex) {
            throw new ExtensionException(ex);
        }
    }

    public synchronized void activateViaNS(String namespaceURI) {
        Collection<Extension> extensions = deferred.get(namespaceURI);
        if (null == extensions) {
            return;
        }
        for (Extension e : extensions) {
            loadAndRegister(e);
        }
        extensions.clear();
        deferred.remove(namespaceURI);
    }
    
    public synchronized void activateAll() {
        while (!deferred.isEmpty()) {
            activateViaNS(deferred.keySet().iterator().next());
        }
    }

    final void load(String resource) throws IOException {
        Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(resource);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            
            InputStream is = url.openStream();
            loadFragment(is);       
        }
        
    }

    final void loadFragment(InputStream is) {
        List<Extension> extensions = new ExtensionFragmentParser().getExtensions(is);
        for (Extension e : extensions) {
            processExtension(e);
        }
    }

    final void processExtension(Extension e) {
        
        if (!e.isDeferred()) {
            loadAndRegister(e);
        }
        Collection<String> namespaces = e.getNamespaces();
        for (String ns : namespaces) {
            Collection<Extension> extensions = deferred.get(ns);
            if (null == extensions) {
                extensions = new ArrayList<Extension>();
                deferred.put(ns, extensions);
            }
            extensions.add(e);
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
 
        Object obj = e.load(loader);
        
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
        invokeSetterActivationNSMethod(obj, e.getNamespaces());
        
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
        for (String ns : e.getNamespaces()) {
            Collection<Object> intf2Obj = namespaced.get(ns);
            if (intf2Obj == null) {
                intf2Obj = new ArrayList<Object>();
                if (!namespaced.containsKey(ns)) {
                    namespaced.put(ns, intf2Obj);
                }
            }
            intf2Obj.add(obj);
        }
    }

    public <T> T getExtension(String ns, Class<T> type) {
        
        Collection<Object> nsExts = namespaced.get(ns);
        if (nsExts != null) {
            for (Object o : nsExts) {
                if (type.isAssignableFrom(o.getClass())) {
                    return type.cast(o);
                }
            }
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

}
