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
package org.apache.cxf.spring.boot.autoconfigure;

import java.util.Map;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.spring.SpringComponentScanServer;
import org.apache.cxf.jaxrs.spring.SpringJaxrsClassesScanServer;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration} for Apache CXF.
 *
 * @author Vedran Pavic
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass({ SpringBus.class, CXFServlet.class })
@EnableConfigurationProperties(CxfProperties.class)
@AutoConfigureAfter(name = {
    "org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration", // Spring Boot 1.x
    "org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration" // Spring Boot 2.x
})
public class CxfAutoConfiguration {

    @Autowired
    private CxfProperties properties;

    @Bean
    @ConditionalOnMissingBean(name = "cxfServletRegistration")
    @ConditionalOnProperty(prefix = "cxf", name = "servlet.enabled", matchIfMissing = true)
    public ServletRegistrationBean<CXFServlet> cxfServletRegistration() {
        String path = this.properties.getPath();
        String urlMapping = path.endsWith("/") ? path + "*" : path + "/*";
        ServletRegistrationBean<CXFServlet> registration = new ServletRegistrationBean<>(
                new CXFServlet(), urlMapping);
        CxfProperties.Servlet servletProperties = this.properties.getServlet();
        registration.setLoadOnStartup(servletProperties.getLoadOnStartup());
        for (Map.Entry<String, String> entry : servletProperties.getInit().entrySet()) {
            registration.addInitParameter(entry.getKey(), entry.getValue());
        }
        return registration;
    }

    @Configuration
    @ConditionalOnMissingBean(SpringBus.class)
    @ImportResource("classpath:META-INF/cxf/cxf.xml")
    protected static class SpringBusConfiguration {

    }

    @Configuration
    @ConditionalOnClass(JAXRSServerFactoryBean.class)
    @ConditionalOnExpression("'${cxf.jaxrs.component-scan}'=='true' && '${cxf.jaxrs.classes-scan}'!='true'")
    @ConditionalOnMissingBean(Server.class)
    @Import(SpringComponentScanServer.class)
    protected static class JaxRsComponentConfiguration {

    }

    @Configuration
    @ConditionalOnClass(JAXRSServerFactoryBean.class)
    @ConditionalOnExpression("'${cxf.jaxrs.classes-scan}'=='true' && '${cxf.jaxrs.component-scan}'!='true'")
    @ConditionalOnMissingBean(Server.class)
    @Import(SpringJaxrsClassesScanServer.class)
    protected static class JaxRsClassesConfiguration {

    }

}
