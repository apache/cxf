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

import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.Configurable;
import jakarta.ws.rs.core.FeatureContext;
import org.apache.cxf.cdi.event.DisposableCreationalContext;
import org.apache.cxf.jaxrs.impl.ConfigurableImpl;
import org.apache.cxf.jaxrs.impl.ConfigurableImpl.Instantiator;
import org.apache.cxf.jaxrs.impl.ConfigurationImpl;
import org.apache.cxf.jaxrs.provider.ServerConfigurableFactory;

/** 
 * Creates the instance of Configurable<?> suitable for CDI-managed runtime.
 */
public class CdiServerConfigurableFactory implements ServerConfigurableFactory {
    private final BeanManager beanManager;
    
    CdiServerConfigurableFactory(final BeanManager beanManager) {
        this.beanManager = beanManager;
    }
    
    @Override
    public Configurable<FeatureContext> create(FeatureContext context) {
        return new CdiServerFeatureContextConfigurable(context, beanManager);
    }
    
    /** 
     * Instantiates the instance of the provider using CDI/BeanManager (or fall back
     * to default strategy of CDI bean is not available).
     */
    private static class CdiInstantiator implements Instantiator {
        private final BeanManager beanManager;
        
        CdiInstantiator(final BeanManager beanManager) {
            this.beanManager = beanManager;
        }
        
        @Override
        public <T> Object create(Class<T> cls) {
            final Set<Bean<?>> candidates = beanManager.getBeans(cls);
            final Bean<?> bean = beanManager.resolve(candidates);

            if (bean != null) {
                final CreationalContext<?> context = beanManager.createCreationalContext(bean);
                
                if (!beanManager.isNormalScope(bean.getScope())) {
                    beanManager.getEvent().fire(new DisposableCreationalContext(context));
                }

                return beanManager.getReference(bean, cls, context);
            } else {
                // No CDI bean available, falling back to default instantiation strategy
                return ConfigurationImpl.createProvider(cls);
            }
        }
    }
    
    private static class CdiServerFeatureContextConfigurable extends ConfigurableImpl<FeatureContext> {
        private final Instantiator instantiator;
        
        CdiServerFeatureContextConfigurable(FeatureContext mc, BeanManager beanManager) {
            super(mc, RuntimeType.SERVER);
            this.instantiator = new CdiInstantiator(beanManager);
        }
        
        @Override
        protected Instantiator getInstantiator() {
            return instantiator;
        }
    }
}
