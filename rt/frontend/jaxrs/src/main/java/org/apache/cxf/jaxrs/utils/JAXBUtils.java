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

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.cxf.common.logging.LogUtils;

public final class JAXBUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(JAXBUtils.class);

    private JAXBUtils() {

    }

    public static JAXBContext createJaxbContext(Set<Class<?>> classes, Class<?>[] extraClass,
                                                Map<String, Object> contextProperties) {
        if (classes == null || classes.isEmpty()) {
            return null;
        }
        org.apache.cxf.common.jaxb.JAXBUtils.scanPackages(classes, extraClass, null);

        JAXBContext ctx;
        try {
            ctx = org.apache.cxf.common.jaxb.JAXBUtils.createContext(classes, contextProperties);
            return ctx;
        } catch (JAXBException ex) {
            LOG.log(Level.SEVERE, "No JAXB context can be created", ex);
        }
        return null;
    }

    public static void closeUnmarshaller(Unmarshaller u) {
        if (u instanceof Closeable) {
            //need to do this to clear the ThreadLocal cache
            //see https://java.net/jira/browse/JAXB-1000

            try {
                ((Closeable)u).close();
            } catch (IOException e) {
                //ignore
            }
        }
    }
    public static Object convertWithAdapter(Object obj,
                                            Class<?> adapterClass,
                                            Annotation[] anns) {
        return useAdapter(obj,
                          getAdapter(adapterClass, anns),
                          false,
                          obj);
    }

    public static Class<?> getValueTypeFromAdapter(Class<?> expectedBoundType,
                                                   Class<?> defaultClass,
                                                   Annotation[] anns) {
        try {
            XmlJavaTypeAdapter adapter = getAdapter(expectedBoundType, anns);
            if (adapter != null) {
                Class<?> boundType = JAXBUtils.getTypeFromAdapter(adapter, null, true);
                if (boundType != null && boundType.isAssignableFrom(expectedBoundType)) {
                    return JAXBUtils.getTypeFromAdapter(adapter, null, false);
                }
            }
        } catch (Throwable ex) {
            // ignore
        }
        return defaultClass;
    }

    public static XmlJavaTypeAdapter getAdapter(Class<?> objectClass, Annotation[] anns) {
        XmlJavaTypeAdapter typeAdapter = AnnotationUtils.getAnnotation(anns, XmlJavaTypeAdapter.class);
        if (typeAdapter == null) {
            typeAdapter = objectClass.getAnnotation(XmlJavaTypeAdapter.class);
            if (typeAdapter == null) {
                // lets just try the 1st interface for now
                Class<?>[] interfaces = objectClass.getInterfaces();
                typeAdapter = interfaces.length > 0
                    ? interfaces[0].getAnnotation(XmlJavaTypeAdapter.class) : null;
            }
        }
        return typeAdapter;
    }

    public static Class<?> getTypeFromAdapter(XmlJavaTypeAdapter adapter, Class<?> theType,
                                              boolean boundType) {
        if (adapter != null) {
            if (adapter.type() != XmlJavaTypeAdapter.DEFAULT.class) {
                theType = adapter.type();
            } else {
                Type topAdapterType = adapter.value().getGenericSuperclass();
                Class<?> superClass = adapter.value().getSuperclass();
                while (superClass != null) {
                    Class<?> nextSuperClass = superClass.getSuperclass();
                    if (nextSuperClass != null && !Object.class.equals(nextSuperClass)) {
                        topAdapterType = superClass.getGenericSuperclass();
                    }
                    superClass = nextSuperClass;
                }
                Type[] types = InjectionUtils.getActualTypes(topAdapterType);
                if (types != null && types.length == 2) {
                    int index = boundType ? 1 : 0;
                    theType = InjectionUtils.getActualType(types[index]);
                }
            }
        }
        return theType;
    }

    public static Object useAdapter(Object obj,
                                    XmlJavaTypeAdapter typeAdapter,
                                    boolean marshal) {
        return useAdapter(obj, typeAdapter, marshal, obj);
    }

    @SuppressWarnings("unchecked")
    public static Object useAdapter(Object obj,
                                    XmlJavaTypeAdapter typeAdapter,
                                    boolean marshal,
                                    Object defaultValue) {
        if (typeAdapter != null) {
            try {
                @SuppressWarnings("rawtypes")
                XmlAdapter xmlAdapter = typeAdapter.value().getDeclaredConstructor().newInstance();
                if (marshal) {
                    return xmlAdapter.marshal(obj);
                }
                return xmlAdapter.unmarshal(obj);
            } catch (Exception ex) {
                LOG.log(Level.INFO, "(un)marshalling failed, using defaultValue", ex);
            }
        }
        return defaultValue;
    }
}
