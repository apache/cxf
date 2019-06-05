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
package org.apache.cxf.feature;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.InterceptorProvider;

/**
 * Enable to convert a {@link AbstractPortableFeature} to a {@link AbstractFeature}.
 *
 * @param <T> the "portable" feature.
 */
public class DelegatingFeature<T extends AbstractPortableFeature> extends AbstractFeature {
    protected T delegate;

    protected DelegatingFeature(final T d) {
        delegate = d == null ? getDelegate() : d;
    }

    protected T getDelegate() { // useful for inheritance
        return delegate;
    }

    public void setDelegate(T delegate) {
        this.delegate = delegate;
    }

    @Override
    public void initialize(final Server server, final Bus bus) {
        delegate.initialize(server, bus);
    }

    @Override
    public void initialize(final Client client, final Bus bus) {
        delegate.initialize(client, bus);
    }

    @Override
    public void initialize(final InterceptorProvider interceptorProvider, final Bus bus) {
        delegate.initialize(interceptorProvider, bus);
    }

    @Override
    public void initialize(final Bus bus) {
        delegate.initialize(bus);
    }

    @Override
    protected void initializeProvider(final InterceptorProvider interceptorProvider, final Bus bus) {
        delegate.doInitializeProvider(interceptorProvider, bus);
    }
}
