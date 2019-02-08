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
package org.apache.cxf.rs.security.oauth2.utils;

import java.util.List;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.rs.security.oauth2.common.OAuthContext;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;

public final class OAuthContextUtils {
    private OAuthContextUtils() {
    }

    /**
     * @param mc the {@link MessageContext}
     * @return the id of the UserSubject of the logged in user or resource owner
     * @throws WebApplicationException with Status 401 if not authenticated
     */
    public static String resolveUserId(final MessageContext mc) {
        final OAuthContext oauth = getContext(mc);
        return oauth.getSubject().getId();
    }

    /**
     * @param mc the {@link MessageContext}
     * @return the name of the UserSubject of the logged in user or resource owner
     * @throws WebApplicationException with Status 401 if not authenticated
     */
    public static String resolveUserName(final MessageContext mc) {
        final OAuthContext oauth = getContext(mc);
        return oauth.getSubject().getLogin();
    }

    /**
     * @param mc the {@link MessageContext}
     * @return the list of roles of the logged in user or resource owner
     * @throws WebApplicationException with Status 401 if not authenticated
     */
    public static List<String> resolveUserRoles(final MessageContext mc) {
        final OAuthContext oauth = getContext(mc);
        return oauth.getSubject().getRoles();
    }

    /**
     * @param mc the {@link MessageContext}
     * @param role the user role to check
     * @return true if user has given role; false otherwise
     * @throws WebApplicationException with Status 401 if not authenticated
     */
    public static boolean isUserInRole(final MessageContext mc, final String role) {
        final List<String> userroles = resolveUserRoles(mc);
        return userroles.contains(role);
    }

    /**
     * @param mc the {@link MessageContext}
     * @param role the role to check
     * @throws WebApplicationException with Status 401 if not authenticated
     * @throws WebApplicationException with Status 403 if user doesn't have needed role
     */
    public static void assertRole(final MessageContext mc, final String role) {
        if (!isUserInRole(mc, role)) {
            throw ExceptionUtils.toForbiddenException(null, null);
        }
    }

    /**
     * @param mc the {@link MessageContext}
     * @return the list of permissions of the used access token
     * @throws WebApplicationException with Status 401 if not authenticated
     */
    public static List<OAuthPermission> resolvePermissions(final MessageContext mc) {
        final OAuthContext oauth = getContext(mc);
        return oauth.getPermissions();
    }

    /**
     * @param mc the {@link MessageContext}
     * @return the token key used to access
     * @throws WebApplicationException with Status 401 if not authenticated
     */
    public static String resolveTokenKey(MessageContext mc) {
        OAuthContext oauth = getContext(mc);
        return oauth.getTokenKey();
    }

    /**
     * @param mc the {@link MessageContext}
     * @return the client registration id
     * @throws WebApplicationException with Status 401 if not authenticated
     */
    public static String resolveClient(MessageContext mc) {
        OAuthContext oauth = getContext(mc);
        return oauth.getClientId();
    }

    /**
     * @param mc the {@link MessageContext}
     * @param client the desired client registration id
     * @throws WebApplicationException with Status 403 if the current client id is not valid
     */
    public static void assertClient(MessageContext mc, String client) {
        String cl = resolveClient(mc);
        if ((cl == null) || !cl.equals(client)) {
            throw ExceptionUtils.toForbiddenException(null, null);
        }
    }

    /**
     * @param mc the {@link MessageContext}
     * @return the {@link OAuthContext} of the given {@link MessageContext}
     * @throws WebApplicationException with Status 401 if not authenticated
     */
    public static OAuthContext getContext(final MessageContext mc) {
        final OAuthContext oauth = mc.getContent(OAuthContext.class);
        if ((oauth == null) || (oauth.getSubject() == null) || (oauth.getSubject().getLogin() == null)) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
        return oauth;
    }

}

