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

package org.apache.cxf.jaxrs.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy;
import org.apache.cxf.jaxrs.utils.InjectionUtils;

public abstract class AbstractResourceInfo {
    private static final String FIELD_PROXY_MAP = "jaxrs-field-proxy-map";
    private static final String SETTER_PROXY_MAP = "jaxrs-setter-proxy-map";
    private static final String CONSTRUCTOR_PROXY_MAP = "jaxrs-constructor-proxy-map";
    
    protected boolean root;
    protected Class<?> resourceClass;
    protected Class<?> serviceClass;

    private Map<Class<?>, List<Field>> contextFields;
    private Map<Class<?>, Map<Class<?>, Method>> contextMethods;
    private Bus bus;
    private boolean constructorProxiesAvailable;
    private boolean contextsAvailable;
    
    protected AbstractResourceInfo(Bus bus) {
        this.bus = bus;
    }

    protected AbstractResourceInfo(Class<?> resourceClass, Class<?> serviceClass, 
                                   boolean isRoot, boolean checkContexts, Bus bus) {
        this(resourceClass, serviceClass, isRoot, checkContexts, null, bus, null);
    }
    
    protected AbstractResourceInfo(Class<?> resourceClass, 
                                   Class<?> serviceClass, 
                                   boolean isRoot, 
                                   boolean checkContexts,
                                   Map<Class<?>, ThreadLocalProxy<?>> constructorProxies,
                                   Bus bus,
                                   Object provider) {
        this.bus = bus;
        this.serviceClass = serviceClass;
        this.resourceClass = resourceClass;
        root = isRoot;
        if (checkContexts && resourceClass != null) {
            findContexts(serviceClass, provider, constructorProxies);
        }
    }
    
    private void findContexts(Class<?> cls, Object provider, 
                              Map<Class<?>, ThreadLocalProxy<?>> constructorProxies) {
        findContextFields(cls, provider);
        findContextSetterMethods(cls, provider);
        if (constructorProxies != null) {
            Map<Class<?>, Map<Class<?>, ThreadLocalProxy<?>>> proxies = getConstructorProxyMap(true);
            proxies.put(serviceClass, constructorProxies);
            constructorProxiesAvailable = true;
        }
        
        
        contextsAvailable = contextFields != null && !contextFields.isEmpty() 
            || contextMethods != null && !contextMethods.isEmpty()
            || constructorProxiesAvailable;
    }
    
    public boolean contextsAvailable() {
        return contextsAvailable;
    }
    
    public Bus getBus() {
        return bus;
    }
    
    public void setResourceClass(Class<?> rClass) {
        resourceClass = rClass;
        if (serviceClass.isInterface() && resourceClass != null && !resourceClass.isInterface()) {
            findContexts(resourceClass, null, null);
        }
    }
    
    public Class<?> getServiceClass() {
        return serviceClass;
    }
    
    private void findContextFields(Class<?> cls, Object provider) {
        if (cls == Object.class || cls == null) {
            return;
        }
        for (Field f : cls.getDeclaredFields()) {
            for (Annotation a : f.getAnnotations()) {
                if (a.annotationType() == Context.class) {
                    contextFields = addContextField(contextFields, f);
                    if (f.getType() != Application.class) {
                        addToMap(getFieldProxyMap(true), f, getFieldThreadLocalProxy(f, provider));
                    }
                }
            }
        }
        findContextFields(cls.getSuperclass(), provider);
    }
    
    private static ThreadLocalProxy<?> getFieldThreadLocalProxy(Field f, Object provider) {
        ThreadLocalProxy<?> defaultValue = InjectionUtils.createThreadLocalProxy(f.getType()); 
        if (provider != null) {
            Object proxy = null;
            synchronized (provider) {
                try {
                    proxy = InjectionUtils.extractFieldValue(f, provider);
                } catch (Throwable t) {
                    // continue
                }
                if (!(proxy instanceof ThreadLocalProxy)) {
                    proxy = defaultValue;
                    InjectionUtils.injectFieldValue(f, provider, proxy);
                }
            }
            return (ThreadLocalProxy<?>)proxy;
        } else {
            return defaultValue;
        }
    }
    
    private static ThreadLocalProxy<?> getMethodThreadLocalProxy(Method m, Object provider) {
        ThreadLocalProxy<?> defaultValue = InjectionUtils.createThreadLocalProxy(m.getParameterTypes()[0]); 
        if (provider != null) {
            Object proxy = null;
            synchronized (provider) {
                try {
                    Method getter = m.getClass().getMethod("get" + m.getName().substring(3), new Class[]{});
                    proxy = InjectionUtils.extractFromMethod(provider, getter);
                } catch (Throwable t) {
                    // continue
                }
                if (!(proxy instanceof ThreadLocalProxy)) {
                    proxy = defaultValue;
                    InjectionUtils.injectThroughMethod(provider, m, proxy);
                }
            }
            return (ThreadLocalProxy<?>)proxy;
        } else {
            return defaultValue;
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> Map<Class<?>, Map<T, ThreadLocalProxy<?>>> getProxyMap(Class<T> keyCls, String prop, boolean create) {
        Object property = bus.getProperty(prop);
        if (property == null && create) {
            Map<Class<?>, Map<T, ThreadLocalProxy<?>>> map
                = new ConcurrentHashMap<Class<?>, Map<T, ThreadLocalProxy<?>>>(2);
            bus.setProperty(prop, map);
            property = map;
        }
        return (Map<Class<?>, Map<T, ThreadLocalProxy<?>>>)property;
    }
    
    public Map<Class<?>, ThreadLocalProxy<?>> getConstructorProxies() {
        if (constructorProxiesAvailable) {
            return getConstructorProxyMap(false).get(serviceClass);
        } else {
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<Class<?>, Map<Class<?>, ThreadLocalProxy<?>>> getConstructorProxyMap(boolean create) {
        Object property = bus.getProperty(CONSTRUCTOR_PROXY_MAP);
        if (property == null) {
            Map<Class<?>, Map<Class<?>, ThreadLocalProxy<?>>> map
                = new ConcurrentHashMap<Class<?>, Map<Class<?>, ThreadLocalProxy<?>>>(2);
            bus.setProperty(CONSTRUCTOR_PROXY_MAP, map);
            property = map;
        }
        return (Map<Class<?>, Map<Class<?>, ThreadLocalProxy<?>>>)property;
    }
    
    private Map<Class<?>, Map<Field, ThreadLocalProxy<?>>> getFieldProxyMap(boolean create) {
        return getProxyMap(Field.class, FIELD_PROXY_MAP, create);
    }
    
    private Map<Class<?>, Map<Method, ThreadLocalProxy<?>>> getSetterProxyMap(boolean create) {
        return getProxyMap(Method.class, SETTER_PROXY_MAP, create);
    }
    
    private void findContextSetterMethods(Class<?> cls, Object provider) {
        
        for (Method m : cls.getMethods()) {
        
            if (!m.getName().startsWith("set") || m.getParameterTypes().length != 1) {
                continue;
            }
            for (Annotation a : m.getAnnotations()) {
                if (a.annotationType() == Context.class) {
                    checkContextMethod(m, provider);
                    break;
                }
            }
        }
        Class<?>[] interfaces = cls.getInterfaces();
        for (Class<?> i : interfaces) {
            findContextSetterMethods(i, provider);
        }
        Class<?> superCls = cls.getSuperclass();
        if (superCls != null && superCls != Object.class) {
            findContextSetterMethods(superCls, provider);
        }
    }
    
    private void checkContextMethod(Method m, Object provider) {
        Class<?> type = m.getParameterTypes()[0];
        if (m.getName().equals("set" + type.getSimpleName())) {        
            addContextMethod(type, m, provider);
        }
    }
    
    @SuppressWarnings("unchecked")
    public Map<Class<?>, Method> getContextMethods() {
        Map<Class<?>, Method> methods = contextMethods == null ? null : contextMethods.get(getServiceClass());
        return methods == null ? Collections.EMPTY_MAP 
                                      : Collections.unmodifiableMap(methods);
    }
    
    private void addContextMethod(Class<?> contextClass, Method m, Object provider) {
        if (contextMethods == null) {
            contextMethods = new HashMap<Class<?>, Map<Class<?>, Method>>();
        }
        addToMap(contextMethods, contextClass, m);
        if (m.getParameterTypes()[0] != Application.class) {
            addToMap(getSetterProxyMap(true), m, getMethodThreadLocalProxy(m, provider));
        }
    }
    
    public boolean isRoot() {
        return root;
    }
    
    public Class<?> getResourceClass() {
        return resourceClass;
    }
    
    public List<Field> getContextFields() {
        return getList(contextFields);
    }
    
    public ThreadLocalProxy<?> getContextFieldProxy(Field f) {
        return getProxy(getFieldProxyMap(true), f);
    }
    
    public ThreadLocalProxy<?> getContextSetterProxy(Method m) {
        return getProxy(getSetterProxyMap(true), m);
    }
    
    public abstract boolean isSingleton();

    @SuppressWarnings("rawtypes")
    public static void clearAllMaps() {
        Bus bus = BusFactory.getThreadDefaultBus(false);
        if (bus != null) {
            Object property = bus.getProperty(FIELD_PROXY_MAP);
            if (property != null) {
                ((Map)property).clear();
            }
            property = bus.getProperty(SETTER_PROXY_MAP);
            if (property != null) {
                ((Map)property).clear();
            }
            property = bus.getProperty(CONSTRUCTOR_PROXY_MAP);
            if (property != null) {
                ((Map)property).clear();
            }
        }
    }
    
    public void clearThreadLocalProxies() {
        clearProxies(getFieldProxyMap(false));
        clearProxies(getSetterProxyMap(false));
        clearProxies(getConstructorProxyMap(false));
    }
    
    private <T> void clearProxies(Map<Class<?>, Map<T, ThreadLocalProxy<?>>> tlps) {
        Map<T, ThreadLocalProxy<?>> proxies = tlps == null ? null : tlps.get(getServiceClass());
        if (proxies == null) {
            return;
        }
        for (ThreadLocalProxy<?> tlp : proxies.values()) {
            if (tlp != null) {
                tlp.remove();
            }
        }
    }
    
    private Map<Class<?>, List<Field>> addContextField(Map<Class<?>, List<Field>> theFields, Field f) {
        
        theFields = theFields == null ? new HashMap<Class<?>, List<Field>>() : theFields;
        
        List<Field> fields = theFields.get(serviceClass);
        if (fields == null) {
            fields = new ArrayList<Field>();
            theFields.put(serviceClass, fields);
        }
        if (!fields.contains(f)) {
            fields.add(f);
        }
        return theFields;
    }
    
    private <T, V> void addToMap(Map<Class<?>, Map<T, V>> theFields, 
                               T f, V proxy) {
        Map<T, V> proxies = theFields.get(serviceClass);
        if (proxies == null) {
            proxies = new HashMap<T, V>();
            theFields.put(serviceClass, proxies);
        }
        if (!proxies.containsKey(f)) {
            proxies.put(f, proxy);
        }
    }

    private List<Field> getList(Map<Class<?>, List<Field>> fields) {
        List<Field> ret = fields == null ? null : fields.get(getServiceClass());
        if (ret != null) {
            ret = Collections.unmodifiableList(ret);
        } else {
            ret = Collections.emptyList();
        }
        return ret;
    }
    
    private <T> ThreadLocalProxy<?> getProxy(Map<Class<?>, Map<T, ThreadLocalProxy<?>>> proxies,
                                             T key) {
        
        Map<?, ThreadLocalProxy<?>> theMap = proxies == null ? null : proxies.get(getServiceClass());
        ThreadLocalProxy<?> ret = null;
        if (theMap != null) {
            ret = theMap.get(key);
        }
        return ret;
    }
}
