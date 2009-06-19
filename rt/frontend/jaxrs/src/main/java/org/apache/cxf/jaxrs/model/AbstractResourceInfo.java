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

import javax.annotation.Resource;
import javax.ws.rs.core.Context;

import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;

public abstract class AbstractResourceInfo {
    
    private static Map<Class<?>, List<Field>> contextFields;
    private static Map<Class<?>, List<Field>> resourceFields;
    private static Map<Class<?>, Map<Class<?>, Method>> contextMethods;
    private static Map<Class<?>, Map<Field, ThreadLocalProxy>> fieldProxyMap;
    private static Map<Class<?>, Map<Field, ThreadLocalProxy>> resourceProxyMap;
    private static Map<Class<?>, Map<Method, ThreadLocalProxy>> setterProxyMap;
    
    protected boolean root;
    protected Class<?> resourceClass;
    protected Class<?> serviceClass;
    
    protected AbstractResourceInfo() {
        
    }
    
    protected AbstractResourceInfo(Class<?> resourceClass, Class<?> serviceClass, boolean isRoot) {
        this.serviceClass = serviceClass;
        this.resourceClass = resourceClass;
        root = isRoot;
        if (root) {
            initContextFields();
            initContextSetterMethods();
        }
    }
    
    public void setResourceClass(Class<?> rClass) {
        resourceClass = rClass;
    }
    
    public Class<?> getServiceClass() {
        return serviceClass;
    }
    
    private void initContextFields() {
        if (resourceClass == null || !root) {
            return;
        }
        findContextFields(serviceClass);
    }
    
    private void findContextFields(Class<?> cls) {
        if (cls == Object.class || cls == null) {
            return;
        }
        for (Field f : cls.getDeclaredFields()) {
            for (Annotation a : f.getAnnotations()) {
                if (a.annotationType() == Context.class) {
                    if (contextFields == null) {
                        contextFields = new HashMap<Class<?>, List<Field>>();
                    }
                    addContextField(contextFields, f);
                    if (fieldProxyMap == null) {
                        fieldProxyMap = new HashMap<Class<?>, Map<Field, ThreadLocalProxy>>();
                    }
                    addToMap(fieldProxyMap, f, InjectionUtils.createThreadLocalProxy(f.getType()));
                } else if (a.annotationType() == Resource.class 
                           && AnnotationUtils.isContextClass(f.getType())) {
                    if (resourceFields == null) {
                        resourceFields = new HashMap<Class<?>, List<Field>>();
                    }
                    addContextField(resourceFields, f);
                    if (resourceProxyMap == null) {
                        resourceProxyMap = new HashMap<Class<?>, Map<Field, ThreadLocalProxy>>();
                    }
                    addToMap(resourceProxyMap, f, InjectionUtils.createThreadLocalProxy(f.getType()));
                }
            }
        }
        findContextFields(cls.getSuperclass());
    }
    
    private void initContextSetterMethods() {
        
        for (Method m : getServiceClass().getMethods()) {
        
            if (!m.getName().startsWith("set") || m.getParameterTypes().length != 1) {
                continue;
            }
            for (Annotation a : m.getAnnotations()) {
                if (a.annotationType() == Context.class) {
                    checkContextMethod(m);
                    break;
                }
            }
        }
    }
    
    private void checkContextMethod(Method m) {
        Class<?> type = m.getParameterTypes()[0];
        if (AnnotationUtils.isContextClass(type)
            && m.getName().equals("set" + type.getSimpleName())) {        
            addContextMethod(type, m);
        }
    }
    
    @SuppressWarnings("unchecked")
    public Map<Class<?>, Method> getContextMethods() {
        Map<Class<?>, Method> methods = contextMethods == null ? null : contextMethods.get(getServiceClass());
        return methods == null ? Collections.EMPTY_MAP 
                                      : Collections.unmodifiableMap(methods);
    }
    
    private void addContextMethod(Class<?> contextClass, Method m) {
        if (contextMethods == null) {
            contextMethods = new HashMap<Class<?>, Map<Class<?>, Method>>();
        }
        addToMap(contextMethods, contextClass, m);
        if (setterProxyMap == null) {
            setterProxyMap = new HashMap<Class<?>, Map<Method, ThreadLocalProxy>>();
        }
        addToMap(setterProxyMap, m, 
                 InjectionUtils.createThreadLocalProxy(m.getParameterTypes()[0]));
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
    
    public List<Field> getResourceFields() {
        return getList(resourceFields);
    }
    
    public ThreadLocalProxy getContextFieldProxy(Field f) {
        return getProxy(fieldProxyMap, f);
    }
    
    public ThreadLocalProxy getResourceFieldProxy(Field f) {
        return getProxy(resourceProxyMap, f);
    }
    
    public ThreadLocalProxy getContextSetterProxy(Method m) {
        return getProxy(setterProxyMap, m);
    }
    
    public abstract boolean isSingleton();
    
    public void clearThreadLocalProxies() {
        clearProxies(fieldProxyMap);
        clearProxies(resourceProxyMap);
        clearProxies(setterProxyMap);
    }
    
    private <T> void clearProxies(Map<Class<?>, Map<T, ThreadLocalProxy>> tlps) {
        Map<T, ThreadLocalProxy> proxies = tlps == null ? null : tlps.get(getServiceClass());
        if (proxies == null) {
            return;
        }
        for (ThreadLocalProxy tlp : proxies.values()) {
            if (tlp != null) {
                tlp.remove();
            }
        }
    }
    
    private void addContextField(Map<Class<?>, List<Field>> theFields, Field f) {
        List<Field> fields = theFields.get(serviceClass);
        if (fields == null) {
            fields = new ArrayList<Field>();
            theFields.put(serviceClass, fields);
        }
        if (!fields.contains(f)) {
            fields.add(f);
        }
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
    
    private <T> ThreadLocalProxy getProxy(Map<Class<?>, Map<T, ThreadLocalProxy>> proxies, T key) {
        
        Map<T, ThreadLocalProxy> theMap = proxies == null ? null : proxies.get(getServiceClass());
        
        return theMap != null ? theMap.get(key) : null;
    }
}
