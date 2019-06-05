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
package org.apache.cxf.binding.coloc.feature;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.coloc.ColocInInterceptor;
import org.apache.cxf.binding.coloc.ColocOutInterceptor;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.DeferredConduitSelector;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

@NoJSR250Annotations
public class ColocFeature extends DelegatingFeature<ColocFeature.Portable> {

    public ColocFeature() {
        super(new Portable());
    }

    public static class Portable implements AbstractPortableFeature {
        @Override
        public void initialize(Client client, Bus bus) {
            ConduitSelector selector = new DeferredConduitSelector();
            selector.setEndpoint(client.getEndpoint());
            client.setConduitSelector(selector);
            doInitializeProvider(client, bus);
        }

        @Override
        public void doInitializeProvider(InterceptorProvider provider, Bus bus) {
            provider.getInInterceptors().add(new ColocInInterceptor());
            provider.getOutInterceptors().add(new ColocOutInterceptor(bus));
        }
    }
}
