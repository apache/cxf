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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class SpringResourceFactory implements ResourceProvider, ApplicationContextAware {

    private Constructor<?> c;
    private ApplicationContext ac;
    private String beanId;
    private Method postConstructMethod;
    private Method preDestroyMethod;
    private boolean isSingleton;
    
    public SpringResourceFactory() {
        
    }
    
    public SpringResourceFactory(String name) {
        beanId = name;
    }
    
    private void init() {
        Class<?> type = ClassHelper.getRealClassFromClass(ac.getType(beanId));
        if (Proxy.isProxyClass(type)) {
            type = ClassHelper.getRealClass(ac.getBean(beanId));
        }
        c = ResourceUtils.findResourceConstructor(type, !isSingleton());
        if (c == null) {
            throw new RuntimeException("Resource class " + type
                                       + " has no valid constructor");
        }
        postConstructMethod = ResourceUtils.findPostConstructMethod(type);
        preDestroyMethod = ResourceUtils.findPreDestroyMethod(type);
        isSingleton = ac.isSingleton(beanId);
    }
    
    public Object getInstance(Message m) {
        Object[] values = ResourceUtils.createConstructorArguments(c, m);
        Object instance = values.length > 0 ? ac.getBean(beanId, values) : ac.getBean(beanId);
        if (!isSingleton || m == null) {
            InjectionUtils.invokeLifeCycleMethod(instance, postConstructMethod);
        }
        return instance;
    }

    public boolean isSingleton() {
        return isSingleton; 
    }

    public void releaseInstance(Message m, Object o) {
        if (!isSingleton) {
            InjectionUtils.invokeLifeCycleMethod(o, preDestroyMethod);
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ac = applicationContext;
        init();
    }

    public void setBeanId(String serviceBeanId) {
        this.beanId = serviceBeanId;
    }

    public ApplicationContext getApplicationContext() {
        return ac;    
    }
    
    Constructor getBeanConstructor() {
        return c;
    }

    public Class<?> getResourceClass() {
        return c.getDeclaringClass();
    }

}
