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
package org.apache.cxf.systest.ws.wssec10.server;

import java.security.Principal;

import javax.security.auth.Subject;

import org.apache.cxf.common.security.SimpleGroup;
import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.common.security.UsernameToken;
import org.apache.cxf.interceptor.security.AbstractUsernameTokenInInterceptor;
import org.apache.cxf.security.SecurityContext;

public class SimpleUsernameTokenInterceptor extends AbstractUsernameTokenInInterceptor {

    protected Subject createSubject(UsernameToken ut) {
        return createSubject(ut.getName(), ut.getPassword(), ut.isHashed(),
                             ut.getNonce(), ut.getCreatedTime());
    }

    protected SecurityContext createSecurityContext(Principal p, Subject subject) {
        if (p == null || p != subject.getPrincipals().toArray()[0]) {
            throw new SecurityException();
        }
        return super.createSecurityContext(p, subject);
    }

    protected Subject createSubject(String name,
                                    String password,
                                    boolean isDigest,
                                    String nonce,
                                    String created) throws SecurityException {
        Subject subject = new Subject();

        // delegate to the external security system if possible

        // authenticate the user somehow
        subject.getPrincipals().add(new SimplePrincipal(name));

        // add roles this user is in
        String roleName = "Alice".equals(name) ? "developers" : "pms";
        subject.getPrincipals().add(new SimpleGroup(roleName, name));
        subject.setReadOnly();
        return subject;
    }

}


