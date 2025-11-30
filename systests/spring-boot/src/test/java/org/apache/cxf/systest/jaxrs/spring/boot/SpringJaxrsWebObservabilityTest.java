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

import java.time.Duration;

import brave.handler.SpanHandler;
import brave.sampler.Sampler;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.systest.ArrayListSpanReporter;
import org.apache.cxf.systest.jaxrs.resources.Library;
import org.apache.cxf.tracing.micrometer.jaxrs.ObservationClientProvider;
import org.apache.cxf.tracing.micrometer.jaxrs.ObservationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span.Kind;
import io.micrometer.tracing.test.simple.SpansAssert;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringJaxrsWebObservabilityTest.TestConfig.class)
@ActiveProfiles("jaxrs")
@AutoConfigureMetrics
@AutoConfigureTracing
public class SpringJaxrsWebObservabilityTest {

    @Autowired
    private MeterRegistry registry;
    
    @Autowired
    private ObservationRegistry observationRegistry;

    @Autowired
    private ArrayListSpanReporter arrayListSpanReporter;

    @LocalServerPort
    private int port;

    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = Library.class)
    static class TestConfig {
        @Bean
        public Feature observationFeature(ObservationRegistry observationRegistry) {
            return new ObservationFeature(observationRegistry);
        }
        
        @Bean
        public JacksonJsonProvider jacksonJsonProvider() {
            return new JacksonJsonProvider();
        }

        @Bean
        Sampler sampler() {
            return Sampler.ALWAYS_SAMPLE;
        }

        @Bean
        ArrayListSpanReporter arrayListSpanReporter() {
            return new ArrayListSpanReporter();
        }

        @Bean
        SpanHandler spanHandler() {
            return SpanHandler.NOOP;
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
        arrayListSpanReporter.close();
    }

    @Test
    public void testJaxrsSuccessMetric() {
        final WebTarget target = createWebTarget();
        
        try (Response r = target.request().get()) {
            assertThat(r.getStatus()).isEqualTo(200);
        }
        
        await()
            .atMost(Duration.ofSeconds(1))
            .until(() -> arrayListSpanReporter.getSpans(), hasSize(3));

        // Micrometer Observation with Micrometer Tracing
        SpansAssert.assertThat(arrayListSpanReporter.getSpans())
               .haveSameTraceId()
               .hasASpanWithName("GET", spanAssert -> {
                   spanAssert.hasKindEqualTo(Kind.CLIENT)
                             .hasTag("http.request.method", "GET")
                             .hasTag("http.response.status_code", "200")
                             .hasTag("network.protocol.name", "http")
                             .hasTag("url.scheme", "http")
                             .hasTagWithKey("server.address")
                             .hasTagWithKey("server.port")
                             .hasTagWithKey("url.full")
                             .isStarted()
                             .isEnded();
               })
               .hasASpanWithName("GET /api/library", spanAssert -> {
                   spanAssert.hasKindEqualTo(Kind.SERVER)
                             .hasTag("http.request.method", "GET")
                             .hasTag("http.response.status_code", "200")
                             .hasTag("http.route", "/api/library")
                             .hasTag("network.protocol.name", "http")
                             .hasTag("url.scheme", "http")
                             .hasTagWithKey("server.address")
                             .hasTagWithKey("server.port")
                             .isStarted()
                             .isEnded();
               });

        // Micrometer Observation with Micrometer Core
        MeterRegistryAssert.assertThat(registry)
            .hasTimerWithNameAndTags("http.client.duration", Tags.of("error", "none",
                "http.request.method", "GET", "http.response.status_code", "200",
                "network.protocol.name", "http", "url.scheme", "http",
                "server.address", "localhost", "server.port", String.valueOf(port)))
            .hasTimerWithNameAndTags("http.server.duration", Tags.of("error", "none",
                "http.request.method", "GET", "http.response.status_code", "200",
                "http.route", "/api/library", "network.protocol.name", "http",
                "server.address", "localhost", "server.port", String.valueOf(port)))
            .hasTimerWithNameAndTags("http.server.requests", Tags.of("error", "none",
                "method", "GET", "status", "200",
                "uri", "/api/library", "outcome", "SUCCESS",
                "exception", "none"));
    }

    private WebTarget createWebTarget() {
        return ClientBuilder
            .newClient()
            .register(JacksonJsonProvider.class)
            .register(new ObservationClientProvider(observationRegistry))
            .target("http://localhost:" + port + "/api/library");
    }

}
