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
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.security.GroupPrincipal;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.security.LoginSecurityContext;

/**
 * SecurityContext which implements isUserInRole using the
 * following approach : skip the first Subject principal, and then checks
 * Groups the principal is a member of
 */
public class DefaultSecurityContext implements LoginSecurityContext {
    private static final Logger LOG = LogUtils.getL7dLogger(DefaultSecurityContext.class);
    private static Class<?> javaGroup; 
    private static Class<?> karafGroup;
    
    private Principal p;
    private Subject subject;

    static {
        try {
            javaGroup = Class.forName("java.security.acl.Group");
        } catch (Exception e) {
            javaGroup = null;
        }
        try {
            karafGroup = Class.forName("org.apache.karaf.jaas.boot.principal.Group");
        } catch (Exception e) {
            karafGroup = null;
        }
    }
    
    public DefaultSecurityContext(Subject subject) {
        this.p = findPrincipal(null, subject);
        this.subject = subject;
    }

    public DefaultSecurityContext(String principalName, Subject subject) {
        this.p = findPrincipal(principalName, subject);
        this.subject = subject;
    }

    public DefaultSecurityContext(Principal p, Subject subject) {
        this.p = p;
        this.subject = subject;
        if (p == null) {
            this.p = findPrincipal(null, subject);
        }
    }

    private static Principal findPrincipal(String principalName, Subject subject) {
        if (subject == null) {
            return null;
        }

        for (Principal principal : subject.getPrincipals()) {
            if (!isGroupPrincipal(principal)
                && (principalName == null || principal.getName().equals(principalName))) {
                return principal;
            }
        }

        // No match for the principalName. Just return first non-Group Principal
        if (principalName != null) {
            for (Principal principal : subject.getPrincipals()) {
                if (!isGroupPrincipal(principal)) {
                    return principal;
                }
            }
        }

        return null;
    }

    public Principal getUserPrincipal() {
        return p;
    }

    public boolean isUserInRole(String role) {
        if (subject != null) {
            for (Principal principal : subject.getPrincipals()) {
                if (isGroupPrincipal(principal) 
                    && checkGroup(principal, role)) {
                    return true;
                } else if (p != principal
                           && role.equals(principal.getName())) {
                    return true;
                }
            }
        }
        return false;
    }


    protected boolean checkGroup(Principal principal, String role) {
        if (principal.getName().equals(role)) {
            return true;
        }

        Enumeration<? extends Principal> members;
        try {
            Method m = ReflectionUtil.getMethod(principal.getClass(), "members");
            m.setAccessible(true);
            @SuppressWarnings("unchecked")
            Enumeration<? extends Principal> ms = (Enumeration<? extends Principal>)m.invoke(principal);
            members = ms;
        } catch (Exception e) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Unable to invoke memebers in " + principal.getName() + ":" + e.getMessage());
            }
            return false;
        }
        
        while (members.hasMoreElements()) {
            // this might be a plain role but could represent a group consisting of other groups/roles
            Principal member = members.nextElement();
            if (member.getName().equals(role)
                || isGroupPrincipal(member) 
                && checkGroup((GroupPrincipal)member, role)) {
                return true;
            }
        }
        return false;
    }


    public Subject getSubject() {
        return subject;
    }

    public Set<Principal> getUserRoles() {
        Set<Principal> roles = new HashSet<>();
        if (subject != null) {
            for (Principal principal : subject.getPrincipals()) {
                if (principal != p) {
                    roles.add(principal);
                }
            }
        }
        return roles;
    }
    
    
    private static boolean instanceOfGroup(Object obj) { 
        try {
            return (javaGroup != null && javaGroup.isInstance(obj)) 
                || (karafGroup != null && karafGroup.isInstance(obj));
        } catch (Exception ex) {
            return false;
        }
    }
    
    public static boolean isGroupPrincipal(Principal principal) {
        return principal instanceof GroupPrincipal
            || instanceOfGroup(principal);
    }

}
