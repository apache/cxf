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

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.Service.Mode;
import jakarta.xml.ws.WebServiceFeature;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.systest.jaxws.resources.HelloService;
import org.apache.cxf.systest.jaxws.resources.HelloServiceWithContextImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = SpringJaxwsInjectionTest.TestConfig.class)
@ActiveProfiles("jaxws")
public class SpringJaxwsInjectionTest {

    private static final String DUMMY_REQUEST_BODY = "<q0:sayHello xmlns:q0=\"http://service.ws.sample/\">"
            + "<name>Elan</name>"
            + "</q0:sayHello>";
    private static final String HELLO_SERVICE_NAME = "Hello";

    @LocalServerPort
    private int port;

    @EnableAutoConfiguration
    static class TestConfig {
        @Autowired
        private Bus bus;

        @Bean
        public HelloService helloService() {
            return new HelloServiceWithContextImpl();
        }
        
        @Bean
        public Endpoint helloEndpoint(HelloService helloService) {
            EndpointImpl endpoint = new EndpointImpl(bus, helloService, null, null, new WebServiceFeature[0]);
            endpoint.publish("/" + HELLO_SERVICE_NAME);
            return endpoint;
        }
    }

    @Test
    public void testJaxwsWebServiceContext() throws MalformedURLException {
        // given in setUp

        // when
        String actual = sendSoapRequest(DUMMY_REQUEST_BODY, HELLO_SERVICE_NAME);

        // then
        assertThat(actual)
                .isEqualTo("<ns2:sayHelloResponse xmlns:ns2=\"http://service.ws.sample/\">"
                        + "<return>Hello, Elan</return>"
                        + "</ns2:sayHelloResponse>");
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
