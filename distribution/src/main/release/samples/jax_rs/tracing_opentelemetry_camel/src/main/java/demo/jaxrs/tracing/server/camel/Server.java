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

package demo.jaxrs.tracing.server.camel;

import org.apache.camel.opentelemetry.starter.CamelOpenTelemetry;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import demo.jaxrs.tracing.OpenTelemetrySetup;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;

@EnableAutoConfiguration
@SpringBootApplication
@CamelOpenTelemetry
public class Server {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Server.class)
            .web(WebApplicationType.NONE)
            .build()
            .run(args);
    }
    
    @Bean
    Tracer tracer() {
        return OpenTelemetrySetup.setup("camel-server").getTracer("camel-server");
    }
    
    @Bean
    ContextPropagators contextPropagators() {
        return ContextPropagators.create(TextMapPropagator.composite(W3CBaggagePropagator.getInstance()));
    }
}

