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

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.metrics.MetricsFeature;
import org.apache.cxf.metrics.MetricsProvider;
import org.apache.cxf.systest.jaxrs.resources.Book;
import org.apache.cxf.systest.jaxrs.resources.Library;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.search.RequiredSearch;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = SpringJaxrsTest.TestConfig.class)
@ActiveProfiles("jaxrs")
public class SpringJaxrsTest {
    @Rule
    public OutputCaptureRule output = new OutputCaptureRule();

    @Autowired
    private MeterRegistry registry;
    
    @LocalServerPort
    private int port;

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = Library.class)
    static class TestConfig {
        @Bean
        public SpringBus cxf() {
            final SpringBus bus = new SpringBus();
            // Bye default, the exception are propagated and out fault interceptors are not called 
            bus.setProperty("org.apache.cxf.propagate.exception", Boolean.FALSE);
            return bus;
        }
        
        @Bean
        public Feature metricsFeature(MetricsProvider metricsProvider) {
            return new MetricsFeature(metricsProvider);
        }
        
        @Bean
        public JacksonJsonProvider jacksonJsonProvider() {
            return new JacksonJsonProvider();
        }
    }
    
    @Before
    public void setUp() {
        this.registry.getMeters().forEach(meter -> registry.remove(meter));
    }

    @Test
    public void testJaxrsSuccessMetric() {
        final WebTarget target = createWebTarget();
        
        try (Response r = target.request().get()) {
            assertThat(r.getStatus()).isEqualTo(200);
        }
        
        RequiredSearch requestMetrics = registry.get("cxf.server.requests");

        Map<Object, Object> tags = requestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(tags)
            .containsOnly(
                    entry("exception", "None"),
                    entry("method", "GET"),
                    entry("operation", "getBooks"),
                    entry("uri", "/api/library"),
                    entry("outcome", "SUCCESS"),
                    entry("status", "200"));
    }
    
    @Test
    public void testJaxrsFailedMetric() {
        final WebTarget target = createWebTarget();
        
        assertThatThrownBy(() -> target.path("100").request().get(Book.class))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Not Found");

        RequiredSearch requestMetrics = registry.get("cxf.server.requests");

        Map<Object, Object> tags = requestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(tags)
                .containsOnly(
                        entry("exception", "None"),
                        entry("method", "GET"),
                        entry("operation", "getBook"),
                        entry("uri", "/api/library/100"),
                        entry("outcome", "CLIENT_ERROR"),
                        entry("status", "404"));
    }
    
    @Test
    public void testJaxrsExceptionMetric() {
        final WebTarget target = createWebTarget();
        
        assertThatThrownBy(() -> target.request().delete(String.class))
            .isInstanceOf(InternalServerErrorException.class)
            .hasMessageContaining("Internal Server Error");

        RequiredSearch requestMetrics = registry.get("cxf.server.requests");

        Map<Object, Object> tags = requestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(tags)
                .containsOnly(
                        entry("exception", "UnsupportedOperationException"),
                        entry("method", "DELETE"),
                        entry("operation", "deleteBooks"),
                        entry("uri", "/api/library"),
                        entry("outcome", "SERVER_ERROR"),
                        entry("status", "500"));
    }
    
    private WebTarget createWebTarget() {
        return ClientBuilder
            .newClient()
            .register(JacksonJsonProvider.class)
            .target("http://localhost:" + port + "/api/library");
    }

}
