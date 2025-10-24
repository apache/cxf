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

package org.apache.cxf.systest.jaxws.spring.boot;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.Service.Mode;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.metrics.MetricsFeature;
import org.apache.cxf.metrics.MetricsProvider;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.systest.jaxws.resources.HelloServiceEmptyTimedImpl;
import org.apache.cxf.systest.jaxws.resources.HelloServiceTimedImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ActiveProfiles;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.search.RequiredSearch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;

@SpringBootApplication
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = {
            SpringJaxwsTimedTest.TestConfig.class,
      
        },
        properties = {
            "cxf.metrics.server.max-uri-tags=2"
        })
@ImportResource("classpath:spring/jaxws-client.xml") 
@ActiveProfiles("jaxws")

public class SpringJaxwsTimedTest {

    private static final String DUMMY_REQUEST_BODY = "<q0:sayHello xmlns:q0=\"http://service.ws.sample/\">"
            + "<name>Elan</name>"
            + "</q0:sayHello>";
    private static final String HELLO_SERVICE_NAME_V1 = "HelloV1";
    private static final String HELLO_SERVICE_NAME_V2 = "HelloV2";

    @Autowired
    private MeterRegistry registry;
    
    @Autowired
    private MetricsProvider metricsProvider;

    @LocalServerPort
    private int port;

    @EnableAutoConfiguration
    static class TestConfig {
        @Autowired
        private Bus bus;

        @Autowired
        private MetricsProvider metricsProvider;

        @Bean
        public Endpoint helloEndpoint() {
            EndpointImpl endpoint = new EndpointImpl(bus, new HelloServiceTimedImpl(), null, null, new MetricsFeature[]{
                new MetricsFeature(metricsProvider)
            });
            endpoint.publish("/" + HELLO_SERVICE_NAME_V1);
            return endpoint;
        }
        
        @Bean
        public Endpoint secondHelloEndpoint() {
            EndpointImpl endpoint = new EndpointImpl(bus, new HelloServiceEmptyTimedImpl(), null, null, 
                new MetricsFeature[] {
                    new MetricsFeature(metricsProvider)
                }
            );
            endpoint.publish("/" + HELLO_SERVICE_NAME_V2);
            return endpoint;
        }

    }

    @AfterEach
    public void clear() {
        registry.clear();
    }

    @Test
    public void testJaxwsTimedSuccessMetric() throws MalformedURLException {
        // given in setUp

        // when
        String actual = sendSoapRequest(DUMMY_REQUEST_BODY, HELLO_SERVICE_NAME_V1);

        // then
        assertThat(actual)
                .isEqualTo("<ns2:sayHelloResponse xmlns:ns2=\"http://service.ws.sample/\">"
                        + "<return>Hello, Elan</return>"
                        + "</ns2:sayHelloResponse>");

        await()
            .atMost(Duration.ofSeconds(1))
            .ignoreException(MeterNotFoundException.class)
            .until(() -> registry.get("cxf.server.requests").timers(), not(empty()));
        RequiredSearch serverRequestMetrics = registry.get("cxf.server.requests");

        Map<Object, Object> serverTags = serverRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(serverTags)
            .containsOnly(
                entry("exception", "None"),
                entry("faultCode", "None"),
                entry("method", "POST"),
                entry("operation", "sayHello"),
                entry("uri", "/Service/" + HELLO_SERVICE_NAME_V1),
                entry("outcome", "SUCCESS"),
                entry("status", "200"));
        
        RequiredSearch timedMetrics = registry.get("sayHello.requests");

        Map<Object, Object> timedTags = timedMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(timedTags)
            .containsOnly(
                entry("exception", "None"),
                entry("faultCode", "None"),
                entry("method", "POST"),
                entry("operation", "sayHello"),
                entry("uri", "/Service/" + HELLO_SERVICE_NAME_V1),
                entry("outcome", "SUCCESS"),
                entry("status", "200"));
            
        RequiredSearch clientRequestMetrics = registry.get("cxf.client.requests");

        Map<Object, Object> clientTags = clientRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(clientTags)
            .containsOnly(
                entry("exception", "None"),
                entry("faultCode", "None"),
                entry("method", "POST"),
                entry("operation", "Invoke"),
                entry("uri", "http://localhost:" + port + "/Service/" + HELLO_SERVICE_NAME_V1),
                entry("outcome", "SUCCESS"),
                entry("status", "200"));
    }
    
    @Test
    public void testJaxwsEmptyTimedSuccessMetric() throws MalformedURLException {
        // given in setUp

        // when
        String actual = sendSoapRequest(DUMMY_REQUEST_BODY, HELLO_SERVICE_NAME_V2);

        // then
        assertThat(actual)
                .isEqualTo("<ns2:sayHelloResponse xmlns:ns2=\"http://service.ws.sample/\">"
                        + "<return>Hello, Elan</return>"
                        + "</ns2:sayHelloResponse>");

        await()
            .atMost(Duration.ofSeconds(1))
            .ignoreException(MeterNotFoundException.class)
            .until(() -> registry.get("cxf.server.requests").timers(), not(empty()));
        RequiredSearch serverRequestMetrics = registry.get("cxf.server.requests");

        Map<Object, Object> serverTags = serverRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(serverTags)
            .containsOnly(
                entry("exception", "None"),
                entry("faultCode", "None"),
                entry("method", "POST"),
                entry("operation", "sayHello"),
                entry("uri", "/Service/" + HELLO_SERVICE_NAME_V2),
                entry("outcome", "SUCCESS"),
                entry("status", "200"));
        
        RequiredSearch timedMetrics = registry.get("sayHello");

        Map<Object, Object> timedTags = timedMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(timedTags)
            .containsOnly(
                entry("exception", "None"),
                entry("faultCode", "None"),
                entry("method", "POST"),
                entry("operation", "sayHello"),
                entry("uri", "/Service/" + HELLO_SERVICE_NAME_V2),
                entry("outcome", "SUCCESS"),
                entry("status", "200"));
            
        RequiredSearch clientRequestMetrics = registry.get("cxf.client.requests");

        Map<Object, Object> clientTags = clientRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(clientTags)
            .containsOnly(
                entry("exception", "None"),
                entry("faultCode", "None"),
                entry("method", "POST"),
                entry("operation", "Invoke"),
                entry("uri", "http://localhost:" + port + "/Service/" + HELLO_SERVICE_NAME_V2),
                entry("outcome", "SUCCESS"),
                entry("status", "200"));
    }
        
    private String sendSoapRequest(String requestBody, final String serviceName) throws MalformedURLException {
        String address = "http://localhost:" + port + "/Service/" + serviceName;

        StreamSource source = new StreamSource(new StringReader(requestBody));
        Service service = Service.create(new URL(address + "?wsdl"),
                new QName("http://service.ws.sample/", "HelloService"), new MetricsFeature(metricsProvider));
        Dispatch<Source> dispatch = service.createDispatch(new QName("http://service.ws.sample/", "HelloPort"),
            Source.class, Mode.PAYLOAD);

        Source result = dispatch.invoke(source);
        return StaxUtils.toString(result);
    }
}
