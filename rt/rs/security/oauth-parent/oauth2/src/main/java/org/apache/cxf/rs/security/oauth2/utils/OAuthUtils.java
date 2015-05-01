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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.common.AuthenticationMethod;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.security.LoginSecurityContext;
import org.apache.cxf.security.SecurityContext;

/**
 * Various utility methods 
 */
public final class OAuthUtils {

    private OAuthUtils() {
    }
    
    public static String setDefaultSessionToken(MessageContext mc) {
        return setDefaultSessionToken(mc, 0);
    }
    public static String setDefaultSessionToken(MessageContext mc, int maxInactiveInterval) {
        return setDefaultSessionToken(mc, generateRandomTokenKey());
    }
    public static String setDefaultSessionToken(MessageContext mc, String sessionToken) {
        return setDefaultSessionToken(mc, sessionToken, 0);
    }
    public static String setDefaultSessionToken(MessageContext mc, String sessionToken, int maxInactiveInterval) {
        HttpSession session = mc.getHttpServletRequest().getSession();
        if (maxInactiveInterval > 0) {
            session.setMaxInactiveInterval(maxInactiveInterval);
        }
        session.setAttribute(OAuthConstants.SESSION_AUTHENTICITY_TOKEN, sessionToken);
        return sessionToken;
    }
    public static String getDefaultSessionToken(MessageContext mc) {
        HttpSession session = mc.getHttpServletRequest().getSession();
        String sessionToken = (String)session.getAttribute(OAuthConstants.SESSION_AUTHENTICITY_TOKEN);
        if (sessionToken != null) {
            session.removeAttribute(OAuthConstants.SESSION_AUTHENTICITY_TOKEN);    
        }
        return sessionToken;
    }
    
    public static UserSubject createSubject(SecurityContext securityContext) {
        List<String> roleNames = Collections.emptyList();
        if (securityContext instanceof LoginSecurityContext) {
            roleNames = new ArrayList<String>();
            Set<Principal> roles = ((LoginSecurityContext)securityContext).getUserRoles();
            for (Principal p : roles) {
                roleNames.add(p.getName());
            }
        }
        UserSubject subject = new UserSubject(securityContext.getUserPrincipal().getName(), roleNames);
        Message m = JAXRSUtils.getCurrentMessage();
        if (m != null && m.get(AuthenticationMethod.class) != null) {
            subject.setAthenticationMethod(m.get(AuthenticationMethod.class));
        }
        return subject;
    }
    
    public static String convertPermissionsToScope(List<OAuthPermission> perms) {
        StringBuilder sb = new StringBuilder();
        for (OAuthPermission perm : perms) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(perm.getPermission());
        }
        return sb.toString();
    }
    
    public static List<String> convertPermissionsToScopeList(List<OAuthPermission> perms) {
        List<String> list = new LinkedList<String>();
        for (OAuthPermission perm : perms) {
            list.add(perm.getPermission());
        }
        return list;
    }
    
    public static boolean isGrantSupportedForClient(Client client, 
                                                    boolean canSupportPublicClients, 
                                                    String grantType) {
        if (!client.isConfidential() && !canSupportPublicClients) {
            return false;
        }
        List<String> allowedGrants = client.getAllowedGrantTypes();
        return allowedGrants.isEmpty() || allowedGrants.contains(grantType);
    }
    
    public static List<String> parseScope(String requestedScope) {
        List<String> list = new LinkedList<String>();
        if (requestedScope != null) {
            String[] scopeValues = requestedScope.split(" ");
            for (String scope : scopeValues) {
                if (!StringUtils.isEmpty(scope)) {        
                    list.add(scope);
                }
            }
        }
        return list;
    }

    public static String generateRandomTokenKey() throws OAuthServiceException {
        return generateRandomTokenKey(16);
    }
    public static String generateRandomTokenKey(int byteSize) {
        if (byteSize < 16) {
            throw new OAuthServiceException();
        }
        return StringUtils.toHexString(CryptoUtils.generateSecureRandomBytes(byteSize));
    }
    
    public static long getIssuedAt() {
        return System.currentTimeMillis() / 1000;
    }
    
    public static boolean isExpired(Long issuedAt, Long lifetime) {
        return lifetime != -1
            && issuedAt + lifetime < System.currentTimeMillis() / 1000;
    }
    
    public static boolean validateAudience(String audience, List<String> audiences) {
        return audience == null || !audiences.isEmpty() && audiences.contains(audience);
    }
    
    public static boolean checkRequestURI(String servletPath, String uri) {
        boolean wildcard = uri.endsWith("*");
        String theURI = wildcard ? uri.substring(0, uri.length() - 1) : uri;
        try {
            URITemplate template = new URITemplate(theURI);
            MultivaluedMap<String, String> map = new MetadataMap<String, String>();
            if (template.match(servletPath, map)) {
                String finalGroup = map.getFirst(URITemplate.FINAL_MATCH_GROUP);
                if (wildcard || StringUtils.isEmpty(finalGroup) || "/".equals(finalGroup)) {
                    return true;
                }
            }
        } catch (Exception ex) {
            // ignore
        }
        return false;
    }
    
    public static List<String> getRequestedScopes(Client client, String scopeParameter, 
                                                  boolean partialMatchScopeValidation) {
        List<String> requestScopes = parseScope(scopeParameter);
        List<String> registeredScopes = client.getRegisteredScopes();
        if (requestScopes.isEmpty()) {
            requestScopes.addAll(registeredScopes);
            return requestScopes;
        }
        if (!validateScopes(requestScopes, registeredScopes, partialMatchScopeValidation)) {
            throw new OAuthServiceException("Unexpected scope");
        }
        return requestScopes;
    }
    
    public static boolean validateScopes(List<String> requestScopes, List<String> registeredScopes,
                                         boolean partialMatchScopeValidation) {
        if (!registeredScopes.isEmpty()) {
            // if it is a strict validation then pre-registered scopes have to contains all 
            // the current request scopes
            if (!partialMatchScopeValidation) {
                return registeredScopes.containsAll(requestScopes);
            } else {
                for (String requestScope : requestScopes) {
                    boolean match = false;
                    for (String registeredScope : registeredScopes) { 
                        if (requestScope.startsWith(registeredScope)) {
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static ClientAccessToken toClientAccessToken(ServerAccessToken serverToken, boolean supportOptionalParams) {
        ClientAccessToken clientToken = new ClientAccessToken(serverToken.getTokenType(),
                                                              serverToken.getTokenKey());
        clientToken.setRefreshToken(serverToken.getRefreshToken());
        if (supportOptionalParams) {
            clientToken.setExpiresIn(serverToken.getExpiresIn());
            List<OAuthPermission> perms = serverToken.getScopes();
            if (!perms.isEmpty()) {
                clientToken.setApprovedScope(OAuthUtils.convertPermissionsToScope(perms));    
            }
            clientToken.setParameters(serverToken.getParameters());
        }
        return clientToken;
    }

    
}
