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
package org.apache.cxf.interceptor.security.test;

import java.security.Principal;
import java.security.acl.Group;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.cxf.common.security.SimplePrincipal;

public class GroupWrapper {

    private Principal group;

    public GroupWrapper(String groupName, String userName) {
        SimpleGroup simpeG = new SimpleGroup(groupName);
        simpeG.addMember(new SimplePrincipal(userName));
        group = simpeG;

    }

    public Principal getGroup() {
        return this.group;
    }



    private static class SimpleGroup implements Group {
        private String name;
        private final Set<Principal> principals;

        SimpleGroup(String name) {
            this.name = name;
            this.principals = new HashSet<>();
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public boolean addMember(Principal principal) {
            return this.principals.add(principal);
        }

        @Override
        public boolean removeMember(Principal principal) {
            return this.principals.remove(principal);
        }

        @Override
        public Enumeration<? extends Principal> members() {
            return Collections.enumeration(this.principals);
        }

        @Override
        public boolean isMember(Principal principal) {
            return this.principals.contains(principal);
        }
    }

}
