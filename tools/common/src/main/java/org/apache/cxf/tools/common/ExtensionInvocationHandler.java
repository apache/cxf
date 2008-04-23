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

package org.apache.cxf.tools.common;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ExtensionInvocationHandler implements InvocationHandler {

    private Object obj;
    public ExtensionInvocationHandler(Object o) {
        obj = o;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class[] clzs = null;
        if (args != null) {
            clzs = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                clzs[i] = args[i].getClass();
            }
        }
        
        Method m = obj.getClass().getDeclaredMethod(method.getName(), method.getParameterTypes());
        return m.invoke(obj, args);
    }
}
