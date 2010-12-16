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

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.cxf.security.SecurityContext;

public class RolePrefixSecurityContextImpl implements SecurityContext {
    private Principal p;
    private Set<String> roles; 
    
    public RolePrefixSecurityContextImpl(Subject subject, String rolePrefix) {
        this.p = findPrincipal(subject, rolePrefix);
        this.roles = findRoles(subject, rolePrefix);
    }
    
    public Principal getUserPrincipal() {
        return p;
    }

    public boolean isUserInRole(String role) {
        return roles.contains(role);
    }
    
    private static Principal findPrincipal(Subject subject, String rolePrefix) {
        for (Principal p : subject.getPrincipals()) {
            if (!p.getName().startsWith(rolePrefix)) {
                return p;
            }
        }
        return null;
    }
    
    private static Set<String> findRoles(Subject subject, String rolePrefix) {
        Set<String> set = new HashSet<String>();
        for (Principal p : subject.getPrincipals()) {
            if (p.getName().startsWith(rolePrefix)) {
                set.add(p.getName());
            }
        }
        return set;
    }
}