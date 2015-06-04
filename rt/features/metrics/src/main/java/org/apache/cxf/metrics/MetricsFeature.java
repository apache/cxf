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

package org.apache.cxf.metrics;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.metrics.interceptors.CountingOutInterceptor;
import org.apache.cxf.metrics.interceptors.MetricsMessageClientOutInterceptor;
import org.apache.cxf.metrics.interceptors.MetricsMessageInInterceptor;
import org.apache.cxf.metrics.interceptors.MetricsMessageInOneWayInterceptor;
import org.apache.cxf.metrics.interceptors.MetricsMessageInPostInvokeInterceptor;
import org.apache.cxf.metrics.interceptors.MetricsMessageInPreInvokeInterceptor;
import org.apache.cxf.metrics.interceptors.MetricsMessageOutInterceptor;

/**
 * 
 */
@NoJSR250Annotations
public class MetricsFeature extends AbstractFeature {
    final MetricsProvider[] providers;
    
    public MetricsFeature() {
        this.providers = null;
    }
    public MetricsFeature(MetricsProvider provider) {
        this.providers = new MetricsProvider[] {provider};
    }
    public MetricsFeature(MetricsProvider ... providers) {
        this.providers = providers.length > 0 ? providers : null;
    }
    
    @Override
    public void initialize(Server server, Bus bus) {
        //can optimize for server case and just put interceptors it needs
        Endpoint provider = server.getEndpoint();
        MetricsMessageOutInterceptor out = new MetricsMessageOutInterceptor(providers);
        CountingOutInterceptor countingOut = new CountingOutInterceptor();
        
        provider.getInInterceptors().add(new MetricsMessageInInterceptor(providers));
        provider.getInInterceptors().add(new MetricsMessageInOneWayInterceptor(providers));
        provider.getInInterceptors().add(new MetricsMessageInPreInvokeInterceptor(providers));
        
        provider.getOutInterceptors().add(countingOut);
        provider.getOutInterceptors().add(out);
        provider.getOutFaultInterceptors().add(countingOut);
        provider.getOutFaultInterceptors().add(out);
    }
    
    @Override
    public void initialize(Client client, Bus bus) {
        //can optimize for client case and just put interceptors it needs
        MetricsMessageOutInterceptor out = new MetricsMessageOutInterceptor(providers);
        CountingOutInterceptor countingOut = new CountingOutInterceptor();
        
        client.getInInterceptors().add(new MetricsMessageInInterceptor(providers));
        client.getInInterceptors().add(new MetricsMessageInPostInvokeInterceptor(providers));
        client.getInFaultInterceptors().add(new MetricsMessageInPostInvokeInterceptor(providers));
        client.getOutInterceptors().add(countingOut);
        client.getOutInterceptors().add(out);
        client.getOutInterceptors().add(new MetricsMessageClientOutInterceptor(providers));
    }
    
    
    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        //if feature is added to the bus, we need to add all the interceptors
        MetricsMessageOutInterceptor out = new MetricsMessageOutInterceptor(providers);
        CountingOutInterceptor countingOut = new CountingOutInterceptor();
        
        provider.getInInterceptors().add(new MetricsMessageInInterceptor(providers));
        provider.getInInterceptors().add(new MetricsMessageInOneWayInterceptor(providers));
        provider.getInInterceptors().add(new MetricsMessageInPreInvokeInterceptor(providers));
        provider.getInInterceptors().add(new MetricsMessageInPostInvokeInterceptor(providers));
        provider.getInFaultInterceptors().add(new MetricsMessageInPreInvokeInterceptor(providers));
        provider.getInFaultInterceptors().add(new MetricsMessageInPostInvokeInterceptor(providers));
        
        provider.getOutInterceptors().add(countingOut);
        provider.getOutInterceptors().add(out);
        provider.getOutInterceptors().add(new MetricsMessageClientOutInterceptor(providers));
        provider.getOutFaultInterceptors().add(countingOut);
        provider.getOutFaultInterceptors().add(out);
    }
}
