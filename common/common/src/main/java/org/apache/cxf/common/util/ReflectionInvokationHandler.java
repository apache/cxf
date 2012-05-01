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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import java.lang.reflect.Proxy;
import java.util.Iterator;

/**
 * 
 */
public class ReflectionInvokationHandler implements InvocationHandler {
    private Object target;
    
    public ReflectionInvokationHandler(Object obj) {
        target = obj;
    }
    
    public Object getTarget() {
        return target;        
    }
    
    /** {@inheritDoc}*/
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        WrapReturn wr = method.getAnnotation(WrapReturn.class);
        try {
            Method m = target.getClass().getMethod(method.getName(), getParameterTypes(method, args));
            ReflectionUtil.setAccessible(m);
            return wrapReturn(wr, m.invoke(target, args));                
        } catch (NoSuchMethodException e) {
            for (Method m2 : target.getClass().getMethods()) {
                if (m2.getName().equals(method.getName())
                    && m2.getParameterTypes().length == method.getParameterTypes().length) {
                    boolean found = true;
                    for (int x = 0; x < m2.getParameterTypes().length; x++) {
                        if (args[x] != null 
                            && !m2.getParameterTypes()[x].isInstance(args[x])) {
                            found = false;
                        }
                    }
                    if (found) {
                        ReflectionUtil.setAccessible(m2);
                        return wrapReturn(wr, m2.invoke(target, args));                            
                    }
                }
            }
            throw e;
        }
    }
    private Class<?>[] getParameterTypes(Method method, Object[] args) {
        Class<?>[] types = method.getParameterTypes();
        for (int x = 0; x < types.length; x++) {
            UnwrapParam p = getUnwrapParam(method.getParameterAnnotations()[x]);
            if (p != null) {
                String s = p.methodName();
                String tn = p.typeMethodName();
                try {
                    Method m = args[x].getClass().getMethod(s);
                    if ("#default".equals(tn)) {
                        types[x] = m.getReturnType();
                    } else {
                        Method m2 = args[x].getClass().getMethod(tn);
                        types[x] = (Class<?>)ReflectionUtil.setAccessible(m2).invoke(args[x]);
                    }
                    args[x] = ReflectionUtil.setAccessible(m).invoke(args[x]);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return types;
    }

    private UnwrapParam getUnwrapParam(Annotation[] annotations) {
        for (Annotation a : annotations) {
            if (a instanceof UnwrapParam) {
                return (UnwrapParam)a;
            }
        }
        return null;
    }

    private static Object wrapReturn(WrapReturn wr, Object t) {
        if (wr == null || t == null) {
            return t;
        }
        if (wr.iterator()) {
            return new WrapperIterator(wr.value(), (Iterator)t);
        }
        return createProxyWrapper(t, wr.value());
    }
    
    public static final <T> T createProxyWrapper(Object target, Class<T> inf) {
        InvocationHandler h = new ReflectionInvokationHandler(target);
        return inf.cast(Proxy.newProxyInstance(inf.getClassLoader(), new Class[] {inf}, h));
    }
    
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface WrapReturn {
        Class<?> value();
        boolean iterator() default false;
    }
    
<<<<<<< HEAD:common/common/src/main/java/org/apache/cxf/common/util/ReflectionInvokationHandler.java
    private static class WrapperIterator implements Iterator {
=======
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface UnwrapParam {
        String methodName() default "getValue";
        String typeMethodName() default "#default";
    }
    
    private static class WrapperIterator implements Iterator<Object> {
>>>>>>> ef63251... Add ability to unwrap a parameter into a real type.:api/src/main/java/org/apache/cxf/common/util/ReflectionInvokationHandler.java
        Class<?> cls;
        Iterator internal;
        public WrapperIterator(Class<?> c, Iterator it) {
            internal = it;
            cls = c;
        }
        public boolean hasNext() {
            return internal.hasNext();
        }
        public Object next() {
            Object obj = internal.next();
            return createProxyWrapper(obj, cls);
        }
        public void remove() {
            internal.remove();
        }
    }
}
