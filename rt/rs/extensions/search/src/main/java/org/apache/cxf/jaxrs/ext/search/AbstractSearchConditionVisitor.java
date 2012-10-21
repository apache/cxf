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
import java.util.Map;

import org.apache.cxf.jaxrs.utils.InjectionUtils;


public abstract class AbstractSearchConditionVisitor <T, E> implements SearchConditionVisitor<T, E> {
    
    private Map<String, String> fieldMap;
    private Map<String, Class<?>> primitiveFieldTypeMap;
    
    protected AbstractSearchConditionVisitor(Map<String, String> fieldMap) {
        this.fieldMap = fieldMap;
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
        
        int index = name.indexOf(".");
        if (index != -1) {
            String[] names = name.split("\\.");
            name = name.substring(index + 1);
            if (value != null && !InjectionUtils.isPrimitive(valueCls)) {
                try {
                    String nextPart = names[1];
                    if (nextPart.length() == 1) {
                        nextPart = nextPart.toUpperCase();
                    } else {
                        nextPart = Character.toUpperCase(nextPart.charAt(0)) + nextPart.substring(1);
                    }
                    Method m = valueCls.getMethod("get" + nextPart, new Class[]{});
                    value = m.invoke(value, new Object[]{});
                    valueCls = value.getClass();
                    
                } catch (Throwable ex) {
                    throw new RuntimeException();
                }
                return getPrimitiveFieldClass(name, valueCls, value);
            }
            
        }
        
        Class<?> cls = null;
        if (primitiveFieldTypeMap != null) {
            cls = primitiveFieldTypeMap.get(name);
        }
        if (cls == null) {  
            cls = valueCls;
        }
        return new ClassValue(cls, value);
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
        public ClassValue(Class<?> cls, Object value) {
            this.cls = cls;
            this.value = value;
                
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
    }
}
