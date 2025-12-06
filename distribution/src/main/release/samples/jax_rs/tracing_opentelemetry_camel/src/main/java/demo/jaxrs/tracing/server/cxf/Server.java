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

import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.tracing.opentelemetry.jaxrs.OpenTelemetryFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import demo.jaxrs.tracing.OpenTelemetrySetup;
import io.opentelemetry.api.OpenTelemetry;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

@EnableAutoConfiguration
@SpringBootApplication
public class Server {
    @Autowired private Bus bus;
    
    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);
    }
    
    @Bean
    org.apache.cxf.endpoint.Server cxfServer(@Qualifier("cxf") final OpenTelemetry otel) {
        final JAXRSServerFactoryBean endpoint = new JAXRSServerFactoryBean();
        endpoint.setBus(bus);
        endpoint.setAddress("/");
        endpoint.setServiceBean(new Catalog());
        endpoint.setProvider(new OpenTelemetryFeature(otel, "cxf-service"));
        endpoint.setProvider(new JacksonJsonProvider());
        return endpoint.create();
    }
    
    @Bean @Qualifier("cxf")
    OpenTelemetry cxfOpenTelemetry() {
        return OpenTelemetrySetup.setup("cxf-service");
    }
}

