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

package org.apache.cxf.common.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;

public class ProxyClassLoaderCache {

    private static final Logger LOG = LogUtils.getL7dLogger(ProxyClassLoaderCache.class);
    private static final ThreadLocal<ClassLoader> PARENT_CLASSLOADER = new ThreadLocal<>();
    private static final ThreadLocal<Class<?>[]> PROXY_INTERFACES = new ThreadLocal<>();

    private final ClassValue<ClassLoader> backend = new ClassValue<ClassLoader>() {
        @Override
        protected ClassLoader computeValue(Class<?> proxyInterface) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("can't find ProxyClassLoader from ClassValue Cache, will create a new one");
                LOG.fine("interface for new created ProxyClassLoader is " + proxyInterface.getName());
                LOG.fine("interface's classloader for new created ProxyClassLoader is "
                    + ClassLoaderUtils.getClassLoaderName(proxyInterface));
            }
            return createProxyClassLoader(proxyInterface); // Parameter 'proxyInterface' is not used inside method body
        }
    };

    private ClassLoader createProxyClassLoader(Class<?> proxyInterface) {
        final SecurityManager sm = System.getSecurityManager();
        final ProxyClassLoader ret;
        if (sm == null) {
            ret = new ProxyClassLoader(PARENT_CLASSLOADER.get(), PROXY_INTERFACES.get());
        } else {
            ret = AccessController.doPrivileged(new PrivilegedAction<ProxyClassLoader>() {
                @Override
                public ProxyClassLoader run() {
                    return new ProxyClassLoader(PARENT_CLASSLOADER.get(), PROXY_INTERFACES.get());
                }
            });
        }
        for (Class<?> currentInterface : PROXY_INTERFACES.get()) {
            ret.addLoader(getClassLoader(currentInterface));
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("interface for new created ProxyClassLoader is " + currentInterface.getName());
                LOG.fine("interface's classloader for new created ProxyClassLoader is "
                    + getClassLoader(currentInterface));
            }
        }
        return ret;
    }

    public ClassLoader getProxyClassLoader(ClassLoader parent, Class<?>[] proxyInterfaces) {
        try {
            PARENT_CLASSLOADER.set(parent);
            PROXY_INTERFACES.set(proxyInterfaces);
            for (Class<?> currentInterface : proxyInterfaces) {
                String ifName = currentInterface.getName();
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("the interface we are checking is " + ifName);
                    LOG.fine("the interface's classloader we are checking is " 
                        + getClassLoader(currentInterface));
                }
                if (!ifName.startsWith("org.apache.cxf") && !ifName.startsWith("java")) {
                    // cache and retrieve customer interface
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("the customer interface is " + ifName + ". Will try to fetch it from Cache");
                    }
                    return backend.get(currentInterface);
                }
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Non of interfaces are customer interface, retrive the last interface as key:" 
                        + proxyInterfaces[proxyInterfaces.length - 1].getName());
            }
            //the last interface is the variable type
            return backend.get(proxyInterfaces[proxyInterfaces.length - 1]);
        } finally {
            PARENT_CLASSLOADER.remove();
            PROXY_INTERFACES.remove();
        }
    }

    public void removeStaleProxyClassLoader(Class<?> proxyInterface) {
        backend.remove(proxyInterface);
    }

    private static ClassLoader getClassLoader(final Class<?> clazz) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return clazz.getClassLoader();
                }
            });
        }
        return clazz.getClassLoader();
    }
}
