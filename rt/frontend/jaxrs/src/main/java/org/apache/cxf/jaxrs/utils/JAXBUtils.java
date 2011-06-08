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
import java.lang.reflect.Type;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public final class JAXBUtils {
    private JAXBUtils() {
        
    }
    
    public static Object convertWithAdapter(Object obj, 
                                            Annotation[] anns) {
        return useAdapter(obj, 
                          getAdapter(obj.getClass(), anns),
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
                Type[] types = InjectionUtils.getActualTypes(adapter.value().getGenericSuperclass());
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
            if (InjectionUtils.isSupportedCollectionOrArray(typeAdapter.value().getClass())) {
                return obj;
            }
            try {
                XmlAdapter xmlAdapter = typeAdapter.value().newInstance();
                if (marshal) {
                    return xmlAdapter.marshal(obj);
                } else {
                    return xmlAdapter.unmarshal(obj);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return defaultValue;
    }
}
