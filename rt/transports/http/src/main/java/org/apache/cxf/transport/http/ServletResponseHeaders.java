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

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.message.Message;

/**
 * A response header view evaluated when read, after application interceptors have added headers.
 * Each iteration uses an immutable snapshot; the transport owns servlet access and merging rules.
 */
final class ServletResponseHeaders extends AbstractMap<String, List<String>> {
    private final Message message;
    private final HttpServletResponse response;

    private ServletResponseHeaders(Message message, HttpServletResponse response) {
        this.message = message;
        this.response = response;
    }

    /** Returns the view, which cannot be modified. */
    static Map<String, List<String>> of(Message message, HttpServletResponse response) {
        return Collections.unmodifiableMap(new ServletResponseHeaders(message, response));
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet() {
        return snapshot().entrySet();
    }

    // Look up in the case-insensitive snapshot, as AbstractMap compares the keys with equals
    @Override
    public List<String> get(Object key) {
        return snapshot().get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return snapshot().containsKey(key);
    }

    private Map<String, List<String>> snapshot() {
        return Headers.getResponseHeaders(message, response);
    }
}
