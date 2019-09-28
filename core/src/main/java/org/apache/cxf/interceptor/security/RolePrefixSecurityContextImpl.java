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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.cxf.security.LoginSecurityContext;

public class RolePrefixSecurityContextImpl implements LoginSecurityContext {
    private Principal p;
    private Set<Principal> roles;
    private Subject theSubject;

    public RolePrefixSecurityContextImpl(Subject subject, String rolePrefix) {
        this(subject, rolePrefix, JAASLoginInterceptor.ROLE_CLASSIFIER_PREFIX);
    }

    public RolePrefixSecurityContextImpl(Subject subject, String roleClassifier,
                                         String roleClassifierType) {
        this.p = findPrincipal(subject, roleClassifier, roleClassifierType);
        this.roles = findRoles(subject, roleClassifier, roleClassifierType);
        this.theSubject = subject;
    }

    public Principal getUserPrincipal() {
        return p;
    }

    public boolean isUserInRole(String role) {
        // there is no guarantee the Principal instances retrieved
        // from the Subject properly implement equalTo
        for (Principal principal : roles) {
            if (principal.getName().equals(role)) {
                return true;
            }
        }
        return false;
    }

    private static Principal findPrincipal(Subject subject,
        String roleClassifier, String roleClassifierType) {
        for (Principal p : subject.getPrincipals()) {
            if (!isRole(p, roleClassifier, roleClassifierType)) {
                return p;
            }
        }
        return null;
    }

    private static Set<Principal> findRoles(Subject subject,
        String roleClassifier, String roleClassifierType) {
        Set<Principal> set = new HashSet<>();
        for (Principal p : subject.getPrincipals()) {
            if (isRole(p, roleClassifier, roleClassifierType)) {
                set.add(p);
            }
        }
        return Collections.unmodifiableSet(set);
    }

    private static boolean isRole(Principal p, String roleClassifier, String roleClassifierType) {
        if (JAASLoginInterceptor.ROLE_CLASSIFIER_PREFIX.equals(roleClassifierType)) {
            return p.getName().startsWith(roleClassifier);
        }
        return p.getClass().getName().endsWith(roleClassifier);
    }

    public Subject getSubject() {
        return theSubject;
    }

    public Set<Principal> getUserRoles() {
        return roles;
    }
}