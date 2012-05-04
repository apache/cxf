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

import org.apache.cxf.security.SecurityContext;


public class SimpleAuthorizingInterceptor extends AbstractAuthorizingInInterceptor {

    private Map<String, List<String>> methodRolesMap = new HashMap<String, List<String>>();
    private Map<String, List<String>> userRolesMap = Collections.emptyMap();
    private List<String> globalRoles = Collections.emptyList();
    
    @Override 
    protected boolean isUserInRole(SecurityContext sc, List<String> roles, boolean deny) {
        if (!super.isUserInRole(sc, roles, deny)) {
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
        } else {
            return true;
        }
    }
    
    protected String createMethodSig(Method method) {
        StringBuilder b = new StringBuilder(method.getReturnType().getName());
        b.append(' ').append(method.getName()).append('(');
        boolean first = true;
        for (Class<?> cls : method.getParameterTypes()) {
            if (!first) {
                b.append(", ");
                first = false;
            }
            b.append(cls.getName());
        }
        b.append(')');
        return b.toString();
    }
    
    @Override
    protected List<String> getExpectedRoles(Method method) {
        List<String> roles = methodRolesMap.get(createMethodSig(method));
        if (roles == null) {
            roles = methodRolesMap.get(method.getName());
        }
        if (roles != null) {
            return roles;
        }
        return globalRoles;
    }


    public void setMethodRolesMap(Map<String, String> rolesMap) {
        methodRolesMap.putAll(parseRolesMap(rolesMap)); 
    }
    
    public void setUserRolesMap(Map<String, String> rolesMap) {
        userRolesMap = parseRolesMap(rolesMap);
    }
    
    public void setGlobalRoles(String roles) {
        globalRoles = Arrays.asList(roles.split(" "));
    }
    
    private static Map<String, List<String>> parseRolesMap(Map<String, String> rolesMap) {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        for (Map.Entry<String, String> entry : rolesMap.entrySet()) {
            map.put(entry.getKey(), Arrays.asList(entry.getValue().split(" ")));
        }
        return map;
    }
}
