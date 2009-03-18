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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;


/**
 * 
 */
class CglibProxyHelper extends ProxyHelper {
    CglibProxyHelper() throws Exception {
        Class.forName("net.sf.cglib.proxy.Enhancer");
        Class.forName("net.sf.cglib.proxy.MethodInterceptor");
        Class.forName("net.sf.cglib.proxy.MethodProxy");
    }
    
    @Override
    protected Object getProxyInternal(ClassLoader loader, Class[] interfaces, 
                                      final java.lang.reflect.InvocationHandler h) {
        
        Class superClass = null;
        List<Class> theInterfaces = new ArrayList<Class>();
        
        for (Class c : interfaces) {
            if (!c.isInterface()) {
                if (superClass != null) {
                    throw new IllegalArgumentException("Only a single superclass is supported");
                }
                superClass = c; 
            } else {
                theInterfaces.add(c);
            }
        }
        if (superClass != null) {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(superClass);
            enhancer.setInterfaces(theInterfaces.toArray(new Class[]{}));
            enhancer.setCallback(new MethodInterceptor() {

                public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) 
                    throws Throwable {
                    return h.invoke(obj, method, args);
                }
                
            });
            return enhancer.create();
        } else {
            return super.getProxyInternal(loader, interfaces, h);
        }
    }
    
    
}
