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
package org.apache.cxf.ws.security.wss4j;

import java.security.Principal;
import java.security.acl.Group;

import javax.security.auth.Subject;

import org.apache.cxf.common.security.SimpleGroup;
import org.apache.cxf.common.security.SimplePrincipal;

import org.junit.Assert;
import org.junit.Test;

public class DefaultSecurityContextTest extends Assert {

    @Test
    public void testUserNotInRole() {
        Subject s = new Subject();
        Principal p = new SimplePrincipal("Barry");
        s.getPrincipals().add(p);
        assertFalse(new DefaultSecurityContext(p, s).isUserInRole("friend"));
    }
    
    @Test
    public void testUserInRole() {
        Subject s = new Subject();
        Principal p = new SimplePrincipal("Barry");
        s.getPrincipals().add(p);
        s.getPrincipals().add(new SimpleGroup("friend", p));
        assertTrue(new DefaultSecurityContext(p, s).isUserInRole("friend"));
    }
    
    @Test
    public void testUserInRole2() {
        Subject s = new Subject();
        Principal p = new SimplePrincipal("Barry");
        s.getPrincipals().add(p);
        Group group = new SimpleGroup("Roles", p);
        group.addMember(new SimpleGroup("friend"));
        s.getPrincipals().add(group);
        assertTrue(new DefaultSecurityContext(p, s).isUserInRole("friend"));
    }
    
    @Test
    public void testUserInRole3() {
        Subject s = new Subject();
        Principal p = new SimplePrincipal("Barry");
        s.getPrincipals().add(p);
        Group group = new SimpleGroup("Roles", p);
        Group subgroup = new SimpleGroup("subgroup");
        subgroup.addMember(new SimpleGroup("friend"));
        group.addMember(subgroup);
        s.getPrincipals().add(group);
        assertTrue(new DefaultSecurityContext(p, s).isUserInRole("friend"));
    }
    
}
