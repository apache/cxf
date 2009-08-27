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
import java.util.List;

import org.apache.cxf.helpers.CastUtils;

public class UserOperation {

    private String methodName;
    private String httpMethodName;
    private String pathValue;
    private String consumesTypes;
    private String producesTypes;
    private List<Parameter> params; 
    
    public UserOperation() {
        
    }
    
    public UserOperation(String methodName) {
        this(methodName, null);
    }
    
    public UserOperation(String methodName, String pathValue) {
        this(methodName, pathValue, null);
    }
    
    public UserOperation(String methodName, String pathValue, List<Parameter> ps) {
        this.methodName = methodName;
        this.pathValue = pathValue;
        this.params = ps;
    }
    
    public String getName() {
        return methodName;
    }
    
    public void setName(String name) {
        if (!"".equals(name)) {
            methodName = name;
        }
    }
    
    public String getVerb() {
        return httpMethodName;
    }
    
    public void setVerb(String name) {
        if (!"".equals(name)) {
            httpMethodName = name;
        }
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
    
    public String getPath() {
        return pathValue == null ? "/" : pathValue;
    }
    
    public void setPath(String path) {
        if (!"".equals(path)) {
            pathValue = path;
        }
    }
    
    public void setParameters(List<Parameter> ps) {
        params = ps;
    }
    
    public List<Parameter> getParameters() {
        return params == null ? CastUtils.cast(Collections.emptyList(), Parameter.class)
            : Collections.unmodifiableList(params);
    }
}
