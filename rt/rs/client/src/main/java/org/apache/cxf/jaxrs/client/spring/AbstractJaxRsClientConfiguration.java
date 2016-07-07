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
package org.apache.cxf.jaxrs.client.spring;

import javax.ws.rs.ext.Provider;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.spring.JaxRsConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

@Import(JaxRsConfig.class)
@ComponentScan(
               includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, 
                                                      value = {Provider.class})
           )
public abstract class AbstractJaxRsClientConfiguration implements ApplicationContextAware {

    protected ApplicationContext context;
    @Autowired
    private Bus bus;
    @Value("${cxf.jaxrs.client.address}")
    private String address;
    @Value("${cxf.jaxrs.client.thread-safe:false}")
    private Boolean threadSafe;
    
    
    protected Client createClient() {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setBus(bus);        
        bean.setAddress(address);
        bean.setThreadSafe(threadSafe);
        setJaxrsResources(bean);
        
        for (String beanName : context.getBeanDefinitionNames()) {
            if (context.findAnnotationOnBean(beanName, Provider.class) != null) {
                bean.setProvider(context.getBean(beanName));
            } 
        }
        
        return bean.create();
    }
    
    protected abstract void setJaxrsResources(JAXRSClientFactoryBean factory);
    
    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        this.context = ac;
        
    }
}
