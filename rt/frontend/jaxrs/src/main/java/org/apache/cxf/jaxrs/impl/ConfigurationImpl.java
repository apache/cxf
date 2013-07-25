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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;

public class ConfigurationImpl implements Configuration {
    private Map<String, Object> props = new HashMap<String, Object>();
    private RuntimeType runtimeType;
    private Map<Object, Map<Class<?>, Integer>> providers = 
        new HashMap<Object, Map<Class<?>, Integer>>(); 
    private Set<Feature> features = new HashSet<Feature>();
    
    public ConfigurationImpl(RuntimeType rt) {
        this.runtimeType = rt;
    }
    
    public ConfigurationImpl(RuntimeType rt, Configuration parent) {
        props = parent.getProperties();
        this.runtimeType = rt;
    }
    
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        for (Object o : getInstances()) {
            classes.add(o.getClass());
        }
        return classes;
    }

    @Override
    public Map<Class<?>, Integer> getContracts(Class<?> cls) {
        for (Object o : getInstances()) {
            if (cls.isAssignableFrom(o.getClass())) {
                return Collections.unmodifiableMap(providers.get(o));
            }
        }
        return Collections.emptyMap();
    }

    @Override
    public Set<Object> getInstances() {
        return providers.keySet();
    }

    @Override
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(props);
    }

    @Override
    public Object getProperty(String name) {
        return props.get(name);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return props.keySet();
    }

    @Override
    public RuntimeType getRuntimeType() {
        return runtimeType;
    }

    @Override
    public boolean isEnabled(Feature f) {
        return features.contains(f);
    }

    @Override
    public boolean isEnabled(Class<? extends Feature> f) {
        for (Feature feature : features) {
            if (feature.getClass().isAssignableFrom(f)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isRegistered(Object obj) {
        return providers.containsKey(obj);
    }

    @Override
    public boolean isRegistered(Class<?> cls) {
        for (Object o : getInstances()) {
            if (cls.isAssignableFrom(o.getClass())) {
                return true;
            }
        }
        return false;
    }

    public void setProperty(String name, Object value) {
        if (name == null) {
            props.remove(name);
        } else {
            props.put(name, value);
        }
    }
    
    public void setFeature(Feature f) {
        features.add(f);
    }
    
    public void register(Object provider, Class<?> contract, int bindingPriority) {
        Map<Class<?>, Integer> metadata = providers.get(provider);
        if (metadata == null) {
            metadata = new HashMap<Class<?>, Integer>();
            providers.put(provider, metadata);
        }
        metadata.put(contract, bindingPriority);
    }
}
