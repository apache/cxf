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

package org.apache.cxf.transport.http;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Functions used to work with URLs in HTTP-related code. 
 */
public final class UrlUtilities {
    
    private UrlUtilities() {
    }

    /**
     * Create a map from String to String that represents the contents of the query
     * portion of a URL. For each x=y, x is the key and y is the value.
     * @param s the query part of the URI.
     * @return the map.
     */
    public static Map<String, String> parseQueryString(String s) {
        Map<String, String> ht = new HashMap<String, String>();
        StringTokenizer st = new StringTokenizer(s, "&");
        while (st.hasMoreTokens()) {
            String pair = (String)st.nextToken();
            int pos = pair.indexOf('=');
            if (pos == -1) {
                ht.put(pair.toLowerCase(), "");
            } else {
                ht.put(pair.substring(0, pos).toLowerCase(),
                       pair.substring(pos + 1));
            }
        }
        return ht;
    }
    
    /**
     * Return everything in the path up to the last slash in a URI.
     * @param baseURI
     * @return the trailing 
     */
    public static String getStem(String baseURI) {
        URI uri = URI.create(baseURI);
        baseURI = uri.getRawPath();
        int idx = baseURI.lastIndexOf('/');
        if (idx != -1) {
            baseURI = baseURI.substring(0, idx);
        }
        return URI.create(baseURI).getPath();
    }
}
