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

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import brave.handler.SpanHandler;
import brave.sampler.Sampler;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.Service.Mode;
import jakarta.xml.ws.WebServiceFeature;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.systest.ArrayListSpanReporter;
import org.apache.cxf.systest.jaxws.resources.HelloServiceImpl;
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
import org.springframework.test.context.ActiveProfiles;

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

@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = SpringJaxwsObservabilityTest.TestConfig.class
)
@ActiveProfiles("jaxws")
@AutoConfigureObservability
public class SpringJaxwsObservabilityTest {

    private static final String DUMMY_REQUEST_BODY = "<q0:sayHello xmlns:q0=\"http://service.ws.sample/\">"
            + "<name>Elan</name>"
            + "</q0:sayHello>";
    private static final String HELLO_SERVICE_NAME_V1 = "HelloV1";
    private static final String HELLO_SERVICE_NAME_V2 = "HelloV2";
    private static final String HELLO_SERVICE_NAME_V3 = "HelloV3";

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private ObservationRegistry observationRegistry;

    @Autowired
    private ArrayListSpanReporter arrayListSpanReporter;

    @LocalServerPort
    private int port;

    // We're excluding the autoconfig because we don't want to have HTTP related spans
    // only RPC related ones
    @EnableAutoConfiguration(exclude = WebMvcObservationAutoConfiguration.class)
    static class TestConfig {
        @Autowired
        private Bus bus;

        @Bean
        public Endpoint helloEndpoint(ObservationRegistry observationRegistry) {
            EndpointImpl endpoint = new EndpointImpl(bus, new HelloServiceImpl(), null, null, new WebServiceFeature[]{
                new ObservationFeature(observationRegistry)
            });
            endpoint.publish("/" + HELLO_SERVICE_NAME_V1);
            return endpoint;
        }

        @Bean
        public Endpoint secondHelloEndpoint(ObservationRegistry observationRegistry) {
            EndpointImpl endpoint = new EndpointImpl(bus, new HelloServiceImpl(), null, null, new WebServiceFeature[]{
                new ObservationFeature(observationRegistry)
            });
            endpoint.publish("/" + HELLO_SERVICE_NAME_V2);
            return endpoint;
        }

        @Bean
        public Endpoint thirdHelloEndpoint(ObservationRegistry observationRegistry) {
            EndpointImpl endpoint = new EndpointImpl(bus, new HelloServiceImpl(), null, null, new WebServiceFeature[]{
                new ObservationFeature(observationRegistry)
            });
            endpoint.publish("/" + HELLO_SERVICE_NAME_V3);
            return endpoint;
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

    @AfterEach
    public void clear() {
        registry.clear();
        arrayListSpanReporter.close();
    }

    @Test
    public void testJaxwsObservation() throws MalformedURLException {
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
            .until(() -> arrayListSpanReporter.getSpans(), hasSize(2));

        // Micrometer Observation with Micrometer Tracing
        SpansAssert.assertThat(arrayListSpanReporter.getSpans())
                   .haveSameTraceId()
                   .hasASpanWithName("HelloService/Invoke", spanAssert -> {
                       spanAssert.hasKindEqualTo(Kind.CLIENT)
                                 .hasTag("rpc.method", "Invoke")
                                 .hasTag("rpc.service", "HelloService")
                                 .hasTag("rpc.system", "cxf")
                                 .hasTagWithKey("server.address")
                                 .hasTagWithKey("server.port")
                                 .isStarted()
                                 .isEnded();
                   })
                   .hasASpanWithName("HelloService/sayHello", spanAssert -> {
                       spanAssert.hasKindEqualTo(Kind.SERVER)
                                 .hasTag("rpc.method", "sayHello")
                                 .hasTag("rpc.service", "HelloService")
                                 .hasTag("rpc.system", "cxf")
                                 .hasTagWithKey("server.address")
                                 .hasTagWithKey("server.port")
                                 .isStarted()
                                 .isEnded();
                   });

        // Micrometer Observation with Micrometer Core
        MeterRegistryAssert.assertThat(registry)
            .hasTimerWithNameAndTags("rpc.client.duration", Tags.of("error", "none", "rpc.method", "Invoke",
                "rpc.service", "HelloService", "rpc.system", "cxf",
                "server.address", "localhost", "server.port", String.valueOf(port)))
            .hasTimerWithNameAndTags("rpc.server.duration", Tags.of("error", "none", "rpc.method", "sayHello",
                "rpc.service", "HelloService", "rpc.system", "cxf",
                "server.address", "localhost", "server.port", String.valueOf(port)));
    }
    
    private String sendSoapRequest(String requestBody, final String serviceName) throws MalformedURLException {
        String address = "http://localhost:" + port + "/Service/" + serviceName;

        StreamSource source = new StreamSource(new StringReader(requestBody));
        Service service = Service.create(new URL(address + "?wsdl"),
             new QName("http://service.ws.sample/", "HelloService"), new ObservationClientFeature(observationRegistry));
        Dispatch<Source> dispatch = service.createDispatch(new QName("http://service.ws.sample/", "HelloPort"),
            Source.class, Mode.PAYLOAD);

        Source result = dispatch.invoke(source);
        return StaxUtils.toString(result);
    }
}
