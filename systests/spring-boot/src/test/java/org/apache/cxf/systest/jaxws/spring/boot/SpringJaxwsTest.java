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
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Service;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.metrics.MetricsFeature;
import org.apache.cxf.metrics.MetricsProvider;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.systest.jaxws.resources.HelloServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.entry;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = SpringJaxwsTest.TestConfig.class)
@ActiveProfiles("jaxws")
@TestPropertySource(properties = {"cxf.metrics.server.max-uri-tags=2"})
public class SpringJaxwsTest {

    private static final String DUMMY_REQUEST_BODY = "<q0:sayHello xmlns:q0=\"http://service.ws.sample/\">"
            + "<name>Elan</name>"
            + "</q0:sayHello>";
    private static final String HELLO_SERVICE_NAME_V1 = "HelloV1";
    private static final String HELLO_SERVICE_NAME_V2 = "HelloV2";
    private static final String HELLO_SERVICE_NAME_V3 = "HelloV3";

    @Rule
    public OutputCaptureRule output = new OutputCaptureRule();

    @Autowired
    MeterRegistry registry;

    @LocalServerPort
    private int port;

    @Configuration
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

    @Before
    public void setUp() {
        this.registry.getMeters().forEach(meter -> registry.remove(meter));
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

        RequiredSearch requestMetrics = registry.get("cxf.server.requests");

        Map<Object, Object> tags = requestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(tags)
                .containsOnly(
                        entry("exception", "None"),
                        entry("faultCode", "None"),
                        entry("method", "POST"),
                        entry("operation", "sayHello"),
                        entry("uri", "/Service/" + HELLO_SERVICE_NAME_V1),
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


        RequiredSearch requestMetrics = registry.get("cxf.server.requests");

        Map<Object, Object> tags = requestMetrics.timer().getId().getTags().stream()
                .collect(toMap(Tag::getKey, Tag::getValue));

        assertThat(tags)
                .containsOnly(
                        entry("exception", "NullPointerException"),
                        entry("faultCode", "UNCHECKED_APPLICATION_FAULT"),
                        entry("method", "POST"),
                        entry("operation", "sayHello"),
                        entry("uri", "/Service/" + HELLO_SERVICE_NAME_V1),
                        entry("outcome", "SERVER_ERROR"),
                        entry("status", "500"));
    }

    @Test
    public void testAfterMaxUrisReachedFurtherUrisAreDenied() throws MalformedURLException {
        // given in setUp

        // when
        sendSoapRequest(DUMMY_REQUEST_BODY, HELLO_SERVICE_NAME_V1);
        sendSoapRequest(DUMMY_REQUEST_BODY, HELLO_SERVICE_NAME_V2);
        sendSoapRequest(DUMMY_REQUEST_BODY, HELLO_SERVICE_NAME_V3);

        // then
        assertThat(registry.get("cxf.server.requests").meters()).hasSize(2);
        assertThat(this.output).contains("Reached the maximum number of URI tags " + "for 'cxf.server.requests'");
    }

    @Test
    public void testDoesNotDenyNorLogIfMaxUrisIsNotReached() throws MalformedURLException {
        // given in setUp

        // when
        sendSoapRequest(DUMMY_REQUEST_BODY, HELLO_SERVICE_NAME_V1);

        // then
        assertThat(registry.get("cxf.server.requests").meters()).hasSize(1);
        assertThat(this.output).doesNotContain("Reached the maximum number of URI tags " + "for 'cxf.server.requests'");

    }

    private String sendSoapRequest(String requestBody, final String serviceName) throws MalformedURLException {
        String address = "http://localhost:" + port + "/Service/" + serviceName;

        StreamSource source = new StreamSource(new StringReader(requestBody));
        Service service = Service.create(new URL(address + "?wsdl"),
                new QName("http://service.ws.sample/", "HelloService"));
        Dispatch<Source> dispatch = service.createDispatch(new QName("http://service.ws.sample/", "HelloPort"),
                Source.class, Mode.PAYLOAD);

        Source result = dispatch.invoke(source);
        return StaxUtils.toString(result);
    }
}
