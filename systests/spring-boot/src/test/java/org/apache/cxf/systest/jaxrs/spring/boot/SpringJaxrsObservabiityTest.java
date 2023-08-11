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

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import brave.sampler.Sampler;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.systest.ArrayListSpanReporter;
import org.apache.cxf.systest.jaxrs.resources.Library;
import org.apache.cxf.tracing.micrometer.ObservationClientFeature;
import org.apache.cxf.tracing.micrometer.ObservationFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.observation.web.servlet.WebMvcObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import zipkin2.Span;
import zipkin2.reporter.Reporter;

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

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringJaxrsObservabiityTest.TestConfig.class)
@ActiveProfiles("jaxrs")
@AutoConfigureObservability
public class SpringJaxrsObservabiityTest {

    @Autowired
    private MeterRegistry registry;
    
    @Autowired
    private ObservationRegistry observationRegistry;

    @Autowired
    private ArrayListSpanReporter arrayListSpanReporter;

    @LocalServerPort
    private int port;

    @EnableAutoConfiguration(exclude = WebMvcObservationAutoConfiguration.class)
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
        Reporter<Span> reporter() {
            return Reporter.CONSOLE;
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
            .until(() -> arrayListSpanReporter.getSpans(), hasSize(2));

        // Micrometer Observation with Micrometer Tracing
        SpansAssert.assertThat(arrayListSpanReporter.getSpans())
               .haveSameTraceId()
               .hasASpanWithName("GET", spanAssert -> {
                   spanAssert.hasKindEqualTo(Kind.CLIENT)
                             .hasTag("rpc.service", "WebClient")
                             .hasTag("rpc.system", "cxf")
                             .hasTagWithKey("server.address")
                             .hasTagWithKey("server.port")
                             .isStarted()
                             .isEnded();
               })
               .hasASpanWithName("rpc.server.duration", spanAssert -> {
                   spanAssert.hasKindEqualTo(Kind.SERVER)
                             .hasTag("rpc.service", "Library")
                             .hasTag("rpc.system", "cxf")
                             .hasTagWithKey("server.address")
                             .hasTagWithKey("server.port")
                             .isStarted()
                             .isEnded();
               });

        // Micrometer Observation with Micrometer Core
        MeterRegistryAssert.assertThat(registry)
            .hasTimerWithNameAndTags("rpc.client.duration", Tags.of("error", "none",
                "rpc.service", "WebClient", "rpc.system", "cxf",
                "server.address", "localhost", "server.port", String.valueOf(port)))
            .hasTimerWithNameAndTags("rpc.server.duration", Tags.of("error", "none",
                "rpc.service", "Library", "rpc.system", "cxf",
                "server.address", "localhost", "server.port", String.valueOf(port)));
    }

    private WebTarget createWebTarget() {
        return ClientBuilder
            .newClient()
            .register(JacksonJsonProvider.class)
            .register(new ObservationClientFeature(observationRegistry))
            .target("http://localhost:" + port + "/api/library");
    }

}
