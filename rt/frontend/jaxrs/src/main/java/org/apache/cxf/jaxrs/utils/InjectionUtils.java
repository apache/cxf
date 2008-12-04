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

package org.apache.cxf.jaxrs.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyWorkers;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PrimitiveUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.impl.PathSegmentImpl;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalContextResolver;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalHttpHeaders;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalHttpServletRequest;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalHttpServletResponse;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalMessageBodyWorkers;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalMessageContext;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalRequest;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalSecurityContext;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalServletContext;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalUriInfo;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.message.Message;

public final class InjectionUtils {
    
    private static final Logger LOG = LogUtils.getL7dLogger(InjectionUtils.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(InjectionUtils.class);
    
    private InjectionUtils() {
        
    }

    public static Method checkProxy(Method methodToInvoke, Object resourceObject) {
        if (Proxy.class.isInstance(resourceObject)) {
            
            for (Class<?> c : resourceObject.getClass().getInterfaces()) {
                try {
                    Method m = c.getMethod(
                        methodToInvoke.getName(), methodToInvoke.getParameterTypes());
                    if (m != null) {
                        return m;
                    }
                } catch (NoSuchMethodException ex) {
                    //ignore
                }
            }
            
        }
        return methodToInvoke; 
    }
 
    @SuppressWarnings("unchecked")
    public static void injectFieldValue(final Field f, 
                                        final Object o, 
                                        final Object v) {
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                f.setAccessible(true);
                try {
                    f.set(o, v);
                } catch (IllegalAccessException ex) {
                    LOG.warning(new org.apache.cxf.common.i18n.Message("FIELD_INJECTION_FAILURE", 
                                                                       BUNDLE, 
                                                                       f.getName()).toString());
                }
                return null;
            }
        });
        
    }
    
    public static Class<?> getActualType(Type genericType) {
        if (genericType == null 
            || !ParameterizedType.class.isAssignableFrom(genericType.getClass())) {
            return null;
        }
        ParameterizedType paramType = (ParameterizedType)genericType;
        return (Class<?>)paramType.getActualTypeArguments()[0];
    }
    
    public static void injectThroughMethod(Object requestObject,
                                           Method method,
                                           Object parameterValue) {
        try {
            Method methodToInvoke = checkProxy(method, requestObject);
            methodToInvoke.invoke(requestObject, new Object[]{parameterValue});
        } catch (Exception ex) {
            LOG.warning(new org.apache.cxf.common.i18n.Message("METHOD_INJECTION_FAILURE", 
                                                               BUNDLE, 
                                                               method.getName()).toString());
        }
    }
    
    public static Object handleParameter(String value, Class<?> pClass, boolean pathParam) {
        
        if (pathParam) {
            PathSegment ps = new PathSegmentImpl(value, false);    
            if (PathSegment.class.isAssignableFrom(pClass)) {
                return ps;   
            } else {
                value = ps.getPath();                 
            }
        }
        
        if (pClass.isPrimitive()) {
            return PrimitiveUtils.read(value, pClass);
        }
        // check constructors accepting a single String value
        try {
            Constructor<?> c = pClass.getConstructor(new Class<?>[]{String.class});
            if (c !=  null) {
                return c.newInstance(new Object[]{value});
            }
        } catch (Exception ex) {
            // try valueOf
        }
        // check for valueOf(String) static methods
        try {
            Method m = pClass.getMethod("valueOf", new Class<?>[]{String.class});
            if (m != null && Modifier.isStatic(m.getModifiers())) {
                return m.invoke(null, new Object[]{value});
            }
        } catch (Exception ex) {
            // no luck
        }
        
        return null;
    }
    
    public static Object handleBean(Class<?> paramType, MultivaluedMap<String, String> values,
                                    boolean pathParam) {
        Object bean = null;
        try {
            bean = paramType.newInstance();
            for (Map.Entry<String, List<String>> entry : values.entrySet()) {
                boolean injected = false;
                for (Method m : paramType.getMethods()) {
                    if (m.getName().equalsIgnoreCase("set" + entry.getKey())
                        && m.getParameterTypes().length == 1) {
                        Object paramValue = handleParameter(entry.getValue().get(0), 
                                                            m.getParameterTypes()[0],
                                                            pathParam);
                        if (paramValue != null) {
                            injectThroughMethod(bean, m, paramValue);
                            injected = true;
                            break;
                        }
                    }
                }
                if (injected) {
                    continue;
                }
                for (Field f : paramType.getFields()) {
                    if (f.getName().equalsIgnoreCase(entry.getKey())) {
                        Object paramValue = handleParameter(entry.getValue().get(0), 
                                                            f.getType(), pathParam);
                        if (paramValue != null) {
                            injectFieldValue(f, bean, paramValue);
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOG.warning(new org.apache.cxf.common.i18n.Message("CLASS_INSTANCIATION_FAILURE", 
                                                               BUNDLE, 
                                                               paramType.getName()).toString());    
        }
        return bean;
    }
    
    @SuppressWarnings("unchecked")
    public static Object injectIntoList(Type genericType, List<String> values,
                                        boolean decoded, boolean pathParam) {
        Class<?> realType = InjectionUtils.getActualType(genericType);
        List theValues = new ArrayList();
        for (String r : values) {
            if (decoded) {
                r = JAXRSUtils.uriDecode(r);
            }
            Object o = InjectionUtils.handleParameter(r, realType, pathParam);
            if (o != null) {
                theValues.add(o);
            }
        }
        return theValues;
    }
    
    
    
    @SuppressWarnings("unchecked")
    public static Object injectIntoSet(Type genericType, List<String> values, 
                                       boolean sorted, boolean decoded, boolean pathParam) {
        Class<?> realType = InjectionUtils.getActualType(genericType);
        Set theValues = sorted ? new TreeSet() : new HashSet();
        for (String r : values) {
            if (decoded) {
                r = JAXRSUtils.uriDecode(r);
            }
            Object o = InjectionUtils.handleParameter(r, realType, pathParam);
            if (o != null) {
                theValues.add(o);
            }
        }
        return theValues;
    }
    
    public static Object createParameterObject(List<String> paramValues,
                                               Class<?> paramType,
                                               Type genericType,
                                               String defaultValue,
                                               boolean isLast,
                                               boolean decoded,
                                               boolean pathParam) {
        
        if (paramValues == null) {
            if (defaultValue != null) {
                paramValues = Collections.singletonList(defaultValue);
            } else {
                if (paramType.isPrimitive()) {
                    paramValues = Collections.singletonList(
                        boolean.class == paramType ? "false" : "0");
                } else {
                    return null;
                }
            }
        }
        
        if (List.class.isAssignableFrom(paramType)) {
            return InjectionUtils.injectIntoList(genericType, paramValues, decoded, pathParam);
        } else if (Set.class.isAssignableFrom(paramType)) {
            return InjectionUtils.injectIntoSet(genericType, paramValues, false, decoded, pathParam);
        } else if (SortedSet.class.isAssignableFrom(paramType)) {
            return InjectionUtils.injectIntoSet(genericType, paramValues, true, decoded, pathParam);
        } else {
            String result = null;
            if (paramValues.size() > 0) {
                result = isLast ? paramValues.get(paramValues.size() - 1)
                                : paramValues.get(0);
            }
            if (result != null) {
                if (decoded) {
                    result = JAXRSUtils.uriDecode(result);
                }
                return InjectionUtils.handleParameter(result, paramType, pathParam);
            } else {
                return null;
            }
        }
    }
    
    public static ThreadLocalProxy createThreadLocalProxy(Class<?> type) {
        ThreadLocalProxy proxy = null;
        if (UriInfo.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalUriInfo();
        } else if (HttpHeaders.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalHttpHeaders();
        } else if (SecurityContext.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalSecurityContext();
        } else if (ContextResolver.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalContextResolver();
        } else if (Request.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalRequest();
        }  else if (MessageBodyWorkers.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalMessageBodyWorkers();
        } else if (HttpServletRequest.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalHttpServletRequest();
        } else if (ServletContext.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalServletContext();
        } else if (HttpServletResponse.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalHttpServletResponse();
        } else if (MessageContext.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalMessageContext();
        }
        return proxy;
    }
    
    public static void injectContextProxies(AbstractResourceInfo cri, Object instance) {
        if (!cri.isSingleton()) {
            return;
        }
        
        for (Map.Entry<Class<?>, Method> entry : cri.getContextMethods().entrySet()) {
            ThreadLocalProxy proxy = cri.getContextSetterProxy(entry.getValue());
            InjectionUtils.injectThroughMethod(instance, entry.getValue(), proxy);
        }
        
        for (Field f : cri.getContextFields()) {
            ThreadLocalProxy proxy = cri.getContextFieldProxy(f);
            InjectionUtils.injectFieldValue(f, instance, proxy);
        }
        
        for (Field f : cri.getResourceFields()) {
            ThreadLocalProxy proxy = cri.getResourceFieldProxy(f);
            InjectionUtils.injectFieldValue(f, instance, proxy);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static void injectContextField(AbstractResourceInfo cri, 
                                          Field f, Object o, Object value,
                                          boolean resource) {
        if (!cri.isSingleton()) {
            InjectionUtils.injectFieldValue(f, o, value);
        } else {
            ThreadLocalProxy proxy = resource ? cri.getResourceFieldProxy(f)
                                              : cri.getContextFieldProxy(f);
            if (proxy != null) {
                proxy.set(value);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public static void injectContextMethods(Object requestObject,
                                            AbstractResourceInfo cri,
                                            Message message) {
        for (Map.Entry<Class<?>, Method> entry : cri.getContextMethods().entrySet()) {
            Object o = JAXRSUtils.createContextValue(message, 
                                              entry.getValue().getGenericParameterTypes()[0],
                                              entry.getKey());
            
            if (o != null) {
                if (!cri.isSingleton()) {
                    InjectionUtils.injectThroughMethod(requestObject, entry.getValue(), o);
                } else {
                    ThreadLocalProxy proxy = cri.getContextSetterProxy(entry.getValue());
                    if (proxy != null) {
                        proxy.set(o);
                    }
                }
                
            }
        }
    }
    
    // TODO : should we have context and resource fields be treated as context fields ?
    
    public static void injectContextFields(Object o,
                                           AbstractResourceInfo cri,
                                           Message m) {
        
        for (Field f : cri.getContextFields()) {
            Object value = JAXRSUtils.createContextValue(m, f.getGenericType(), f.getType());
            InjectionUtils.injectContextField(cri, f, o, value, false);
        }
    }
    
    public static void injectResourceFields(Object o,
                                            AbstractResourceInfo cri,
                                            Message m) {
        
        for (Field f : cri.getResourceFields()) {
            Object value = JAXRSUtils.createResourceValue(m, f.getGenericType(), f.getType());
            InjectionUtils.injectContextField(cri, f, o, value, true);
        }
    }
}
