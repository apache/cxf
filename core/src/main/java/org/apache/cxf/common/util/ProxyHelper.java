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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

/**
 *
 */
public class ProxyHelper {
    static final ProxyHelper HELPER;
    static {
        ProxyHelper theHelper = null;
        try {
            theHelper = new CglibProxyHelper();
        } catch (Throwable ex) {
            theHelper = new ProxyHelper();
        }
        HELPER = theHelper;
    }
   
    private static final Logger LOG = LogUtils.getL7dLogger(ProxyHelper.class);
    
    protected Map<String, ClassLoader> proxyClassLoaderCache = 
        Collections.synchronizedMap(new HashMap<String, ClassLoader>());
    protected int cacheSize =
        Integer.parseInt(System.getProperty("org.apache.cxf.proxy.classloader.size", "3000"));
    
    
    protected ProxyHelper() {
    }

    protected Object getProxyInternal(ClassLoader loader, Class<?>[] interfaces, InvocationHandler handler) {
        ClassLoader combinedLoader = getClassLoaderForInterfaces(loader, interfaces);
        return Proxy.newProxyInstance(combinedLoader, interfaces, handler);
    }

    /**
     * Return a classloader that can see all the given interfaces If the given loader can see all interfaces
     * then it is used. If not then a combined classloader of all interface classloaders is returned.
     *
     * @param loader use supplied class loader
     * @param interfaces
     * @return classloader that sees all interfaces
     */
    private ClassLoader getClassLoaderForInterfaces(final ClassLoader loader, final Class<?>[] interfaces) {
        if (canSeeAllInterfaces(loader, interfaces)) {
            LOG.log(Level.FINE, "current classloader " + loader + " can see all interface");
            return loader;
        }
        String sortedNameFromInterfaceArray = getSortedNameFromInterfaceArray(interfaces);
        ClassLoader cachedLoader = proxyClassLoaderCache.get(sortedNameFromInterfaceArray);
        if (cachedLoader != null) {
            if (canSeeAllInterfaces(cachedLoader, interfaces)) {
                //found cached loader
                LOG.log(Level.FINE, "find required loader from ProxyClassLoader cache with key" 
                        + sortedNameFromInterfaceArray);
                return cachedLoader;
            } else {
                //found cached loader somehow can't see all interfaces
                LOG.log(Level.FINE, "find a loader from ProxyClassLoader cache with key " 
                        + sortedNameFromInterfaceArray
                        + " but can't see all interfaces");
            }
        }
        ProxyClassLoader combined;
        LOG.log(Level.FINE, "can't find required ProxyClassLoader from cache, create a new one with parent " + loader);
        final SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            combined = new ProxyClassLoader(loader, interfaces);
        } else {
            combined = AccessController.doPrivileged(new PrivilegedAction<ProxyClassLoader>() {
                @Override
                public ProxyClassLoader run() {
                    return new ProxyClassLoader(loader, interfaces);
                }
            });
        }
        for (Class<?> currentInterface : interfaces) {
            combined.addLoader(getClassLoader(currentInterface));
            LOG.log(Level.FINE, "interface for new created ProxyClassLoader is "
                + currentInterface.getName());
            LOG.log(Level.FINE, "interface's classloader for new created ProxyClassLoader is "
                + currentInterface.getClassLoader());
        }
        if (proxyClassLoaderCache.size() >= cacheSize) {
            LOG.log(Level.FINE, "proxyClassLoaderCache is full, need clear it");
            proxyClassLoaderCache.clear();
        }
        proxyClassLoaderCache.put(sortedNameFromInterfaceArray, combined);
        return combined;
    }
    
    private String getSortedNameFromInterfaceArray(Class<?>[] interfaces) {
        SortedArraySet<String> arraySet = new SortedArraySet<String>();
        for (Class<?> currentInterface : interfaces) {
            arraySet.add(currentInterface.getName() + currentInterface.getClassLoader());
        }
        return arraySet.toString();
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

    private boolean canSeeAllInterfaces(ClassLoader loader, Class<?>[] interfaces) {
        for (Class<?> currentInterface : interfaces) {
            String ifName = currentInterface.getName();
            try {
                Class<?> ifClass = Class.forName(ifName, true, loader);
                if (ifClass != currentInterface) {
                    return false;
                }
                //we need to check all the params/returns as well as the Proxy creation
                //will try to create methods for all of this even if they aren't used
                //by the client and not available in the clients classloader
                for (Method m : ifClass.getMethods()) {
                    Class<?> returnType = m.getReturnType();
                    if (!returnType.isPrimitive()) {
                        Class.forName(returnType.getName(), true, loader);
                    }
                    for (Class<?> p : m.getParameterTypes()) {
                        if (!p.isPrimitive()) {
                            Class.forName(p.getName(), true, loader);
                        }
                    }
                }
            } catch (NoClassDefFoundError | ClassNotFoundException e) {
                return false;
            }
        }
        return true;
    }

    public static Object getProxy(ClassLoader loader, Class<?>[] interfaces, InvocationHandler handler) {
        return HELPER.getProxyInternal(loader, interfaces, handler);
    }
    
}
