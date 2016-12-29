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
package org.apache.cxf.tracing.brave.jaxws;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.http.DefaultSpanNameProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.tracing.brave.BraveStartInterceptor;
import org.apache.cxf.tracing.brave.BraveStopInterceptor;

@NoJSR250Annotations
@Provider(value = Type.Feature)
public class BraveFeature extends AbstractFeature {
    private BraveStartInterceptor in;
    private BraveStopInterceptor out;

    public BraveFeature(Brave brave) {
        DefaultSpanNameProvider nameProvider = new DefaultSpanNameProvider();
        in = new BraveStartInterceptor(brave, nameProvider);
        out = new BraveStopInterceptor(brave, nameProvider);
    }

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        provider.getInInterceptors().add(in);
        provider.getInFaultInterceptors().add(in);

        provider.getOutInterceptors().add(out);
        provider.getOutFaultInterceptors().add(out);
    }
}
