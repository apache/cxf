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
import java.util.WeakHashMap;

import javax.annotation.Resource;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;

public abstract class AbstractResourceInfo {
    private static final String FIELD_PROXY_MAP = "jaxrs-field-proxy-map";
    private static final String SETTER_PROXY_MAP = "jaxrs-setter-proxy-map";
    
    protected boolean root;
    protected Class<?> resourceClass;
    protected Class<?> serviceClass;

    private Map<Class<?>, List<Field>> contextFields;
    private Map<Class<?>, Map<Class<?>, Method>> contextMethods;
    private Bus bus;
    private boolean contextsAvailable;
    
    protected AbstractResourceInfo(Bus bus) {
        this.bus = bus;
    }

    protected AbstractResourceInfo(Class<?> resourceClass, Class<?> serviceClass, 
                                   boolean isRoot, Bus bus) {
        this(resourceClass, serviceClass, isRoot, true, bus, null);
    }

    protected AbstractResourceInfo(Class<?> resourceClass, Class<?> serviceClass, 
                                   boolean isRoot, boolean checkContexts, Bus bus) {
        this(resourceClass, serviceClass, isRoot, checkContexts, bus, null);
    }
    
    protected AbstractResourceInfo(Class<?> resourceClass, Class<?> serviceClass, 
                                   boolean isRoot, boolean checkContexts, Bus bus,
                                   Object provider) {
        this.bus = bus;
        this.serviceClass = serviceClass;
        this.resourceClass = resourceClass;
        root = isRoot;
        if (checkContexts && resourceClass != null) {
            findContexts(serviceClass, provider);
        }
    }
    private void findContexts(Class<?> cls, Object provider) {
        findContextFields(cls, provider);
        findContextSetterMethods(cls, provider);
        contextsAvailable = contextFields != null && !contextFields.isEmpty() 
            || contextMethods != null && !contextMethods.isEmpty();
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
            findContexts(resourceClass, null);
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
                if (a.annotationType() == Context.class || a.annotationType() == Resource.class 
                    && AnnotationUtils.isContextClass(f.getType())) {
                    contextFields = addContextField(contextFields, f);
                    if (f.getType() != Application.class) {
                        addToMap(getFieldProxyMap(), f, getFieldThreadLocalProxy(f, provider));
                    }
                }
            }
        }
        findContextFields(cls.getSuperclass(), provider);
    }
    
    private static ThreadLocalProxy<?> getFieldThreadLocalProxy(Field f, Object provider) {
        if (provider != null) {
            Object proxy = null;
            synchronized (provider) {
                try {
                    proxy = InjectionUtils.extractFieldValue(f, provider);
                } catch (Throwable t) {
                    // continue
                }
                if (!(proxy instanceof ThreadLocalProxy)) {
                    proxy = InjectionUtils.createThreadLocalProxy(f.getType());
                    InjectionUtils.injectFieldValue(f, provider, proxy);
                }
            }
            return (ThreadLocalProxy<?>)proxy;
        } else {
            return InjectionUtils.createThreadLocalProxy(f.getType());
        }
    }
    
    private static ThreadLocalProxy<?> getMethodThreadLocalProxy(Method m, Object provider) {
        if (provider != null) {
            Object proxy = null;
            synchronized (provider) {
                try {
                    proxy = InjectionUtils.extractFromMethod(provider, 
                                                             InjectionUtils.getGetterFromSetter(m), 
                                                             false);
                } catch (Throwable t) {
                    // continue
                }
                if (!(proxy instanceof ThreadLocalProxy)) {
                    proxy = InjectionUtils.createThreadLocalProxy(m.getParameterTypes()[0]);
                    InjectionUtils.injectThroughMethod(provider, m, proxy);
                }
            }
            return (ThreadLocalProxy<?>)proxy;
        } else {
            return InjectionUtils.createThreadLocalProxy(m.getParameterTypes()[0]);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> Map<Class<?>, Map<T, ThreadLocalProxy<?>>> getProxyMap(Class<T> keyCls, String prop) {
        Object property = null;
        synchronized (bus) {
            property = bus.getProperty(prop);
            if (property == null) {
                Map<Class<?>, Map<T, ThreadLocalProxy<?>>> map
                    = Collections.synchronizedMap(new WeakHashMap<Class<?>, Map<T, ThreadLocalProxy<?>>>(2));
                bus.setProperty(prop, map);
                property = map;
            }
        }
        return (Map<Class<?>, Map<T, ThreadLocalProxy<?>>>)property;
    }
    
    private Map<Class<?>, Map<Field, ThreadLocalProxy<?>>> getFieldProxyMap() {
        return getProxyMap(Field.class, FIELD_PROXY_MAP);
    }
    
    private Map<Class<?>, Map<Method, ThreadLocalProxy<?>>> getSetterProxyMap() {
        return getProxyMap(Method.class, SETTER_PROXY_MAP);
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
            addToMap(getSetterProxyMap(), m, getMethodThreadLocalProxy(m, provider));
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
        return getProxy(getFieldProxyMap(), f);
    }
    
    public ThreadLocalProxy<?> getContextSetterProxy(Method m) {
        return getProxy(getSetterProxyMap(), m);
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
        }
    }
    
    public void clearThreadLocalProxies() {
        clearProxies(getFieldProxyMap());
        clearProxies(getSetterProxyMap());
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
        if (theFields == null) {
            theFields = new HashMap<Class<?>, List<Field>>();
        }
        
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
    
    private <T, V> void addToMap(Map<Class<?>, Map<T, V>> proxyMap,
                                 T f, 
                                 V proxy) {
        Map<T, V> proxies = proxyMap.get(serviceClass);
        if (proxies == null) {
            proxies = Collections.synchronizedMap(new WeakHashMap<T, V>());
            proxyMap.put(serviceClass, proxies);
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
