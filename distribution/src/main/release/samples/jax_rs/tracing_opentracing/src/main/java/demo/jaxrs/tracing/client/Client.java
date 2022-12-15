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

package demo.jaxrs.tracing.client;

import java.util.Arrays;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.tracing.opentracing.jaxrs.OpenTracingClientProvider;

import demo.jaxrs.tracing.Slf4jLogSender;
import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.Configuration.SenderConfiguration;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.spi.Sender;
import io.opentracing.Tracer;

public final class Client {
    private Client() {
    }

    public static void main(final String[] args) throws Exception {
        final Tracer tracer = new Configuration("tracer-client") 
            .withSampler(new SamplerConfiguration().withType(ConstSampler.TYPE).withParam(1))
            .withReporter(new ReporterConfiguration().withSender(
                new SenderConfiguration() {
                    @Override
                    public Sender getSender() {
                        return new Slf4jLogSender();
                    }
                }
            ))
            .getTracer();
        final OpenTracingClientProvider provider = new OpenTracingClientProvider(tracer);

        final Response response = WebClient
            .create("http://localhost:9000/catalog", Arrays.asList(provider))
            .accept(MediaType.APPLICATION_JSON)
            .get();

        System.out.println(response.readEntity(String.class));
        response.close();
        tracer.close();
    }
}
