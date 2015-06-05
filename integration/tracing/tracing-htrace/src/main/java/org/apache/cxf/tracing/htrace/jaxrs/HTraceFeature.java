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
package org.apache.cxf.tracing.htrace.jaxrs;

import java.util.Arrays;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.htrace.HTraceConfiguration;
import org.apache.htrace.Sampler;
import org.apache.htrace.SamplerBuilder;
import org.apache.htrace.SpanReceiver;
import org.apache.htrace.SpanReceiverBuilder;
import org.apache.htrace.Trace;

public class HTraceFeature extends AbstractFeature {
    private HTraceConfiguration configuration;
    
    public HTraceFeature() {
        this(HTraceConfiguration.EMPTY);
    }

    public HTraceFeature(final HTraceConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void initialize(final Server server, final Bus bus) {
        final ServerProviderFactory providerFactory = (ServerProviderFactory)server
            .getEndpoint()
            .get(ServerProviderFactory.class.getName());

        final Sampler< ? > sampler = new SamplerBuilder(configuration).build();
        final SpanReceiver spanReceiver = new SpanReceiverBuilder(configuration).build();
        
        if (spanReceiver != null) {
            Trace.addReceiver(spanReceiver);
        }

        if (providerFactory != null) {
            providerFactory.setUserProviders(Arrays.asList(new HTraceProvider(sampler), 
                new HTraceContextProvider(sampler)));
        }
    }
    
    public void setConfiguration(final HTraceConfiguration configuration) {
        this.configuration = configuration;
    }
    
    public HTraceConfiguration getConfiguration() {
        return configuration;
    }
}
