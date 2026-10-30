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
package org.apache.cxf.rs.security.oauth2.filters;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.rs.security.oauth2.common.OAuthContext;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.utils.OAuthContextUtils;

@Priority(Priorities.AUTHENTICATION + 1)
public class OAuthScopesFilter implements ContainerRequestFilter {

    private static final Logger LOG = LogUtils.getL7dLogger(OAuthScopesFilter.class);
    private static final Set<String> SKIP_METHODS = new HashSet<>(
            Arrays.asList("wait", "notify", "notifyAll", "equals", "toString", "hashCode"));

    @Context
    private MessageContext mc;
    // Scopes configured explicitly via setScopesMap/setScopesStringMap, keyed by method name
    private Map<String, List<String>> scopesMap = new HashMap<>();
    private Map<String, Boolean> scopesMatchAllMap = new HashMap<>();
    // The method-level and class-level @Scopes declarations which apply to each method signature
    // in the secured object's hierarchy. Keying by signature means that overloaded methods do
    // not share a rule
    private final Map<String, List<ScopesDeclaration>> declarationsBySignature = new HashMap<>();
    // The erased signatures a method overrides with a different signature, e.g. update(Object)
    // for update(String) implementing update(T) of Repository<String>
    private final Map<String, Set<String>> overriddenSignatures = new HashMap<>();
    private final Map<String, Set<String>> signaturesByName = new HashMap<>();
    private final Set<Class<?>> securedClasses = new HashSet<>();
    private boolean annotationScopesFound;
    private Set<String> confidentialClientMethods = new HashSet<>();

    public void setSecuredObject(Object object) {
        Class<?> cls = ClassHelper.getRealClass(object);
        checkSecureClass(cls);
        if (!annotationScopesFound) {
            LOG.warning("The scopes map is empty");
        } else if (LOG.isLoggable(Level.FINE)) {
            for (String signature : declarationsBySignature.keySet()) {
                for (Scopes scopes : getAnnotationScopes(signature)) {
                    LOG.fine("Method: " + signature + ", scopes: " + Arrays.asList(scopes.value()));
                }
            }
        }
    }

    protected void checkSecureClass(Class<?> cls) {
        if (cls == null || cls == Object.class || !securedClasses.add(cls)) {
            return;
        }
        // The class-level @Scopes may be inherited from a superclass (@Scopes is @Inherited)
        Scopes classScopes = cls.getAnnotation(Scopes.class);
        Class<?> classScopesDeclarer = getScopesDeclarer(cls);
        ConfidentialClient classConfClient = cls.getAnnotation(ConfidentialClient.class);
        for (Method m : cls.getMethods()) {
            if (SKIP_METHODS.contains(m.getName())) {
                continue;
            }
            String signature = ReflectionUtil.getMethodSignature(m);
            Scopes methodScopes = m.getAnnotation(Scopes.class);
            List<ScopesDeclaration> declarations =
                declarationsBySignature.computeIfAbsent(signature, k -> new ArrayList<>());
            if (methodScopes != null) {
                declarations.add(new ScopesDeclaration(m.getDeclaringClass(), methodScopes, true));
            }
            if (classScopes != null) {
                declarations.add(new ScopesDeclaration(classScopesDeclarer, classScopes, false));
            }
            signaturesByName.computeIfAbsent(m.getName(), k -> new HashSet<>()).add(signature);
            annotationScopesFound |= methodScopes != null || classScopes != null;
            if (!m.isBridge()) {
                for (Method overridden : ReflectionUtil.getGenericallyOverriddenMethods(cls, m)) {
                    overriddenSignatures.computeIfAbsent(signature, k -> new HashSet<>())
                        .add(ReflectionUtil.getMethodSignature(overridden));
                }
            }

            ConfidentialClient mConfClient = m.getAnnotation(ConfidentialClient.class);
            if (classConfClient != null || mConfClient != null) {
                confidentialClientMethods.add(m.getName());
            }
        }
        checkSecureClass(cls.getSuperclass());
        for (Class<?> interfaceCls : cls.getInterfaces()) {
            checkSecureClass(interfaceCls);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Method m = getTargetMethod();
        checkClient(m);
        checkScopes(m);
    }
    protected void checkClient(Method m) {
        if (confidentialClientMethods.contains(m.getName())) {
            OAuthContext context = OAuthContextUtils.getContext(mc);
            if (!context.isClientConfidential()) {
                LOG.warning("Non confidential client " + context.getClientId()
                    + " has attempted to invoke " + m.getName());
                throw ExceptionUtils.toForbiddenException(null, null);
            }
        }
    }
    protected void checkScopes(Method m) {
        List<Scopes> annotationScopes = getAnnotationScopes(m);
        List<String> configuredScopes = scopesMap.get(m.getName());
        if (annotationScopes.isEmpty() && configuredScopes == null) {
            return;
        }
        OAuthContext context = OAuthContextUtils.getContext(mc);
        List<String> requestScopes = new LinkedList<>();
        for (OAuthPermission perm : context.getPermissions()) {
            requestScopes.add(perm.getPermission());
        }

        // Every rule which applies to the method has to be satisfied
        boolean allowed = true;
        for (Scopes scopes : annotationScopes) {
            allowed &= isAllowed(requestScopes, Arrays.asList(scopes.value()), scopes.matchAll());
        }
        if (configuredScopes != null) {
            allowed &= isAllowed(requestScopes, configuredScopes,
                                 Boolean.TRUE.equals(scopesMatchAllMap.get(m.getName())));
        }
        if (!allowed) {
            LOG.warning("Scopes do not match");
            throw ExceptionUtils.toForbiddenException(null, null);
        }
    }

    private static Class<?> getScopesDeclarer(Class<?> cls) {
        Class<?> declarer = cls;
        while (declarer != null && declarer.getDeclaredAnnotation(Scopes.class) == null) {
            declarer = declarer.getSuperclass();
        }
        return declarer == null ? cls : declarer;
    }

    private List<Scopes> getAnnotationScopes(Method m) {
        String signature = ReflectionUtil.getMethodSignature(m);
        Set<String> signatures = new HashSet<>();
        if (declarationsBySignature.containsKey(signature)) {
            signatures.add(signature);
            signatures.addAll(overriddenSignatures.getOrDefault(signature, Collections.emptySet()));
        } else {
            // The method is not one of the secured object's methods, so every rule
            // recorded for its name applies
            signatures.addAll(signaturesByName.getOrDefault(m.getName(), Collections.emptySet()));
        }
        Set<Scopes> scopes = new LinkedHashSet<>();
        for (String s : signatures) {
            scopes.addAll(getAnnotationScopes(s));
        }
        return new ArrayList<>(scopes);
    }

    private List<Scopes> getAnnotationScopes(String signature) {
        // Every @Scopes which applies to the method has to be met, from any class or interface
        // in the hierarchy. A class-level @Scopes is replaced by a method-level @Scopes declared on
        // the same class or interface, or on a subclass of it
        List<ScopesDeclaration> declarations =
            declarationsBySignature.getOrDefault(signature, Collections.emptyList());
        Set<Scopes> scopes = new LinkedHashSet<>();
        for (ScopesDeclaration declaration : declarations) {
            if (declaration.methodLevel || !isReplacedByMethodScopes(declaration, declarations)) {
                scopes.add(declaration.scopes);
            }
        }
        return new ArrayList<>(scopes);
    }

    private static boolean isReplacedByMethodScopes(ScopesDeclaration classDeclaration,
                                                    List<ScopesDeclaration> declarations) {
        for (ScopesDeclaration declaration : declarations) {
            if (declaration.methodLevel
                && classDeclaration.declaringClass.isAssignableFrom(declaration.declaringClass)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAllowed(List<String> requestScopes, List<String> methodScopes,
                                     boolean matchAll) {
        if (matchAll || methodScopes.isEmpty()) {
            return requestScopes.containsAll(methodScopes);
        }
        for (String scope : requestScopes) {
            if (methodScopes.contains(scope)) {
                return true;
            }
        }
        return false;
    }

    protected Method getTargetMethod() {
        Method method = (Method)mc.get("org.apache.cxf.resource.method");
        if (method != null) {
            return method;
        }
        throw ExceptionUtils.toForbiddenException(null, null);
    }

    public void setScopesMap(Map<String, List<String>> scopesMap) {
        this.scopesMap = scopesMap;
    }

    public void setScopesStringMap(Map<String, String> scopesStringMap) {
        for (Map.Entry<String, String> entry : scopesStringMap.entrySet()) {
            scopesMap.put(entry.getKey(), Arrays.asList(entry.getValue().split(" ")));
        }
    }

    public void setScopesMatchAllMap(Map<String, Boolean> scopesMatchAllMap) {
        this.scopesMatchAllMap = scopesMatchAllMap;
    }

    public void setConfidentialClientMethods(Set<String> confidentialClientMethods) {
        this.confidentialClientMethods = confidentialClientMethods;
    }

    private static final class ScopesDeclaration {
        // The class or interface the annotation is declared on
        private final Class<?> declaringClass;
        private final Scopes scopes;
        private final boolean methodLevel;

        ScopesDeclaration(Class<?> declaringClass, Scopes scopes, boolean methodLevel) {
            this.declaringClass = declaringClass;
            this.scopes = scopes;
            this.methodLevel = methodLevel;
        }
    }
}
