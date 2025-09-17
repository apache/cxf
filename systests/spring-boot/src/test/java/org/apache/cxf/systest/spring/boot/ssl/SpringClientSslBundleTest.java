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

package org.apache.cxf.systest.spring.boot.ssl;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.spring.boot.autoconfigure.ssl.CxfClientSslProperties;
import org.apache.cxf.spring.boot.autoconfigure.ssl.CxfSslBundlesAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;

/**
 * End‑to‑end TLS integration test:
 *  - Boots Spring Boot on HTTPS using server.ssl.bundle=cxf-server with mutual TLS required
 *  - Publishes a CXF JAX‑RS endpoint at /api
 *  - Configures outbound client via CxfClientSslProperties (bound from YAML)
 *  - Calls endpoint and asserts TLS handshake & invocation succeed
 */

@SpringBootTest(classes = SpringClientSslBundleTest.TestCfg.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("ssl")



public class SpringClientSslBundleTest {

    interface HelloApi {
        @GET 
        @Path("/hello") 
        @Produces(MediaType.TEXT_PLAIN)
        String hello();
    }

    @Path("/")
    public static class HelloResource implements HelloApi {
        @Override
        public String hello() { 
            return "hello"; 
        }
    }

    @Configuration
    @EnableConfigurationProperties(CxfClientSslProperties.class)
    @ImportAutoConfiguration({SslAutoConfiguration.class, CxfSslBundlesAutoConfiguration.class})
    @EnableAutoConfiguration
    static class TestCfg {
        
        
        @Bean
        public ServletWebServerFactory servletWebServerFactory() {
            return new UndertowServletWebServerFactory(0);
        }
        
        @Bean
        public org.apache.cxf.endpoint.Server jaxrsServer(Bus bus, HelloResource resource) {
            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setBus(bus);
            sf.setAddress("/api");
            sf.setServiceBean(resource);
            return sf.create();
        }

        @Bean
        public HelloResource helloResource() { 
            return new HelloResource(); 
        }
    }

    @Autowired
    private SslBundles sslBundles;

    @Autowired
    private CxfClientSslProperties clientSslProperties;

    @LocalServerPort
    private int port;

    @Autowired
    private Bus bus;


    
    
    @Test
    void testSSL() {
        BusFactory.setThreadDefaultBus(bus);
        String base = "https://localhost:" + port + "/ssl/api";

        // Create CXF client
        HelloApi api = JAXRSClientFactory.create(base, HelloApi.class);

        // Retrieve configured bundle from properties
        String bundleName = clientSslProperties.getDefaultBundle();
        assertThat(bundleName).isEqualTo("cxf-client");
        SslBundle clientBundle = sslBundles.getBundle(bundleName);
        assertThat(clientBundle).isNotNull();

        SSLContext ctx = clientBundle.createSslContext();
        assertThat(ctx).isNotNull();

        

        String resp = api.hello();
        assertThat(resp).isEqualTo("hello");
    }
}

