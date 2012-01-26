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
package org.apache.cxf.common.security;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Simple Group implementation
 *
 */
public class SimpleGroup extends SimplePrincipal implements Group {
    
    private Set<Principal> members = new HashSet<Principal>();
    
    public SimpleGroup(String groupName) {
        super(groupName);
    }
    
    public SimpleGroup(String groupName, String memberName) {
        super(groupName);
        members.add(new SimplePrincipal(memberName));
    }
    
    public SimpleGroup(String groupName, Principal member) {
        super(groupName);
        members.add(member);
    }

    public boolean isMember(Principal p) {
        return members.contains(p);
    }

    public boolean addMember(Principal p) {
        return members.add(p);
    }
    
    public Enumeration<? extends Principal> members() {
        
        final Iterator<Principal> it = members.iterator();
        
        return new Enumeration<Principal>() {

            public boolean hasMoreElements() {
                return it.hasNext();
            }

            public Principal nextElement() {
                return it.next();
            }
            
        };
    }

    public boolean removeMember(Principal p) {
        return members.remove(p);
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof SimpleGroup)) {
            return false;
        }
        SimpleGroup other = (SimpleGroup)obj;
        return getName().equals(other.getName()) && members.equals(other.members);
    }
    
    public int hashCode() {
        return getName().hashCode() + 37 * members.hashCode();
    }
}

