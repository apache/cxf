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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import javax.ws.rs.ext.Providers;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PrimitiveUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.ParameterHandler;
import org.apache.cxf.jaxrs.ext.ProtocolHeaders;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.PathSegmentImpl;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalContextResolver;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalHttpHeaders;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalHttpServletRequest;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalHttpServletResponse;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalMessageContext;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProtocolHeaders;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProviders;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalRequest;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalSecurityContext;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalServletConfig;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalServletContext;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalUriInfo;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.message.Message;

public final class InjectionUtils {
    
    private static final Logger LOG = LogUtils.getL7dLogger(InjectionUtils.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(InjectionUtils.class);
    
    private InjectionUtils() {
        
    }

    public static boolean isConcreteClass(Class<?> cls) {
        return !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers());
    }
    
    public static Type getSuperType(Class<?> serviceClass, TypeVariable var) {
        
        int pos = 0;
        TypeVariable[] vars = var.getGenericDeclaration().getTypeParameters();
        for (; pos < vars.length; pos++) {
            if (vars[pos].getName().equals(var.getName())) {
                break;
            }
        }
        
        Type[] bounds = var.getBounds();
        if (bounds.length > pos && bounds[pos] != Object.class) {
            return bounds[pos];
        }
                
        Type genericSubtype = serviceClass.getGenericSuperclass();
        if (genericSubtype == Object.class) {
            Type[] genInterfaces = serviceClass.getGenericInterfaces();
            for (Type t : genInterfaces) {
                genericSubtype = t;
                break;
            }
        }
        Type result = genericSubtype != Object.class ? InjectionUtils.getActualType(genericSubtype, pos)
                                              : genericSubtype;
        return result == null ? Object.class : result;
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

    @SuppressWarnings("unchecked")
    public static Object extractFieldValue(final Field f, 
                                        final Object o) {
        return AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                f.setAccessible(true);
                try {
                    return f.get(o);
                } catch (IllegalAccessException ex) {
                    reportServerError("FIELD_ACCESS_FAILURE", 
                                      f.getType().getName());
                }
                return null;
            }
        });
    }
    
    public static Class<?> getActualType(Type genericType) {
        
        return getActualType(genericType, 0);
    }
    
    public static Class<?> getActualType(Type genericType, int pos) {
        
        if (genericType == null) {
            return null;
        }
        if (!ParameterizedType.class.isAssignableFrom(genericType.getClass())) {
            if (genericType instanceof TypeVariable) {
                genericType = getType(((TypeVariable)genericType).getBounds(), pos);
            }
            Class<?> cls = (Class<?>)genericType;
            return cls.isArray() ? cls.getComponentType() : cls;
        }
        ParameterizedType paramType = (ParameterizedType)genericType;
        Type t = getType(paramType.getActualTypeArguments(), pos);
        return t instanceof Class ? (Class<?>)t : getActualType(t, pos);
    }
    
    public static Type getType(Type[] types, int pos) {
        if (pos >= types.length) {
            throw new RuntimeException("No type can be found at position " + pos);
        }
        return types[pos];    
    }
    
    public static Class<?> getRawType(Type genericType) {
        
        if (genericType == null) {
            return null;
        }
        if (!ParameterizedType.class.isAssignableFrom(genericType.getClass())) {
            return (Class<?>)genericType;
        }
        ParameterizedType paramType = (ParameterizedType)genericType;
        
        Type t = paramType.getRawType();
        
        return t instanceof Class ? (Class<?>)t : null;
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

    public static Object extractFromMethod(Object requestObject,
                                           Method method) {
        try {
            Method methodToInvoke = checkProxy(method, requestObject);
            return methodToInvoke.invoke(requestObject);
        } catch (IllegalAccessException ex) {
            reportServerError("METHOD_ACCESS_FAILURE", method.getName());
        } catch (Exception ex) {
            reportServerError("METHOD_INJECTION_FAILURE", method.getName());
        }
        return null;
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
            try {
                return PrimitiveUtils.read(value, pClass);
            } catch (NumberFormatException nfe) {
                //
                //  For a path parameter this is probably a 404,
                //  for others a 400...
                //
                if (pType == ParameterType.PATH) {
                    throw new WebApplicationException(nfe, Response.Status.NOT_FOUND);
                }
                throw new WebApplicationException(nfe, Response.Status.BAD_REQUEST);
            }
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
        
        Object result = null;
        // check for valueOf(String) static methods
        String[] methodNames = pClass.isEnum() 
            ? new String[] {"fromString", "fromValue", "valueOf"} 
            : new String[] {"valueOf", "fromString"};
        for (String mName : methodNames) {   
            result = evaluateFactoryMethod(value, pClass, pType, mName);
            if (result != null) {
                break;
            }
        }
        
        if (result == null && message != null) {
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
                                    ParameterType pType, Message message, boolean decoded) {
        Object bean = null;
        try {
            bean = paramType.newInstance();
        } catch (IllegalAccessException ex) {
            reportServerError("CLASS_ACCESS_FAILURE", paramType.getName());
        } catch (Exception ex) {
            reportServerError("CLASS_INSTANTIATION_FAILURE", paramType.getName());
        }    
        
        Map<String, MultivaluedMap<String, String>> parsedValues =
            new HashMap<String, MultivaluedMap<String, String>>();
        for (Map.Entry<String, List<String>> entry : values.entrySet()) {
            String memberKey = entry.getKey();
            String beanKey = null;

            int idx = memberKey.indexOf('.');
            if (idx == -1) {
                beanKey = "." + memberKey;
            } else {
                beanKey = memberKey.substring(0, idx);
                memberKey = memberKey.substring(idx + 1);
            }

            MultivaluedMap<String, String> value = parsedValues.get(beanKey);
            if (value == null) {
                value = new MetadataMap<String, String>();
                parsedValues.put(beanKey, value);
            }
            value.put(memberKey, entry.getValue());
        }

        if (parsedValues.size() > 0) {
            for (Map.Entry<String, MultivaluedMap<String, String>> entry : parsedValues.entrySet()) {
                String memberKey = entry.getKey();

                boolean isbean = !memberKey.startsWith(".");
                if (!isbean) {
                    memberKey = memberKey.substring(1);
                }

                Object setter = null;
                Object getter = null;
                for (Method m : paramType.getMethods()) {
                    if (m.getName().equalsIgnoreCase("set" + memberKey)
                        && m.getParameterTypes().length == 1) {
                        setter = m;
                    } else if (m.getName().equalsIgnoreCase("get" + memberKey)
                        && m.getReturnType() != Void.TYPE) {
                        getter = m;
                    }
                    if (setter != null && getter != null) {
                        break;
                    }
                }
                if (setter == null) {
                    for (Field f : paramType.getFields()) {
                        if (f.getName().equalsIgnoreCase(memberKey)) {
                            setter = f;
                            getter = f;
                            break;
                        }
                    }
                }

                if (setter != null && getter != null) {
                    Class<?> type = null;
                    Type genericType = null;
                    Object paramValue = null;
                    if (setter instanceof Method) {
                        type = Method.class.cast(setter).getParameterTypes()[0];
                        genericType = Method.class.cast(setter).getGenericParameterTypes()[0];
                        paramValue = InjectionUtils.extractFromMethod(bean, (Method) getter);
                    } else {
                        type = Field.class.cast(setter).getType();
                        genericType = Field.class.cast(setter).getGenericType();
                        paramValue = InjectionUtils.extractFieldValue((Field) getter, bean);
                    }

                    List<MultivaluedMap<String, String>> processedValuesList =
                        processValues(type, genericType, entry.getValue(), isbean);

                    for (MultivaluedMap<String, String> processedValues : processedValuesList) {
                        if (InjectionUtils.isSupportedCollectionOrArray(type)) {
                            Object appendValue = InjectionUtils.injectIntoCollectionOrArray(type,
                                                            genericType, processedValues,
                                                            isbean, true,
                                                            pType, message);
                            paramValue = InjectionUtils.mergeCollectionsOrArrays(paramValue, appendValue,
                                                            genericType);
                        } else if (isbean) {
                            paramValue = InjectionUtils.handleBean(type, processedValues,
                                                            pType, message, decoded);
                        } else {
                            String value = decodeValue(processedValues.values().iterator().next().get(0),
                                                       decoded, pType);
                            paramValue = InjectionUtils.handleParameter(value, type, pType, message);
                        }

                        if (paramValue != null) {
                            if (setter instanceof Method) {
                                InjectionUtils.injectThroughMethod(bean, (Method) setter, paramValue);
                            } else {
                                InjectionUtils.injectFieldValue((Field) setter, bean, paramValue);
                            }
                        }
                    }
                }
            }
        }
        
        return bean;
    }

    private static List<MultivaluedMap<String, String>> processValues(Class<?> type, Type genericType,
                                        MultivaluedMap<String, String> values,
                                        boolean isbean) {
        List<MultivaluedMap<String, String>> valuesList =
            new ArrayList<MultivaluedMap<String, String>>();

        if (isbean && InjectionUtils.isSupportedCollectionOrArray(type)) {
            Class<?> realType = InjectionUtils.getActualType(genericType);
            for (Map.Entry<String, List<String>> entry : values.entrySet()) {
                String memberKey = entry.getKey();
                Class<?> memberType = null;

                for (Method m : realType.getMethods()) {
                    if (m.getName().equalsIgnoreCase("set" + memberKey)
                        && m.getParameterTypes().length == 1) {
                        memberType = m.getParameterTypes()[0];
                        break;
                    }
                }
                if (memberType == null) {
                    for (Field f : realType.getFields()) {
                        if (f.getName().equalsIgnoreCase(memberKey)) {
                            memberType = f.getType();
                            break;
                        }
                    }
                }

                // Strip values tied to collection/array fields from beans that are within
                // collection/array themselves, the only way to support this would be to have
                // an indexing syntax for nested beans, perhaps like this:
                //    a(0).b=1&a(0).b=2&a(1).b=3&a(1).b=4
                // For now though we simply don't support this capability. To illustrate, the 'c'
                // param is dropped from this multivaluedmap example since it is a list:
                //    {c=[71, 81, 91, 72, 82, 92], a=[C1, C2], b=[790, 791]}
                if (memberType != null && InjectionUtils.isSupportedCollectionOrArray(memberType)) {
                    continue;
                }

                // Split multivaluedmap value list contents into separate multivaluedmap instances
                // whose list contents are only 1 level deep, for example: 
                //    {a=[C1, C2], b=[790, 791]}
                // becomes these 2 separate multivaluedmap instances:
                //    {a=[C1], b=[790]} and {a=[C2], b=[791]}
                int idx = 0;
                for (String value : entry.getValue()) {
                    MultivaluedMap<String, String> splitValues =
                        (idx < valuesList.size()) ? valuesList.get(idx) : null;
                    if (splitValues == null) {
                        splitValues = new MetadataMap<String, String>();
                        valuesList.add(splitValues);
                    }
                    splitValues.add(memberKey, value);
                    idx++;
                }
            }
        } else {
            valuesList.add(values);
        }

        return valuesList;
    }

    public static boolean isSupportedCollectionOrArray(Class<?> type) {
        return Collection.class.isAssignableFrom(type) || type.isArray();
    }

    @SuppressWarnings("unchecked")
    private static Object mergeCollectionsOrArrays(Object first, Object second, Type genericType) {
        if (first == null) {
            return second;
        } else if (first instanceof Collection) {
            Collection.class.cast(first).addAll((Collection) second);
            return first;
        } else {
            int firstLen = Array.getLength(first);
            int secondLen = Array.getLength(second);
            Object mergedArray = Array.newInstance(InjectionUtils.getActualType(genericType),
                                                    firstLen + secondLen);
            System.arraycopy(first, 0, mergedArray, 0, firstLen);
            System.arraycopy(second, 0, mergedArray, firstLen, secondLen);
            return mergedArray;
        }
    }

    
    static Class<?> getCollectionType(Class<?> rawType) {
        Class<?> type = null;
        if (SortedSet.class.isAssignableFrom(rawType)) {
            type = TreeSet.class;
        } else if (Set.class.isAssignableFrom(rawType)) {
            type = HashSet.class;
        } else if (Collection.class.isAssignableFrom(rawType)) {
            type = ArrayList.class;
        }
        return type;
        
    }
    
    private static Object injectIntoCollectionOrArray(Class<?> rawType, Type genericType,
                                        MultivaluedMap<String, String> values,
                                        boolean isbean, boolean decoded,
                                        ParameterType pathParam, Message message) {
        Class<?> type = getCollectionType(rawType);

        Class<?> realType = InjectionUtils.getActualType(genericType);
        
        Object theValues = null;
        if (type != null) {
            try {
                theValues = type.newInstance();
            } catch (IllegalAccessException ex) {
                reportServerError("CLASS_ACCESS_FAILURE", type.getName());
            } catch (Exception ex) {
                reportServerError("CLASS_INSTANTIATION_FAILURE", type.getName());
            }
        } else {
            theValues = Array.newInstance(realType, isbean ? 1 : values.values().iterator().next().size());
        }
        if (isbean) {
            Object o = InjectionUtils.handleBean(realType, values, pathParam, message, decoded);
            addToCollectionValues(theValues, o, 0);
        } else {
            List<String> valuesList = values.values().iterator().next();
            valuesList = checkPathSegment(valuesList, realType, pathParam);
            for (int ind = 0; ind < valuesList.size(); ind++) {
                String value = decodeValue(valuesList.get(ind), decoded, pathParam);
                Object o = InjectionUtils.handleParameter(value, realType, pathParam, message);
                addToCollectionValues(theValues, o, ind);
            }
        }
        return theValues;
    }
    
    @SuppressWarnings("unchecked")
    private static void addToCollectionValues(Object theValues, Object o, int index) {
        if (o != null) {
            if (theValues instanceof Collection) {
                Collection.class.cast(theValues).add(o);
            } else {
                ((Object[]) theValues)[index] = o;
            }
        }
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
                } else if (InjectionUtils.isSupportedCollectionOrArray(paramType)) {
                    paramValues = Collections.emptyList();
                } else {
                    return null;
                }
            }
        }

        Object value = null;
        if (InjectionUtils.isSupportedCollectionOrArray(paramType)) {
            MultivaluedMap<String, String> paramValuesMap = new MetadataMap<String, String>();
            paramValuesMap.put("", paramValues);
            value = InjectionUtils.injectIntoCollectionOrArray(paramType, genericType, paramValuesMap,
                                                false, decoded, pathParam, message);
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
        } else if (ProtocolHeaders.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalProtocolHeaders();
        } else if (SecurityContext.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalSecurityContext();
        } else if (ContextResolver.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalContextResolver();
        } else if (Request.class.isAssignableFrom(type)) {
            proxy = new ThreadLocalRequest();
        }  else if (Providers.class.isAssignableFrom(type)) {
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
    
    public static MultivaluedMap<String, Object> extractValuesFromBean(Object bean, String baseName) {
        MultivaluedMap<String, Object> values = new MetadataMap<String, Object>();
        fillInValuesFromBean(bean, baseName, values);
        return values;
    }
    
    public static void fillInValuesFromBean(Object bean, String baseName, 
                                            MultivaluedMap<String, Object> values) {
        for (Method m : bean.getClass().getMethods()) {
            if (m.getName().startsWith("get") && m.getParameterTypes().length == 0 
                && m.getName().length() > 3) {
                String propertyName = m.getName().substring(3);
                if (propertyName.length() == 1) {
                    propertyName = propertyName.toLowerCase();
                } else {
                    propertyName = propertyName.substring(0, 1).toLowerCase()
                                   + propertyName.substring(1);
                }
                if (baseName.contains(propertyName) || "class".equals(propertyName)) {
                    continue;
                }
                if (!"".equals(baseName)) {
                    propertyName = baseName + "." + propertyName;
                }
                
                Object value = extractFromMethod(bean, m);
                if (value == null) {
                    continue;
                }
                if (isPrimitive(value.getClass())) {
                    values.putSingle(propertyName, value);
                } else if (isSupportedCollectionOrArray(value.getClass())) {
                    // ignoring arrrays for a moment
                    List<Object> theValues = null;
                    if (value.getClass().isArray()) {
                        theValues = Arrays.asList(value);
                    } else {
                        theValues = CastUtils.cast((List<?>)value);
                    }
                    values.put(propertyName, theValues);
                } else {
                    fillInValuesFromBean(value, propertyName, values);
                }
            }
        }
    }
    
    public static Map<Parameter, Class<?>> getParametersFromBeanClass(Class<?> beanClass, 
                                                                      ParameterType type,
                                                                      boolean checkIgnorable) {
        Map<Parameter, Class<?>> params = new LinkedHashMap<Parameter, Class<?>>();
        for (Method m : beanClass.getMethods()) {
            if (m.getName().startsWith("get") && m.getParameterTypes().length == 0 
                && m.getName().length() > 3) {
                String propertyName = m.getName().substring(3).toLowerCase();
                if (m.getReturnType() == Class.class
                    || checkIgnorable && canPropertyBeIgnored(m, propertyName)) {
                    continue;
                }
                params.put(new Parameter(type, propertyName), m.getReturnType());
            }
        }
        return params;
    }
    
    private static boolean canPropertyBeIgnored(Method m, String propertyName) {
        for (Annotation ann : m.getAnnotations()) {
            String annType = ann.annotationType().getName();
            if ("org.apache.cxf.aegis.type.java5.IgnoreProperty".equals(annType)
                || "javax.xml.bind.annotation.XmlTransient".equals(annType)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive() 
            || Number.class.isAssignableFrom(type)
            || Boolean.class.isAssignableFrom(type)
            || String.class == type;
    }
    
    public static String decodeValue(String value, boolean decode, ParameterType param) {
        if (!decode) {
            return value;
        }
        if (param == ParameterType.PATH || param == ParameterType.MATRIX) {
            return HttpUtils.pathDecode(value);
        } else {
            return HttpUtils.urlDecode(value);
        }
    }
    
    public static void invokeLifeCycleMethod(Object instance, Method method) {
        if (method != null) {
            method = InjectionUtils.checkProxy(method, instance);
            try {
                method.invoke(instance, new Object[]{});
            } catch (InvocationTargetException ex) {
                String msg = "Method " + method.getName() + " can not be invoked"
                    + " due to InvocationTargetException";
                throw new WebApplicationException(Response.serverError().entity(msg).build());
            } catch (IllegalAccessException ex) {
                String msg = "Method " + method.getName() + " can not be invoked"
                    + " due to IllegalAccessException";
                throw new WebApplicationException(Response.serverError().entity(msg).build());
            } 
        }
    }
}
