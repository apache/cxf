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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility bean for simplifying the way Search expressions can be captured and
 * subsequently introspected or converted into different language expressions  
 */
public class SearchBean {
    private Map<String, String> values = new HashMap<String, String>(1);
    
    public void set(String name, String value) {
        values.put(name, value);
    }
    
    public String get(String name) {
        return values.get(name);
    }
    
    public Set<String> getKeySet() {
        return values.keySet();
    }
    
    @Override
    public int hashCode() {
        return values.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof SearchBean) {
            return values.equals(((SearchBean)o).values);
        } else {
            return false;
        }
    }
}
