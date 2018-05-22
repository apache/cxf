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

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Singleton;

import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.message.Message;

public class CdiResourceProvider implements ResourceProvider {
    private final boolean singleton;
    private final Lifecycle lifecycle;

    private final BeanManager beanManager;
    private final Bean< ? > bean;

    CdiResourceProvider(final BeanManager beanManager, final Bean< ? > bean) {
        this.beanManager = beanManager;
        this.bean = bean;

        // normal scoped beans will return us a proxy in getInstance so it is singletons for us,
        // @Singleton is indeed a singleton
        // @Dependent should be a request scoped instance but for backward compat we kept it a singleton
        //
        // other scopes are considered request scoped (for jaxrs)
        // and are created per request (getInstance/releaseInstance)
        this.singleton = beanManager.isNormalScope(bean.getScope()) || isConsideredSingleton();
        this.lifecycle = singleton ? new Lifecycle(beanManager, bean) {
            private Object instance;

            @Override
            protected Object create(final Message m) {
                if (instance == null) {
                    instance = super.create(null);
                }
                return instance;
            }
        } : null;
    }

    Lifecycle getLifecycle() {
        return lifecycle;
    }

    @Override
    public Object getInstance(final Message m) {
        if (singleton) {
            return lifecycle.create(m);
        }
        return new Lifecycle(beanManager, bean) {
            @Override
            protected Object create(final Message m) {
                final Object instance = super.create(m);
                m.put(Lifecycle.class, this);
                return instance;
            }
        }.create(m);
    }

    @Override
    public void releaseInstance(final Message m, final Object o) {
        if (singleton) {
            return;
        }
        final Lifecycle contextualLifecycle = m.get(Lifecycle.class);
        if (contextualLifecycle != null) {
            contextualLifecycle.destroy();
        }
    }

    @Override
    public Class<?> getResourceClass() {
        return bean.getBeanClass();
    }

    @Override
    public boolean isSingleton() {
        return singleton;
    }

    // warn: several impls use @Dependent == request so we should probably add a flag
    private boolean isConsideredSingleton() {
        return Singleton.class == bean.getScope() || Dependent.class == bean.getScope();
    }

    public abstract static class Lifecycle {
        BeanManager beanManager;
        Bean<?> bean;
        Object instance;
        CreationalContext< ? > context;

        protected Lifecycle(final BeanManager beanManager, final Bean<?> bean) {
            this.beanManager = beanManager;
            this.bean = bean;
        }

        protected Object create(final Message m) {
            context = beanManager.createCreationalContext(bean);
            instance = beanManager.getReference(bean, bean.getBeanClass(), context);
            return instance;
        }

        protected void destroy() {
            if (context != null) {
                context.release();
                instance = null;
                context = null;
            }
        }
    }
}
