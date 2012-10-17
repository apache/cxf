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

import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;


public abstract class AbstractSearchConditionVisitor <T, E> implements SearchConditionVisitor<T, E> {

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractSearchConditionVisitor.class);
    
    private Map<String, String> fieldMap;
    private Map<String, Class<?>> primitiveFieldTypeMap;
    
    protected AbstractSearchConditionVisitor(Map<String, String> fieldMap) {
        this.fieldMap = fieldMap;
    }
    
    protected String getRealPropertyName(String name) {
        if (fieldMap != null && !fieldMap.isEmpty()) {
            if (fieldMap.containsKey(name)) {
                return fieldMap.get(name);
            } else {
                LOG.warning("Unrecognized field alias : " + name);
            }
        }
        return name;
    }

    protected Class<?> getPrimitiveFieldClass(String name, Class<?> defaultCls) {
        Class<?> cls = null;
        if (primitiveFieldTypeMap != null) {
            cls = primitiveFieldTypeMap.get(name);
        }
        if (cls == null) {  
            cls = defaultCls;
        }
        return cls;
    }

    public void setPrimitiveFieldTypeMap(Map<String, Class<?>> primitiveFieldTypeMap) {
        this.primitiveFieldTypeMap = primitiveFieldTypeMap;
    }
    
    public SearchConditionVisitor<T, E> visitor() {
        return this;
    }
}
