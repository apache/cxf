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
package sample.rs.service;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet;
import org.apache.cxf.jaxrs.spring.JaxRsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(JaxRsConfig.class)
public class SampleScanRestApplication2 {
    public static void main(String[] args) {
        SpringApplication.run(SampleScanRestApplication2.class, args);
    }
 
    @Bean
    public ServletRegistrationBean servletRegistrationBean(ApplicationContext context) {
        Application app = (Application)context.getBean("helloApp");
        @SuppressWarnings("serial")
        CXFNonSpringJaxrsServlet servlet = new CXFNonSpringJaxrsServlet(app) {
            @Override
            protected boolean isIgnoreApplicationPath(ServletConfig servletConfig) {
                return false;
            }
            
        };
        return new ServletRegistrationBean(servlet, "/*");
    }
 
    
    @Bean
    public Application helloApp() {
        return new JaxrsApplication();
    }

    @ApplicationPath("/services/helloservice")
    public static class JaxrsApplication extends Application { 
        public Set<Object> getSingletons() {
            return new HashSet<Object>(Arrays.asList(new HelloService(), new HelloService2()));
        }
    }
    
}
