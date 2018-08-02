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

package demo.jaxrs.tracing.server.cxf;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.tracing.opentracing.jaxrs.OpenTracingFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.Configuration.SenderConfiguration;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Tracer;

@EnableAutoConfiguration
@SpringBootApplication
public class Server {
    @Autowired private Bus bus;
    
    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);
    }
    
    @Bean
    org.apache.cxf.endpoint.Server cxfServer(@Qualifier("cxf") final Tracer tracer) {
        final JAXRSServerFactoryBean endpoint = new JAXRSServerFactoryBean();
        endpoint.setBus(bus);
        endpoint.setAddress("/");
        endpoint.setServiceBean(new Catalog());
        endpoint.setProvider(new OpenTracingFeature(tracer));
        endpoint.setProvider(new JacksonJsonProvider());
        return endpoint.create();
    }
    
    @Bean @Qualifier("cxf")
    Tracer cxfTracer() {
        return new Configuration("cxf-service")
                .withSampler(new SamplerConfiguration().withType(ConstSampler.TYPE).withParam(1))
                .withReporter(new ReporterConfiguration().withSender(
                    new SenderConfiguration()
                        .withEndpoint("http://localhost:14268/api/traces")
                ))
                .getTracer();
    }
}

