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
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.cxf.security.LoginSecurityContext;

/**
 * SecurityContext which implements isUserInRole using the
 * following approach : skip the first Subject principal, and then checks
 * Groups the principal is a member of
 * 
 * TODO : consider moving this class into a rt-core-security module
 */
public class DefaultSecurityContext implements LoginSecurityContext {

    private Principal p;
    private Subject subject; 
    
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
            if (!(principal instanceof Group) 
                && (principalName == null || principal.getName().equals(principalName))) {
                return principal;
            }
        }
        
        // No match for the principalName. Just return first non-Group Principal
        if (principalName != null) {
            for (Principal principal : subject.getPrincipals()) {
                if (!(principal instanceof Group)) { 
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
                if (principal instanceof Group && checkGroup((Group)principal, role)) {
                    return true;
                } else if (p != principal
                           && role.equals(principal.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean checkGroup(Group group, String role) {
        if (group.getName().equals(role)) {
            return true;
        }
            
        for (Enumeration<? extends Principal> members = group.members(); members.hasMoreElements();) {
            // this might be a plain role but could represent a group consisting of other groups/roles
            Principal member = members.nextElement();
            if (member.getName().equals(role) 
                || member instanceof Group && checkGroup((Group)member, role)) {
                return true;
            }
        }
        return false;    
    }

    
    public Subject getSubject() {
        return subject;
    }

    public Set<Principal> getUserRoles() {
        Set<Principal> roles = new HashSet<Principal>();
        if (subject != null) {
            for (Principal principal : subject.getPrincipals()) {
                if (principal != p) { 
                    roles.add(principal);
                }
            }
        }
        return roles;
    }
}
