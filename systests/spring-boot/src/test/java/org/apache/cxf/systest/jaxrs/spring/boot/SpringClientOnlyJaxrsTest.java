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

package org.apache.cxf.systest.jaxrs.spring.boot;

import java.util.Map;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.metrics.MetricsFeature;
import org.apache.cxf.metrics.MetricsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.search.RequiredSearch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringClientOnlyJaxrsTest.TestConfig.class)
@ActiveProfiles("client")
public class SpringClientOnlyJaxrsTest {

    @Autowired
    private MeterRegistry registry;
    
    @Autowired
    private MetricsProvider metricsProvider;
    
    @LocalServerPort
    private int port;

    @EnableAutoConfiguration
    static class TestConfig {
        @Bean
        public Feature metricsFeature(MetricsProvider metricsProvider) {
            return new MetricsFeature(metricsProvider);
        }
        
        @Bean
        public JacksonJsonProvider jacksonJsonProvider() {
            return new JacksonJsonProvider();
        }
        
        @Bean
        RouterFunction<ServerResponse> get() {
            return RouterFunctions.route(RequestPredicates.GET("/api/library"), 
                req -> ServerResponse
                    .ok()
                    .build());
        }
        
        @Bean
        RouterFunction<ServerResponse> delete() {
            return RouterFunctions.route(RequestPredicates.DELETE("/api/library"), 
                req -> ServerResponse
                    .badRequest()
                    .build());
        }
    }

    @Autowired
    public void setBus(Bus bus) {
        // By default, the exception are propagated and out fault interceptors are not called 
        bus.setProperty("org.apache.cxf.propagate.exception", Boolean.FALSE);
    }

    @AfterEach
    public void clear() {
        registry.clear();
    }

    @Test
    public void testJaxrsClientSuccessMetric() {
        final WebTarget target = createWebTarget();
        
        final Builder builder = target.request().header(HttpHeaders.CONTENT_TYPE, "text/plain"); 
        try (Response r = builder.get()) {
            assertThat(r.getStatus()).isEqualTo(200);
        }
        
        // no server meters
        assertThat(registry.getMeters())
            .noneMatch(m -> "cxf.server.requests".equals(m.getId().getName()));
        
        RequiredSearch clientRequestMetrics = registry.get("cxf.client.requests");

        Map<Object, Object> clientTags = clientRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(clientTags)
            .containsOnly(
                entry("exception", "None"),
                entry("method", "GET"),
                entry("operation", "UNKNOWN"),
                entry("uri", "http://localhost:" + port + "/api/library"),
                entry("outcome", "SUCCESS"),
                entry("status", "200"));
    }
    
    @Test
    public void testJaxrsClientExceptionMetric() {
        final WebTarget target = ClientBuilder
            .newClient()
            .register(new MetricsFeature(metricsProvider))
            .target("http://localhost:" + port + "/api/library");
        
        final Builder builder = target.request().header(HttpHeaders.CONTENT_TYPE, "text/plain"); 
        assertThatThrownBy(() -> builder.delete(String.class))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Bad Request");

        // no server meters
        assertThat(registry.getMeters())
            .noneMatch(m -> "cxf.server.requests".equals(m.getId().getName()));
        
        RequiredSearch clientRequestMetrics = registry.get("cxf.client.requests");

        Map<Object, Object> clientTags = clientRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(clientTags)
            .containsOnly(
                entry("exception", "None"),
                entry("method", "DELETE"),
                entry("operation", "UNKNOWN"),
                entry("uri", "http://localhost:" + port + "/api/library"),
                entry("outcome", "CLIENT_ERROR"),
                entry("status", "400"));
    }

    private WebTarget createWebTarget() {
        return ClientBuilder
            .newClient()
            .register(JacksonJsonProvider.class)
            .register(new MetricsFeature(metricsProvider))
            .target("http://localhost:" + port + "/api/library");
    }

}
