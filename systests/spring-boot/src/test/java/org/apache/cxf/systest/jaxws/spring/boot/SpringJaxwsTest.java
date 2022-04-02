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
import java.util.Arrays;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.Service.Mode;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.metrics.MetricsFeature;
import org.apache.cxf.metrics.MetricsProvider;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.systest.jaxws.resources.HelloService;
import org.apache.cxf.systest.jaxws.resources.HelloServiceImpl;
import org.apache.cxf.testutil.common.TestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.search.RequiredSearch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.entry;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = SpringJaxwsTest.TestConfig.class,
        properties = {
            "cxf.metrics.server.max-uri-tags=2"
        })
@ActiveProfiles("jaxws")
public class SpringJaxwsTest {

    private static final String DUMMY_REQUEST_BODY = "<q0:sayHello xmlns:q0=\"http://service.ws.sample/\">"
            + "<name>Elan</name>"
            + "</q0:sayHello>";
    private static final String HELLO_SERVICE_NAME_V1 = "HelloV1";
    private static final String HELLO_SERVICE_NAME_V2 = "HelloV2";
    private static final String HELLO_SERVICE_NAME_V3 = "HelloV3";

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
            EndpointImpl endpoint = new EndpointImpl(bus, new HelloServiceImpl(), null, null, new MetricsFeature[]{
                new MetricsFeature(metricsProvider)
            });
            endpoint.publish("/" + HELLO_SERVICE_NAME_V1);
            return endpoint;
        }

        @Bean
        public Endpoint secondHelloEndpoint() {
            EndpointImpl endpoint = new EndpointImpl(bus, new HelloServiceImpl(), null, null, new MetricsFeature[]{
                new MetricsFeature(metricsProvider)
            });
            endpoint.publish("/" + HELLO_SERVICE_NAME_V2);
            return endpoint;
        }

        @Bean
        public Endpoint thirdHelloEndpoint() {
            EndpointImpl endpoint = new EndpointImpl(bus, new HelloServiceImpl(), null, null, new MetricsFeature[]{
                new MetricsFeature(metricsProvider)
            });
            endpoint.publish("/" + HELLO_SERVICE_NAME_V3);
            return endpoint;
        }
    }

    @AfterEach
    public void clear() {
        registry.clear();
    }

    @Test
    public void testJaxwsSuccessMetric() throws MalformedURLException {
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
    public void testJaxwsFailedMetric() {
        // given
        String requestBody = "<q0:sayHello xmlns:q0=\"http://service.ws.sample/\"></q0:sayHello>";

        // when
        Throwable throwable = catchThrowable(() -> sendSoapRequest(requestBody, HELLO_SERVICE_NAME_V1));

        // then
        assertThat(throwable)
            .isInstanceOf(SOAPFaultException.class)
            .hasMessageContaining("Fault occurred while processing");


        await()
            .atMost(Duration.ofSeconds(1))
            .ignoreException(MeterNotFoundException.class)
            .until(() -> registry.get("cxf.server.requests").timers(), not(empty()));
        RequiredSearch serverRequestMetrics = registry.get("cxf.server.requests");

        Map<Object, Object> serverTags = serverRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(serverTags)
            .containsOnly(
                entry("exception", "NullPointerException"),
                entry("faultCode", "UNCHECKED_APPLICATION_FAULT"),
                entry("method", "POST"),
                entry("operation", "sayHello"),
                entry("uri", "/Service/" + HELLO_SERVICE_NAME_V1),
                entry("outcome", "SERVER_ERROR"),
                entry("status", "500"));
        
        RequiredSearch clientRequestMetrics = registry.get("cxf.client.requests");

        Map<Object, Object> clientTags = clientRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(clientTags)
            .containsOnly(
                entry("exception", "None"),
                entry("faultCode", "UNCHECKED_APPLICATION_FAULT"),
                entry("method", "POST"),
                entry("operation", "Invoke"),
                entry("uri", "http://localhost:" + port + "/Service/" + HELLO_SERVICE_NAME_V1),
                entry("outcome", "SERVER_ERROR"),
                entry("status", "500"));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    public void testAfterMaxUrisReachedFurtherUrisAreDenied(CapturedOutput output) throws MalformedURLException {
        // given in setUp

        // when
        sendSoapRequest(DUMMY_REQUEST_BODY, HELLO_SERVICE_NAME_V1);
        sendSoapRequest(DUMMY_REQUEST_BODY, HELLO_SERVICE_NAME_V2);
        sendSoapRequest(DUMMY_REQUEST_BODY, HELLO_SERVICE_NAME_V3);

        // then
        assertThat(registry.get("cxf.server.requests").meters()).hasSize(2);
        assertThat(output).contains("Reached the maximum number of URI tags for 'cxf.server.requests'");
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    public void testDoesNotDenyNorLogIfMaxUrisIsNotReached(CapturedOutput output) throws MalformedURLException {
        // given in setUp

        // when
        sendSoapRequest(DUMMY_REQUEST_BODY, HELLO_SERVICE_NAME_V1);

        // then
        assertThat(registry.get("cxf.server.requests").meters()).hasSize(1);
        assertThat(output).doesNotContain("Reached the maximum number of URI tags for 'cxf.server.requests'");
    }

    @Test
    public void testJaxwsProxySuccessMetric() throws MalformedURLException {
        final HelloService api = createApi(port, HELLO_SERVICE_NAME_V1); 
        assertThat(api.sayHello("Elan")).isEqualTo("Hello, Elan");

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
        
        RequiredSearch clientRequestMetrics = registry.get("cxf.client.requests");

        Map<Object, Object> clientTags = clientRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(clientTags)
            .containsOnly(
                entry("exception", "None"),
                entry("faultCode", "None"),
                entry("method", "POST"),
                entry("operation", "sayHello"),
                entry("uri", "http://localhost:" + port + "/Service/" + HELLO_SERVICE_NAME_V1),
                entry("outcome", "SUCCESS"),
                entry("status", "200"));
    }
    
    @Test
    public void testJaxwsProxyFailedMetric() {
        final HelloService api = createApi(port, HELLO_SERVICE_NAME_V1); 

        // then
        assertThatThrownBy(() -> api.sayHello(null))
            .isInstanceOf(SOAPFaultException.class)
            .hasMessageContaining("Fault occurred while processing");

        await()
            .atMost(Duration.ofSeconds(1))
            .ignoreException(MeterNotFoundException.class)
            .until(() -> registry.get("cxf.server.requests").timers(), not(empty()));
        RequiredSearch serverRequestMetrics = registry.get("cxf.server.requests");

        Map<Object, Object> serverTags = serverRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(serverTags)
            .containsOnly(
                entry("exception", "NullPointerException"),
                entry("faultCode", "UNCHECKED_APPLICATION_FAULT"),
                entry("method", "POST"),
                entry("operation", "sayHello"),
                entry("uri", "/Service/" + HELLO_SERVICE_NAME_V1),
                entry("outcome", "SERVER_ERROR"),
                entry("status", "500"));
        
        RequiredSearch clientRequestMetrics = registry.get("cxf.client.requests");

        Map<Object, Object> clientTags = clientRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(clientTags)
            .containsOnly(
                entry("exception", "None"),
                entry("faultCode", "UNCHECKED_APPLICATION_FAULT"),
                entry("method", "POST"),
                entry("operation", "sayHello"),
                entry("uri", "http://localhost:" + port + "/Service/" + HELLO_SERVICE_NAME_V1),
                entry("outcome", "SERVER_ERROR"),
                entry("status", "500"));
    }

    @Test
    public void testJaxwsProxyClientExceptionMetric() throws MalformedURLException {
        final int fakePort = Integer.parseInt(TestUtil.getPortNumber("proxy-client-exception"));
        final HelloService api = createApi(fakePort, HELLO_SERVICE_NAME_V1); 
        
        assertThatThrownBy(() -> api.sayHello("Elan"))
            .isInstanceOf(WebServiceException.class)
            .hasMessageContaining("Could not send Message");

        // no server meters
        assertThat(registry.getMeters())
            .noneMatch(m -> "cxf.server.requests".equals(m.getId().getName()));
        
        RequiredSearch clientRequestMetrics = registry.get("cxf.client.requests");

        Map<Object, Object> clientTags = clientRequestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(clientTags)
            .containsOnly(
                entry("exception", "None"),
                entry("faultCode", "RUNTIME_FAULT"),
                entry("method", "POST"),
                entry("operation", "sayHello"),
                entry("uri", "http://localhost:" + fakePort + "/Service/" + HELLO_SERVICE_NAME_V1),
                entry("outcome", "UNKNOWN"),
                entry("status", "UNKNOWN"));
    }
    
    private HelloService createApi(final int portToUse, final String serviceName) {
        final JaxWsProxyFactoryBean  factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(HelloService.class);
        factory.setFeatures(Arrays.asList(new MetricsFeature(metricsProvider)));
        factory.setAddress("http://localhost:" + portToUse + "/Service/" + serviceName);
        return factory.create(HelloService.class);
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
