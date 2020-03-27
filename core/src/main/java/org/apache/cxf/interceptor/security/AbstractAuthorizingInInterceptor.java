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
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.SecurityContext;

public abstract class AbstractAuthorizingInInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractAuthorizingInInterceptor.class);
    private static final String ALL_ROLES = "*";
    private boolean allowAnonymousUsers = true;

    public AbstractAuthorizingInInterceptor() {
        this(true);
    }
    public AbstractAuthorizingInInterceptor(boolean uniqueId) {
        super(null, Phase.PRE_INVOKE, uniqueId);
    }
    public void handleMessage(Message message) {
        Method method = MessageUtils.getTargetMethod(message).orElseThrow(() -> 
            new AccessDeniedException("Method is not available : Unauthorized"));
        SecurityContext sc = message.get(SecurityContext.class);
        if (sc != null && sc.getUserPrincipal() != null) {
            if (authorize(sc, method)) {
                return;
            }
        } else if (!isMethodProtected(method) && isAllowAnonymousUsers()) {
            return;
        }


        throw new AccessDeniedException("Unauthorized");
    }

    protected boolean authorize(SecurityContext sc, Method method) {
        List<String> expectedRoles = getExpectedRoles(method);
        if (expectedRoles.isEmpty()) {

            List<String> denyRoles = getDenyRoles(method);

            return denyRoles.isEmpty() || isUserInRole(sc, denyRoles, true);
        }

        if (isUserInRole(sc, expectedRoles, false)) {
            return true;
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(sc.getUserPrincipal().getName() + " is not authorized");
        }
        return false;
    }
    protected boolean isMethodProtected(Method method) {
        return !getExpectedRoles(method).isEmpty() || !getDenyRoles(method).isEmpty();
    }

    protected boolean isUserInRole(SecurityContext sc, List<String> roles, boolean deny) {

        if (roles.size() == 1 && ALL_ROLES.equals(roles.get(0))) {
            return !deny;
        }

        for (String role : roles) {
            if (sc.isUserInRole(role)) {
                return !deny;
            }
        }
        return deny;
    }

    /**
     * Returns a list of expected roles for a given method.
     * @param method Method
     * @return list, empty if no roles are available
     */
    protected abstract List<String> getExpectedRoles(Method method);


    /**
     * Returns a list of roles to be denied for a given method.
     * @param method Method
     * @return list, empty if no roles are available
     */
    protected List<String> getDenyRoles(Method method) {
        return Collections.emptyList();
    }

    public boolean isAllowAnonymousUsers() {
        return allowAnonymousUsers;
    }

    public void setAllowAnonymousUsers(boolean allowAnonymousUsers) {
        this.allowAnonymousUsers = allowAnonymousUsers;
    }

}
