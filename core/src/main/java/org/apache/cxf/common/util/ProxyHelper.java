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
    private ClassLoader getClassLoaderForInterfaces(ClassLoader loader, Class<?>[] interfaces) {
        if (canSeeAllInterfaces(loader, interfaces)) {
            return loader;
        }
        ProxyClassLoader combined = new ProxyClassLoader(loader, interfaces);
        for (Class<?> currentInterface : interfaces) {
            combined.addLoader(currentInterface.getClassLoader());
        }
        return combined;
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
            } catch (NoClassDefFoundError e) {
                return false;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
        return true;
    }

    public static Object getProxy(ClassLoader loader, Class<?>[] interfaces, InvocationHandler handler) {
        return HELPER.getProxyInternal(loader, interfaces, handler);
    }
}
