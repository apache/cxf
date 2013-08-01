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

import org.apache.cxf.jaxrs.utils.AnnotationUtils;

public class ConfigurationImpl implements Configuration {
    private Map<String, Object> props = new HashMap<String, Object>();
    private RuntimeType runtimeType;
    private Map<Object, Map<Class<?>, Integer>> providers = 
        new HashMap<Object, Map<Class<?>, Integer>>(); 
    private Set<Feature> features = new HashSet<Feature>();
    
    public ConfigurationImpl(RuntimeType rt) {
        this.runtimeType = rt;
    }
    
    public ConfigurationImpl(Configuration parent, Class<?>[] defaultContracts) {
        if (parent != null) {
            this.props.putAll(parent.getProperties());
            this.runtimeType = parent.getRuntimeType();
            
            Set<Class<?>> providerClasses = new HashSet<Class<?>>(parent.getClasses());
            for (Object o : parent.getInstances()) {
                registerParentProvider(o, parent, defaultContracts);
                providerClasses.remove(o.getClass());
            }
            for (Class<?> cls : providerClasses) {
                registerParentProvider(createProvider(cls), parent, defaultContracts);
            }
            
        }
    }
    
    private void registerParentProvider(Object o, Configuration parent, Class<?>[] defaultContracts) {
        Map<Class<?>, Integer> contracts = parent.getContracts(o.getClass());
        if (contracts != null) {
            providers.put(o, contracts);
        } else {
            register(o, AnnotationUtils.getBindingPriority(o.getClass()), defaultContracts);
        }
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
    
    
    private void register(Object provider, int bindingPriority, Class<?>... contracts) {
        register(provider, initContractsMap(bindingPriority, contracts));
    }
    
    public void register(Object provider, Map<Class<?>, Integer> contracts) {    
        Map<Class<?>, Integer> metadata = providers.get(provider);
        if (metadata == null) {
            metadata = new HashMap<Class<?>, Integer>();
            providers.put(provider, metadata);
        }
        for (Class<?> contract : contracts.keySet()) {
            if (contract.isAssignableFrom(provider.getClass())) {
                metadata.put(contract, contracts.get(contract));
            }
        }
    }
    
    public static Map<Class<?>, Integer> initContractsMap(int bindingPriority, Class<?>... contracts) {
        Map<Class<?>, Integer> metadata = new HashMap<Class<?>, Integer>();
        for (Class<?> contract : contracts) {
            metadata.put(contract, bindingPriority);
        }
        return metadata;
    }
    
    public static Object createProvider(Class<?> cls) {
        try {
            return cls.newInstance();
        } catch (Throwable ex) {
            throw new RuntimeException(ex); 
        }
    }
}
