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
package demo.jaxrs.sse;

import javax.inject.Inject;
import javax.ws.rs.ext.RuntimeDelegate;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.transport.sse.SseHttpTransportFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
@ComponentScan(basePackageClasses = StatsRestServiceImpl.class)
public class StatsConfig {
    @Inject 
    private StatsRestServiceImpl statsRestService;
    
    @Bean(destroyMethod = "shutdown")
    SpringBus cxf() {
        return new SpringBus();
    }

    @Bean @DependsOn("cxf")
    Server jaxRsServer() {
        final JAXRSServerFactoryBean factory = RuntimeDelegate
            .getInstance()
            .createEndpoint(new StatsApplication(), JAXRSServerFactoryBean.class);
        factory.setServiceBean(statsRestService);
        factory.setProvider(new JacksonJsonProvider());
        factory.setTransportId(SseHttpTransportFactory.TRANSPORT_ID);
        return factory.create();
    }
}
