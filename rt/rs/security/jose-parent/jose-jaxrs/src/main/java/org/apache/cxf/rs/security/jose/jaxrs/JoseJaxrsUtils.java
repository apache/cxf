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
package org.apache.cxf.rs.security.jose.jaxrs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.rs.security.jose.common.JoseHeaders;

public final class JoseJaxrsUtils {
    private static final String HTTP_PREFIX = "http.";
    private static final Set<String> DEFAULT_PROTECTED_HTTP_HEADERS =
        new HashSet<>(Arrays.asList(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT));

    private JoseJaxrsUtils() {

    }

    public static void protectHttpHeaders(MultivaluedMap<String, Object> httpHeaders,
                                          JoseHeaders joseHeaders,
                                          Set<String> protectedHttpHeaders) {
        if (protectedHttpHeaders == null) {
            protectedHttpHeaders = DEFAULT_PROTECTED_HTTP_HEADERS;
        }
        for (String headerName : protectedHttpHeaders) {
            List<Object> headerValues = httpHeaders.get(headerName);
            if (headerValues != null) {
                String joseHeaderValue = getJoseHeaderValue(headerValues);
                String prefixedHeaderName = HTTP_PREFIX + headerName;
                joseHeaders.setHeader(prefixedHeaderName, joseHeaderValue);
            }
        }
    }
    private static String getJoseHeaderValue(List<? extends Object> headerValues) {
        StringBuilder sb = new StringBuilder();
        for (Object o : headerValues) {
            String[] parts = o.toString().split(",");
            for (String part : parts) {
                sb.append(part);
            }
        }
        return sb.toString();
    }

    public static void validateHttpHeaders(MultivaluedMap<String, String> httpHeaders,
                                           JoseHeaders joseHeaders,
                                           Set<String> protectedHttpHeaders) {
        if (protectedHttpHeaders == null) {
            protectedHttpHeaders = DEFAULT_PROTECTED_HTTP_HEADERS;
        }
        Map<String, String> joseHttpHeaders = new HashMap<>();
        Map<String, String> updatedHttpHeaders = new HashMap<>();
        for (String headerName : protectedHttpHeaders) {
            List<String> headerValues = httpHeaders.get(headerName);
            if (headerValues != null && !headerValues.isEmpty() && headerValues.get(0) != null) {
                String headerValue = getJoseHeaderValue(headerValues);
                String prefixedHeaderName = HTTP_PREFIX + headerName;
                updatedHttpHeaders.put(prefixedHeaderName, headerValue);
                String joseHeaderValue = joseHeaders.getStringProperty(prefixedHeaderName);
                if (joseHeaderValue != null) {
                    joseHttpHeaders.put(prefixedHeaderName, joseHeaderValue);
                }
            }

        }
        if (joseHttpHeaders.size() != updatedHttpHeaders.size()
            || !joseHttpHeaders.entrySet().containsAll(updatedHttpHeaders.entrySet())) {
            throw ExceptionUtils.toBadRequestException(null, null);
        }
    }
}
