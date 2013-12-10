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
package org.apache.cxf.jaxrs.spring;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@Import(JaxRsConfig.class)
@ComponentScan
public class SpringResourceServer {
    @Autowired
    protected ApplicationContext applicationContext;

    private String address = "/";
    private Set<String> supportedBeanNames;
    private List<ResourceProvider> resourceProviders = new LinkedList<ResourceProvider>();
    private List<Object> jaxrsProviders = new LinkedList<Object>();
    
    @Bean
    public Server jaxRsServer() {
        boolean checkJaxrsRoots = checkJaxrsRoots();
        boolean checkJaxrsProviders = checkJaxrsProviders(); 
        
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            if (checkJaxrsRoots && isAnnotationAvailable(beanName, Path.class)) {
                SpringResourceFactory factory = new SpringResourceFactory(beanName);
                factory.setApplicationContext(applicationContext);
                resourceProviders.add(factory);
            } else if (checkJaxrsProviders && isAnnotationAvailable(beanName, Provider.class)) {
                jaxrsProviders.add(applicationContext.getBean(beanName));
            }
        }

        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setBus(applicationContext.getBean(SpringBus.class));
        factory.setResourceProviders(getResourceProviders());
        factory.setProviders(getJaxrsProviders());
        factory.setAddress(getAddress());
        finalizeFactorySetup(factory);
        return factory.create();
    }
    
    protected <A extends Annotation> boolean isAnnotationAvailable(String beanName, Class<A> annClass) {
        return isBeanSupported(beanName) 
            && applicationContext.findAnnotationOnBean(beanName, annClass) != null;
    }
    
    protected void finalizeFactorySetup(JAXRSServerFactoryBean factory) {
        // complete
    }
    
    protected boolean checkJaxrsProviders() {
        return true;    
    }
    
    protected boolean checkJaxrsRoots() {
        return true;    
    }

    protected List<ResourceProvider> getResourceProviders() {
        return resourceProviders;
    }
    
    protected List<Object> getJaxrsProviders() {
        return jaxrsProviders;
    }
    
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Set<String> getSupportedBeanNames() {
        return supportedBeanNames;
    }

    public void setSupportedBeanNames(Set<String> supportedBeanNames) {
        this.supportedBeanNames = supportedBeanNames;
    }
    
    protected boolean isBeanSupported(String beanName) {
        return supportedBeanNames == null || supportedBeanNames.contains(beanName);
    }
}
