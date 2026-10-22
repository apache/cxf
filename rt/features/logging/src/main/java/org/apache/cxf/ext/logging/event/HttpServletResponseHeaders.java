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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
        HttpServletResponse servletResponse = (HttpServletResponse)response;
        Map<String, String> names = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.keySet().forEach(name -> names.put(name, name));
        // See AbstractHTTPDestination.RESPONSE_HEADERS_COPIED and Headers.ADD_HEADERS_PROPERTY.
        boolean copied = PropertyUtils.isTrue(message.get("http.headers.copied"));
        boolean append = MessageUtils.getContextualBoolean(message, "org.apache.cxf.http.add-headers", false);
        Map<?, ?> protocolHeaders = (Map<?, ?>)message.get(Message.PROTOCOL_HEADERS);
        for (String name : servletResponse.getHeaderNames()) {
            String key = names.getOrDefault(name, name);
            Collection<?> values = servletResponse.getHeaders(name);
            if (!copied && headers.containsKey(key)) {
                // Before copying, ordinary protocol headers will replace servlet values. Cookies,
                // Link, and headers configured with add-headers will instead be appended.
                if (!append && !"Set-Cookie".equalsIgnoreCase(name) && !"Link".equalsIgnoreCase(name)) {
                    continue;
                }
                List<Object> combined = new ArrayList<>(values);
                Collection<?> protocolValues = (Collection<?>)protocolHeaders.get(key);
                if (protocolValues != null) {
                    for (Object value : protocolValues) {
                        if (value != null) {
                            combined.add(value);
                        }
                    }
                }
                values = combined;
            }
            // After copying, the servlet response already contains the merged values.
            if (values.size() == 1) {
                headers.put(key, values.iterator().next().toString());
            } else {
                headers.put(key, Arrays.deepToString(values.toArray()));
            }
        }
    }
}
