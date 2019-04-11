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
package org.apache.cxf.systest.microprofile.rest.client.tracing.brave;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import zipkin2.Span;
import zipkin2.reporter.Reporter;

public class TestSpanReporter implements Reporter<Span> {
    private static List<Span> spans = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void report(Span span) {
        spans.add(span);
    }

    public static List<Span> getAllSpans() {
        return spans;
    }

    public static void clear() {
        spans.clear();
    }
}
