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

import org.apache.camel.opentracing.starter.CamelOpenTracing;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.Bean;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.CodecConfiguration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.Configuration.SenderConfiguration;
import io.jaegertracing.internal.propagation.TextMapCodec;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format.Builtin;

@EnableAutoConfiguration
@SpringBootApplication
@CamelOpenTracing
public class Server {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Server.class)
            .web(WebApplicationType.NONE)
            .build()
            .run(args);
    }
    
    @Bean
    Tracer tracer() {
        return new Configuration("camel-server")
            .withSampler(
                new SamplerConfiguration()
                    .withType(ConstSampler.TYPE)
                    .withParam(1))
            .withReporter(new ReporterConfiguration().withSender(
                new SenderConfiguration()
                    .withEndpoint("http://localhost:14268/api/traces")
            ))
            .withCodec(
                new CodecConfiguration()
                    .withCodec(Builtin.TEXT_MAP, new TextMapCodec(true))
            )
            .getTracer();
    }
}

