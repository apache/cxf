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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.security.SecurityContext;


public class SecureAnnotationsInterceptor extends SimpleAuthorizingInterceptor {

    private static final Logger LOG = LogUtils.getL7dLogger(SecureAnnotationsInterceptor.class);
    private static final String DEFAULT_ANNOTATION_CLASS_NAME = "jakarta.annotation.security.RolesAllowed";

    private static final Set<String> SKIP_METHODS = new HashSet<>(
            Arrays.asList("wait", "notify", "notifyAll", "equals", "toString", "hashCode"));

    private String annotationClassName = DEFAULT_ANNOTATION_CLASS_NAME;
    // The method-level and class-level role annotations which apply to each method signature in
    // the secured object's hierarchy. Keying by signature means that overloaded methods do not
    // share a rule
    private final Map<String, List<RolesDeclaration>> declarationsBySignature = new HashMap<>();
    private final Map<String, Set<String>> signaturesByName = new HashMap<>();
    private final Set<Class<?>> securedClasses = new HashSet<>();
    // The classes passed to setSecuredObject, so their roles can be read again if the
    // annotation class changes afterwards
    private final List<Class<?>> securedObjectClasses = new ArrayList<>();

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
        addRoles(cls);
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
        declarationsBySignature.clear();
        signaturesByName.clear();
        securedClasses.clear();
        for (Class<?> cls : securedObjectClasses) {
            addRoles(cls);
        }
    }

    private void addRoles(Class<?> cls) {
        Map<String, String> rolesMap = new HashMap<>();
        findRoles(cls, rolesMap);
        // Entries added to the roles map, e.g. by a subclass, apply as configured method roles
        if (!rolesMap.isEmpty()) {
            super.setMethodRolesMap(rolesMap);
        }
        boolean protectedMethodFound = !rolesMap.isEmpty();
        for (String signature : declarationsBySignature.keySet()) {
            List<List<String>> rules = getAnnotationRoles(signature);
            if (!rules.isEmpty()) {
                protectedMethodFound = true;
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Method: " + signature + ", roles: " + rules);
                }
            }
        }
        if (!protectedMethodFound) {
            LOG.warning("The roles map is empty, the service object is not protected");
        }
    }

    /**
     * Records the role annotations of the methods of a class, its superclasses and interfaces.
     * The annotations are not added to the roles map: entries which are added to it, e.g. by
     * a subclass, apply as configured method roles.
     */
    protected void findRoles(Class<?> cls, Map<String, String> rolesMap) {
        if (cls == null || cls == Object.class || !securedClasses.add(cls)) {
            return;
        }
        // The class-level annotation may be inherited from a superclass if it is @Inherited
        List<String> classRoles = getRoles(cls.getAnnotations());
        Class<?> classRolesDeclarer = getAnnotationDeclarer(cls, annotationClassName);
        for (Method m : cls.getMethods()) {
            if (SKIP_METHODS.contains(m.getName())) {
                continue;
            }
            String signature = ReflectionUtil.getMethodSignature(m);
            List<String> methodRoles = getRoles(m.getAnnotations());
            List<RolesDeclaration> declarations =
                declarationsBySignature.computeIfAbsent(signature, k -> new ArrayList<>());
            if (methodRoles != null) {
                declarations.add(new RolesDeclaration(m.getDeclaringClass(), methodRoles, true));
            }
            // As in JSR-250, a class-level annotation does not apply to a method inherited from
            // a superclass which has its own method-level annotation, only to the class's own
            // methods and to unannotated inherited methods
            if (classRoles != null && (methodRoles == null || m.getDeclaringClass() == cls)) {
                declarations.add(new RolesDeclaration(classRolesDeclarer, classRoles, false));
            }
            signaturesByName.computeIfAbsent(m.getName(), k -> new HashSet<>()).add(signature);
        }
        findRoles(cls.getSuperclass(), rolesMap);
        for (Class<?> interfaceCls : cls.getInterfaces()) {
            findRoles(interfaceCls, rolesMap);
        }
    }

    @Override
    protected boolean authorize(SecurityContext sc, Method method) {
        List<List<String>> rules = getRoleRules(method);
        if (rules.isEmpty()) {
            return super.authorize(sc, method);
        }
        // Every rule which applies to the method has to be satisfied
        for (List<String> roles : rules) {
            if (roles.isEmpty() || !isUserInRole(sc, roles, false)) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine(sc.getUserPrincipal().getName() + " is not authorized");
                }
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean isMethodProtected(Method method) {
        return !getRoleRules(method).isEmpty() || super.isMethodProtected(method);
    }

    @Override
    protected List<String> getExpectedRoles(Method method) {
        List<List<String>> rules = getRoleRules(method);
        if (rules.isEmpty()) {
            return super.getExpectedRoles(method);
        }
        Set<String> roles = new LinkedHashSet<>();
        for (List<String> rule : rules) {
            roles.addAll(rule);
        }
        return new ArrayList<>(roles);
    }

    private List<List<String>> getRoleRules(Method method) {
        List<List<String>> rules = getAnnotationRoles(method);
        List<String> configuredRoles = getConfiguredRoles(method);
        if (configuredRoles != null) {
            rules.add(configuredRoles);
        }
        return rules;
    }

    private List<List<String>> getAnnotationRoles(Method method) {
        List<List<String>> rules = getAnnotationRoles(ReflectionUtil.getMethodSignature(method));
        if (!rules.isEmpty()) {
            return rules;
        }
        // No rule applies to the method's signature, e.g. because it implements a generic
        // interface method whose erased signature differs, so every rule recorded for its
        // name applies
        Set<List<String>> nameRules = new LinkedHashSet<>();
        for (String s : signaturesByName.getOrDefault(method.getName(), Collections.emptySet())) {
            nameRules.addAll(getAnnotationRoles(s));
        }
        return new ArrayList<>(nameRules);
    }

    private List<List<String>> getAnnotationRoles(String signature) {
        // Every role annotation which applies to the method has to be met, from any class or
        // interface in the hierarchy. A class-level annotation is replaced by a method-level one
        // declared on the same class or interface, or on a subclass of it
        List<RolesDeclaration> declarations =
            declarationsBySignature.getOrDefault(signature, Collections.emptyList());
        Set<List<String>> rules = new LinkedHashSet<>();
        for (RolesDeclaration declaration : declarations) {
            if (declaration.methodLevel || !isReplacedByMethodRoles(declaration, declarations)) {
                rules.add(declaration.roles);
            }
        }
        return new ArrayList<>(rules);
    }

    private static boolean isReplacedByMethodRoles(RolesDeclaration classDeclaration,
                                                   List<RolesDeclaration> declarations) {
        for (RolesDeclaration declaration : declarations) {
            if (declaration.methodLevel
                && classDeclaration.declaringClass.isAssignableFrom(declaration.declaringClass)) {
                return true;
            }
        }
        return false;
    }

    private static Class<?> getAnnotationDeclarer(Class<?> cls, String annName) {
        for (Class<?> declarer = cls; declarer != null; declarer = declarer.getSuperclass()) {
            if (hasAnnotation(declarer.getDeclaredAnnotations(), annName)) {
                return declarer;
            }
        }
        return cls;
    }

    private static boolean hasAnnotation(Annotation[] anns, String annName) {
        for (Annotation ann : anns) {
            if (ann.annotationType().getName().equals(annName)) {
                return true;
            }
        }
        return false;
    }

    private List<String> getRoles(Annotation[] anns) {
        for (Annotation ann : anns) {
            if (ann.annotationType().getName().equals(annotationClassName)) {
                try {
                    Method valueMethod = ann.annotationType().getMethod("value");
                    String[] roles = (String[])valueMethod.invoke(ann);
                    return Arrays.asList(roles);
                } catch (Exception ex) {
                    // ignore
                }
                break;
            }
        }
        return null;
    }

    private static final class RolesDeclaration {
        // The class or interface the annotation is declared on
        private final Class<?> declaringClass;
        // The roles of which one is required
        private final List<String> roles;
        private final boolean methodLevel;

        RolesDeclaration(Class<?> declaringClass, List<String> roles, boolean methodLevel) {
            this.declaringClass = declaringClass;
            this.roles = roles;
            this.methodLevel = methodLevel;
        }
    }
}
