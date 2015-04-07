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

package org.apache.cxf.metrics.interceptors;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.metrics.ExchangeMetrics;
import org.apache.cxf.metrics.MetricsContext;
import org.apache.cxf.metrics.MetricsProvider;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.service.model.BindingOperationInfo;



public abstract class AbstractMetricsInterceptor extends AbstractPhaseInterceptor<Message> {
    MetricsProvider providers[];
    public AbstractMetricsInterceptor(String phase, MetricsProvider p[]) {
        super(phase);
        providers = p;
    }
    
    protected Collection<? extends MetricsProvider> getMetricProviders(Bus bus) {
        if (providers != null) {
            return Arrays.asList(providers);
        }
        ConfiguredBeanLocator b = bus.getExtension(ConfiguredBeanLocator.class);
        if (b == null) {
            return Collections.emptyList();
        }
        return b.getBeansOfType(MetricsProvider.class);
    }
    
    protected ExchangeMetrics getExchangeMetrics(Message m, boolean create) {
        ExchangeMetrics ctx = m.getExchange().get(ExchangeMetrics.class);
        if (ctx == null && create) {
            ctx = new ExchangeMetrics(m.getExchange());
            m.getExchange().put(ExchangeMetrics.class, ctx);
            
            addEndpointMetrics(ctx, m);
        }
        return ctx;
    }

    private void addEndpointMetrics(ExchangeMetrics ctx, Message m) {
        Endpoint ep = m.getExchange().getEndpoint();
        Object o = ep.get(MetricsContext.class.getName());
        if (o == null) {
            synchronized (ep) {
                o = createEndpointMetrics(m);
            }
        }
        if (o instanceof List) {
            List<MetricsContext> list = CastUtils.cast((List<?>)o);
            for (MetricsContext c : list) {
                ctx.addContext(c);
            }
        } else if (o instanceof MetricsContext) {
            ctx.addContext((MetricsContext)o);
        }
    }

    private Object createEndpointMetrics(Message m) {
        final Endpoint ep = m.getExchange().getEndpoint();
        Object o = ep.get(MetricsContext.class.getName());
        if (o == null) {
            List<MetricsContext> contexts = new ArrayList<MetricsContext>();
            for (MetricsProvider p : getMetricProviders(m.getExchange().getBus())) {
                MetricsContext c = p.createEndpointContext(ep, MessageUtils.isRequestor(m),
                                                           (String)m.getContextualProperty(MetricsProvider.CLIENT_ID));
                if (c != null) {
                    contexts.add(c);
                }
                if (c instanceof Closeable) {
                    ep.addCleanupHook((Closeable)c);
                }
            }
            if (contexts.size() == 1) {
                o = contexts.get(0);
            } else {
                o = contexts;
            }
            ep.put(MetricsContext.class.getName(), o);
        }
        return o;
    }

    protected void addOperationMetrics(ExchangeMetrics ctx, Message m, BindingOperationInfo boi) {
        if (boi.isUnwrapped()) {
            boi = boi.getWrappedOperation();
        }
        Object o = boi.getProperty(MetricsContext.class.getName());
        if (o == null) {
            synchronized (boi) {
                o = createMetricsContextForOperation(m, boi);
            }
        }
        if (o instanceof List) {
            List<MetricsContext> list = CastUtils.cast((List<?>)o);
            for (MetricsContext c : list) {
                ctx.addContext(c);
            }
        } else if (o instanceof MetricsContext) {
            ctx.addContext((MetricsContext)o);
        }
    }
    
    private Object createMetricsContextForOperation(Message message, BindingOperationInfo boi) {
        Object o = boi.getProperty(MetricsContext.class.getName());
        if (o == null) {
            List<MetricsContext> contexts = new ArrayList<MetricsContext>();
            for (MetricsProvider p : getMetricProviders(message.getExchange().getBus())) {
                MetricsContext c = p.createOperationContext(message.getExchange().getEndpoint(),
                                         boi, MessageUtils.isRequestor(message),
                                         (String)message.getContextualProperty(MetricsProvider.CLIENT_ID));
                if (c != null) {
                    contexts.add(c);
                }
                if (c instanceof Closeable) {
                    message.getExchange().getEndpoint().addCleanupHook((Closeable)c);
                }
            }
            if (contexts.size() == 1) {
                o = contexts.get(0);
            } else {
                o = contexts;
            }
            boi.setProperty(MetricsContext.class.getName(), o);
        }
        return o;
    }
   
    public void stop(Message m) {
        ExchangeMetrics ctx = getExchangeMetrics(m, false);
        if (ctx != null) {
            ctx.stop();
        }
    }
}
