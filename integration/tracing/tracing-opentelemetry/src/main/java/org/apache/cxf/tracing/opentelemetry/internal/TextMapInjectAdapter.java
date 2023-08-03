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
package org.apache.cxf.tracing.opentelemetry.internal;

import java.util.List;
import java.util.Map;

import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;

public final class TextMapInjectAdapter
    implements TextMapGetter<Map<String, List<String>>>, TextMapSetter<Map<String, List<String>>> {
    private static final TextMapInjectAdapter INSTANCE = new TextMapInjectAdapter();

    private TextMapInjectAdapter() {

    }

    public static TextMapInjectAdapter get() {
        return INSTANCE;
    }

    @Override
    public Iterable<String> keys(Map<String, List<String>> carrier) {
        return carrier.keySet();
    }

    @Override
    public String get(Map<String, List<String>> carrier, String key) {
        List<String> values = carrier.get(key);
        return values == null || values.isEmpty() ? null : values.get(values.size() - 1);
    }

    @Override
    public void set(Map<String, List<String>> carrier, String key, String value) {
        carrier.put(key, List.of(value));
    }
}
