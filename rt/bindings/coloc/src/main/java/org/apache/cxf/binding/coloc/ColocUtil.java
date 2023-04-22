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
package org.apache.cxf.binding.coloc;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.staxutils.StaxUtils;

public final class ColocUtil {
    private static final Logger LOG = LogUtils.getL7dLogger(ColocUtil.class);

    private ColocUtil() {
        //Completge
    }

    public static void setPhases(SortedSet<Phase> list, String start, String end) {
        Phase startPhase = new Phase(start, 1);
        Phase endPhase = new Phase(end, 2);
        Iterator<Phase> iter = list.iterator();
        boolean remove = true;
        while (iter.hasNext()) {
            Phase p = iter.next();
            if (remove
                && p.getName().equals(startPhase.getName())) {
                remove = false;
            } else if (p.getName().equals(endPhase.getName())) {
                remove = true;
            } else if (remove) {
                iter.remove();
            }
        }
    }

    public static InterceptorChain getOutInterceptorChain(Exchange ex, SortedSet<Phase> phases) {
        Bus bus = ex.getBus();
        PhaseInterceptorChain chain = new PhaseInterceptorChain(phases);

        Endpoint ep = ex.getEndpoint();
        List<Interceptor<? extends Message>> il = ep.getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by endpoint: " + il);
        }
        chain.add(il);
        il = ep.getService().getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by service: " + il);
        }
        chain.add(il);
        il = bus.getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by bus: " + il);
        }
        chain.add(il);

        if (ep.getService().getDataBinding() instanceof InterceptorProvider) {
            il = ((InterceptorProvider)ep.getService().getDataBinding()).getOutInterceptors();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Interceptors contributed by databinding: " + il);
            }
            chain.add(il);
        }
        modifyChain(chain, ex, false);

        return chain;
    }

    public static InterceptorChain getInInterceptorChain(Exchange ex, SortedSet<Phase> phases) {
        Bus bus = ex.getBus();
        PhaseInterceptorChain chain = new PhaseInterceptorChain(phases);

        Endpoint ep = ex.getEndpoint();
        List<Interceptor<? extends Message>> il = ep.getInInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by endpoint: " + il);
        }
        chain.add(il);
        il = ep.getService().getInInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by service: " + il);
        }
        chain.add(il);
        il = bus.getInInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by bus: " + il);
        }
        chain.add(il);

        if (ep.getService().getDataBinding() instanceof InterceptorProvider) {
            il = ((InterceptorProvider)ep.getService().getDataBinding()).getInInterceptors();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Interceptors contributed by databinding: " + il);
            }
            chain.add(il);
        }
        chain.setFaultObserver(new ColocOutFaultObserver(bus));
        modifyChain(chain, ex, true);
        return chain;
    }
    private static void modifyChain(PhaseInterceptorChain chain, Exchange ex, boolean in) {
        modifyChain(chain, ex.getInMessage(), in);
        modifyChain(chain, ex.getOutMessage(), in);
    }
    private static void modifyChain(PhaseInterceptorChain chain, Message m, boolean in) {
        if (m == null) {
            return;
        }
        Collection<InterceptorProvider> providers
            = CastUtils.cast((Collection<?>)m.get(Message.INTERCEPTOR_PROVIDERS));
        if (providers != null) {
            for (InterceptorProvider p : providers) {
                if (in) {
                    chain.add(p.getInInterceptors());
                } else {
                    chain.add(p.getOutInterceptors());
                }
            }
        }
        String key = in ? Message.IN_INTERCEPTORS : Message.OUT_INTERCEPTORS;
        Collection<Interceptor<? extends Message>> is
            = CastUtils.cast((Collection<?>)m.get(key));
        if (is != null) {
            chain.add(is);
        }
    }

    public static boolean isSameOperationInfo(OperationInfo oi1, OperationInfo oi2) {
        return oi1.getName().equals(oi2.getName())
                && isSameMessageInfo(oi1.getInput(), oi2.getInput())
                && isSameMessageInfo(oi1.getOutput(), oi2.getOutput())
                && isSameFaultInfo(oi1.getFaults(), oi2.getFaults());
    }

    public static boolean isCompatibleOperationInfo(OperationInfo oi1, OperationInfo oi2) {
        return isSameOperationInfo(oi1, oi2)
               || isAssignableOperationInfo(oi1, Source.class)
               || isAssignableOperationInfo(oi2, Source.class);
    }

    public static boolean isAssignableOperationInfo(OperationInfo oi, Class<?> cls) {
        MessageInfo mi = oi.getInput();
        List<MessagePartInfo> mpis = mi.getMessageParts();
        return mpis.size() == 1 && cls.isAssignableFrom(mpis.get(0).getTypeClass());
    }

    public static boolean isSameMessageInfo(MessageInfo mi1, MessageInfo mi2) {
        if ((mi1 == null && mi2 != null)
            || (mi1 != null && mi2 == null)) {
            return false;
        }

        if (mi1 != null && mi2 != null) {
            List<MessagePartInfo> mpil1 = mi1.getMessageParts();
            List<MessagePartInfo> mpil2 = mi2.getMessageParts();
            if (mpil1.size() != mpil2.size()) {
                return false;
            }
            int idx = 0;
            for (MessagePartInfo mpi1 : mpil1) {
                MessagePartInfo mpi2 = mpil2.get(idx);
                if (!mpi1.getTypeClass().equals(mpi2.getTypeClass())) {
                    return false;
                }
                ++idx;
            }
        }
        return true;
    }

    public static boolean isSameFaultInfo(Collection<FaultInfo> fil1,
                                          Collection<FaultInfo> fil2) {
        if ((fil1 == null && fil2 != null)
            || (fil1 != null && fil2 == null)) {
            return false;
        }

        if (fil1 != null && fil2 != null) {
            if (fil1.size() != fil2.size()) {
                return false;
            }
            for (FaultInfo fi1 : fil1) {
                Iterator<FaultInfo> iter = fil2.iterator();
                Class<?> fiClass1 = fi1.getProperty(Class.class.getName(),
                                                    Class.class);
                boolean match = false;
                while (iter.hasNext()) {
                    FaultInfo fi2 = iter.next();
                    Class<?> fiClass2 = fi2.getProperty(Class.class.getName(),
                                                        Class.class);
                    //Sender/Receiver Service Model not same for faults wr.t message names.
                    //So Compare Exception Class Instance.
                    if (fiClass1.equals(fiClass2)) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void convertSourceToObject(Message message) {
        List<Object> content = CastUtils.cast(message.getContent(List.class));
        if (content == null || content.isEmpty()) {
            // nothing to convert
            return;
        }
        // only supporting the wrapped style for now  (one pojo <-> one source)
        Source source = (Source)content.get(0);
        DataReader<XMLStreamReader> reader =
            message.getExchange().getService().getDataBinding().createReader(XMLStreamReader.class);
        MessagePartInfo mpi = getMessageInfo(message).getMessagePart(0);
        XMLStreamReader streamReader = null;
        Object wrappedObject;
        try {
            streamReader = StaxUtils.createXMLStreamReader(source);
            wrappedObject = reader.read(mpi, streamReader);
        } finally {
            try {
                StaxUtils.close(streamReader);
            } catch (XMLStreamException e) {
                // Ignore
            }
        }
        MessageContentsList parameters = new MessageContentsList();
        parameters.put(mpi, wrappedObject);

        message.setContent(List.class, parameters);
    }

    public static void convertObjectToSource(Message message) {
        List<Object> content = CastUtils.cast(message.getContent(List.class));
        if (content == null || content.isEmpty()) {
            // nothing to convert
            return;
        }
        // only supporting the wrapped style for now  (one pojo <-> one source)
        Object object = content.get(0);
        DataWriter<OutputStream> writer =
            message.getExchange().getService().getDataBinding().createWriter(OutputStream.class);
        LoadingByteArrayOutputStream bos = new LoadingByteArrayOutputStream();
        writer.write(object, bos);
        content.set(0, new StreamSource(bos.createInputStream()));
    }

    private static MessageInfo getMessageInfo(Message message) {
        OperationInfo oi = message.getExchange().getBindingOperationInfo().getOperationInfo();
        if (MessageUtils.isOutbound(message)) {
            return oi.getOutput();
        }
        return oi.getInput();
    }
}
