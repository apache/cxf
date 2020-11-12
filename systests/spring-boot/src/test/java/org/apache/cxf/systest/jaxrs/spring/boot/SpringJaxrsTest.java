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

import java.util.Arrays;
import java.util.Map;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.metrics.MetricsFeature;
import org.apache.cxf.metrics.MetricsProvider;
import org.apache.cxf.systest.jaxrs.resources.Book;
import org.apache.cxf.systest.jaxrs.resources.Library;
import org.apache.cxf.systest.jaxrs.resources.LibraryApi;
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
import org.springframework.util.SocketUtils;

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
    
    @Autowired
    private MetricsProvider metricsProvider;
    
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
        
        RequiredSearch serverRequestMetrics = registry.get("cxf.server.requests");

        Map<Object, Object> serverTags = serverRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(serverTags)
            .containsOnly(
                entry("exception", "None"),
                entry("method", "GET"),
                entry("operation", "getBooks"),
                entry("uri", "/api/library"),
                entry("outcome", "SUCCESS"),
                entry("status", "200"));
        
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
    public void testJaxrsFailedMetric() {
        final WebTarget target = createWebTarget();
        
        assertThatThrownBy(() -> target.path("100").request().get(Book.class))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("Not Found");

        RequiredSearch serverRequestMetrics = registry.get("cxf.server.requests");

        Map<Object, Object> serverTags = serverRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(serverTags)
            .containsOnly(
                entry("exception", "None"),
                entry("method", "GET"),
                entry("operation", "getBook"),
                entry("uri", "/api/library/100"),
                entry("outcome", "CLIENT_ERROR"),
                entry("status", "404"));
        
        RequiredSearch clientRequestMetrics = registry.get("cxf.client.requests");

        Map<Object, Object> clientTags = clientRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(clientTags)
            .containsOnly(
                entry("exception", "None"),
                entry("method", "GET"),
                entry("operation", "UNKNOWN"),
                entry("uri", "http://localhost:" + port + "/api/library/100"),
                entry("outcome", "CLIENT_ERROR"),
                entry("status", "404"));
    }
    
    @Test
    public void testJaxrsExceptionMetric() {
        final WebTarget target = createWebTarget();
        
        assertThatThrownBy(() -> target.request().delete(String.class))
            .isInstanceOf(InternalServerErrorException.class)
            .hasMessageContaining("Internal Server Error");

        RequiredSearch serverRequestMetrics = registry.get("cxf.server.requests");

        Map<Object, Object> serverTags = serverRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(serverTags)
            .containsOnly(
                entry("exception", "UnsupportedOperationException"),
                entry("method", "DELETE"),
                entry("operation", "deleteBooks"),
                entry("uri", "/api/library"),
                entry("outcome", "SERVER_ERROR"),
                entry("status", "500"));
        
        RequiredSearch clientRequestMetrics = registry.get("cxf.client.requests");

        Map<Object, Object> clientTags = clientRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(clientTags)
            .containsOnly(
                entry("exception", "None"),
                entry("method", "DELETE"),
                entry("operation", "UNKNOWN"),
                entry("uri", "http://localhost:" + port + "/api/library"),
                entry("outcome", "SERVER_ERROR"),
                entry("status", "500"));
    }
    
    @Test
    public void testJaxrsClientExceptionMetric() {
        final int fakePort = SocketUtils.findAvailableTcpPort();
        
        final WebTarget target = ClientBuilder
            .newClient()
            .register(new MetricsFeature(metricsProvider))
            .target("http://localhost:" + fakePort + "/api/library");
        
        assertThatThrownBy(() -> target.request().delete(String.class))
            .isInstanceOf(ProcessingException.class)
            .hasMessageContaining("Connection refused");

        // no server meters
        assertThat(registry.getMeters())
            .noneMatch(m -> m.getId().getName().equals("cxf.server.requests"));
        
        RequiredSearch clientRequestMetrics = registry.get("cxf.client.requests");

        Map<Object, Object> clientTags = clientRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(clientTags)
            .containsOnly(
                entry("exception", "None"),
                entry("method", "DELETE"),
                entry("operation", "UNKNOWN"),
                entry("uri", "http://localhost:" + fakePort + "/api/library"),
                entry("outcome", "UNKNOWN"),
                entry("status", "UNKNOWN"));
    }
    
    @Test
    public void testJaxrsProxySuccessMetric() {
        final LibraryApi api = createApi(port);
        
        try (Response r = api.getBooks(1)) {
            assertThat(r.getStatus()).isEqualTo(200);
        }
        
        RequiredSearch serverRequestMetrics = registry.get("cxf.server.requests");

        Map<Object, Object> serverTags = serverRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(serverTags)
            .containsOnly(
                entry("exception", "None"),
                entry("method", "GET"),
                entry("operation", "getBooks"),
                entry("uri", "/api/library"),
                entry("outcome", "SUCCESS"),
                entry("status", "200"));
        
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
    public void testJaxrsProxyExceptionMetric() {
        final LibraryApi api = createApi(port);
        
        assertThatThrownBy(() -> api.deleteBooks())
            .isInstanceOf(InternalServerErrorException.class)
            .hasMessageContaining("Internal Server Error");

        RequiredSearch serverRequestMetrics = registry.get("cxf.server.requests");

        Map<Object, Object> serverTags = serverRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(serverTags)
            .containsOnly(
                entry("exception", "UnsupportedOperationException"),
                entry("method", "DELETE"),
                entry("operation", "deleteBooks"),
                entry("uri", "/api/library"),
                entry("outcome", "SERVER_ERROR"),
                entry("status", "500"));
        
        RequiredSearch clientRequestMetrics = registry.get("cxf.client.requests");

        Map<Object, Object> clientTags = clientRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(clientTags)
            .containsOnly(
                entry("exception", "None"),
                entry("method", "DELETE"),
                entry("operation", "UNKNOWN"),
                entry("uri", "http://localhost:" + port + "/api/library"),
                entry("outcome", "SERVER_ERROR"),
                entry("status", "500"));
    }
    
    @Test
    public void testJaxrsProxyFailedMetric() {
        final LibraryApi api = createApi(port);

        try (Response r = api.getBook("100")) {
            assertThat(r.getStatus()).isEqualTo(404);
        }

        RequiredSearch serverRequestMetrics = registry.get("cxf.server.requests");

        Map<Object, Object> serverTags = serverRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(serverTags)
            .containsOnly(
                entry("exception", "None"),
                entry("method", "GET"),
                entry("operation", "getBook"),
                entry("uri", "/api/library/100"),
                entry("outcome", "CLIENT_ERROR"),
                entry("status", "404"));
        
        RequiredSearch clientRequestMetrics = registry.get("cxf.client.requests");

        Map<Object, Object> clientTags = clientRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(clientTags)
            .containsOnly(
                entry("exception", "None"),
                entry("method", "GET"),
                entry("operation", "UNKNOWN"),
                entry("uri", "http://localhost:" + port + "/api/library/100"),
                entry("outcome", "CLIENT_ERROR"),
                entry("status", "404"));
    }
    
    @Test
    public void testJaxrsProxyClientExceptionMetric() {
        final int fakePort = SocketUtils.findAvailableTcpPort();
        final LibraryApi api = createApi(fakePort);

        assertThatThrownBy(() -> api.deleteBooks())
            .isInstanceOf(ProcessingException.class)
            .hasMessageContaining("Connection refused");

        // no server meters
        assertThat(registry.getMeters())
            .noneMatch(m -> m.getId().getName().equals("cxf.server.requests"));
        
        RequiredSearch clientRequestMetrics = registry.get("cxf.client.requests");

        Map<Object, Object> clientTags = clientRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(clientTags)
            .containsOnly(
                entry("exception", "None"),
                entry("method", "DELETE"),
                entry("operation", "UNKNOWN"),
                entry("uri", "http://localhost:" + fakePort + "/api/library"),
                entry("outcome", "UNKNOWN"),
                entry("status", "UNKNOWN"));
    }
    
    private LibraryApi createApi(int portToUse) {
        final JAXRSClientFactoryBean factory = new JAXRSClientFactoryBean();
        factory.setAddress("http://localhost:" + portToUse + "/api/library");
        factory.setFeatures(Arrays.asList(new MetricsFeature(metricsProvider)));
        factory.setResourceClass(LibraryApi.class);
        return factory.create(LibraryApi.class);
    }

    private WebTarget createWebTarget() {
        return ClientBuilder
            .newClient()
            .register(JacksonJsonProvider.class)
            .register(new MetricsFeature(metricsProvider))
            .target("http://localhost:" + port + "/api/library");
    }

}
