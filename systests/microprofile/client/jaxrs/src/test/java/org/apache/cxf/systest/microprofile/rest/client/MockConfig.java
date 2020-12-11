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

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

public class MockConfig implements Config {
    private final Map<String, String> configValues;
    
    public MockConfig(Map<String, String> configValues) {
        this.configValues = configValues;
    }
    
    @Override
    public <T> T getValue(String propertyName, Class<T> propertyType) {
        String value = configValues.get(propertyName);
        if (value != null) {
            if (propertyType == String.class) {
                return propertyType.cast(value);
            }
            if (propertyType == Integer.class) {
                return propertyType.cast(Integer.parseInt(value));
            }
        }
        return null;
    }
    @Override
    public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType) {
        return Optional.ofNullable(getValue(propertyName, propertyType));
    }
    @Override
    public Iterable<String> getPropertyNames() {
        return configValues.keySet();
    }
    @Override
    public Iterable<ConfigSource> getConfigSources() {
        ConfigSource source = new ConfigSource() {
            @Override
            public Map<String, String> getProperties() {
                return configValues;
            }
            @Override
            public String getValue(String propertyName) {
                return (String) configValues.get(propertyName);
            }
            @Override
            public String getName() {
                return "stub";
            } 
            
            @Override
            public Set<String> getPropertyNames() {
                return configValues.keySet();
            }
        };
        return Arrays.asList(source);
    }
    @Override
    public ConfigValue getConfigValue(String propertyName) {
        return null;
    }
    @Override
    public <T> Optional<Converter<T>> getConverter(Class<T> forType) {
        return Optional.empty();
    }
    
    @Override
    public <T> T unwrap(Class<T> type) {
        throw new IllegalArgumentException("Unsupported type: " + type);
    }
}
