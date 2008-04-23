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
/**
 * 
 */
package org.apache.cxf.aegis.type.basic;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class InterfaceInvocationHandler implements InvocationHandler {
    private Map<Object, Object> attributes = new HashMap<Object, Object>();

    InterfaceInvocationHandler() {
    }


    public void writeProperty(Object key, Object value) {
        attributes.put(key, value);
    }

    public Object readProperty(Object key) {
        return attributes.get(key);
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class.getMethod("hashCode", (Class[]) null).equals(method)) {
            return proxyHashCode(proxy);
        } else if (Object.class.getMethod("equals", new Class[] {Object.class}).equals(method)) {
            return proxyEquals(proxy, args[0]);
        } else if (Object.class.getMethod("toString", (Class[]) null).equals(method)) {
            return proxyToString(proxy);
        } else {
            if (isGetterMethod(method, args)) {
                return doGetter(method, args);
            } else if (isSetterMethod(method, args)) {
                return doSetter(method, args);
            } else {
                throw new IllegalAccessError(method.getName() + " is not delegated.");
            }
        }
    }

    protected boolean isGetterMethod(Method method, Object[] args) {
        if (args != null && args.length > 0) {
            return false;
        }

        String methodName = method.getName();

        if (methodName.startsWith("get") && methodName.length() > 3) {
            return true;
        } 
            
        return methodName.length() > 2 && methodName.startsWith("is");
        /*
         * // should "hasXXX()" be considered a getter method? else if
         * (methodName.startsWith("has") && methodName.length() > 3) { return
         * true; }
         */
    }

    protected boolean isSetterMethod(Method method, Object[] args) {
        if (args == null || args.length != 1) {
            return false;
        }

        if (!"void".equals(method.getReturnType().getName())) {
            return false;
        }

        String methodName = method.getName();

        return methodName.startsWith("set") && methodName.length() > 3;
    }

    protected Object doGetter(Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        String attrName = null;

        if (methodName.startsWith("get")) {
            attrName = convertMethodName(methodName, 3);
        } else if (methodName.startsWith("is")) {
            attrName = convertMethodName(methodName, 2);
        } else {
            throw new IllegalAccessError(methodName + " is not a valid getter method.");
        }
        
        Object prop = readProperty(attrName);
        if (prop == null && method.getReturnType().isPrimitive()) {
            if (method.getReturnType() == int.class) {
                prop = new Integer(0);
            } else if (method.getReturnType() == boolean.class) {
                prop = Boolean.FALSE;
            } else if (method.getReturnType() == long.class) {
                prop = new Long(0);
            } else if (method.getReturnType() == double.class) {
                prop = new Double(0);
            } else if (method.getReturnType() == short.class) {
                prop = new Short((short)0);
            } else if (method.getReturnType() == byte.class) {
                prop = new Byte((byte)0);
            } else if (method.getReturnType() == char.class) {
                prop = new Character((char)0);
            }
        }
        return prop;
    }

    protected Object doSetter(Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        String attrName = null;

        if (methodName.startsWith("set")) {
            attrName = convertMethodName(methodName, 3);
        } else {
            throw new IllegalAccessError(methodName + " is not a valid setter method.");
        }

        writeProperty(attrName, args[0]);

        return null;
    }

    private String convertMethodName(String methodName, int firstCharacter) {
        if (methodName.length() >= firstCharacter + 2) {
            if (!Character.isUpperCase(methodName.charAt(firstCharacter + 1))) {
                return Character.toLowerCase(methodName.charAt(firstCharacter))
                       + methodName.substring(firstCharacter + 1);
            } else {
                return methodName.substring(3);
            }
        } else {
            return Character.toLowerCase(methodName.charAt(firstCharacter))
                   + methodName.substring(firstCharacter + 1);
        }
    }

    protected Integer proxyHashCode(Object proxy) {
        return new Integer(System.identityHashCode(proxy));
    }

    protected Boolean proxyEquals(Object proxy, Object other) {
        return proxy == other ? Boolean.TRUE : Boolean.FALSE;
    }

    protected String proxyToString(Object proxy) {
        return proxy.getClass().getName() + '@' + proxy.hashCode();
    }
}
