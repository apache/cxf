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

package org.apache.cxf.interceptor;

import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.transport.MessageObserver;

public abstract class AbstractFaultChainInitiatorObserver implements MessageObserver {

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractFaultChainInitiatorObserver.class);

    private Bus bus;
    private ClassLoader loader;

    public AbstractFaultChainInitiatorObserver(Bus bus) {
        this.bus = bus;
        if (bus != null) {
            loader = bus.getExtension(ClassLoader.class);
        }
    }

    public void onMessage(Message message) {

        assert null != message;

        Bus origBus = BusFactory.getAndSetThreadDefaultBus(bus);
        ClassLoaderHolder origLoader = null;
        try {
            if (loader != null) {
                origLoader = ClassLoaderUtils.setThreadContextClassloader(loader);
            }

            Exchange exchange = message.getExchange();

            Message faultMessage = null;

            // now that we have switched over to the fault chain,
            // prevent any further operations on the in/out message

            if (isOutboundObserver()) {
                Exception ex = message.getContent(Exception.class);
                if (!(ex instanceof Fault)) {
                    ex = new Fault(ex);
                }
                FaultMode mode = message.get(FaultMode.class);

                faultMessage = exchange.getOutMessage();
                if (null == faultMessage) {
                    faultMessage = new MessageImpl();
                    faultMessage.setExchange(exchange);
                    faultMessage = exchange.getEndpoint().getBinding().createMessage(faultMessage);
                }
                faultMessage.setContent(Exception.class, ex);
                if (null != mode) {
                    faultMessage.put(FaultMode.class, mode);
                }
                //CXF-3981
                if (message.get("javax.xml.ws.addressing.context.inbound") != null) {
                    faultMessage.put("javax.xml.ws.addressing.context.inbound",
                                     message.get("javax.xml.ws.addressing.context.inbound"));
                }
                exchange.setOutMessage(null);
                exchange.setOutFaultMessage(faultMessage);
                if (message.get(BindingFaultInfo.class) != null) {
                    faultMessage.put(BindingFaultInfo.class, message.get(BindingFaultInfo.class));
                }
            } else {
                faultMessage = message;
                exchange.setInMessage(null);
                exchange.setInFaultMessage(faultMessage);
            }


            // setup chain
            PhaseInterceptorChain chain = new PhaseInterceptorChain(getPhases());
            initializeInterceptors(faultMessage.getExchange(), chain);

            faultMessage.setInterceptorChain(chain);
            try {
                chain.doIntercept(faultMessage);
            } catch (RuntimeException exc) {
                LOG.log(Level.SEVERE, "ERROR_DURING_ERROR_PROCESSING", exc);
                throw exc;
            } catch (Exception exc) {
                LOG.log(Level.SEVERE, "ERROR_DURING_ERROR_PROCESSING", exc);
                throw new RuntimeException(exc);
            }
        } finally {
            if (origBus != bus) {
                BusFactory.setThreadDefaultBus(origBus);
            }
            if (origLoader != null) {
                origLoader.reset();
            }
        }
    }

    protected abstract boolean isOutboundObserver();

    protected abstract SortedSet<Phase> getPhases();

    protected void initializeInterceptors(Exchange ex, PhaseInterceptorChain chain) {

    }

    public Bus getBus() {
        return bus;
    }

}
