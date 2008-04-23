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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;

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
        Bus bus = ex.get(Bus.class);
        PhaseInterceptorChain chain = new PhaseInterceptorChain(phases);
        
        Endpoint ep = ex.get(Endpoint.class);
        List<Interceptor> il = ep.getOutInterceptors();
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

        return chain;
    }
    
    public static InterceptorChain getInInterceptorChain(Exchange ex, SortedSet<Phase> phases) {
        Bus bus = ex.get(Bus.class);
        PhaseInterceptorChain chain = new PhaseInterceptorChain(phases);
        
        Endpoint ep = ex.get(Endpoint.class);
        List<Interceptor> il = ep.getInInterceptors();
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
        chain.setFaultObserver(new ColocOutFaultObserver(bus));

        return chain;
    }    
    
    public static boolean isSameOperationInfo(OperationInfo oi1, OperationInfo oi2) {
        return  oi1.getName().equals(oi2.getName())
                && isSameMessageInfo(oi1.getInput(), oi2.getInput())
                && isSameMessageInfo(oi1.getOutput(), oi2.getOutput())
                && isSameFaultInfo(oi1.getFaults(), oi2.getFaults());
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
}
