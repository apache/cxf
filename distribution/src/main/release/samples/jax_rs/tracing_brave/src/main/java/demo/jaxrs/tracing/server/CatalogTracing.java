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

package demo.jaxrs.tracing.server;

import brave.Tracing;
import brave.context.slf4j.MDCScopeDecorator;
import brave.http.HttpTracing;
import brave.propagation.ThreadLocalCurrentTraceContext;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.okhttp3.OkHttpSender;

public class CatalogTracing implements AutoCloseable {
    private volatile AsyncReporter<Span> reporter;
    private volatile BytesMessageSender sender;
    private volatile HttpTracing httpTracing;
    private final String serviceName;
    
    public CatalogTracing(final String serviceName) {
        this.serviceName = serviceName;
    }
    
    public HttpTracing getHttpTracing() {
        HttpTracing result = httpTracing;
        
        if (result == null) {
            synchronized (this) {
                result = httpTracing;
                if (result == null) {
                    sender = OkHttpSender.create("http://localhost:9411/api/v2/spans");
                    reporter = AsyncReporter.create(sender);
                    result = createHttpTracing(serviceName, reporter);
                    httpTracing = result;
                }
            }
        }
        
        return result;
    }
    
    @Override
    public void close() throws Exception {
        if (reporter != null) {
            reporter.close();
        }
        
        if (sender != null) {
            sender.close();
        }
    }
    
    private static HttpTracing createHttpTracing(String serviceName, AsyncReporter<Span> reporter) {
        final Tracing tracing = Tracing
            .newBuilder()
            .localServiceName(serviceName)
            .currentTraceContext(
                ThreadLocalCurrentTraceContext
                    .newBuilder()
                    .addScopeDecorator(MDCScopeDecorator.get()) 
                    .build()
              )
            .addSpanHandler(ZipkinSpanHandler.create(reporter))
            .build();
        
        return HttpTracing.create(tracing);
    }
}
