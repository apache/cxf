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
package org.apache.cxf.jaxrs.ext.search;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.cxf.jaxrs.ext.search.Beanspector.TypeInfo;
import org.apache.cxf.jaxrs.ext.search.collections.CollectionCheck;
import org.apache.cxf.jaxrs.ext.search.collections.CollectionCheckInfo;
import org.apache.cxf.jaxrs.utils.InjectionUtils;

public abstract class AbstractSearchConditionParser<T> implements SearchConditionParser<T> {
    public static final String EXTENSION_COUNT = "count";
    protected static final String EXTENSION_COUNT_OPEN = EXTENSION_COUNT + "(";
    
    protected final Map<String, String> contextProperties;
    protected final Class<T> conditionClass;
    
    protected AbstractSearchConditionParser(Class<T> tclass) {
        this(tclass, Collections.<String, String>emptyMap());
    }
    
    protected AbstractSearchConditionParser(Class<T> tclass, Map<String, String> contextProperties) {
        this.conditionClass = tclass;
        this.contextProperties = contextProperties == null 
            ? Collections.<String, String>emptyMap() : contextProperties;
    }
    
    protected String getSetter(String setter) {
        int index = getDotIndex(setter);
        if (index != -1) {
            return setter.substring(0, index).toLowerCase();
        } else {
            return setter;
        }
    }
    
    protected Object parseType(String originalPropName, 
                             Object ownerBean, 
                             Object lastCastedValue, 
                             String setter, 
                             TypeInfo typeInfo, 
                             String value) throws SearchParseException {
        Class<?> valueType = typeInfo.getTypeClass();
        boolean isCollection = InjectionUtils.isSupportedCollectionOrArray(valueType);
        Class<?> actualType = isCollection ? InjectionUtils.getActualType(typeInfo.getGenericType()) : valueType;
        
        int index = getDotIndex(setter);
        if (index == -1) {
            Object castedValue = value;
            if (Date.class.isAssignableFrom(valueType)) {
                castedValue = convertToDate(value);
            } else {
                boolean isPrimitive = InjectionUtils.isPrimitive(valueType);
                boolean isPrimitiveOrEnum = isPrimitive || valueType.isEnum();
                if (ownerBean == null || isPrimitiveOrEnum) {
                    try {
                        CollectionCheck collCheck = getCollectionCheck(originalPropName, isCollection, actualType);
                        if (collCheck == null) {
                            castedValue = InjectionUtils.convertStringToPrimitive(value, actualType);
                        } 
                        if (collCheck == null && isCollection) {
                            castedValue = getCollectionSingleton(valueType, castedValue);
                        } else if (isCollection) {
                            typeInfo.setCollectionCheckInfo(new CollectionCheckInfo(collCheck, castedValue));
                            castedValue = getEmptyCollection(valueType);
                        }
                    } catch (Exception e) {
                        throw new SearchParseException("Cannot convert String value \"" + value
                                                     + "\" to a value of class " + valueType.getName(), e);
                    }
                } else {
                    Class<?> classType = isCollection ? valueType : value.getClass(); 
                    try {
                        Method setterM = valueType.getMethod("set" + getMethodNameSuffix(setter),
                                                             new Class[]{classType});
                        Object objectValue = !isCollection ? value : getCollectionSingleton(valueType, value);
                        setterM.invoke(ownerBean, new Object[]{objectValue});
                        castedValue = objectValue; 
                    } catch (Throwable ex) {
                        throw new SearchParseException("Cannot convert String value \"" + value
                                                       + "\" to a value of class " + valueType.getName(), ex);
                    }
                    
                }
            }
            if (lastCastedValue != null) {
                castedValue = lastCastedValue;
            }
            return castedValue;
        } else {
            String[] names = setter.split("\\.");
            try {
                String nextPart = getMethodNameSuffix(names[1]);
                Method getterM = actualType.getMethod("get" + nextPart, new Class[]{});   
                Class<?> returnType = getterM.getReturnType();
                boolean returnCollection = InjectionUtils.isSupportedCollectionOrArray(returnType);
                Class<?> actualReturnType = !returnCollection ? returnType 
                    : InjectionUtils.getActualType(getterM.getGenericReturnType());
                
                boolean isPrimitive = InjectionUtils.isPrimitive(returnType) || returnType.isEnum();
                boolean lastTry = names.length == 2 
                    && (isPrimitive || returnType == Date.class || returnCollection);
                
                Object valueObject = ownerBean != null ? ownerBean 
                    : actualType.isInterface() 
                    ? Proxy.newProxyInstance(this.getClass().getClassLoader(), 
                                             new Class[]{actualType}, 
                                             new InterfaceProxy())
                    : actualType.newInstance();
                Object nextObject;
                
                if (lastTry) {
                    if (!returnCollection) {
                        nextObject = isPrimitive ? InjectionUtils.convertStringToPrimitive(value, returnType) 
                            : convertToDate(value);
                    } else {
                        CollectionCheck collCheck = getCollectionCheck(originalPropName, true, actualReturnType);
                        if (collCheck == null) {
                            nextObject = getCollectionSingleton(valueType, value);
                        } else {
                            typeInfo.setCollectionCheckInfo(new CollectionCheckInfo(collCheck, value));
                            nextObject = getEmptyCollection(valueType);
                        }
                    }
                } else {
                    nextObject = returnType.newInstance();
                }
                
                Method setterM = actualType.getMethod("set" + nextPart, new Class[]{returnType});
                setterM.invoke(valueObject, new Object[]{nextObject});
                
                if (lastTry) {
                    lastCastedValue = lastCastedValue == null ? valueObject : lastCastedValue;
                    return isCollection ? getCollectionSingleton(valueType, lastCastedValue) : lastCastedValue;
                } else {
                    lastCastedValue = valueObject;
                }
                
                TypeInfo nextTypeInfo = new TypeInfo(nextObject.getClass(), getterM.getGenericReturnType()); 
                Object response = parseType(originalPropName,
                                 nextObject, 
                                 lastCastedValue, 
                                 setter.substring(index + 1), 
                                 nextTypeInfo, 
                                 value);
                if (ownerBean == null) {
                    return isCollection ? getCollectionSingleton(valueType, lastCastedValue) : lastCastedValue;
                } else {
                    return response;
                }
            } catch (Throwable e) {
                throw new SearchParseException("Cannot convert String value \"" + value
                                               + "\" to a value of class " + valueType.getName(), e);
            }
        }
    }

    private CollectionCheck getCollectionCheck(String propName, boolean isCollection, Class<?> actualCls) {
        if (isCollection) {
            if (InjectionUtils.isPrimitive(actualCls)) {
                if (propName.startsWith(EXTENSION_COUNT_OPEN)) {
                    return CollectionCheck.SIZE;
                }
            } else {
                return CollectionCheck.SIZE;
            }
        }
        return null;
    }
    
    private Object getCollectionSingleton(Class<?> collectionCls, Object value) {
        if (Set.class.isAssignableFrom(collectionCls)) {
            return Collections.singleton(value);
        } else {
            return Collections.singletonList(value);
        }
    }
    
    private Object getEmptyCollection(Class<?> collectionCls) {
        if (Set.class.isAssignableFrom(collectionCls)) {
            return Collections.emptySet();
        } else {
            return Collections.emptyList();
        }
    }
    
    private Object convertToDate(String value) throws SearchParseException {
        try {
            DateFormat df = SearchUtils.getDateFormat(contextProperties);
            String dateValue = value;
            if (SearchUtils.isTimeZoneSupported(contextProperties, Boolean.FALSE)) {
                // zone in XML is "+01:00" in Java is "+0100"; stripping semicolon
                int idx = value.lastIndexOf(':');
                if (idx != -1) {
                    dateValue = value.substring(0, idx) + value.substring(idx + 1);
                }
            }
            return df.parse(dateValue);
        } catch (ParseException e) {
            // is that duration?
            try {
                Date now = new Date();
                DatatypeFactory.newInstance().newDuration(value).addTo(now);
                return now;
            } catch (DatatypeConfigurationException e1) {
                throw new SearchParseException(e1);
            } catch (IllegalArgumentException e1) {
                throw new SearchParseException("Can parse " + value + " neither as date nor duration", e);
            }
        }
    }
    
    private String getMethodNameSuffix(String name) {
        if (name.length() == 1) {
            return name.toUpperCase();
        } else {
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }       

    private int getDotIndex(String setter) {
        return this.conditionClass == SearchBean.class ? -1 : setter.indexOf(".");
    }    
}
