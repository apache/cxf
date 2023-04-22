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

import jakarta.ws.rs.core.Configurable;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.FeatureContext;

public class FeatureContextImpl implements FeatureContext {
    private Configurable<?> cfg;
    public FeatureContextImpl(Configurable<?> cfg) {
        this.cfg = cfg;
    }
    public FeatureContextImpl() {
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
    public Configurable<?> getConfigurable() {
        return cfg;
    }
    public void setConfigurable(Configurable<?> configurable) {
        this.cfg = configurable;
    }
}
