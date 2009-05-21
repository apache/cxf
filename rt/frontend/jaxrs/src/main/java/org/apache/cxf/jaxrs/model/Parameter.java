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


public class Parameter {
    private ParameterType type;
    private int ind;
    private String aValue;
    private boolean isEncoded;
    private String defaultValue;
    
    public Parameter() {
        
    }
    
    public Parameter(String type, String aValue) {
        this(ParameterType.valueOf(type), 0, aValue); 
    }
    
    public Parameter(ParameterType type, String aValue) {
        this(type, 0, aValue); 
    }
    
    public Parameter(ParameterType type, int ind, String aValue) {
        this.type = type;
        this.ind = ind;
        this.aValue = aValue; 
    }
    
    public Parameter(ParameterType type, int ind, String aValue, 
                     boolean isEncoded, String defaultValue) {
        this.type = type;
        this.ind = ind;
        this.aValue = aValue; 
        this.isEncoded = isEncoded;
        this.defaultValue = defaultValue;
    }
    
    public int getIndex() {
        return ind;
    }
    
    public String getName() {
        return aValue;
    }
    
    public void setName(String value) {
        aValue = value;
    }
    
    public ParameterType getType() {
        return type;
    }
    
    public void setType(String stype) {
        type = ParameterType.valueOf(stype);
    }
    
    public boolean isEncoded() {
        return isEncoded;
    }
    
    public void setEncoded(boolean encoded) { 
        isEncoded = encoded;
    }
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    public void setDefaultValue(String dValue) {
        defaultValue = dValue;
    }
}
