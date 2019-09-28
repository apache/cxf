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
import java.util.logging.Logger;

import javax.security.auth.Subject;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.security.SecurityToken;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.SecurityContext;

public abstract class AbstractSecurityContextInInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG =
        LogUtils.getL7dLogger(AbstractSecurityContextInInterceptor.class);

    public AbstractSecurityContextInInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    public void handleMessage(Message message) {
        SecurityToken token = message.get(SecurityToken.class);
        if (token == null) {
            reportSecurityException("Security Token is not available on the current message");
        }

        SecurityContext context = message.get(SecurityContext.class);
        if (context == null || context.getUserPrincipal() == null) {
            reportSecurityException("User Principal is not available on the current message");
        }

        Subject subject = null;
        try {
            subject = createSubject(token);
        } catch (Exception ex) {
            reportSecurityException("Failed Authentication : Subject has not been created, "
                                    + ex.getMessage());
        }
        if (subject == null || subject.getPrincipals().isEmpty()) {
            reportSecurityException("Failed Authentication : Invalid Subject");
        }

        Principal principal = getPrincipal(context.getUserPrincipal(), subject);
        SecurityContext sc = createSecurityContext(principal, subject);
        message.put(SecurityContext.class, sc);
    }

    protected Principal getPrincipal(Principal originalPrincipal, Subject subject) {
        Principal[] ps = subject.getPrincipals().toArray(new Principal[subject.getPrincipals().size()]);
        if (ps != null && ps.length > 0 
            && !DefaultSecurityContext.isGroupPrincipal(ps[0])) {
            return ps[0];
        }
        return originalPrincipal;
    }

    protected SecurityContext createSecurityContext(Principal p, Subject subject) {
        return new DefaultSecurityContext(p, subject);
    }

    protected abstract Subject createSubject(SecurityToken token);

    protected void reportSecurityException(String errorMessage) {
        LOG.severe(errorMessage);
        throw new SecurityException(errorMessage);
    }
}
