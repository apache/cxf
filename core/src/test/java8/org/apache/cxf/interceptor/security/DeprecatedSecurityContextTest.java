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

import javax.security.auth.Subject;

import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.interceptor.security.test.GroupWrapper;
import org.apache.cxf.security.LoginSecurityContext;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DeprecatedSecurityContextTest {
    @Test
    public void testPrivateStaticGroup() {
        Subject s = new Subject();
        Principal p = new SimplePrincipal("Barry");
        s.getPrincipals().add(p);
        //create a friend group and add Barry to this group
        GroupWrapper test = new GroupWrapper("friend", "Barry");
        s.getPrincipals().add(test.getGroup());
        LoginSecurityContext context = new DefaultSecurityContext(p, s);
        assertTrue(context.isUserInRole("Barry"));
    }
}