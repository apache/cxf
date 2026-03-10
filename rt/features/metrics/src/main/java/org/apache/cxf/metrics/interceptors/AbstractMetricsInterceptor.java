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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final String REST_METRICS_MAP = AbstractMetricsInterceptor.class.getName() + ".METRICS_MAP";
    MetricsProvider[] providers;
    public AbstractMetricsInterceptor(String phase, MetricsProvider[] p) {
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
            List<MetricsContext> contexts = new ArrayList<>();
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

    private Map<String, Object> getRestMetricsMap(Endpoint e) {
        synchronized (e) {
            Object mmo = e.get(REST_METRICS_MAP);
            if (mmo == null) {
                e.put(REST_METRICS_MAP, new ConcurrentHashMap<String, Object>());
                mmo = e.get(REST_METRICS_MAP);
            }
            return CastUtils.cast((Map<?, ?>)mmo);
        }
    }
    protected void addOperationMetrics(ExchangeMetrics ctx, Message m, BindingOperationInfo boi) {
        Object metrics = null;
        if (boi == null) {
            //likely a REST service, let's see if we have a resource name
            Object nameProperty = m.getExchange().get("org.apache.cxf.resource.operation.name");
            if (nameProperty != null) {
                metrics = getRestMetricsMap(m.getExchange().getEndpoint()).get(nameProperty.toString());
                if (metrics == null) {
                    metrics = createMetricsContextForRestResource(m, nameProperty.toString());
                }
            }
        } else {
            if (boi.isUnwrapped()) {
                boi = boi.getWrappedOperation();
            }
            metrics = boi.getProperty(MetricsContext.class.getName());
            if (metrics == null) {
                synchronized (boi) {
                    metrics = createMetricsContextForOperation(m, boi);
                }
            }
        }
        if (metrics instanceof List) {
            List<MetricsContext> list = CastUtils.cast((List<?>)metrics);
            for (MetricsContext c : list) {
                ctx.addContext(c);
            }
        } else if (metrics instanceof MetricsContext) {
            ctx.addContext((MetricsContext)metrics);
        }
    }

    private synchronized Object createMetricsContextForRestResource(Message message, String resource) {
        Map<String, Object> restMap = getRestMetricsMap(message.getExchange().getEndpoint());
        Object o = restMap.get(resource);
        if (o != null) {
            return o;
        }
        List<MetricsContext> contexts = new ArrayList<>();
        for (MetricsProvider p : getMetricProviders(message.getExchange().getBus())) {
            MetricsContext c = p.createResourceContext(message.getExchange().getEndpoint(),
                                     resource, MessageUtils.isRequestor(message),
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
        restMap.put(resource, o);
        return o;
    }
    private Object createMetricsContextForOperation(Message message, BindingOperationInfo boi) {
        Object o = null;
        if (isRequestor(message)) {
            o = boi.getProperty(MetricsContext.class.getName());
        } else {
            //on the server side the MetricsContext may already be created
            //at endpoint level; avoid recreating another one
            o = message.getExchange().getEndpoint().get(MetricsContext.class.getName());
            if (o == null || (o instanceof List && ((List)o).isEmpty())) {
                // if no MetricsContext retrieved from message exchange created before for endpoint
                // use the one from operation
                o = boi.getProperty(MetricsContext.class.getName());
            } else {
                boi.setProperty(MetricsContext.class.getName(), o);
            }
        }
       
        if (o == null) {
            List<MetricsContext> contexts = new ArrayList<>();
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
