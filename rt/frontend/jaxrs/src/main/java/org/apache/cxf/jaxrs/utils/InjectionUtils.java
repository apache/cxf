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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyWorkers;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PrimitiveUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.ParameterHandler;
import org.apache.cxf.jaxrs.impl.PathSegmentImpl;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalContextResolver;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalHttpHeaders;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalHttpServletRequest;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalHttpServletResponse;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalMessageContext;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProviders;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalRequest;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalSecurityContext;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalServletConfig;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalServletContext;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalUriInfo;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.message.Message;

public final class InjectionUtils {
    
    private static final Logger LOG = LogUtils.getL7dLogger(InjectionUtils.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(InjectionUtils.class);
    
    private InjectionUtils() {
        
    }

    public static boolean invokeBooleanGetter(Object o, String name) {
        try {
            Method method = o.getClass().getMethod(name, new Class[]{});
            return (Boolean)method.invoke(o, new Object[]{});
        } catch (Exception ex) {
            LOG.finest("Can not invoke method " + name + " on object of class " + o.getClass().getName());
        }
        return false;
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
                    reportServerError("FIELD_ACCESS_FAILURE", 
                                      f.getType().getName());
                }
                return null;
            }
        });
        
    }
    
    public static Class<?> getActualType(Type genericType) {
        
        if (genericType == null) {
            return null;
        }
        if (!ParameterizedType.class.isAssignableFrom(genericType.getClass())) {
            Class<?> cls =  (Class<?>)genericType;
            return cls.isArray() ? cls.getComponentType() : null;
        }
        ParameterizedType paramType = (ParameterizedType)genericType;
        return (Class<?>)paramType.getActualTypeArguments()[0];
    }
    
    public static Type[] getActualTypes(Type genericType) {
        if (genericType == null 
            || !ParameterizedType.class.isAssignableFrom(genericType.getClass())) {
            return null;
        }
        ParameterizedType paramType = (ParameterizedType)genericType;
        return paramType.getActualTypeArguments();
    }
    
    public static void injectThroughMethod(Object requestObject,
                                           Method method,
                                           Object parameterValue) {
        try {
            Method methodToInvoke = checkProxy(method, requestObject);
            methodToInvoke.invoke(requestObject, new Object[]{parameterValue});
        } catch (IllegalAccessException ex) {
            reportServerError("METHOD_ACCESS_FAILURE", method.getName());
        } catch (Exception ex) {
            reportServerError("METHOD_INJECTION_FAILURE", method.getName());
        }
    }
    
    public static Object handleParameter(String value, 
                                         Class<?> pClass, 
                                         ParameterType pType,
                                         Message message) {
        
        if (value == null) {
            return null;
        }
        
        if (pType == ParameterType.PATH) {
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
            return c.newInstance(new Object[]{value});
        } catch (NoSuchMethodException ex) {
            // try valueOf
        } catch (WebApplicationException ex) {
            throw ex;
        } catch (Exception ex) { 
            LOG.severe(new org.apache.cxf.common.i18n.Message("CLASS_CONSTRUCTOR_FAILURE", 
                                                               BUNDLE, 
                                                               pClass.getName()).toString());
            throw new WebApplicationException(ex, HttpUtils.getParameterFailureStatus(pType));
        }
        
        // check for valueOf(String) static methods
        String[] methodNames = pClass.isEnum() 
            ? new String[] {"fromString", "valueOf"} 
            : new String[] {"valueOf", "fromString"};
        Object result = evaluateFactoryMethod(value, pClass, pType, methodNames[0]);
        if (result == null) {
            result = evaluateFactoryMethod(value, pClass, pType, methodNames[1]);
        }
        
        if (message != null) {
            ParameterHandler<?> pm = ProviderFactory.getInstance(message)
                .createParameterHandler(pClass);
            if (pm != null) {
                result = pm.fromString(value);
            }
        }
        
        if (result != null) {
            return result;
        }
        
        reportServerError("WRONG_PARAMETER_TYPE", pClass.getName());
        return null;
    }

    public static void reportServerError(String messageName, String parameter) {
        org.apache.cxf.common.i18n.Message errorMessage = 
            new org.apache.cxf.common.i18n.Message(messageName, 
                                                   BUNDLE, 
                                                   parameter);
        LOG.severe(errorMessage.toString());
        Response r = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                         .type(MediaType.TEXT_PLAIN_TYPE)
                         .entity(errorMessage.toString()).build();
        throw new WebApplicationException(r);
    }
    
    private static Object evaluateFactoryMethod(String value,
                                                Class<?> pClass, 
                                                ParameterType pType, 
                                                String methodName) {
        try {
            Method m = pClass.getMethod(methodName, new Class<?>[]{String.class});
            if (Modifier.isStatic(m.getModifiers())) {
                return m.invoke(null, new Object[]{value});
            }
        } catch (NoSuchMethodException ex) {
            // no luck
        } catch (Exception ex) {
            Throwable t = ex instanceof InvocationTargetException 
                ? ((InvocationTargetException)ex).getTargetException() : ex; 
            LOG.severe(new org.apache.cxf.common.i18n.Message("CLASS_VALUE_OF_FAILURE", 
                                                               BUNDLE, 
                                                               pClass.getName()).toString());
            throw new WebApplicationException(t, HttpUtils.getParameterFailureStatus(pType));
        }
        return null;
    }
    
    public static Object handleBean(Class<?> paramType, MultivaluedMap<String, String> values,
                                    ParameterType pType, Message message) {
        Object bean = null;
        try {
            bean = paramType.newInstance();
        } catch (IllegalAccessException ex) {
            reportServerError("CLASS_ACCESS_FAILURE", paramType.getName());
        } catch (Exception ex) {
            reportServerError("CLASS_INSTANTIATION_FAILURE", paramType.getName());
        }    
        
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            boolean injected = false;
            for (Method m : paramType.getMethods()) {
                if (m.getName().equalsIgnoreCase("set" + entry.getKey())
                    && m.getParameterTypes().length == 1) {
                    Object paramValue = handleParameter(entry.getValue().get(0), 
                                                        m.getParameterTypes()[0],
                                                        pType, message);
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
                                                        f.getType(), pType, message);
                    if (paramValue != null) {
                        injectFieldValue(f, bean, paramValue);
                        break;
                    }
                }
            }
        }
        
        return bean;
    }
    
    @SuppressWarnings("unchecked")
    public static Object injectIntoList(Type genericType, List<String> values,
                                        boolean decoded, ParameterType pathParam, Message message) {
        Class<?> realType = InjectionUtils.getActualType(genericType);
        values = checkPathSegment(values, realType, pathParam);
        List theValues = new ArrayList();
        for (String r : values) {
            String value = decodeValue(r, decoded, pathParam);
            
            Object o = InjectionUtils.handleParameter(value, realType, pathParam, message);
            if (o != null) {
                theValues.add(o);
            }
        }
        return theValues;
    }
    
    public static Object injectIntoArray(Type genericType, List<String> values,
                                         boolean decoded, ParameterType pathParam, Message message) {
        Class<?> realType = InjectionUtils.getActualType(genericType);
        values = checkPathSegment(values, realType, pathParam);
        Object[] array = (Object[])Array.newInstance(realType, values.size());
        for (int i = 0; i < values.size(); i++) {
            String value = decodeValue(values.get(i), decoded, pathParam);
            Object o = InjectionUtils.handleParameter(value, realType, pathParam, message);
            if (o != null) {
                array[i] = o;
            }
        }
        return array;
    }
    
    
    @SuppressWarnings("unchecked")
    public static Object injectIntoSet(Type genericType, List<String> values, 
                                       boolean sorted, 
                                       boolean decoded, 
                                       ParameterType pathParam, Message message) {
        Class<?> realType = InjectionUtils.getActualType(genericType);
        
        values = checkPathSegment(values, realType, pathParam);
        
        Set theValues = sorted ? new TreeSet() : new HashSet();
        for (String r : values) {
            String value = decodeValue(r, decoded, pathParam);
            Object o = InjectionUtils.handleParameter(value, realType, pathParam, message);
            if (o != null) {
                theValues.add(o);
            }
        }
        return theValues;
    }
    
    private static List<String> checkPathSegment(List<String> values, Class<?> type, 
                                                 ParameterType pathParam) {
        if (pathParam != ParameterType.PATH || !PathSegment.class.isAssignableFrom(type)) {
            return values;
        }
        List<String> newValues = new ArrayList<String>();
        for (String v : values) {
            String[] segments = v.split("/");
            for (String s : segments) {
                if (s.length() != 0) {
                    newValues.add(s);
                }
            }
            if (v.endsWith("/")) {
                newValues.add("");
            }
        }
        return newValues;
    }
    
    public static Object createParameterObject(List<String> paramValues,
                                               Class<?> paramType,
                                               Type genericType,
                                               String defaultValue,
                                               boolean decoded,
                                               ParameterType pathParam,
                                               Message message) {
        
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

        Object value = null;
        if (List.class.isAssignableFrom(paramType)) {
            value = InjectionUtils.injectIntoList(genericType, paramValues, decoded, pathParam,
                                                 message);
        } else if (Set.class.isAssignableFrom(paramType)) {
            value = InjectionUtils.injectIntoSet(genericType, paramValues, false, decoded, pathParam,
                                                message);
        } else if (SortedSet.class.isAssignableFrom(paramType)) {
            value = InjectionUtils.injectIntoSet(genericType, paramValues, true, decoded, pathParam,
                                                message);
        } else if (paramType.isArray()) {
            value = InjectionUtils.injectIntoArray(genericType, paramValues, decoded, pathParam, message);
        } else {
            String result = null;
            if (paramValues.size() > 0) {
                boolean isLast = pathParam == ParameterType.PATH ? true : false;
                result = isLast ? paramValues.get(paramValues.size() - 1)
                                : paramValues.get(0);
            }
            if (result != null) {
                result = decodeValue(result, decoded, pathParam);
                value = InjectionUtils.handleParameter(result, paramType, pathParam, message);
            }
        }
        return value;
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
            proxy = new ThreadLocalProviders();
        } else if (HttpServletRequest.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalHttpServletRequest();
        } else if (ServletContext.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalServletContext();
        } else if (HttpServletResponse.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalHttpServletResponse();
        } else if (MessageContext.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalMessageContext();
        }  else if (ServletConfig.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalServletConfig();
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
    
    public static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive() 
            || Number.class.isAssignableFrom(type)
            || Boolean.class.isAssignableFrom(type)
            || String.class == type;
    }
    
    private static String decodeValue(String value, boolean decode, ParameterType param) {
        if (!decode) {
            return value;
        }
        if (param == ParameterType.PATH || param == ParameterType.MATRIX) {
            return HttpUtils.pathDecode(value);
        } else {
            return HttpUtils.urlDecode(value);
        }
    }
}
