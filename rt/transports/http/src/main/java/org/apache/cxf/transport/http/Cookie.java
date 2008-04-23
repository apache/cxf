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

import java.util.List;
import java.util.Map;

/**
 * Container for HTTP cookies used to track
 * session state.
 *
 */
class Cookie {
    public static final String DISCARD_ATTRIBUTE = "discard";
    public static final String MAX_AGE_ATTRIBUTE = "max-age";
    public static final String PATH_ATTRIBUTE = "path";
    
    /**
     * The name of this cookie
     */
    private String name;
    
    /**
     * The value of this cookie
     */
    private String value;
    
    /**
     * The path on the server where this cookie is valid.
     * Used to distinguish between identical cookies from different contexts.
     */
    private String path;
    
    /**
     * The maximum age of the cookie
     */
    private int maxAge = -1;

    /**
     * Create a new cookie with the supplied name/value pair
     * @param name
     * @param value
     */
    public Cookie(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Get the name of this cookie
     * @return cookie name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Change the value of this cookie
     * @param value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Get the value of this cookie
     * @return cookie value
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Set the path of this cookie
     * @param path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Get the path of this cookie
     * @return cookie path
     */
    public String getPath() {
        return this.path;
    }

    /**
     * Set the max-age of this cookie. If set to 0, it
     * should be removed from the session.
     * @param maxAge
     */
    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    /**
     * Get the max-age of this cookie
     * @return
     */
    public int getMaxAge() {
        return this.maxAge;
    }

    /**
     * 
     */
    @Override
    public int hashCode() {
        return (17 * this.name.hashCode())
            + ((this.path != null) ? 11 * this.path.hashCode() : 0);
    }

    /**
     * 
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Cookie)) {
            return false;
        }
        Cookie c = (Cookie)o;
        boolean result = this.name.equals(c.name)
                && ((this.path == null && c.path == null)
                        || (this.path != null && this.path.equals(c.path)));
        return result;
    }

    /**
     * Convert a list of cookies into a string suitable for sending
     * as a "Cookie:" header
     * @return Cookie header text
     */
    public String requestCookieHeader() {
        StringBuilder b = new StringBuilder();
        b.append("$Version=\"1\"");
        b.append("; ").append(getName())
            .append("=").append(getValue());
        if (getPath() != null && getPath().length() > 0) {
            b.append("; $Path=").append(getPath());
        }
        return b.toString();
    }
    
    /**
     * Given a list of current cookies and a new Set-Cookie: request, construct
     * a new set of current cookies and return it.
     * @param current Set of previously set cookies
     * @param header Text of a Set-Cookie: header
     * @return New set of cookies
     */
    public static void handleSetCookie(Map<String, Cookie> current, List<String> headers) {
        if (headers == null || headers.size() == 0) {
            return;
        }
        

        for (String header : headers) {
            String[] cookies = header.split(",");
            for (String cookie : cookies) {
                String[] parts = cookie.split(";");
    
                String[] kv = parts[0].split("=", 2);
                if (kv.length != 2) {
                    continue;
                }
                String name = kv[0].trim();
                String value = kv[1].trim();
                Cookie newCookie = new Cookie(name, value);
    
                for (int i = 1; i < parts.length; i++) {
                    kv = parts[i].split("=", 2);
                    name = kv[0].trim();
                    value = (kv.length > 1) ? kv[1].trim() : null;
                    if (name.equalsIgnoreCase(DISCARD_ATTRIBUTE)) {
                        newCookie.setMaxAge(0);
                    } else if (name.equalsIgnoreCase(MAX_AGE_ATTRIBUTE) && value != null) {
                        try {
                            newCookie.setMaxAge(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            // do nothing here
                        }
                    } else if (name.equalsIgnoreCase(PATH_ATTRIBUTE) && value != null) {
                        newCookie.setPath(value);
                    }
                }
                if (newCookie.getMaxAge() != 0) {
                    current.put(newCookie.getName(), newCookie);                    
                } else {
                    current.remove(newCookie.getName());
                }
            }
        }
    }
}
