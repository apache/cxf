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

package org.apache.cxf.common.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClassLoaderProxyService implements ClassLoaderService {
    private final ProxiesClassLoader loader;

    public ClassLoaderProxyService() {
        this(new ProxiesClassLoader());
    }

    public ClassLoaderProxyService(ProxiesClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public <T> Class<T> defineAndLoad(String name, byte[] bytecode, Class<T> proxiedClass) {
        return (Class<T>) loader.getOrRegister(name, bytecode);
    }

    @Override
    public ClassLoader getProxyClassLoader() {
        return loader;
    }
    public static class LoadFirst extends ClassLoaderProxyService {
        public LoadFirst() {
            super();
        }

        @Override
        public <T> Class<T> defineAndLoad(final String name, final byte[] bytecode, final Class<T> proxiedClass) {
            ClassLoader proxyClassLoader = getProxyClassLoader();
            if (proxyClassLoader == null) {
                proxyClassLoader = Thread.currentThread().getContextClassLoader();
            }
            try {
                return (Class<T>) proxyClassLoader.loadClass(name);
            } catch (final ClassNotFoundException e) {
                return super.defineAndLoad(name, bytecode, proxiedClass);
            }
        }
    }
    public static class Spy extends ClassLoaderProxyService {
        private final Map<String, Class<?>> proxies = new HashMap<>();

        public Spy() {
            super();
        }

        public Map<String, Class<?>> getProxies() {
            return proxies;
        }

        @Override
        public <T> Class<T> defineAndLoad(final String name, final byte[] bytecode, final Class<T> proxiedClass) {
            proxies.put(name, proxiedClass);
            return super.defineAndLoad(name, bytecode, proxiedClass);
        }
    }
    private static final class ProxiesClassLoader extends ClassLoader {
        private final ConcurrentMap<String, Class<?>> classes = new ConcurrentHashMap<>();

        private ProxiesClassLoader() {
            super();
        }


        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            final Class<?> clazz = classes.get(name);
            if (clazz == null) {
                return getParent().loadClass(name);
            }
            return clazz;
        }

        private Class<?> getOrRegister(final String proxyClassName, final byte[] proxyBytes) {
            final String key = proxyClassName.replace('/', '.');
            Class<?> existing = classes.get(key);
            if (existing == null) {
                synchronized (this) {
                    existing = classes.get(key);
                    if (existing == null) {
                        existing = super.defineClass(proxyClassName, proxyBytes, 0, proxyBytes.length);
                        resolveClass(existing);
                        classes.put(key, existing);
                    }
                }
            }
            return existing;
        }
    }
}
