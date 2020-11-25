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

import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.StringUtils;

public class ClassGeneratorClassLoader {
    protected final Bus bus;

    public ClassGeneratorClassLoader(final Bus bus) {
        this.bus = bus == null ? BusFactory.getDefaultBus() : bus;
    }
    protected Class<?> loadClass(String className, byte[] bytes) {
        TypeHelperClassLoader loader = getOrCreateLoader();
        Class<?> cls = loader.lookupDefinedClass(className);
        if (cls == null) {
            return loader.defineClass(className, bytes);
        }
        return cls;
    }
    protected Class<?> findClass(String className) {
        return getOrCreateLoader().lookupDefinedClass(className);
    }
    private TypeHelperClassLoader getOrCreateLoader() {
        TypeHelperClassLoader loader = bus.getExtension(TypeHelperClassLoader.class);
        if (loader == null) {
            ClassLoader parent = bus.getExtension(ClassLoader.class);
            if (parent == null) {
                parent = ClassGeneratorClassLoader.class.getClassLoader();
            }
            loader = new TypeHelperClassLoader(parent);
            bus.setExtension(loader, TypeHelperClassLoader.class);
        }
        return loader;
    }


    public static class TypeHelperClassLoader extends ClassLoader {
        ConcurrentHashMap<String, Class<?>> defined = new ConcurrentHashMap<>();

        TypeHelperClassLoader(ClassLoader parent) {
            super(parent);
        }
        public Class<?> lookupDefinedClass(String name) {
            return defined.get(StringUtils.slashesToPeriod(name));
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (name.endsWith("package-info")) {
                return getParent().loadClass(name);
            }
            return super.findClass(name);
        }

        public Class<?> defineClass(String name, byte[] bytes) {
            Class<?> ret = defined.get(StringUtils.slashesToPeriod(name));
            if (ret != null) {
                return ret;
            }
            if (name.endsWith("package-info")) {
                Package p = super.getPackage(name.substring(0, name.length() - 13));
                if (p == null) {
                    definePackage(StringUtils.slashesToPeriod(name.substring(0, name.length() - 13)),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null);
                }
            }

            ret = defined.computeIfAbsent(StringUtils.slashesToPeriod(name),
                key -> TypeHelperClassLoader.super.defineClass(key, bytes, 0, bytes.length));

            return ret;
        }
    }
}
