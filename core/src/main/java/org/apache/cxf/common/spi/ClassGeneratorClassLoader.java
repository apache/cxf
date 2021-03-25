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

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.WeakIdentityHashMap;

/** Class loader used to store and retrieve class generated during runtime to avoid class generation each time.
 *  inherited class use asmHelper to generate bytes and use @see #loadClass(String, Class&lt;?&gt;, byte[])
 *  or @see #loadClass(String, ClassLoader, byte[]) to store generated class.Class can be generated during buildtime.
 *  equivalent class is @see org.apache.cxf.common.spi.GeneratedClassClassLoader
 * @author olivier dufour
 */
public class ClassGeneratorClassLoader {
    protected static final Map<Class<?>, WeakReference<TypeHelperClassLoader>> CLASS_MAP
            = new WeakIdentityHashMap<>();
    protected static final Map<ClassLoader, WeakReference<TypeHelperClassLoader>> LOADER_MAP
            = new WeakIdentityHashMap<>();
    protected final Bus bus;

    public ClassGeneratorClassLoader(final Bus bus) {
        this.bus = bus == null ? BusFactory.getDefaultBus() : bus;
    }

    protected Class<?> loadClass(String className, Class<?> cls, byte[] bytes) {
        GeneratedClassClassLoaderCapture capture = bus.getExtension(GeneratedClassClassLoaderCapture.class);
        if (capture != null) {
            capture.capture(className, bytes);
        }
        TypeHelperClassLoader loader = getOrCreateLoader(cls);
        synchronized (loader) {
            Class<?> clz = loader.lookupDefinedClass(className);
            if (clz == null) {
                return loader.defineClass(className, bytes);
            }
            return clz;
        }
    }
    protected Class<?> loadClass(String className, ClassLoader l, byte[] bytes) {
        GeneratedClassClassLoaderCapture capture = bus.getExtension(GeneratedClassClassLoaderCapture.class);
        if (capture != null) {
            capture.capture(className, bytes);
        }
        TypeHelperClassLoader loader = getOrCreateLoader(l);
        synchronized (loader) {
            Class<?> clz = loader.lookupDefinedClass(className);
            if (clz == null) {
                return loader.defineClass(className, bytes);
            }
            return clz;
        }
    }
    protected Class<?> findClass(String className, Class<?> cls) {
        return getOrCreateLoader(cls).lookupDefinedClass(className);
    }

    protected Class<?> findClass(String className, ClassLoader classLoader) {
        return getOrCreateLoader(classLoader).lookupDefinedClass(className);
    }
    
    private static synchronized TypeHelperClassLoader getOrCreateLoader(Class<?> cls) {
        WeakReference<TypeHelperClassLoader> ref = CLASS_MAP.get(cls);
        TypeHelperClassLoader ret;
        if (ref == null || ref.get() == null) {
            ret = new TypeHelperClassLoader(cls.getClassLoader());
            CLASS_MAP.put(cls, new WeakReference<>(ret));
        } else {
            ret = ref.get();
        }
        return ret;
    }
    
    private static synchronized TypeHelperClassLoader getOrCreateLoader(ClassLoader l) {
        WeakReference<TypeHelperClassLoader> ref = LOADER_MAP.get(l);
        TypeHelperClassLoader ret;
        if (ref == null || ref.get() == null) {
            ret = new TypeHelperClassLoader(l);
            LOADER_MAP.put(l, new WeakReference<>(ret));
        } else {
            ret = ref.get();
        }
        return ret;
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
