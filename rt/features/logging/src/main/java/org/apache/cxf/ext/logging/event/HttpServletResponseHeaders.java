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
package org.apache.cxf.ext.logging.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;

/**
 * Reads the headers that were added directly to the HttpServletResponse (by the application or by
 * a servlet filter) rather than to the protocol headers of the message. Kept separate from
 * DefaultLogEventMapper so that the Servlet API stays an optional dependency.
 */
final class HttpServletResponseHeaders {
    /** See AbstractHTTPDestination.HTTP_RESPONSE */
    private static final String HTTP_RESPONSE = "HTTP.RESPONSE";

    private HttpServletResponseHeaders() {
    }

    /**
     * Merges servlet headers with the protocol headers, accounting for transport header copying.
     */
    static void addHeaders(Message message, Map<String, String> headers) {
        Object response = message.get(HTTP_RESPONSE);
        if (!(response instanceof HttpServletResponse)) {
            return;
        }
        Map<String, String> merged = new HashMap<>(headers);
        try {
            addHeaders(message, (HttpServletResponse)response, merged);
        } catch (RuntimeException ex) {
            // Servlet wrappers or recycled responses may not expose headers. Keep the protocol
            // headers unchanged so logging does not prevent the response from completing.
            return;
        }
        headers.putAll(merged);
    }

    private static void addHeaders(Message message, HttpServletResponse servletResponse,
                                   Map<String, String> headers) {
        Map<String, String> names = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.keySet().forEach(name -> {
            if (name != null) {
                names.put(name, name);
            }
        });
        // See AbstractHTTPDestination.RESPONSE_HEADERS_COPIED and Headers.ADD_HEADERS_PROPERTY.
        boolean copied = PropertyUtils.isTrue(message.get("http.headers.copied"));
        boolean append = MessageUtils.getContextualBoolean(message, "org.apache.cxf.http.add-headers", false);
        Map<?, ?> protocolHeaders = (Map<?, ?>)message.get(Message.PROTOCOL_HEADERS);
        if (protocolHeaders != null) {
            for (Map.Entry<?, ?> entry : protocolHeaders.entrySet()) {
                if (entry.getKey() != null && entry.getValue() instanceof Collection<?>) {
                    Collection<?> values = (Collection<?>)entry.getValue();
                    if (values.size() > 1) {
                        String name = (String)entry.getKey();
                        headers.put(name, formatValues(name, values, append));
                    }
                }
            }
        }
        Collection<String> headerNames = servletResponse.getHeaderNames();
        if (headerNames != null) {
            for (String name : headerNames) {
                if (name == null) {
                    continue;
                }
                String key = names.getOrDefault(name, name);
                Collection<?> values = servletResponse.getHeaders(name);
                if (values == null) {
                    continue;
                }
                if (!copied && headers.containsKey(key)) {
                    // Before copying, ordinary protocol headers will replace servlet values. Cookies,
                    // Link, and headers configured with add-headers will instead be appended.
                    if (!isAppendHeader(name, append)) {
                        continue;
                    }
                    List<Object> combined = new ArrayList<>(values);
                    if (protocolHeaders != null && protocolHeaders.get(key) instanceof Collection<?>) {
                        Collection<?> protocolValues = (Collection<?>)protocolHeaders.get(key);
                        for (Object value : protocolValues) {
                            if (value != null) {
                                combined.add(value);
                            }
                        }
                    }
                    values = combined;
                }
                // After copying, the servlet response already contains the merged values.
                headers.put(key, formatValues(name, values, append));
            }
        }
    }

    private static boolean isAppendHeader(String name, boolean append) {
        return append || "Set-Cookie".equalsIgnoreCase(name) || "Link".equalsIgnoreCase(name);
    }

    private static String formatValues(String name, Collection<?> values, boolean append) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        if (!isAppendHeader(name, append)) {
            // copyToResponse combines ordinary values into one comma-separated servlet header.
            // Use that representation before copying too, without splitting existing comma values.
            return values.stream().map(value -> value == null ? "" : value.toString())
                .collect(Collectors.joining(","));
        }
        if (values.size() == 1) {
            Object first = values.iterator().next();
            return first == null ? "" : first.toString();
        }
        return Arrays.deepToString(values.toArray());
    }
}
