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
package org.apache.cxf.validation;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

@Provider(value = Type.Feature, scope = Scope.Client)
public class ClientBeanValidationFeature extends AbstractFeature {
    private final Portable delegate = getDelegate();

    protected Portable getDelegate() {
        return new Portable();
    }

    public void addInterceptor(InterceptorProvider interceptorProvider, ClientBeanValidationOutInterceptor out) {
        delegate.addInterceptor(interceptorProvider, out);
    }

    public void setProvider(BeanValidationProvider provider) {
        delegate.setProvider(provider);
    }

    @Override
    public void initialize(Server server, Bus bus) {
        delegate.initialize(server, bus);
    }

    @Override
    public void initialize(Client client, Bus bus) {
        delegate.initialize(client, bus);
    }

    @Override
    public void initialize(InterceptorProvider interceptorProvider, Bus bus) {
        delegate.initialize(interceptorProvider, bus);
    }

    @Override
    public void initialize(Bus bus) {
        delegate.initialize(bus);
    }

    @Override
    protected void initializeProvider(InterceptorProvider interceptorProvider, Bus bus) {
        delegate.doInitializeProvider(interceptorProvider, bus);
    }

    @Provider(value = Type.Feature, scope = Scope.Client)
    public static class Portable implements AbstractPortableFeature {
        private BeanValidationProvider validationProvider;

        @Override
        public void doInitializeProvider(InterceptorProvider interceptorProvider, Bus bus) {
            ClientBeanValidationOutInterceptor out = new ClientBeanValidationOutInterceptor();
            addInterceptor(interceptorProvider, out);
        }

        protected void addInterceptor(InterceptorProvider interceptorProvider, ClientBeanValidationOutInterceptor out) {
            if (validationProvider != null) {
                out.setProvider(validationProvider);
            }
            interceptorProvider.getOutInterceptors().add(out);

        }

        public void setProvider(BeanValidationProvider provider) {
            this.validationProvider = provider;
        }
    }
}
