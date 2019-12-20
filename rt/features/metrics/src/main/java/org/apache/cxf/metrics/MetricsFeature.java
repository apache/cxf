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

import java.lang.reflect.Constructor;
import java.util.Collection;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
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
@Provider(Type.Feature)
public class MetricsFeature extends DelegatingFeature<MetricsFeature.Portable> {
    public MetricsFeature() {
        super(new Portable());
    }
    public MetricsFeature(MetricsProvider provider) {
        super(new Portable(provider));
    }
    public MetricsFeature(MetricsProvider ... providers) {
        super(new Portable(providers));
    }

    public static class Portable implements AbstractPortableFeature {
        MetricsProvider[] providers;

        public Portable() {
            this.providers = null;
        }
        public Portable(MetricsProvider provider) {
            this.providers = new MetricsProvider[] {provider};
        }
        public Portable(MetricsProvider ... providers) {
            this.providers = providers.length > 0 ? providers : null;
        }

        @Override
        public void initialize(Server server, Bus bus) {
            createDefaultProvidersIfNeeded(bus);
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
            createDefaultProvidersIfNeeded(bus);
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
        public void doInitializeProvider(InterceptorProvider provider, Bus bus) {
            createDefaultProvidersIfNeeded(bus);
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
        private void createDefaultProvidersIfNeeded(Bus bus) {
            if (providers == null) {
                ConfiguredBeanLocator b = bus.getExtension(ConfiguredBeanLocator.class);
                if (b != null) {
                    Collection<?> coll = b.getBeansOfType(MetricsProvider.class);
                    if (coll != null) {
                        providers = coll.toArray(new MetricsProvider[]{});
                    }
                }
            }
            if (providers == null) {
                try {
                    Class<?> cls = ClassLoaderUtils.loadClass("org.apache.cxf.metrics.codahale.CodahaleMetricsProvider",
                            org.apache.cxf.metrics.MetricsFeature.class);
                    Constructor<?> c = cls.getConstructor(Bus.class);
                    providers = new MetricsProvider[] {(MetricsProvider)c.newInstance(bus)};
                } catch (Throwable t) {
                    // ignore;
                }
            }
        }
    }

}
