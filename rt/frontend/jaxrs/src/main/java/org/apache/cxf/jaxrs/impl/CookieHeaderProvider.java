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
package org.apache.cxf.jaxrs.impl;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

public class CookieHeaderProvider implements HeaderDelegate<Cookie> {

    private static final String VERSION = "$Version";
    private static final String PATH = "$Path";
    private static final String DOMAIN = "$Domain";
    
    public Cookie fromString(String c) {
        
        int version = -1;
        String name = null;
        String value = null;
        String path = null;
        String domain = null;
        
        // ignore the fact the possible version may be seperated by ','
        String[] tokens = c.split(";");
        for (String token : tokens) {
            if (token.startsWith(VERSION)) {
                version = Integer.parseInt(token.substring(VERSION.length() + 1));
            } else if (token.startsWith(PATH)) {
                path = token.substring(PATH.length() + 1);
            } else if (token.startsWith(DOMAIN)) {
                domain = token.substring(DOMAIN.length() + 1);
            } else {
                int i = token.indexOf('=');
                if (i != -1) {
                    name = token.substring(0, i);
                    value = i == token.length()  + 1 ? "" : token.substring(i + 1);
                }
            }
        }
        
        if (name == null || value == null) {
            throw new IllegalArgumentException("Cookie is malformed : " + c);
        }
        
        return new Cookie(name, value, path, domain, version);
    }

    
    public String toString(Cookie c) {
        StringBuilder sb = new StringBuilder();
        
        if (c.getVersion() != -1) {
            sb.append(VERSION).append('=').append(c.getVersion()).append(';');
        }
        sb.append(c.getName()).append('=').append(c.getValue());
        if (c.getPath() != null) {
            sb.append(';').append(PATH).append('=').append(c.getPath());
        }
        if (c.getDomain() != null) {
            sb.append(';').append(DOMAIN).append('=').append(c.getDomain());
        }
        return sb.toString();
    }

}
