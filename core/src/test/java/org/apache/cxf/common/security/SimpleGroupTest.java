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

import org.junit.Assert;
import org.junit.Test;

public class SimpleGroupTest extends Assert {

    @Test
    public void testName() {
        assertEquals("group", new SimpleGroup("group", "friend").getName());
        assertEquals("group", new SimpleGroup("group", new SimplePrincipal("friend")).getName());
    }
    
    @Test
    public void testIsMember() {
        assertTrue(new SimpleGroup("group", "friend").isMember(new SimplePrincipal("friend")));
        assertFalse(new SimpleGroup("group", "friend").isMember(new SimplePrincipal("frogs")));
    }
        
    @Test
    public void testAddRemoveMembers() {
        
        Group group = new SimpleGroup("group");   
        assertFalse(group.members().hasMoreElements());
        
        group.addMember(new SimpleGroup("group", "friend"));
        
        Enumeration<? extends Principal> members = group.members();
        assertEquals(new SimpleGroup("group", "friend"), members.nextElement());
        assertFalse(members.hasMoreElements());
        
        group.removeMember(new SimpleGroup("group", "friend"));
        assertFalse(group.members().hasMoreElements());
    }
}
