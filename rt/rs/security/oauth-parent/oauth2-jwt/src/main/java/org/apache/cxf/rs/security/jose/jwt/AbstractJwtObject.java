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

package org.apache.cxf.rs.security.jose.jwt;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractJwtObject {
    protected Map<String, Object> values = new LinkedHashMap<String, Object>();
    
    protected AbstractJwtObject() {
        
    }
    
    protected AbstractJwtObject(Map<String, Object> values) {
        this.values = values;
    }
    
    protected void setValue(String name, Object value) {
        values.put(name, value);
    }
    
    protected Object getValue(String name) {
        return values.get(name);
    }

    public Map<String, Object> asMap() {
        return new LinkedHashMap<String, Object>(values);
    }
    
    protected Long getLongDate(String name) {
        Object object = getValue(name);
        return object instanceof Long ? (Long)object : Long.valueOf(object.toString());
    }
    
    public int hashCode() { 
        return values.hashCode();
    }
    
    public boolean equals(Object obj) {
        return obj instanceof AbstractJwtObject && ((AbstractJwtObject)obj).values.equals(this.values);
    }
    
}
