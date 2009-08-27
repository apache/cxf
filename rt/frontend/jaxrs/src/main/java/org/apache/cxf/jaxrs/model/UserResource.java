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
package org.apache.cxf.jaxrs.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;

public class UserResource {

    private String className; 
    private String pathValue;
    private String consumesTypes;
    private String producesTypes;
    private List<UserOperation> opers; 
    
    public UserResource() {
    }
    
    public UserResource(String className) {
        this(className, null);
    }
    
    public UserResource(String className, String pathValue) {
        this(className, pathValue, null);
    }
    
    public UserResource(String className, String pathValue, List<UserOperation> ops) {
        this.className = className;
        this.pathValue = pathValue;
        this.opers = ops;
    }
    
    public String getConsumes() {
        return consumesTypes;
    }
    
    public String getProduces() {
        return producesTypes;
    }
    
    public void setConsumes(String types) {
        if (!"".equals(types)) {
            consumesTypes = types;
        }
    }
    
    public void setProduces(String types) {
        if (!"".equals(types)) {
            producesTypes = types;
        }
    }
    
    public String getName() {
        return className;
    }
    
    public void setName(String name) {
        if (!"".equals(name)) {
            className = name;
        }
    }
    
    public String getPath() {
        return pathValue;
    }
    
    public void setPath(String path) {
        if (!"".equals(path)) {
            pathValue = path;
        }
    }
    
    public void setOperations(List<UserOperation> ops) {
        opers = ops;
    }
    
    public List<UserOperation> getOperations() {
        return opers == null ? CastUtils.cast(Collections.emptyList(), UserOperation.class)
            : Collections.unmodifiableList(opers);
    }
    
    public Map<String, UserOperation> getOperationsAsMap() {
        Map<String, UserOperation> map = new HashMap<String, UserOperation>();
        for (UserOperation op : opers) {
            map.put(op.getName(), op);
        }
        return map;
    }
}
