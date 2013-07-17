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
package org.apache.cxf.jaxrs.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;

public class ConfigurationImpl implements Configuration {

    public ConfigurationImpl() {
        
    }
    
    public ConfigurationImpl(Configuration parent) {
        
    }
    
    @Override
    public Set<Class<?>> getClasses() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Class<?>, Integer> getContracts(Class<?> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Object> getInstances() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getProperty(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<String> getPropertyNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RuntimeType getRuntimeType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isEnabled(Feature arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isEnabled(Class<? extends Feature> arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isRegistered(Object arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isRegistered(Class<?> arg0) {
        // TODO Auto-generated method stub
        return false;
    }

}
