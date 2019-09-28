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

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.common.classloader.ClassLoaderUtils;

public final class ReflectionUtil {

    private static Method springBeanUtilsDescriptorFetcher;
    private static boolean springChecked;

    private ReflectionUtil() {
        // intentionally empty
    }

    public static <T> T accessDeclaredField(final Field f, final Object o, final Class<T> responseClass) {
        return AccessController.doPrivileged(new PrivilegedAction<T>() {
            public T run() {
                boolean b = f.isAccessible();
                try {
                    f.setAccessible(true);
                    return responseClass.cast(f.get(o));
                } catch (SecurityException | IllegalAccessException e) {
                    return null;
                } finally {
                    f.setAccessible(b);
                }
            }
        });
    }
    public static <T> T accessDeclaredField(final String fieldName,
                                            final Class<?> cls,
                                            final Object o,
                                            final Class<T> responseClass) {
        return AccessController.doPrivileged(new PrivilegedAction<T>() {
            public T run() {
                Field f = getDeclaredField(cls, fieldName);
                boolean b = f.isAccessible();
                try {
                    f.setAccessible(true);
                    return responseClass.cast(f.get(o));
                } catch (SecurityException | IllegalAccessException e) {
                    return null;
                } finally {
                    f.setAccessible(b);
                }
            }
        });
    }

    public static Field getDeclaredField(final Class<?> cls, final String name) {
        return AccessController.doPrivileged(new PrivilegedAction<Field>() {
            public Field run() {
                try {
                    return cls.getDeclaredField(name);
                } catch (SecurityException | NoSuchFieldException e) {
                    return null;
                }
            }
        });
    }

    public static <T> Constructor<T> getDeclaredConstructor(final Class<T> cls, final Class<?> ... args) {
        return AccessController.doPrivileged(new PrivilegedAction<Constructor<T>>() {
            public Constructor<T> run() {
                try {
                    return cls.getDeclaredConstructor(args);
                } catch (SecurityException | NoSuchMethodException e) {
                    return null;
                }
            }
        });

    }
    public static <T> Constructor<T> getConstructor(final Class<T> cls, final Class<?> ... args) {
        return AccessController.doPrivileged(new PrivilegedAction<Constructor<T>>() {
            public Constructor<T> run() {
                try {
                    return cls.getConstructor(args);
                } catch (SecurityException | NoSuchMethodException e) {
                    return null;
                }
            }
        });
    }

    public static <T> Constructor<T>[] getDeclaredConstructors(final Class<T> cls) {
        return AccessController.doPrivileged(new PrivilegedAction<Constructor<T>[]>() {
            @SuppressWarnings("unchecked")
            public Constructor<T>[] run() {
                try {
                    return (Constructor<T>[])cls.getDeclaredConstructors();
                } catch (SecurityException e) {
                    return null;
                }
            }
        });
    }

    public static Method[] getDeclaredMethods(final Class<?> cls) {
        return AccessController.doPrivileged(new PrivilegedAction<Method[]>() {
            public Method[] run() {
                return cls.getDeclaredMethods();
            }
        });
    }

    public static Method getDeclaredMethod(final Class<?> clazz, final String name,
                                            final Class<?>... parameterTypes) throws NoSuchMethodException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Method>() {
                public Method run() throws Exception {
                    return clazz.getDeclaredMethod(name, parameterTypes);
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if (e instanceof NoSuchMethodException) {
                throw (NoSuchMethodException)e;
            }
            throw new SecurityException(e);
        }
    }
    public static Method getMethod(final Class<?> clazz, final String name,
                                   final Class<?>... parameterTypes) throws NoSuchMethodException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Method>() {
                public Method run() throws Exception {
                    return clazz.getMethod(name, parameterTypes);
                }
            });
        } catch (PrivilegedActionException pae) {
            Exception e = pae.getException();
            if (e instanceof NoSuchMethodException) {
                throw (NoSuchMethodException)e;
            }
            throw new SecurityException(e);
        }
    }

    public static Field[] getDeclaredFields(final Class<?> cls) {
        return AccessController.doPrivileged(new PrivilegedAction<Field[]>() {
            public Field[] run() {
                return cls.getDeclaredFields();
            }
        });
    }

    public static <T extends AccessibleObject> T setAccessible(final T o) {
        return AccessController.doPrivileged(new PrivilegedAction<T>() {
            public T run() {
                o.setAccessible(true);
                return o;
            }
        });
    }
    public static <T extends AccessibleObject> T setAccessible(final T o, final boolean b) {
        return AccessController.doPrivileged(new PrivilegedAction<T>() {
            public T run() {
                o.setAccessible(b);
                return o;
            }
        });
    }

    /**
     *  create own array of property descriptors to:
     *  <pre>
     *  - prevent memory leaks by Introspector's cache
     *  - get correct type for generic properties from superclass
     *     that are limited to a specific type in beanClass
     *    see http://bugs.sun.com/view_bug.do?bug_id=6528714
     *   we cannot use BeanUtils.getPropertyDescriptors because of issue SPR-6063
     *   </pre>
     * @param refClass calling class for class loading.
     * @param beanInfo Bean in question
     * @param beanClass class for bean in question
     * @param propertyDescriptors raw descriptors
     */
    public static PropertyDescriptor[] getPropertyDescriptorsAvoidSunBug(Class<?> refClass,
                                                                  BeanInfo beanInfo,
                                                                  Class<?> beanClass,
                                                                  PropertyDescriptor[] propertyDescriptors) {
        if (!springChecked) {
            try {
                springChecked = true;
                Class<?> cls = ClassLoaderUtils
                    .loadClass("org.springframework.beans.BeanUtils", refClass);
                springBeanUtilsDescriptorFetcher
                    = cls.getMethod("getPropertyDescriptor", Class.class, String.class);
            } catch (Exception e) {
                //ignore - just assume it's an unsupported/unknown annotation
            }
        }

        if (springBeanUtilsDescriptorFetcher != null) {
            if (propertyDescriptors != null) {
                List<PropertyDescriptor> descriptors = new ArrayList<>(propertyDescriptors.length);
                for (int i = 0; i < propertyDescriptors.length; i++) {
                    PropertyDescriptor propertyDescriptor = propertyDescriptors[i];
                    try {
                        propertyDescriptor = (PropertyDescriptor)springBeanUtilsDescriptorFetcher.invoke(null,
                                                                                     beanClass,
                                                                                     propertyDescriptor.getName());
                        if (propertyDescriptor != null) {
                            descriptors.add(propertyDescriptor);
                        }
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e.getCause());
                    }
                }
                return descriptors.toArray(new PropertyDescriptor[0]);
            }
            return null;
        }
        return beanInfo.getPropertyDescriptors();
    }

    /**
     * Look for a specified annotation on a method. If there, return it. If not, search it's containing class.
     * Assume that the annotation is marked @Inherited.
     *
     * @param m method to examine
     * @param annotationType the annotation type to look for.
     */
    public static <T extends Annotation> T getAnnotationForMethodOrContainingClass(Method m,
                                                                                   Class<T> annotationType) {
        T annotation = m.getAnnotation(annotationType);
        if (annotation != null) {
            return annotation;
        }
        annotation = m.getDeclaringClass().getAnnotation(annotationType);
        if (annotation != null) {
            return annotation;
        }
        for (Class<?> intf : m.getDeclaringClass().getInterfaces()) {
            annotation = getAnnotationForInterface(intf, annotationType);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }
    
    private static <T extends Annotation> T getAnnotationForInterface(Class<?> intf, Class<T> annotationType) {
        T annotation = intf.getAnnotation(annotationType);
        if (annotation != null) {
            return annotation;
        }
        for (Class<?> intf2 : intf.getInterfaces()) {
            return getAnnotationForInterface(intf2, annotationType);
        }
        return null;
    }
}
