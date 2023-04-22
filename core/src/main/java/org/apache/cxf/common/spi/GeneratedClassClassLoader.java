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
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;

/** Class loader used to find class generated during build time to avoid class generation during runtime.
 *  inherited class implement same interface than generator class but find class in TypeHelperClassLoader
 *  Runtime class generator use @see org.apache.cxf.common.spi.ClassGeneratorClassLoader
 * @author olivier dufour
 */
public class GeneratedClassClassLoader {
    private static final Logger LOG = LogUtils.getL7dLogger(GeneratedClassClassLoader.class);
    protected final Bus bus;

    public GeneratedClassClassLoader(Bus bus) {
        this.bus = bus;
    }
    protected Class<?> findClass(String className, Class<?> callingClass) {
        ClassLoader cl = getClassLoader();
        try {
            return cl.loadClass(className);
        } catch (ClassNotFoundException e) {
            //ignore and try with other class loader
        }
        try {
            return ClassLoaderUtils.loadClass(className, callingClass);
        } catch (ClassNotFoundException e) {
            LOG.fine("Failed to load class :" + e.toString());
        }
        return null;
    }
    public TypeHelperClassLoader getClassLoader() {
        TypeHelperClassLoader loader = bus.getExtension(TypeHelperClassLoader.class);
        if (loader == null) {
            loader = bus.getExtension(TypeHelperClassLoader.class);
            if (loader == null) {
                ClassLoader parent = bus.getExtension(ClassLoader.class);
                if (parent == null) {
                    parent = Thread.currentThread().getContextClassLoader();
                }
                loader = new TypeHelperClassLoader(parent);
                bus.setExtension(loader, TypeHelperClassLoader.class);
            }
        }
        return loader;
    }

    public static class TypeHelperClassLoader extends ClassLoader {
        private final ConcurrentHashMap<String, Class<?>> defined = new ConcurrentHashMap<>();

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
                String s = name.substring(0, name.length() - 13);
                Package p = super.getPackage(s);
                if (p == null) {
                    definePackage(StringUtils.slashesToPeriod(s),
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
