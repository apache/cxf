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
import java.util.Arrays;
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
    private Map<String, List<String>> scopesMap = new HashMap<>();
    private Map<String, Boolean> scopesMatchAllMap = new HashMap<>();
    private Set<String> confidentialClientMethods = new HashSet<>();

    public void setSecuredObject(Object object) {
        Class<?> cls = ClassHelper.getRealClass(object);
        checkSecureClass(cls);
        if (scopesMap.isEmpty()) {
            LOG.warning("The scopes map is empty");
        } else if (LOG.isLoggable(Level.FINE)) {
            for (Map.Entry<String, List<String>> entry : scopesMap.entrySet()) {
                LOG.fine("Method: " + entry.getKey() + ", scopes: " + entry.getValue());
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
            Scopes methodScopes = m.getAnnotation(Scopes.class);
            Scopes theScopes = methodScopes == null ? classScopes : methodScopes;
            if (theScopes != null) {
                scopesMap.put(m.getName(), Arrays.asList(theScopes.value()));
                scopesMatchAllMap.put(m.getName(), theScopes.matchAll());
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
        List<String> methodScopes = scopesMap.get(m.getName());
        if (methodScopes == null) {
            return;
        }
        boolean matchAll = scopesMatchAllMap.get(m.getName());
        OAuthContext context = OAuthContextUtils.getContext(mc);
        List<String> requestScopes = new LinkedList<>();
        for (OAuthPermission perm : context.getPermissions()) {
            if (matchAll) {
                requestScopes.add(perm.getPermission());
            } else if (methodScopes.contains(perm.getPermission())) {
                return;
            }
        }

        if (!requestScopes.containsAll(methodScopes)) {
            LOG.warning("Scopes do not match");
            throw ExceptionUtils.toForbiddenException(null, null);
        }

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
