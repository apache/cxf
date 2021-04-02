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
 * A Feature is something that is able to customize a Server, Client, or Bus, typically
 * adding capabilities. For instance, there may be a LoggingFeature which configures
 * one of the above to log each of their messages.
 * <p>
 * By default the initialize methods all delegate to initializeProvider(InterceptorProvider).
 * If you're simply adding interceptors to a Server, Client, or Bus, this allows you to add
 * them easily.
 */
public class WrappedFeature extends AbstractFeature {
    final Feature wrapped;
    public WrappedFeature(Feature f) {
        wrapped = f;
    }

    @Override
    public void initialize(Server server, Bus bus) {
        wrapped.initialize(server, bus);
    }

    @Override
    public void initialize(Client client, Bus bus) {
        wrapped.initialize(client, bus);
    }

    @Override
    public void initialize(InterceptorProvider interceptorProvider, Bus bus) {
        wrapped.initialize(interceptorProvider, bus);
    }

    @Override
    public void initialize(Bus bus) {
        wrapped.initialize(bus);
    }

}
