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
package org.apache.cxf.interceptor.security;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClassHelper;


public class SecureAnnotationsInterceptor extends SimpleAuthorizingInterceptor {

    private static final Logger LOG = LogUtils.getL7dLogger(SecureAnnotationsInterceptor.class);
    private static final String DEFAULT_ANNOTATION_CLASS_NAME = "javax.annotation.security.RolesAllowed";

    private static final Set<String> SKIP_METHODS = new HashSet<>(
            Arrays.asList("wait", "notify", "notifyAll", "equals", "toString", "hashCode"));

    private String annotationClassName = DEFAULT_ANNOTATION_CLASS_NAME;
    // The classes passed to setSecuredObject, so their roles can be read again if the
    // annotation class changes afterwards
    private final List<Class<?>> securedObjectClasses = new ArrayList<>();
    // The methodRolesMap entries added from the annotations, so they can be replaced
    // without touching the entries configured via setMethodRolesMap
    private final Map<String, List<String>> annotationRoles = new HashMap<>();

    public SecureAnnotationsInterceptor() {
        this(true);
    }
    public SecureAnnotationsInterceptor(boolean uniqueId) {
        super(uniqueId);
    }

    public void setAnnotationClassName(String name) {
        try {
            ClassLoaderUtils.loadClass(name, SecureAnnotationsInterceptor.class);
            annotationClassName = name;
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw new IllegalArgumentException("Annotation class " + name + " is not available");
        }
        resolveSecuredObjectRoles();
    }

    public void setSecuredObject(Object object) {
        Class<?> cls = ClassHelper.getRealClass(object);
        securedObjectClasses.add(cls);
        addRoles(cls, Collections.emptyMap());
    }

    /**
     * The roles are read from the annotations when the secured object is set, so read them
     * again with the new annotation class for any secured object which was set before it.
     * Otherwise the roles would come from the default annotation class, and a service using
     * another annotation would be left unprotected.
     */
    private void resolveSecuredObjectRoles() {
        if (securedObjectClasses.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<String>> entry : annotationRoles.entrySet()) {
            // Keep an entry which setMethodRolesMap has replaced since
            if (methodRolesMap.get(entry.getKey()) == entry.getValue()) {
                methodRolesMap.remove(entry.getKey());
            }
        }
        annotationRoles.clear();
        // What is left was configured via setMethodRolesMap after the secured object, so it
        // takes precedence, as it would have if the annotation class had been set first
        Map<String, List<String>> configuredRoles = new HashMap<>(methodRolesMap);
        for (Class<?> cls : securedObjectClasses) {
            addRoles(cls, configuredRoles);
        }
    }

    private void addRoles(Class<?> cls, Map<String, List<String>> configuredRoles) {
        Map<String, String> rolesMap = new HashMap<>();
        findRoles(cls, rolesMap);
        if (rolesMap.isEmpty()) {
            LOG.warning("The roles map is empty, the service object is not protected");
        } else if (LOG.isLoggable(Level.FINE)) {
            for (Map.Entry<String, String> entry : rolesMap.entrySet()) {
                LOG.fine("Method: " + entry.getKey() + ", roles: " + entry.getValue());
            }
        }
        Map<String, List<String>> previousRoles = new HashMap<>(methodRolesMap);
        super.setMethodRolesMap(rolesMap);
        // Record the entries the annotations added or replaced, using the keys as stored
        // by setMethodRolesMap, and restore any configured entry they replaced
        for (Map.Entry<String, List<String>> entry : methodRolesMap.entrySet()) {
            if (entry.getValue() != previousRoles.get(entry.getKey())) {
                List<String> configured = configuredRoles.get(entry.getKey());
                if (configured != null) {
                    entry.setValue(configured);
                } else {
                    annotationRoles.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    protected void findRoles(Class<?> cls, Map<String, String> rolesMap) {
        if (cls == null || cls == Object.class) {
            return;
        }
        String classRolesAllowed = getRoles(cls.getAnnotations(), annotationClassName);
        for (Method m : cls.getMethods()) {
            if (SKIP_METHODS.contains(m.getName())) {
                continue;
            }
            String methodRolesAllowed = getRoles(m.getAnnotations(), annotationClassName);
            String theRoles = methodRolesAllowed != null ? methodRolesAllowed : classRolesAllowed;
            if (theRoles != null) {
                rolesMap.put(m.getName(), theRoles);
                rolesMap.put(createMethodSig(m), theRoles);
            }
        }
        if (!rolesMap.isEmpty()) {
            return;
        }

        findRoles(cls.getSuperclass(), rolesMap);

        if (!rolesMap.isEmpty()) {
            return;
        }

        for (Class<?> interfaceCls : cls.getInterfaces()) {
            findRoles(interfaceCls, rolesMap);
        }
    }

    private String getRoles(Annotation[] anns, String annName) {
        for (Annotation ann : anns) {
            if (ann.annotationType().getName().equals(annName)) {
                try {
                    Method valueMethod = ann.annotationType().getMethod("value");
                    String[] roles = (String[])valueMethod.invoke(ann);
                    return String.join(" ", roles);
                } catch (Exception ex) {
                    // ignore
                }
                break;
            }
        }
        return null;
    }
}
