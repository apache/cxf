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

package org.apache.cxf.management.codahale;


import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ObjectNameFactory;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.MessageSenderInterceptor;
import org.apache.cxf.interceptor.ServiceInvokerInterceptor;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;

public class Metrics {
    private static final String QUESTION_MARK = "?";
    private static final String ESCAPED_QUESTION_MARK = "\\?";
    
    private MetricRegistry registry;
    private Bus bus;
    
    public Metrics() {
        registry = new MetricRegistry();
    }
    public Metrics(MetricRegistry reg) {
        registry = reg;
    }
    public Metrics(Bus b) {
        registry = b.getExtension(MetricRegistry.class);
        bus = b;
        registerInterceptorsToBus();
        if (registry == null) {
            registry = new MetricRegistry();
            setupJMXReporter(b, registry);
        }
    }
    
    protected final void setupJMXReporter(Bus b, MetricRegistry reg) {
        InstrumentationManager im = b.getExtension(InstrumentationManager.class);
        if (im != null) {
            JmxReporter reporter = JmxReporter.forRegistry(reg).registerWith(im.getMBeanServer())
                .inDomain("org.apache.cxf")
                .createsObjectNamesWith(new ObjectNameFactory() {
                    public ObjectName createName(String type, String domain, String name) {
                        try {
                            return new ObjectName(name);
                        } catch (MalformedObjectNameException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
                .build();
            reporter.start();
        }
    }
    public void setBus(Bus b) {
        bus = b;
        if (bus != null) {
            registerInterceptorsToBus();
            setupJMXReporter(bus, registry);
        }
    }

    public Bus getBus() {
        return bus;
    }
            
    private void registerInterceptorsToBus() {
        ResponseTimeMessageInInterceptor in = new ResponseTimeMessageInInterceptor();
        ResponseTimeMessageInOneWayInterceptor oneway = new ResponseTimeMessageInOneWayInterceptor();
        ResponseTimeMessageOutInterceptor out = new ResponseTimeMessageOutInterceptor();
        CountingOutInterceptor countingOut = new CountingOutInterceptor();
        
        bus.getInInterceptors().add(in);
        bus.getInInterceptors().add(oneway);
        bus.getInInterceptors().add(new ResponseTimeMessageInPreInvokeInterceptor());
        bus.getOutInterceptors().add(countingOut);
        bus.getOutInterceptors().add(out);
        bus.getOutFaultInterceptors().add(countingOut);
        bus.getOutFaultInterceptors().add(out);
    }
        
    MetricsContext getMetricsContextForEndpoint(Message message) {
        MetricsContext ti = (MetricsContext)message.getExchange().getEndpoint().get(MetricsContext.class.getName());
        if (ti == null) {
            synchronized (message.getExchange().getEndpoint()) {
                return createMetricsContextForEndpoint(message);
            }
        }
        return ti;
    }
    MetricsContext getMetricsContextForOperation(Message message, BindingOperationInfo boi) {
        if (boi.isUnwrapped()) {
            boi = boi.getWrappedOperation();
        }
        MetricsContext ti = (MetricsContext)boi.getProperty(MetricsContext.class.getName());
        if (ti == null) {
            synchronized (boi) {
                return createMetricsContextForOperation(message, boi);
            }
        }
        return ti;
    }
    private MetricsContext createMetricsContextForOperation(Message message, BindingOperationInfo boi) {
        MetricsContext ti = (MetricsContext)boi.getProperty(MetricsContext.class.getName());
        if (ti == null) {
            StringBuilder buffer = getBaseServiceName(message);
            buffer.append("Operation=").append(boi.getName().getLocalPart()).append(',');
            ti = new CodahaleMetricsContext(buffer.toString(), registry);
            boi.setProperty(MetricsContext.class.getName(), ti);
        }
        return ti;
    }
    StringBuilder getBaseServiceName(Message message) {
        Exchange ex = message.getExchange();
        StringBuilder buffer = new StringBuilder();
        if (ex.get("org.apache.cxf.management.service.counter.name") != null) {
            buffer.append((String)ex.get("org.apache.cxf.management.service.counter.name"));
        } else {
            Service service = ex.getService();
            Endpoint endpoint = ex.getEndpoint();
            Bus b = ex.getBus();

            String serviceName = "\"" + escapePatternChars(service.getName().toString()) + "\"";
            String portName = "\"" + endpoint.getEndpointInfo().getName().getLocalPart() + "\"";

            buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME + ":");
            buffer.append(ManagementConstants.BUS_ID_PROP + "=" + b.getId() + ",");
            buffer.append(ManagementConstants.TYPE_PROP).append("=Metrics");
            if (MessageUtils.isRequestor(message)) {
                buffer.append(".Client,");
            } else {
                buffer.append(".Server,");
            }
            buffer.append(ManagementConstants.SERVICE_NAME_PROP + "=" + serviceName + ",");

            buffer.append(ManagementConstants.PORT_NAME_PROP + "=" + portName + ",");
        }
        return buffer;
    }
    private MetricsContext createMetricsContextForEndpoint(Message message) {
        Exchange ex = message.getExchange();
        final Endpoint endpoint = ex.get(Endpoint.class);
        MetricsContext ti = (MetricsContext)endpoint.get(MetricsContext.class.getName());
        if (ti == null) {
            StringBuilder buffer = getBaseServiceName(message);
            final String baseName = buffer.toString();
            ti = new CodahaleMetricsContext(baseName, registry);
            

            endpoint.put(MetricsContext.class.getName(), ti);
            endpoint.addCleanupHook(new Closeable() {
                public void close() throws IOException {
                    try {
                        MetricsContext mct = (MetricsContext)endpoint.remove(MetricsContext.class.getName());
                        if (mct instanceof Closeable) {
                            ((Closeable)mct).close();
                        }
                        for (BindingOperationInfo boi : endpoint.getBinding().getBindingInfo().getOperations()) {
                            MetricsContext ti = (MetricsContext)boi.removeProperty(MetricsContext.class.getName());
                            if (ti instanceof Closeable) {
                                ((Closeable)ti).close();
                            }
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            });
        }
        return ti;
    }
    protected String escapePatternChars(String value) {
        // This can be replaced if really needed with pattern-based matching
        if (value.lastIndexOf(QUESTION_MARK) != -1) {
            value = value.replace(QUESTION_MARK, ESCAPED_QUESTION_MARK);
        }
        return value;
    }

    public void stopTimers(Message m) {
        MessageMetrics ctx = m.getExchange().get(MessageMetrics.class);
        if (ctx != null) {
            ctx.stop(m);
        }
    }
    

    class ResponseTimeMessageInInterceptor extends AbstractPhaseInterceptor<Message> {
        public ResponseTimeMessageInInterceptor() {
            super(Phase.RECEIVE);
            addBefore(AttachmentInInterceptor.class.getName());
        }
        public void handleMessage(Message message) throws Fault {
            if (isRequestor(message)) {
                //
            } else {
                MessageMetrics ctx = message.getExchange().get(MessageMetrics.class);
                if (ctx == null) {
                    ctx = new MessageMetrics();
                    MetricsContext ti = getMetricsContextForEndpoint(message);
                    ctx.addContext(ti);
                    message.getExchange().put(MessageMetrics.class, ctx);
                }
                InputStream in = message.getContent(InputStream.class);
                if (in != null) {
                    CountingInputStream newIn = new CountingInputStream(in);
                    message.setContent(InputStream.class, newIn);
                    message.getExchange().put(CountingInputStream.class, newIn);
                }
                ctx.start();
            }
        }
        public void handleFault(Message message) {
            if (message.getExchange().isOneWay()) {
                stopTimers(message);
            }
        }
    };
    
    class CountingOutInterceptor extends AbstractPhaseInterceptor<Message> {
        public CountingOutInterceptor() {
            super(Phase.PRE_STREAM);
            addBefore(AttachmentOutInterceptor.class.getName());
        }
        public void handleMessage(Message message) throws Fault {
            if (isRequestor(message)) {
                //
            } else {
                OutputStream out = message.getContent(OutputStream.class);
                if (out != null) {
                    CountingOutputStream newOut = new CountingOutputStream(out);
                    message.setContent(OutputStream.class, newOut);
                    message.getExchange().put(CountingOutputStream.class, newOut);
                }
               
            }
        }    
    };

    class ResponseTimeMessageOutInterceptor extends AbstractPhaseInterceptor<Message> {
        public ResponseTimeMessageOutInterceptor() {
            super(Phase.PREPARE_SEND_ENDING);
            addBefore(MessageSenderInterceptor.MessageSenderEndingInterceptor.class.getName());
        }
        public void handleMessage(Message message) throws Fault {
            if (isRequestor(message)) {
                //
            } else {
                stopTimers(message);
            }
        }    
    };
    class ResponseTimeMessageInPreInvokeInterceptor extends AbstractPhaseInterceptor<Message> {
        public ResponseTimeMessageInPreInvokeInterceptor() {
            super(Phase.PRE_INVOKE);
        }

        public void handleMessage(Message message) throws Fault {
            Exchange ex = message.getExchange();
            if (ex.getBindingOperationInfo() != null) {
                //we now know the operation, start metrics for it
                MessageMetrics ctx = message.getExchange().get(MessageMetrics.class);
                if (ctx != null) {
                    MetricsContext ti = getMetricsContextForOperation(message, ex.getBindingOperationInfo());
                    ctx.addContext(ti);
                }
            }
        }               
    }
    class ResponseTimeMessageInOneWayInterceptor extends AbstractPhaseInterceptor<Message> {
        public ResponseTimeMessageInOneWayInterceptor() {
            super(Phase.INVOKE);
            addAfter(ServiceInvokerInterceptor.class.getName());
        }

        public void handleMessage(Message message) throws Fault {
            Exchange ex = message.getExchange();
            if (ex.isOneWay() && !isRequestor(message)) {
                stopTimers(message);
            }
        }               
    }
}
