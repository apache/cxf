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
import java.lang.reflect.InvocationTargetException;
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
        final Class<?> targetClass = target.getClass();
        Class<?>[] parameterTypes = getParameterTypes(method, args);
        try {
            Method m;
            try {
                m = targetClass.getMethod(method.getName(), parameterTypes);
            } catch (NoSuchMethodException nsme) {

                boolean[] optionals = new boolean[method.getParameterTypes().length];
                int i = 0;
                int optionalNumber = 0;
                for (final Annotation[] a : method.getParameterAnnotations()) {
                    optionals[i] = false;
                    for (final Annotation potential : a) {
                        if (Optional.class.equals(potential.annotationType())) {
                            optionals[i] = true;
                            optionalNumber++;
                            break;
                        }
                    }
                    i++;
                }

                Class<?>[] newParams = new Class<?>[args.length - optionalNumber];
                Object[] newArgs = new Object[args.length - optionalNumber];
                int argI = 0;
                for (int j = 0; j < parameterTypes.length; j++) {
                    if (optionals[j]) {
                        continue;
                    }
                    newArgs[argI] = args[j];
                    newParams[argI] = parameterTypes[j];
                    argI++;
                }
                m = targetClass.getMethod(method.getName(), newParams);
                args = newArgs;
                parameterTypes = newParams;
            }
            ReflectionUtil.setAccessible(m);
            return wrapReturn(wr, m.invoke(target, args));
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } catch (NoSuchMethodException e) {
            for (Method m2 : targetClass.getMethods()) {
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
        final Annotation[][] parAnnotations = method.getParameterAnnotations();
        for (int x = 0; x < types.length; x++) {
            UnwrapParam p = getUnwrapParam(parAnnotations[x]);
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
            return new WrapperIterator(wr.value(), (Iterator<?>)t);
        }
        return createProxyWrapper(t, wr.value());
    }

    public static <T> T createProxyWrapper(Object target, Class<T> inf) {
        InvocationHandler h = new ReflectionInvokationHandler(target);
        return inf.cast(Proxy.newProxyInstance(inf.getClassLoader(), new Class<?>[] {inf}, h));
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Optional {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WrapReturn {
        Class<?> value();
        boolean iterator() default false;
    }

    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface UnwrapParam {
        String methodName() default "getValue";
        String typeMethodName() default "#default";
    }

    private static class WrapperIterator implements Iterator<Object> {
        Class<?> cls;
        Iterator<?> internal;
        WrapperIterator(Class<?> c, Iterator<?> it) {
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
