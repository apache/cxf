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
import java.util.concurrent.TimeUnit;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ObjectNameFactory;
import com.codahale.metrics.Timer;

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
import org.apache.cxf.message.FaultMode;
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
    
    public void setBus(Bus b) {
        bus = b;
        if (bus != null) {
            registerInterceptorsToBus();
        }
        InstrumentationManager im = bus.getExtension(InstrumentationManager.class);
        if (im != null) {
            JmxReporter reporter = JmxReporter.forRegistry(registry).registerWith(im.getMBeanServer())
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

    public Bus getBus() {
        return bus;
    }
            
    void registerInterceptorsToBus() {
        ResponseTimeMessageInInterceptor in = new ResponseTimeMessageInInterceptor();
        ResponseTimeMessageInOneWayInterceptor oneway = new ResponseTimeMessageInOneWayInterceptor();
        ResponseTimeMessageOutInterceptor out = new ResponseTimeMessageOutInterceptor();
        CountingOutInterceptor countingOut = new CountingOutInterceptor();
        //ResponseTimeMessageInvokerInterceptor invoker = new ResponseTimeMessageInvokerInterceptor();
        
        bus.getInInterceptors().add(in);
        bus.getInInterceptors().add(oneway);
        bus.getOutInterceptors().add(countingOut);
        bus.getOutInterceptors().add(out);
        bus.getOutFaultInterceptors().add(countingOut);
        bus.getOutFaultInterceptors().add(out);
        //bus.setExtension(this, CounterRepository.class); 
        
        //create CounterRepositroyMoniter to writer the counter log
        
        //if the service is stopped or removed, the counters should remove itself
    }
    
    static class TimerInfo {
        Counter inFlight;
        Timer totals;
        Timer uncheckedApplicationFaults;
        Timer checkedApplicationFaults;
        Timer runtimeFaults;
        Timer logicalRuntimeFaults;
        Meter incomingData;
        Meter outgoingData;
     
        Context start() {
            inFlight.inc();
            Context ctx = new Context();
            ctx.info = this;
            ctx.t = totals.time();
            return ctx;
        }
        static class Context {
            TimerInfo info;
            Timer.Context t;
        }
    }
    
    TimerInfo getTimerInfo(Message message) {
        TimerInfo ti = (TimerInfo)message.getExchange().getEndpoint().get(TimerInfo.class.getName());
        if (ti == null) {
            synchronized (message.getExchange().getEndpoint()) {
                return createTimerInfo(message);
            }
        }
        return ti;
    }
    TimerInfo getTimerInfo(Message message, BindingOperationInfo boi) {
        if (boi.isUnwrapped()) {
            boi = boi.getWrappedOperation();
        }
        TimerInfo ti = (TimerInfo)boi.getProperty(TimerInfo.class.getName());
        if (ti == null) {
            synchronized (boi) {
                return createTimerInfo(message, boi);
            }
        }
        return ti;
    }
    private TimerInfo createTimerInfo(Message message, BindingOperationInfo boi) {
        TimerInfo ti = (TimerInfo)boi.getProperty(TimerInfo.class.getName());
        if (ti == null) {
            ti = new TimerInfo();
            StringBuilder buffer = getBaseServiceName(message);
            buffer.append("Operation=").append(boi.getName().getLocalPart()).append(',');
            ti.totals = registry.timer(buffer.toString() + "Attribute=Totals");
            ti.uncheckedApplicationFaults = registry.timer(buffer.toString() 
                                                           + "Attribute=Unchecked Application Faults");
            ti.checkedApplicationFaults = registry.timer(buffer.toString() + "Attribute=Checked Application Faults");
            ti.runtimeFaults = registry.timer(buffer.toString() + "Attribute=Runtime Faults");
            ti.logicalRuntimeFaults = registry.timer(buffer.toString() + "Attribute=Logical Runtime Faults");
            
            boi.setProperty(TimerInfo.class.getName(), ti);
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
    private TimerInfo createTimerInfo(Message message) {
        Exchange ex = message.getExchange();
        final Endpoint endpoint = ex.get(Endpoint.class);
        TimerInfo ti = (TimerInfo)endpoint.get(TimerInfo.class.getName());
        if (ti == null) {
            ti = new TimerInfo();
            StringBuilder buffer = getBaseServiceName(message);
            final String baseName = buffer.toString();
            ti.totals = registry.timer(baseName + "Attribute=Totals");
            ti.uncheckedApplicationFaults = registry.timer(baseName 
                                                           + "Attribute=Unchecked Application Faults");
            ti.checkedApplicationFaults = registry.timer(baseName + "Attribute=Checked Application Faults");
            ti.runtimeFaults = registry.timer(baseName + "Attribute=Runtime Faults");
            ti.logicalRuntimeFaults = registry.timer(baseName + "Attribute=Logical Runtime Faults");
            ti.inFlight = registry.counter(baseName + "Attribute=In Flight");
            ti.incomingData = registry.meter(baseName + "Attribute=Data Read");
            ti.outgoingData = registry.meter(baseName + "Attribute=Data Written");
            endpoint.put(TimerInfo.class.getName(), ti);
            endpoint.addCleanupHook(new Closeable() {
                public void close() throws IOException {
                    try {
                        registry.remove(baseName + "Attribute=Totals");
                        registry.remove(baseName + "Attribute=Unchecked Application Faults");
                        registry.remove(baseName + "Attribute=Checked Application Faults");
                        registry.remove(baseName + "Attribute=Runtime Faults");
                        registry.remove(baseName + "Attribute=Logical Runtime Faults");
                        registry.remove(baseName + "Attribute=In Flight");
                        registry.remove(baseName + "Attribute=Data Read");
                        registry.remove(baseName + "Attribute=Data Written");
                        endpoint.remove(TimerInfo.class.getName());
                        System.out.println(endpoint.getBinding().getBindingInfo().getOperations());
                        for (BindingOperationInfo boi : endpoint.getBinding().getBindingInfo().getOperations()) {
                            TimerInfo ti = (TimerInfo)boi.removeProperty(TimerInfo.class.getName());
                            if (ti != null) {
                                String name = baseName + "Operation=" + boi.getName().getLocalPart() + ",";
                                System.out.println("Removing beans for " + boi.getName().getLocalPart());
                                registry.remove(name + "Attribute=Totals");
                                registry.remove(name + "Attribute=Unchecked Application Faults");
                                registry.remove(name + "Attribute=Checked Application Faults");
                                registry.remove(name + "Attribute=Runtime Faults");
                                registry.remove(name + "Attribute=Logical Runtime Faults");                            
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

    static void update(Timer t1, Timer t2, long t) {
        if (t1 != null) {
            t1.update(t,  TimeUnit.NANOSECONDS);
        }
        if (t2 != null) {
            t2.update(t,  TimeUnit.NANOSECONDS);
        }
    }
    public void stopTimers(Message m) {
        TimerInfo.Context ctx = m.getExchange().get(TimerInfo.Context.class);
        long l = ctx.t.stop();
        ctx.info.inFlight.dec();
        BindingOperationInfo bi = m.getExchange().getBindingOperationInfo();
        FaultMode fm = m.getExchange().get(FaultMode.class);
        TimerInfo op = null;
        CountingInputStream in = m.getExchange().get(CountingInputStream.class);
        if (in != null) {
            ctx.info.incomingData.mark(in.getCount());
        }
        CountingOutputStream out = m.getExchange().get(CountingOutputStream.class);
        if (out != null) {
            ctx.info.outgoingData.mark(out.getCount());
        }

        if (bi != null) {
            op = getTimerInfo(m, bi);
            op.totals.update(l, TimeUnit.NANOSECONDS);
        }
        if (fm != null) {
            switch (fm) {
            case CHECKED_APPLICATION_FAULT:
                update(ctx.info.checkedApplicationFaults, op != null ? op.checkedApplicationFaults : null, l);
                break;
            case UNCHECKED_APPLICATION_FAULT:
                update(ctx.info.uncheckedApplicationFaults, op != null ? op.uncheckedApplicationFaults : null, l);
                break;
            case RUNTIME_FAULT:
                update(ctx.info.runtimeFaults, op != null ? op.runtimeFaults : null, l);
                break;
            case LOGICAL_RUNTIME_FAULT:
                update(ctx.info.logicalRuntimeFaults, op != null ? op.logicalRuntimeFaults : null, l);
                break;
            default:
            }
        }
    }
    

    class ResponseTimeMessageInInterceptor extends AbstractPhaseInterceptor<Message> {
        public ResponseTimeMessageInInterceptor() {
            super(Phase.RECEIVE);
            addBefore(AttachmentInInterceptor.class.getName());
        }
        public void handleMessage(Message message) throws Fault {
            TimerInfo ti = getTimerInfo(message);
            if (isRequestor(message)) {
                //
            } else {
                TimerInfo.Context ctx = ti.start();
                message.getExchange().put(TimerInfo.Context.class, ctx);
                InputStream in = message.getContent(InputStream.class);
                if (in != null) {
                    CountingInputStream newIn = new CountingInputStream(in);
                    message.setContent(InputStream.class, newIn);
                    message.getExchange().put(CountingInputStream.class, newIn);
                }
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
