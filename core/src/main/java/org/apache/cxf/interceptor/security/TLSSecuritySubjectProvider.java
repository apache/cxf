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
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.cxf.common.security.SimpleGroup;
import org.apache.cxf.common.security.SimplePrincipal;

public abstract class TLSSecuritySubjectProvider {

    /**
     * Returns created Subject for specified user/certificate
     * 
     * @param userName
     * @param certificate
     * @return
     */
    public abstract Subject getSubject(String userName, X509Certificate certificate) throws SecurityException;

    /**
     * Maps user name and list of roles to Security Subject
     * 
     * @param userName
     * @param roles
     * @return
     */
    public Subject createSubject(String userName, List<String> roles) {
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(new SimplePrincipal(userName));

        if (roles != null) {
            for (String role : roles) {
                principals.add(new SimpleGroup(role));
            }
        }

        Subject subject = new Subject();
        subject.getPrincipals().addAll(principals);
        subject.setReadOnly();

        return subject;
    }
}
