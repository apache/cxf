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
package org.apache.cxf.systest.servlet;

import java.util.Arrays;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletUnitClient;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class SpringServletContextTest extends AbstractServletTest {
    @Configuration
    public static class AppConfig {
        @Bean(destroyMethod = "shutdown")
        public SpringBus cxf() {
            return new SpringBus();
        }
        
        @Bean @DependsOn("cxf")
        public Server jaxRsServer() {
            final JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean(); 
            factory.setServiceBeans(Arrays.asList(contextRestService()));
            factory.setAddress("/services/context");
            return factory.create();
        }

        @Bean
        public ContextRestService contextRestService() {
            return new ContextRestService();
        }
    }
    
    @Path("/") 
    public static class ContextRestService {
        @Autowired 
        private ConfigurableApplicationContext context;
        
        @GET
        @Path("/refresh")
        public void refresh() {
            context.refresh();
        }
    }
    
    @Override
    protected String getConfiguration() {
        return "/org/apache/cxf/systest/servlet/web-spring-context.xml";
    }

    @Override
    protected Bus createBus() throws BusException {
        // don't set up the bus, let the servlet do it
        return null;
    }

    @Test
    public void testContextRefresh() throws Exception {
        ServletUnitClient client = newClient();
        client.setExceptionsThrownOnErrorStatus(true);

        for (int i = 0; i < 5; ++i) {
            final WebRequest request = new GetMethodQueryWebRequest(CONTEXT_URL + "/services/context/refresh");
            final WebResponse response = client.getResponse(request);
            assertThat(response.getResponseCode(), equalTo(204));
        }
    }
}