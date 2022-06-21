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
package org.apache.cxf.jaxrs.ext.search.visitor;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.ext.ParamConverterProvider;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.search.DefaultParamConverterProvider;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchConditionVisitor;
import org.apache.cxf.jaxrs.ext.search.collections.CollectionCheckInfo;
import org.apache.cxf.jaxrs.ext.search.collections.CollectionCheckStatement;
import org.apache.cxf.jaxrs.utils.InjectionUtils;


public abstract class AbstractSearchConditionVisitor <T, E> implements SearchConditionVisitor<T, E> {

    private Map<String, String> fieldMap;
    private Map<String, Class<?>> primitiveFieldTypeMap;
    private PropertyValidator<Object> validator;
    private ParamConverterProvider converterProvider;
    private boolean wildcardStringMatch;

    protected AbstractSearchConditionVisitor(Map<String, String> fieldMap) {
        this.fieldMap = fieldMap;
        this.converterProvider = new DefaultParamConverterProvider();
    }

    protected String getRealPropertyName(String name) {
        if (fieldMap != null && fieldMap.containsKey(name)) {
            return fieldMap.get(name);
        }
        return name;
    }

    protected Class<?> getPrimitiveFieldClass(String name, Class<?> valueCls) {
        return getPrimitiveFieldClass(name, valueCls, null).getCls();
    }

    protected ClassValue getPrimitiveFieldClass(String name, Class<?> valueCls, Object value) {
        return getPrimitiveFieldClass(null, name, valueCls, valueCls, value);
    }

    protected ClassValue getPrimitiveFieldClass(PrimitiveStatement ps, String name, Class<?> valueCls,
                                                Type type, Object value) {
        return doGetPrimitiveFieldClass(ps, name, valueCls, type, value, new HashSet<>());
    }

    @SuppressWarnings("rawtypes")
    private ClassValue doGetPrimitiveFieldClass(PrimitiveStatement ps,
                                                String name, Class<?> valueCls, Type type, Object value,
                                                Set<String> set) {
        boolean isCollection = InjectionUtils.isSupportedCollectionOrArray(valueCls);
        Class<?> actualCls = isCollection ? InjectionUtils.getActualType(type) : valueCls;
        CollectionCheckInfo collInfo = null;
        int index = name.indexOf('.');
        if (index != -1) {
            String[] names = name.split("\\.");
            name = name.substring(index + 1);
            if (value != null && !InjectionUtils.isPrimitive(actualCls)) {
                try {
                    String nextPart = StringUtils.capitalize(names[1]);

                    Method m = actualCls.getMethod("get" + nextPart, new Class[]{});
                    if (isCollection) {
                        value = ((Collection)value).iterator().next();
                        set.add(names[0]);
                    }
                    value = m.invoke(value, new Object[]{});
                    valueCls = value.getClass();
                    type = m.getGenericReturnType();
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
                return doGetPrimitiveFieldClass(ps, name, valueCls, type, value, set);
            }
        } else if (isCollection) {
            set.add(name);
            Collection coll = (Collection)value;
            value = coll.isEmpty() ? null : coll.iterator().next();
            valueCls = actualCls;
            if (ps instanceof CollectionCheckStatement) {
                collInfo = ((CollectionCheckStatement)ps).getCollectionCheckInfo();
            }
        }

        Class<?> cls = null;
        if (primitiveFieldTypeMap != null) {
            cls = primitiveFieldTypeMap.get(name);
        }
        if (cls == null) {
            cls = valueCls;
        }
        return new ClassValue(cls, value, collInfo, set);
    }

    public void setPrimitiveFieldTypeMap(Map<String, Class<?>> primitiveFieldTypeMap) {
        this.primitiveFieldTypeMap = primitiveFieldTypeMap;
    }

    public SearchConditionVisitor<T, E> visitor() {
        return this;
    }

    protected class ClassValue {
        private Class<?> cls;
        private Object value;
        private CollectionCheckInfo collInfo;

        private Set<String> collectionProps;
        public ClassValue(Class<?> cls,
                          Object value,
                          CollectionCheckInfo collInfo,
                          Set<String> collectionProps) {
            this.cls = cls;
            this.value = value;
            this.collInfo = collInfo;
            this.collectionProps = collectionProps;
        }
        public Class<?> getCls() {
            return cls;
        }
        public void setCls(Class<?> cls) {
            this.cls = cls;
        }
        public Object getValue() {
            return value;
        }
        public void setValue(Object value) {
            this.value = value;
        }

        public CollectionCheckInfo getCollectionCheckInfo() {
            return collInfo;
        }

        public boolean isCollection(String name) {
            return collectionProps != null && collectionProps.contains(name);
        }
    }

    protected void validatePropertyValue(String name, Object value) {
        if (validator != null) {
            validator.validate(name, value);
        }
    }

    public void setValidator(PropertyValidator<Object> validator) {
        this.validator = validator;
    }

    public boolean isWildcardStringMatch() {
        return wildcardStringMatch;
    }

    public void setWildcardStringMatch(boolean wildcardStringMatch) {
        this.wildcardStringMatch = wildcardStringMatch;
    }

    public void setFieldTypeConverter(final ParamConverterProvider provider) {
        this.converterProvider = provider;
    }

    public ParamConverterProvider getFieldTypeConverter() {
        return converterProvider;
    }
}
