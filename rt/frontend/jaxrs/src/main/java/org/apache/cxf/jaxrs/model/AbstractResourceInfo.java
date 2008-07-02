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
    
    private boolean root;
    private Class<?> resourceClass;
    private Class<?> serviceClass;
    
    private List<Field> contextFields;
    private List<Field> resourceFields;
    private Map<Class<?>, Method> contextMethods;
    private Map<Field, ThreadLocalProxy> fieldProxyMap;
    private Map<Field, ThreadLocalProxy> resourceProxyMap;
    private Map<Method, ThreadLocalProxy> setterProxyMap;
    
    protected AbstractResourceInfo(Class<?> resourceClass, Class<?> serviceClass, boolean isRoot) {
        this.serviceClass = serviceClass;
        this.resourceClass = resourceClass;
        root = isRoot;
        if (root) {
            initContextFields();
            initContextSetterMethods();
        }
    }
    
    public Class<?> getServiceClass() {
        return serviceClass;
    }
    
    private void initContextFields() {
        if (resourceClass == null || !root) {
            return;
        }
        
        
        for (Field f : getServiceClass().getDeclaredFields()) {
            for (Annotation a : f.getAnnotations()) {
                if (a.annotationType() == Context.class) {
                    if (contextFields == null) {
                        contextFields = new ArrayList<Field>();
                    }
                    contextFields.add(f);
                    if (fieldProxyMap == null) {
                        fieldProxyMap = new HashMap<Field, ThreadLocalProxy>();
                    }
                    fieldProxyMap.put(f, InjectionUtils.createThreadLocalProxy(f.getType()));
                } else if (a.annotationType() == Resource.class) {
                    if (resourceFields == null) {
                        resourceFields = new ArrayList<Field>();
                    }
                    resourceFields.add(f);
                    if (resourceProxyMap == null) {
                        resourceProxyMap = new HashMap<Field, ThreadLocalProxy>();
                    }
                    resourceProxyMap.put(f, InjectionUtils.createThreadLocalProxy(f.getType()));
                }
            }
        }
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
        return contextMethods == null ? Collections.EMPTY_MAP 
                                      : Collections.unmodifiableMap(contextMethods);
    }
    
    private void addContextMethod(Class<?> contextClass, Method m) {
        if (contextMethods == null) {
            contextMethods = new HashMap<Class<?>, Method>();
        }
        contextMethods.put(contextClass, m);
        if (setterProxyMap == null) {
            setterProxyMap = new HashMap<Method, ThreadLocalProxy>();
        }
        setterProxyMap.put(m, 
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
    
    private static List<Field> getList(List<Field> fields) {
        List<Field> ret;
        if (fields != null) {
            ret = Collections.unmodifiableList(fields);
        } else {
            ret = Collections.emptyList();
        }
        return ret;
    }
    
    public ThreadLocalProxy getContextFieldProxy(Field f) {
        return fieldProxyMap == null ? null
               : fieldProxyMap.get(f);
    }
    
    public ThreadLocalProxy getResourceFieldProxy(Field f) {
        return resourceProxyMap == null ? null
               : resourceProxyMap.get(f);
    }
    
    public ThreadLocalProxy getContextSetterProxy(Method m) {
        return setterProxyMap == null ? null : setterProxyMap.get(m);
    }
    
    public abstract boolean isSingleton();
    
    public void clearThreadLocalProxies() {
        clearProxies(fieldProxyMap);
        clearProxies(resourceProxyMap);
        clearProxies(setterProxyMap);
    }
    
    private static void clearProxies(Map<?, ThreadLocalProxy> tlps) {
        if (tlps == null) {
            return;
        }
        for (ThreadLocalProxy tlp : tlps.values()) {
            tlp.remove();
        }
    }
}
