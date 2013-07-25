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

import java.util.Map;

import javax.ws.rs.Priorities;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.apache.cxf.jaxrs.utils.AnnotationUtils;

public class ConfigurableImpl<C extends Configurable<C>> implements Configurable<C> {
    private ConfigurationImpl config;
    private C configurable;
    private Class<?>[] supportedProviderClasses;
    public ConfigurableImpl(C configurable, RuntimeType rt, Class<?>[] supportedProviderClasses) {
        this(configurable, rt, supportedProviderClasses, null);
    }
    
    public ConfigurableImpl(C configurable, RuntimeType rt, 
                            Class<?>[] supportedProviderClasses, Configuration config) {
        this.configurable = configurable;
        this.config = config == null ? new ConfigurationImpl(rt) 
            : new ConfigurationImpl(rt, config);
        this.supportedProviderClasses = supportedProviderClasses;
    }
    
    protected C getConfigurable() {
        return configurable;
    }
    
    @Override
    public Configuration getConfiguration() {
        return config;
    }

    @Override
    public C property(String name, Object value) {
        config.setProperty(name, value);
        return configurable;
    }

    @Override
    public C register(Object provider) {
        return register(provider, AnnotationUtils.getBindingPriority(provider.getClass()));
    }

    @Override
    public C register(Object provider, int bindingPriority) {
        return doRegister(provider, bindingPriority, supportedProviderClasses);
    }
    
    @Override
    public C register(Object provider, Class<?>... contracts) {
        return doRegister(provider, Priorities.USER, contracts);
    }
    
    @Override
    public C register(Object provider, Map<Class<?>, Integer> contracts) {
        for (Map.Entry<Class<?>, Integer> entry : contracts.entrySet()) {
            doRegister(provider, entry.getValue(), entry.getKey());
        }
        return configurable;
    }
    
    @Override
    public C register(Class<?> providerClass) {
        return register(providerClass, AnnotationUtils.getBindingPriority(providerClass));
    }

    @Override
    public C register(Class<?> providerClass, int bindingPriority) {
        return doRegister(createProvider(providerClass), bindingPriority, supportedProviderClasses);
    }

    @Override
    public C register(Class<?> providerClass, Class<?>... contracts) {
        return doRegister(providerClass, Priorities.USER, contracts);
    }

    @Override
    public C register(Class<?> providerClass, Map<Class<?>, Integer> contracts) {
        return register(createProvider(providerClass), contracts);
    }
    
    protected C doRegister(Object provider, int bindingPriority, Class<?>... contracts) {
        if (provider instanceof Feature) {
            Feature feature = (Feature)provider;
            config.setFeature(feature);
            feature.configure(new FeatureContextImpl(this));
            return configurable;
        }
        for (Class<?> contract : contracts) {
            if (contract.isAssignableFrom(provider.getClass())) {
                config.register(provider, contract, bindingPriority);
            }
        }
        return configurable;
    }

    private static Object createProvider(Class<?> cls) {
        try {
            return cls.newInstance();
        } catch (Throwable ex) {
            throw new RuntimeException(ex); 
        }
    }
    
    public static class FeatureContextImpl implements FeatureContext {
        private Configurable<?> cfg;
        public FeatureContextImpl(Configurable<?> cfg) {
            this.cfg = cfg;
        }
        
        @Override
        public Configuration getConfiguration() {
            return cfg.getConfiguration();
        }

        @Override
        public FeatureContext property(String name, Object value) {
            cfg.property(name, value);
            return this;
        }

        @Override
        public FeatureContext register(Class<?> cls) {
            cfg.register(cls);
            return this;
        }

        @Override
        public FeatureContext register(Object obj) {
            cfg.register(obj);
            return this;
        }

        @Override
        public FeatureContext register(Class<?> cls, int priority) {
            cfg.register(cls, priority);
            return this;
        }

        @Override
        public FeatureContext register(Class<?> cls, Class<?>... contract) {
            cfg.register(cls, contract);
            return this;
        }

        @Override
        public FeatureContext register(Class<?> cls, Map<Class<?>, Integer> map) {
            cfg.register(cls, map);
            return this;
        }

        @Override
        public FeatureContext register(Object obj, int priority) {
            cfg.register(obj, priority);
            return this;
        }

        @Override
        public FeatureContext register(Object obj, Class<?>... contract) {
            cfg.register(obj, contract);
            return this;
        }

        @Override
        public FeatureContext register(Object obj, Map<Class<?>, Integer> map) {
            cfg.register(obj, map);
            return this;
        } 
        
    }
}
