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

import java.util.Date;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.ext.RuntimeDelegate.HeaderDelegate;
import org.apache.cxf.jaxrs.utils.HttpUtils;

public class NewCookieHeaderProvider implements HeaderDelegate<NewCookie> {

    private static final String VERSION = "Version";
    private static final String PATH = "Path";
    private static final String DOMAIN = "Domain";
    private static final String MAX_AGE = "Max-Age";
    private static final String COMMENT = "Comment";
    private static final String SECURE = "Secure";
    private static final String EXPIRES = "Expires";
    private static final String HTTP_ONLY = "HttpOnly";

    /** from RFC 2068, token special case characters */

    private static final String TSPECIALS_PATH = "\"()<>@,;:\\[]?={} \t";
    private static final String TSPECIALS_ALL = TSPECIALS_PATH + "/";
    private static final String DOUBLE_QUOTE = "\"";

    public NewCookie fromString(String c) {

        if (c == null) {
            throw new IllegalArgumentException("SetCookie value can not be null");
        }

        String name = null;
        String value = null;
        String path = null;
        String domain = null;
        String comment = null;
        int maxAge = NewCookie.DEFAULT_MAX_AGE;
        boolean isSecure = false;
        Date expires = null;
        boolean httpOnly = false;
        int version = Cookie.DEFAULT_VERSION;

        String[] tokens = c.split(";");
        for (String token : tokens) {
            String theToken = token.trim();

            int sepIndex = theToken.indexOf('=');
            String paramName = sepIndex != -1 ? theToken.substring(0, sepIndex) : theToken;
            String paramValue = null;

            if (sepIndex == theToken.length() - 1) {
                paramValue = "";
            } else if (sepIndex != -1) {
                paramValue = theToken.substring(sepIndex + 1);
            }

            if (paramValue != null) {
                paramValue = stripQuotes(paramValue);
            }

            if (paramName.equalsIgnoreCase(MAX_AGE)) {
                maxAge = Integer.parseInt(paramValue);
            } else if (paramName.equalsIgnoreCase(PATH)) {
                path = paramValue;
            } else if (paramName.equalsIgnoreCase(DOMAIN)) {
                domain = paramValue;
            } else if (paramName.equalsIgnoreCase(COMMENT)) {
                comment = paramValue;
            } else if (paramName.equalsIgnoreCase(SECURE)) {
                isSecure = true;
            } else if (paramName.equalsIgnoreCase(EXPIRES)) {
                expires = HttpUtils.getHttpDate(paramValue);
            } else if (paramName.equalsIgnoreCase(HTTP_ONLY)) {
                httpOnly = true;
            } else if (paramName.equalsIgnoreCase(VERSION)) {
                version = Integer.parseInt(paramValue);
            } else if (paramValue != null) {
                name = paramName;
                value = paramValue;
            }
        }

        if (name == null || value == null) {
            throw new IllegalArgumentException("Set-Cookie is malformed : " + c);
        }

        return new NewCookie(name, value, path, domain, version, comment, maxAge, expires, isSecure, httpOnly);
    }

    @Override
    public String toString(NewCookie value) {

        if (null == value) {
            throw new NullPointerException("Null cookie input");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(value.getName()).append('=').append(maybeQuoteAll(value.getValue()));
        if (value.getComment() != null) {
            sb.append(';').append(COMMENT).append('=').append(maybeQuoteAll(value.getComment()));
        }
        if (value.getDomain() != null) {
            sb.append(';').append(DOMAIN).append('=').append(maybeQuoteAll(value.getDomain()));
        }
        if (value.getMaxAge() != -1) {
            sb.append(';').append(MAX_AGE).append('=').append(value.getMaxAge());
        }
        if (value.getPath() != null) {
            sb.append(';').append(PATH).append('=').append(maybeQuotePath(value.getPath()));
        }
        if (value.getExpiry() != null) {
            sb.append(';').append(EXPIRES).append('=').append(HttpUtils.toHttpDate(value.getExpiry()));
        }
        if (value.isSecure()) {
            sb.append(';').append(SECURE);
        }
        if (value.isHttpOnly()) {
            sb.append(';').append(HTTP_ONLY);
        }
        sb.append(';').append(VERSION).append('=').append(value.getVersion());
        return sb.toString();

    }

    /**
     * Append the input value string to the given buffer, wrapping it with
     * quotes if need be.
     *
     * @param value
     * @return String
     */
    static String maybeQuote(String tSpecials, String value) {
        if (needsQuote(tSpecials, value)) {
            StringBuilder buff = new StringBuilder();
            buff.append('"');
            if (value != null) {
                buff.append(value);
            }
            buff.append('"');
            return buff.toString();
        }
        return value;
    }
    static String maybeQuoteAll(String value) {
        return maybeQuote(TSPECIALS_ALL, value);
    }
    static String maybeQuotePath(String value) {
        return maybeQuote(TSPECIALS_PATH, value);
    }

    /**
     * Return true if the string contains special characters that need to be
     * quoted.
     *
     * @param value
     * @return boolean
     */
    static boolean needsQuote(String tSpecials, String value) {
        if (null == value) {
            return true;
        }
        int len = value.length();
        if (0 == len) {
            return true;
        }
        if ('"' == value.charAt(0) && '"' == value.charAt(len - 1)) {
            // already wrapped with quotes
            return false;
        }

        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c >= 0x7f || tSpecials.indexOf(c) != -1) {
                return true;
            }
        }
        return false;
    }

    static String stripQuotes(String paramValue) {
        if (paramValue.startsWith(DOUBLE_QUOTE)
            && paramValue.endsWith(DOUBLE_QUOTE) && paramValue.length() > 1) {
            paramValue = paramValue.substring(1, paramValue.length() - 1);
        }
        return paramValue;
    }
}
