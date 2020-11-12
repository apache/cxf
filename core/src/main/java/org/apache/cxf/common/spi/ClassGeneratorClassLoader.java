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

import org.apache.cxf.common.util.WeakIdentityHashMap;

public class ClassGeneratorClassLoader {
    protected static final Map<ClassLoader, WeakReference<TypeHelperClassLoader>> LOADER_MAP
            = new WeakIdentityHashMap<>();
    protected static final Map<Class<?>, WeakReference<TypeHelperClassLoader>> CLASS_MAP
            = new WeakIdentityHashMap<>();

    public ClassGeneratorClassLoader() {
    }


    protected Class<?> loadClass(String className, Class<?> clz, byte[] bytes) {
        TypeHelperClassLoader loader = getTypeHelperClassLoader(clz);
        synchronized (loader) {
            Class<?> cls = loader.lookupDefinedClass(className);
            if (cls == null) {
                return loader.defineClass(className, bytes);
            }
            return cls;
        }
    }
    protected Class<?> loadClass(String className, ClassLoader l, byte[] bytes) {
        TypeHelperClassLoader loader = getTypeHelperClassLoader(l);
        synchronized (loader) {
            Class<?> cls = loader.lookupDefinedClass(className);
            if (cls == null) {
                return loader.defineClass(className, bytes);
            }
            return cls;
        }
    }
    protected Class<?> findClass(String className, Class<?> clz) {
        TypeHelperClassLoader loader = getTypeHelperClassLoader(clz);
        return loader.lookupDefinedClass(className);
    }
    protected Class<?> findClass(String className, ClassLoader l) {
        TypeHelperClassLoader loader = getTypeHelperClassLoader(l);
        return loader.lookupDefinedClass(className);
    }

    private synchronized TypeHelperClassLoader getTypeHelperClassLoader(ClassLoader l) {
        WeakReference<TypeHelperClassLoader> ref = LOADER_MAP.get(l);
        TypeHelperClassLoader ret;
        if (ref == null || ref.get() == null) {
            ret = new TypeHelperClassLoader(l);
            LOADER_MAP.put(l, new WeakReference<TypeHelperClassLoader>(ret));
        } else {
            ret = ref.get();
        }
        return ret;
    }
    private synchronized TypeHelperClassLoader getTypeHelperClassLoader(Class<?> cls) {
        WeakReference<TypeHelperClassLoader> ref = CLASS_MAP.get(cls);
        TypeHelperClassLoader ret;
        if (ref == null || ref.get() == null) {
            ret = new TypeHelperClassLoader(cls.getClassLoader());
            CLASS_MAP.put(cls, new WeakReference<TypeHelperClassLoader>(ret));
        } else {
            ret = ref.get();
        }
        return ret;
    }


    public static class TypeHelperClassLoader extends ClassLoader {
        ConcurrentHashMap<String, Class<?>> defined = new ConcurrentHashMap<>();

        TypeHelperClassLoader(ClassLoader parent) {
            super(parent);
        }
        public Class<?> lookupDefinedClass(String name) {
            return defined.get(name.replace('/', '.'));
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (name.endsWith("package-info")) {
                return getParent().loadClass(name);
            }
            return super.findClass(name);
        }
        public void addExternalClass(String name, Class<?> cls) {
            if (name == null) {
                return;
            }
            defined.putIfAbsent(name.replace('/', '.'), cls);
        }
        public Class<?> defineClass(String name, byte[] bytes) {
            Class<?> ret = defined.get(name.replace('/', '.'));
            if (ret != null) {
                return ret;
            }
            if (name.endsWith("package-info")) {
                Package p = super.getPackage(name.substring(0, name.length() - 13));
                if (p == null) {
                    definePackage(name.substring(0, name.length() - 13).replace('/', '.'),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null);
                }
            }

            ret = super.defineClass(name.replace('/', '.'), bytes, 0, bytes.length);
            Class<?> tmpRet = defined.putIfAbsent(name.replace('/', '.'), ret);
            if (tmpRet != null) {
                ret = tmpRet;
            }
            return ret;
        }
    }
}
