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

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;

/**
 * Various utility methods 
 */
public final class OAuthUtils {

    private OAuthUtils() {
    }

    public static boolean isGrantSupportedForClient(Client client, 
                                                    boolean isConfidential, 
                                                    String grantType) {
        List<String> allowedGrants = client.getAllowedGrantTypes();
        return isConfidential == client.isConfidential()
            && (allowedGrants.isEmpty() || allowedGrants.contains(grantType));
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
        try {
            byte[] bytes = UUID.randomUUID().toString().getBytes("UTF-8");
            return new MD5SequenceGenerator().generate(bytes);
        } catch (Exception ex) {
            throw new OAuthServiceException(OAuthConstants.SERVER_ERROR, ex);
        }
    }
    
    public static boolean isExpired(Long issuedAt, Long lifetime) {
        return lifetime != -1
            && issuedAt + lifetime < System.currentTimeMillis() / 1000;
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
    
}
