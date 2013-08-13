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

package org.apache.cxf.jaxrs.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Priority;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.NameBinding;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Priorities;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

public final class AnnotationUtils {

    private static final Set<Class<?>> PARAM_ANNOTATION_CLASSES;
    private static final Set<Class<?>> METHOD_ANNOTATION_CLASSES;
    static {
        PARAM_ANNOTATION_CLASSES = initParamAnnotationClasses();
        METHOD_ANNOTATION_CLASSES = initMethodAnnotationClasses();
    }

    private AnnotationUtils() {

    }
    

    private static Set<Class<?>> initParamAnnotationClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(PathParam.class);
        classes.add(QueryParam.class);
        classes.add(MatrixParam.class);
        classes.add(HeaderParam.class);
        classes.add(CookieParam.class);
        classes.add(FormParam.class);
        classes.add(BeanParam.class);
        return classes;
    }

    private static Set<Class<?>> initMethodAnnotationClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(HttpMethod.class);
        classes.add(Path.class);
        classes.add(Produces.class);
        classes.add(Consumes.class);
        return classes;
    }

    public static int getBindingPriority(Class<?> providerCls) {
        Priority b = getClassAnnotation(providerCls, Priority.class);
        return b == null ? Priorities.USER : b.value();
    }
    public static Set<String> getNameBindings(Annotation[] targetAnns) {
        if (targetAnns.length == 0) {
            return Collections.emptySet();
        }
        Set<String> names = new LinkedHashSet<String>();
        for (Annotation a : targetAnns) {
            NameBinding nb = a.annotationType().getAnnotation(NameBinding.class);
            if (nb != null) {
                names.add(a.annotationType().getName());
            }
        }
        return names;
    }
    
    public static boolean isParamAnnotationClass(Class<?> annotationClass) {
        return PARAM_ANNOTATION_CLASSES.contains(annotationClass);
    }

    public static boolean isValidParamAnnotationClass(Class<?> annotationClass) {
        return PARAM_ANNOTATION_CLASSES.contains(annotationClass) || Context.class == annotationClass;
    }

    public static boolean isValidParamAnnotations(Annotation[] paramAnnotations) {
        for (Annotation a : paramAnnotations) {
            if (AnnotationUtils.isValidParamAnnotationClass(a.annotationType())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMethodAnnotation(Annotation a) {
        return METHOD_ANNOTATION_CLASSES.contains(a.annotationType())
               || a.annotationType().getAnnotation(HttpMethod.class) != null;
    }

    public static String getAnnotationValue(Annotation a) {
        String value = null;
        if (a.annotationType() == PathParam.class) {
            value = ((PathParam)a).value();
        } else if (a.annotationType() == QueryParam.class) {
            value = ((QueryParam)a).value();
        } else if (a.annotationType() == MatrixParam.class) {
            value = ((MatrixParam)a).value();
        } else if (a.annotationType() == HeaderParam.class) {
            value = ((HeaderParam)a).value();
        } else if (a.annotationType() == CookieParam.class) {
            value = ((CookieParam)a).value();
        } else if (a.annotationType() == FormParam.class) {
            value = ((FormParam)a).value();
        }
        return value;
    }

    public static <T> T getAnnotation(Annotation[] anns, Class<T> type) {
        if (anns == null) {
            return null;
        }
        for (Annotation a : anns) {
            if (a.annotationType() == type) {
                return type.cast(a);
            }
        }
        return null;
    }

    public static Method getAnnotatedMethod(Method m) {
        Method annotatedMethod = doGetAnnotatedMethod(m);
        return annotatedMethod == null ? m : annotatedMethod;
    }

    private static Method doGetAnnotatedMethod(Method m) {

        if (m == null) {
            return m;
        }

        for (Annotation a : m.getAnnotations()) {
            if (AnnotationUtils.isMethodAnnotation(a)) {
                return m;
            }
        }
        for (Annotation[] paramAnnotations : m.getParameterAnnotations()) {
            if (isValidParamAnnotations(paramAnnotations)) {
                return m;
            }
        }

        Class<?> superC = m.getDeclaringClass().getSuperclass();
        if (superC != null && Object.class != superC) {
            try {
                Method method = doGetAnnotatedMethod(superC.getMethod(m.getName(), m.getParameterTypes()));
                if (method != null) {
                    return method;
                }
            } catch (NoSuchMethodException ex) {
                // ignore
            }
        }
        for (Class<?> i : m.getDeclaringClass().getInterfaces()) {
            try {
                Method method = doGetAnnotatedMethod(i.getMethod(m.getName(), m.getParameterTypes()));
                if (method != null) {
                    return method;
                }
            } catch (NoSuchMethodException ex) {
                // ignore
            }
        }

        return null;
    }

    public static String getHttpMethodValue(Method m) {
        for (Annotation a : m.getAnnotations()) {
            HttpMethod httpM = a.annotationType().getAnnotation(HttpMethod.class);
            if (httpM != null) {
                return httpM.value();
            }
        }
        return null;
    }

    public static <A extends Annotation> A getMethodAnnotation(Method m, Class<A> aClass) {
        return m == null ? null : m.getAnnotation(aClass);
    }

    public static <A extends Annotation> A getClassAnnotation(Class<?> c, Class<A> aClass) {
        if (c == null) {
            return null;
        }
        A p = c.getAnnotation(aClass);
        if (p != null) {
            return p;
        }

        p = getClassAnnotation(c.getSuperclass(), aClass);
        if (p != null) {
            return p;
        }

        // finally try the first one on the interface
        for (Class<?> i : c.getInterfaces()) {
            p = getClassAnnotation(i, aClass);
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    public static String getDefaultParameterValue(Annotation[] anns) {

        DefaultValue dv = AnnotationUtils.getAnnotation(anns, DefaultValue.class);
        return dv != null ? dv.value() : null;
    }

}
