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

package org.apache.cxf.ws.rm;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;

/**
 * Interceptor responsible for implementing exchange of RM protocol messages,
 * aggregating RM metadata in the application message and processing of 
 * RM metadata contained in incoming application messages.
 * The same interceptor can be used on multiple endpoints.
 *
 */
public abstract class AbstractRMInterceptor<T extends Message> extends AbstractPhaseInterceptor<T> {

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractRMInterceptor.class); 
    private RMManager manager;
    private Bus bus;
    
    protected AbstractRMInterceptor() {
        super(Phase.PRE_LOGICAL);
    }
     
    public RMManager getManager() {
        if (null == manager) {
            return bus.getExtension(RMManager.class);
        }
        return manager;
    }

    public void setManager(RMManager m) {
        manager = m;
    }
    
    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }

    // Interceptor interface 
    
    public void handleMessage(Message msg) throws Fault {
        
        try {
            handle(msg);
        } catch (SequenceFault sf) {
            
            // log the fault as it may not be reported back to the client
            
            Endpoint e = msg.getExchange().get(Endpoint.class);
            Binding b = null;
            if (null != e) {
                b = e.getBinding();
            }
            if (null != b) {
                RMManager m = getManager();
                LOG.fine("Manager: " + m);
                BindingFaultFactory bff = m.getBindingFaultFactory(b);
                Fault f = bff.createFault(sf);
                LogUtils.log(LOG, Level.SEVERE, "SEQ_FAULT_MSG", bff.toString(f));
                throw f;
            }
            throw new Fault(sf);
        }  catch (RMException ex) {
            throw new Fault(ex);
        }
    }
    
    /**
     * Asserts all RMAssertion assertions for the current message, regardless their attributes
     * (if there is more thsn one we have ensured that they are all supported by considering
     * e.g. the minimum acknowledgment interval).
     * @param message the current message
     */
    void assertReliability(Message message) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        if (null == aim) {
            return;
            
        }
        Collection<AssertionInfo> ais = aim.get(RMConstants.getRMAssertionQName());
        if (null == ais || ais.size() == 0) {
            return;
        }
        
        for (AssertionInfo ai : ais) {
            ai.setAsserted(true);
        }
    }
    
    protected abstract void handle(Message message) throws SequenceFault, RMException;

}
