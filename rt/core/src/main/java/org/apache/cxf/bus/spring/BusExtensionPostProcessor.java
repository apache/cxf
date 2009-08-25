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

package org.apache.cxf.bus.spring;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.extension.BusExtension;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;

@NoJSR250Annotations
public class BusExtensionPostProcessor implements BeanPostProcessor, ApplicationContextAware, Ordered {

    private Bus bus;
    private ApplicationContext context;

    public void setApplicationContext(ApplicationContext ctx) {
        context = ctx;
    } 
    
    public int getOrder() {
        return 1001;
    }
    
        
    public Object postProcessAfterInitialization(Object bean, String beanId) throws BeansException {
        return bean;
    }

    @SuppressWarnings("unchecked")
    public Object postProcessBeforeInitialization(Object bean, String beanId) throws BeansException {
        if (null != getBus() && bean instanceof BusExtension) {
            Class cls = ((BusExtension)bean).getRegistrationType();
            getBus().setExtension(bean, cls);
        }
        return bean;
    }
    
    private Bus getBus() {
        if (bus == null) {
            bus = (Bus)context.getBean(Bus.DEFAULT_BUS_ID);
            bus.setExtension(new SpringBeanLocator(context), ConfiguredBeanLocator.class);
        }
        return bus;
    }

}
