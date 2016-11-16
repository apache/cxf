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
package org.apache.cxf.cdi;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.message.Message;

public class CdiResourceProvider implements ResourceProvider {
    private Object instance;
    private CreationalContext< ? > context;
    
    private final BeanManager beanManager;
    private final Bean< ? > bean;
    
    CdiResourceProvider(final BeanManager beanManager, final Bean< ? > bean) {
        this.beanManager = beanManager;
        this.bean = bean;
    }
    
    @Override
    public Object getInstance(Message m) {
        if (instance == null) {
            context = beanManager.createCreationalContext(bean);
            instance = beanManager.getReference(bean, bean.getBeanClass(), context);
        }
        
        return instance;
    }

    @Override
    public void releaseInstance(Message m, Object o) {
        if (context != null) {
            context.release();
            instance = null;
        }
    }

    @Override
    public Class<?> getResourceClass() {
        return bean.getBeanClass();
    }

    @Override
    public boolean isSingleton() {
        return !bean.getScope().isAssignableFrom(RequestScoped.class);
    }
}
