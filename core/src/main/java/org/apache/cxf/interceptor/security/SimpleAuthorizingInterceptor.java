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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.security.SecurityContext;


public class SimpleAuthorizingInterceptor extends AbstractAuthorizingInInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(SimpleAuthorizingInterceptor.class);
    private static final Set<String> PRIMITIVE_TYPES = Set.of(
        "boolean", "byte", "char", "short", "int", "long", "float", "double");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    protected Map<String, List<String>> methodRolesMap = new HashMap<>();
    protected Map<String, List<String>> userRolesMap = Collections.emptyMap();
    protected List<String> globalRoles = Collections.emptyList();
    private boolean checkConfiguredRolesOnly;

    public SimpleAuthorizingInterceptor() {
        this(true);
    }
    public SimpleAuthorizingInterceptor(boolean uniqueId) {
        super(uniqueId);
    }

    @Override
    protected boolean isUserInRole(SecurityContext sc, List<String> roles, boolean deny) {
        if (!checkConfiguredRolesOnly && !super.isUserInRole(sc, roles, deny)) {
            return false;
        }
        // Additional check.
        if (!userRolesMap.isEmpty()) {
            List<String> userRoles = userRolesMap.get(sc.getUserPrincipal().getName());
            if (userRoles == null) {
                return false;
            }
            for (String role : roles) {
                if (userRoles.contains(role)) {
                    return true;
                }
            }
            return false;
        }
        return !checkConfiguredRolesOnly;
    }

    protected String createMethodSig(Method method) {
        StringBuilder b = new StringBuilder(method.getReturnType().getName());
        b.append(' ').append(method.getName()).append('(');
        boolean first = true;
        for (Class<?> cls : method.getParameterTypes()) {
            if (!first) {
                b.append(", ");
            }
            first = false;
            b.append(cls.getName());
        }
        b.append(')');
        return b.toString();
    }

    @Override
    protected List<String> getExpectedRoles(Method method) {
        String sig = createMethodSig(method);
        List<String> roles = methodRolesMap.get(sig);
        if (roles == null) {
            roles = methodRolesMap.get(normalizeMethodSig(sig));
        }
        if (roles == null) {
            roles = methodRolesMap.get(method.getName());
        }
        if (roles != null) {
            return roles;
        }
        return globalRoles;
    }


    public void setMethodRolesMap(Map<String, String> rolesMap) {
        for (Map.Entry<String, List<String>> entry : parseRolesMap(rolesMap).entrySet()) {
            String key = normalizeMethodSig(entry.getKey());
            warnOnUnmatchableSig(key);
            methodRolesMap.put(key, entry.getValue());
        }
    }

    public void setUserRolesMap(Map<String, String> rolesMap) {
        userRolesMap = parseRolesMap(rolesMap);
    }

    public void setGlobalRoles(String roles) {
        globalRoles = Arrays.asList(roles.split(" "));
    }

    public void setCheckConfiguredRolesOnly(boolean checkConfiguredRolesOnly) {
        this.checkConfiguredRolesOnly = checkConfiguredRolesOnly;
    }

    /**
     * Brings a method signature key into the format produced by {@link #createMethodSig(Method)},
     * e.g. "void op(java.lang.String, long)", so that whitespace differences such as
     * "op(java.lang.String,long)" do not prevent a configured role rule from matching.
     * Keys that are not signatures are returned as is.
     */
    private static String normalizeMethodSig(String key) {
        int open = key.indexOf('(');
        int close = key.lastIndexOf(')');
        if (open == -1 || close < open) {
            return key;
        }
        String prefix = WHITESPACE.matcher(key.substring(0, open).trim()).replaceAll(" ");
        String params = WHITESPACE.matcher(key.substring(open + 1, close)).replaceAll("");
        return prefix + '(' + params.replace(",", ", ") + ')';
    }

    private static void warnOnUnmatchableSig(String key) {
        int open = key.indexOf('(');
        if (open == -1 || open + 1 >= key.length() - 1) {
            return;
        }
        for (String param : key.substring(open + 1, key.length() - 1).split(", ")) {
            // Class.getName() yields "java.lang.String", "long" or "[I", never "String" or "int[]"
            if (param.endsWith("[]")
                || param.indexOf('.') == -1 && !param.startsWith("[") && !PRIMITIVE_TYPES.contains(param)) {
                LOG.warning("Method roles key \"" + key + "\" has the parameter type \"" + param
                    + "\" which is not a Class.getName() type name, the key will not match any method");
            }
        }
    }

    private static Map<String, List<String>> parseRolesMap(Map<String, String> rolesMap) {
        Map<String, List<String>> map = new HashMap<>();
        for (Map.Entry<String, String> entry : rolesMap.entrySet()) {
            map.put(entry.getKey(), Arrays.asList(entry.getValue().split(" ")));
        }
        return map;
    }
}
