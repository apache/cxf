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

package org.apache.cxf.jaxrs.impl.tl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class ThreadLocalInvocationHandler<T> extends AbstractThreadLocalProxy<T>
    implements InvocationHandler {

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        final Object target;
        if (m.getDeclaringClass() == ThreadLocalProxy.class) {
            target = this;
        } else {
            target = get();
            if (target == null) {
                if (m.getName().endsWith("toString")) {
                    return null;
                }
                Class<?> contextCls = m.getDeclaringClass();
                throw new NullPointerException(
                                               contextCls.getName()
                                                   + " context class has not been injected."
                                                   + " Check if ContextProvider supporting this class is registered");
            }
        }
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
