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
package org.apache.cxf.rs.security.oauth.utils;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.URITemplate;

/**
 * Various utility methods 
 */
public final class OAuthUtils {

    private OAuthUtils() {
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
