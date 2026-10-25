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
    // Scopes discovered from @Scopes annotations. Method-level and class-level annotations
    // are keyed by method signature so that overloaded methods do not share a rule and an
    // annotation on an overriding method is not replaced by one from a superclass or interface
    private final Map<String, Scopes> methodScopesBySignature = new HashMap<>();
    private final Map<String, Scopes> classScopesBySignature = new HashMap<>();
    // All the annotation-derived rules for a given method name, enforced together when the
    // invoked method's signature is not recorded (e.g. it implements a generic interface method)
    private final Map<String, Set<Scopes>> annotationScopesByName = new HashMap<>();
    private Set<String> confidentialClientMethods = new HashSet<>();

    public void setSecuredObject(Object object) {
        Class<?> cls = ClassHelper.getRealClass(object);
        checkSecureClass(cls);
        if (annotationScopesByName.isEmpty()) {
            LOG.warning("The scopes map is empty");
        } else if (LOG.isLoggable(Level.FINE)) {
            for (Map.Entry<String, Set<Scopes>> entry : annotationScopesByName.entrySet()) {
                for (Scopes scopes : entry.getValue()) {
                    LOG.fine("Method: " + entry.getKey() + ", scopes: " + Arrays.asList(scopes.value()));
                }
            }
        }
    }

    protected void checkSecureClass(Class<?> cls) {
        if (cls == null || cls == Object.class) {
            return;
        }
        Scopes classScopes = cls.getAnnotation(Scopes.class);
        ConfidentialClient classConfClient = cls.getAnnotation(ConfidentialClient.class);
        for (Method m : cls.getMethods()) {
            if (SKIP_METHODS.contains(m.getName())) {
                continue;
            }
            // Subclasses are scanned before their superclasses and interfaces, so the most
            // specific declaration of a given signature is recorded first and kept
            String signature = ReflectionUtil.getMethodSignature(m);
            Scopes methodScopes = m.getAnnotation(Scopes.class);
            // Each overload has its own signature and so its own rule. The rules are also
            // collected by name, but that is only used as a fallback when the invoked
            // method's signature was not recorded, and then all of them have to be met
            if (methodScopes != null) {
                methodScopesBySignature.putIfAbsent(signature, methodScopes);
                annotationScopesByName.computeIfAbsent(m.getName(), k -> new HashSet<>()).add(methodScopes);
            } else if (classScopes != null) {
                classScopesBySignature.putIfAbsent(signature, classScopes);
                annotationScopesByName.computeIfAbsent(m.getName(), k -> new HashSet<>()).add(classScopes);
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

    private List<Scopes> getAnnotationScopes(Method m) {
        String signature = ReflectionUtil.getMethodSignature(m);
        // A method-level annotation anywhere in the hierarchy takes precedence over a
        // class-level one, otherwise an inherited class-level @Scopes would replace it
        Scopes scopes = methodScopesBySignature.get(signature);
        if (scopes == null) {
            scopes = classScopesBySignature.get(signature);
        }
        if (scopes != null) {
            return Collections.singletonList(scopes);
        }
        Set<Scopes> scopesForName = annotationScopesByName.get(m.getName());
        return scopesForName == null ? Collections.emptyList() : new ArrayList<>(scopesForName);
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


}
