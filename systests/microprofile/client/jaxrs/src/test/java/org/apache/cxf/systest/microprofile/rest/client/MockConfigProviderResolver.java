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
package org.apache.cxf.systest.microprofile.rest.client;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

public class MockConfigProviderResolver extends ConfigProviderResolver {

    private final Map<String, String> configValues;
    
    private final Config config;

    public MockConfigProviderResolver() {
        configValues = new HashMap<>();
        config = new MockConfig(configValues);
    }

    public MockConfigProviderResolver(Map<String, String> configValues) {
        this.configValues = configValues;
        this.config = new MockConfig(configValues);
    }

    public void setConfigValues(Map<String, String> configValues) {
        this.configValues.putAll(configValues);
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public Config getConfig(ClassLoader loader) {
        return config;
    }

    @Override
    public ConfigBuilder getBuilder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerConfig(Config newConfig, ClassLoader classLoader) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void releaseConfig(Config newConfig) {
        configValues.clear();
    }

}
