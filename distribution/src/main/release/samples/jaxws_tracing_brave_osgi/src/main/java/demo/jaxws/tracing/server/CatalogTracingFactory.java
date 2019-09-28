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

package demo.jaxws.tracing.server;

import brave.Tracing;
import brave.propagation.ThreadLocalCurrentTraceContext;
import zipkin2.reporter.AsyncReporter;

public final class CatalogTracingFactory {

    private CatalogTracingFactory() {
    }

    public static Tracing create(final String serviceName, final AsyncReporter<zipkin2.Span> reporter) {
        return Tracing
            .newBuilder()
            .localServiceName(serviceName)
            .currentTraceContext(
                ThreadLocalCurrentTraceContext
                    .newBuilder()
                    .build()
              )
            .spanReporter(reporter)
            .build();
    }
}
