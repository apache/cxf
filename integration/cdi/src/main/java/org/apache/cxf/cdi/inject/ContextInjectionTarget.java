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

package org.apache.cxf.cdi.inject;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;

import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

public final class ContextInjectionTarget<X> implements InjectionTarget<X> {
    private final InjectionTarget<X> delegate;
    private final Class<?> providerClass;
    
    public ContextInjectionTarget(final InjectionTarget<X> delegate, final Class<?> providerClass) {
        this.delegate = delegate;
        this.providerClass = providerClass;
    }
    
    @Override
    public void inject(X instance, CreationalContext<X> ctx) {
        delegate.inject(instance, ctx);
        
        final Message message = PhaseInterceptorChain.getCurrentMessage();
        if (message != null) {
            final ServerProviderFactory factory = ServerProviderFactory.getInstance(message);
            factory.injectContextProxiesIntoProvider(providerClass, instance);
        }
    }

    @Override
    public void postConstruct(X instance) {
        delegate.postConstruct(instance);
    }

    @Override
    public void preDestroy(X instance) {
        delegate.dispose(instance);
    }

    @Override
    public void dispose(X instance) {
        delegate.dispose(instance);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return delegate.getInjectionPoints();
    }

    @Override
    public X produce(CreationalContext<X> ctx) {
        return delegate.produce(ctx);
    }
}
