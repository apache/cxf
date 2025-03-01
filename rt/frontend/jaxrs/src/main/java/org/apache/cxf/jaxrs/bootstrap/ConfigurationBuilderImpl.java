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

package org.apache.cxf.jaxrs.bootstrap;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.SeBootstrap.Configuration.SSLClientAuthentication;

public class ConfigurationBuilderImpl implements SeBootstrap.Configuration.Builder {
    private final Map<String, Map.Entry<Class<?>, Consumer<?>>> supported = new HashMap<>();
    private final Map<String, Object> properties = new HashMap<>();
    
    public ConfigurationBuilderImpl() {
        supported.put(SeBootstrap.Configuration.PROTOCOL,
            entry(String.class, this::protocol));
        supported.put(SeBootstrap.Configuration.HOST,
            entry(String.class, this::host));
        supported.put(SeBootstrap.Configuration.PORT,
            entry(Integer.class, this::port));
        supported.put(SeBootstrap.Configuration.ROOT_PATH,
            entry(String.class, this::rootPath));
        supported.put(SeBootstrap.Configuration.SSL_CONTEXT,
            entry(SSLContext.class, this::sslContext));
        supported.put(SeBootstrap.Configuration.SSL_CLIENT_AUTHENTICATION, 
            entry(SSLClientAuthentication.class, this::sslClientAuthentication));
    }

    private <T> SimpleEntry<Class<?>, Consumer<?>> entry(Class<T> clazz, Consumer<T> consumer) {
        return new SimpleEntry<Class<?>, Consumer<?>>(clazz, consumer);
    }

    @Override
    public SeBootstrap.Configuration build() {
        return new ConfigurationImpl(properties);
    }

    @Override
    public SeBootstrap.Configuration.Builder property(String name, Object value) {
        properties.put(name, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> SeBootstrap.Configuration.Builder from(BiFunction<String, Class<T>, Optional<T>> propertiesProvider) {
        for (Map.Entry<String, Map.Entry<Class<?>, Consumer<?>>> entry: supported.entrySet()) {
            propertiesProvider
                .apply(entry.getKey(), (Class<T>)entry.getValue().getKey())
                .ifPresent((Consumer<T>)entry.getValue().getValue());
        }
        return this;
    }
    
    @Override
    public SeBootstrap.Configuration.Builder from(Object externalConfig) {
        if (SeBootstrap.Configuration.class.isInstance(externalConfig)) {
            final SeBootstrap.Configuration other = (SeBootstrap.Configuration) externalConfig;
            from((name, clazz) -> {
                final Object property = other.property(name);
                if (property != null && clazz.equals(property.getClass())) {
                    return Optional.of(property);
                } else {
                    return Optional.empty();
                }
            });
        }
        return this;
    }

}
