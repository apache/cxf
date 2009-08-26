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
import org.apache.cxf.common.injection.ResourceInjector;
import org.apache.cxf.resource.ResourceManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;

public class Jsr250BeanPostProcessor 
    implements DestructionAwareBeanPostProcessor, Ordered, ApplicationContextAware {

    private ResourceManager resourceManager;
    private ApplicationContext context;

    private boolean isProcessing = true;
    //private int count;
    //private int count2;

    Jsr250BeanPostProcessor() {
    }
    
    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        context = arg0;  
        try {
            Class<?> cls = Class
                .forName("org.springframework.context.annotation.CommonAnnotationBeanPostProcessor");
            isProcessing = context.getBeanNamesForType(cls, true, false).length == 0;
        } catch (ClassNotFoundException e) {
            isProcessing = true;
        }
    }
    
    public int getOrder() {
        return 1010;
    }
        
    private boolean injectable(Object bean, String beanId) {
        return !"cxf".equals(beanId) && ResourceInjector.processable(bean.getClass(), bean);
    }
    private ResourceManager getResourceManager(Object bean) {
        if (resourceManager == null) {
            if (bean instanceof ResourceManager) {
                resourceManager = (ResourceManager)bean;
                resourceManager.addResourceResolver(new BusApplicationContextResourceResolver(context));
            } else {
                Bus b = (Bus)context.getBean("cxf");
                ResourceManager m = b.getExtension(ResourceManager.class);
                if (resourceManager == null && m != null) {
                    resourceManager = m;
                    resourceManager.addResourceResolver(new BusApplicationContextResourceResolver(context));
                }
            }
        }
        return resourceManager;
    }
    public Object postProcessAfterInitialization(Object bean, String beanId) throws BeansException {
        if (!isProcessing) {
            return bean;
        }
        if (bean != null 
            && injectable(bean, beanId)) {
            new ResourceInjector(getResourceManager(bean)).construct(bean);
        }
        return bean;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanId) throws BeansException {
        if (!isProcessing) {
            return bean;
        }
        if (bean != null 
            && injectable(bean, beanId)) {
            new ResourceInjector(getResourceManager(bean)).inject(bean);
            /*
            System.out.println("p :" + (++count) + ": " + bean.getClass().getName() + " " + beanId);
        } else if (bean != null) {
            System.out.println("np: " + (++count2) 
                               + ": " + bean.getClass().getName() + " " + beanId);
            */
        }
        return bean;
    }

    public void postProcessBeforeDestruction(Object bean, String beanId) {
        if (!isProcessing) {
            return;
        }
        if (bean != null 
            && injectable(bean, beanId)) {
            new ResourceInjector(getResourceManager(bean)).destroy(bean);
        }
    }

}
