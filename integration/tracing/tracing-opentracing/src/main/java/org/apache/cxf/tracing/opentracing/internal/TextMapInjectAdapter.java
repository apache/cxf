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
package org.apache.cxf.tracing.opentracing.internal;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.opentracing.propagation.TextMap;

public class TextMapInjectAdapter implements TextMap {
    private final Map<String, List<String>> headers;
    
    public TextMapInjectAdapter(final Map<String, List<String>> headers) {
        this.headers = headers;
    }
    
    @Override
    public void put(String key, String value) {
        headers.put(key, Collections.singletonList(value)); 
    }
    
    @Override
    public Iterator<Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("TextMapInjectAdapter should only be used with Tracer.inject()");
    }
}
