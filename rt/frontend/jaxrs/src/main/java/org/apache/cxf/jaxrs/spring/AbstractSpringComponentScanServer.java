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

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@ComponentScan(
    includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = {Path.class, Provider.class })
)
public abstract class AbstractSpringComponentScanServer extends AbstractSpringConfigurationFactory {

    private List<ResourceProvider> resourceProviders = new LinkedList<ResourceProvider>();
    private List<Object> jaxrsProviders = new LinkedList<Object>();

    
    protected void setRootResources(JAXRSServerFactoryBean factory) {
        boolean checkJaxrsRoots = checkJaxrsRoots();
        boolean checkJaxrsProviders = checkJaxrsProviders(); 
        
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            if (checkJaxrsRoots && isAnnotationAvailable(beanName, Path.class)) {
                SpringResourceFactory resourceFactory = new SpringResourceFactory(beanName);
                resourceFactory.setApplicationContext(applicationContext);
                resourceProviders.add(resourceFactory);
            } else if (checkJaxrsProviders && isAnnotationAvailable(beanName, Provider.class)) {
                jaxrsProviders.add(applicationContext.getBean(beanName));
            }
        }

        factory.setResourceProviders(getResourceProviders());
    }
    
    protected <A extends Annotation> boolean isAnnotationAvailable(String beanName, Class<A> annClass) {
        return applicationContext.findAnnotationOnBean(beanName, annClass) != null;
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
    
    
}
