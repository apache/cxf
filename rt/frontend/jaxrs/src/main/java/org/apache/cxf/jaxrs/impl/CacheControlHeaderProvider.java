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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

public class CacheControlHeaderProvider implements HeaderDelegate<CacheControl> {
    
    private static final String PUBLIC = "public";
    private static final String PRIVATE = "private";
    private static final String NO_CACHE = "no-cache";
    private static final String NO_STORE = "no-store";
    private static final String NO_TRANSFORM = "no-transform";
    private static final String MUST_REVALIDATE = "must-revalidate";
    private static final String PROXY_REVALIDATE = "proxy-revalidate";
    private static final String MAX_AGE = "max-age";
    private static final String SMAX_AGE = "s-maxage";

    public CacheControl fromString(String c) {
        boolean isPublic = true;
        boolean isPrivate = false;
        List<String> privateFields = new ArrayList<String>();
        boolean noCache = false;
        List<String> noCacheFields = new ArrayList<String>();
        boolean noStore = false;
        boolean noTransform = false;
        boolean mustRevalidate = false;
        boolean proxyRevalidate = false;
        int maxAge = -1;
        int sMaxAge = -1;
       
        
        String[] tokens = c.split(";");
        for (String token : tokens) {
            if (token.startsWith(MAX_AGE)) {
                maxAge = Integer.parseInt(token.substring(MAX_AGE.length() + 1));
            } else if (token.startsWith(SMAX_AGE)) {
                sMaxAge = Integer.parseInt(token.substring(SMAX_AGE.length() + 1));
            } else if (token.startsWith(PUBLIC)) {
                // ignore
            } else if (token.startsWith(NO_STORE)) {
                noStore = true;
            } else if (token.startsWith(NO_TRANSFORM)) {
                noTransform = true;
            } else if (token.startsWith(MUST_REVALIDATE)) {
                mustRevalidate = true;
            } else if (token.startsWith(PROXY_REVALIDATE)) {
                proxyRevalidate = true;
            } else if (token.startsWith(PRIVATE)) {
                isPublic = false;
                isPrivate = true;
                addFields(privateFields, token);
            }  else if (token.startsWith(NO_CACHE)) {
                noCache = true;
                addFields(noCacheFields, token);
            }
        }
        
        CacheControl cc = new CacheControl();
        cc.setMaxAge(maxAge);
        cc.setSMaxAge(sMaxAge);
        cc.setPublic(isPublic);
        cc.setPrivate(isPrivate);
        cc.getPrivateFields().addAll(privateFields);
        cc.setMustRevalidate(mustRevalidate);
        cc.setProxyRevalidate(proxyRevalidate);
        cc.setNoCache(noCache);
        cc.getNoCacheFields().addAll(noCacheFields);
        cc.setNoStore(noStore);
        cc.setNoTransform(noTransform);
        
        return cc;
    }

    public String toString(CacheControl c) {
        StringBuilder sb = new StringBuilder();
        if (c.isPrivate()) {
            sb.append(PRIVATE);
            handleFields(c.getPrivateFields(), sb);
        }
        if (c.isNoCache()) {
            sb.append(NO_CACHE);
            handleFields(c.getNoCacheFields(), sb);
        }
        if (c.isNoStore()) {
            sb.append(NO_STORE).append(';');
        }
        if (c.isNoTransform()) {
            sb.append(NO_TRANSFORM).append(';');
        }
        if (c.isMustRevalidate()) {
            sb.append(MUST_REVALIDATE).append(';');
        }
        if (c.isProxyRevalidate()) {
            sb.append(PROXY_REVALIDATE).append(';');
        }
        if (c.getMaxAge() != -1) {
            sb.append(MAX_AGE).append('=').append(c.getMaxAge()).append(';');
        }
        if (c.getSMaxAge() != -1) {
            sb.append(SMAX_AGE).append('=').append(c.getSMaxAge()).append(';');
        }
        Map<String, String> exts = c.getCacheExtension();
        for (Map.Entry<String, String> entry : exts.entrySet()) {
            sb.append(entry.getKey());
            String v = entry.getValue();
            if (v != null) {
                sb.append("=");
                if (v.indexOf(' ') != -1) {
                    sb.append('\"').append(v).append('\"');
                } else {
                    sb.append(v);
                }
            }
            sb.append(';');
        }
        String s = sb.toString();
        return s.endsWith(";") ? s.substring(0, s.length() - 1) : s; 
    }
    
    private static void addFields(List<String> fields, String token) {
        String f = null;
        int i = token.indexOf('=');
        if (i != -1) {
            f = i == token.length()  + 1 ? "" : token.substring(i + 1);
            if (f.length() < 2 || !f.startsWith("\"") || !f.endsWith("\"")) {
                f = "";
            } else {
                f = f.length() == 2 ? "" : f.substring(1, f.length() - 1);
            }
        }
        if (f != null) {
            fields.add(f);
        }
    }

    private static void handleFields(List<String> fields, StringBuilder sb) {
        if (!fields.isEmpty()) {
            sb.append('=');
        }
        for (Iterator<String> it = fields.iterator(); it.hasNext();) {
            sb.append('\"').append(it.next()).append('\"');
            if (it.hasNext()) {
                sb.append(',');
            }
        }
        sb.append(';');
    }
}
