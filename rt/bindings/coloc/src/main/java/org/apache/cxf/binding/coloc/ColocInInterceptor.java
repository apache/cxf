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

import java.util.SortedSet;
import java.util.TreeSet;
//import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
//import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.ServiceInvokerInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;

public class ColocInInterceptor extends AbstractPhaseInterceptor<Message> {
    
    private static final Logger LOG = LogUtils.getL7dLogger(ColocInInterceptor.class);

    
    public ColocInInterceptor() {
        super(Phase.INVOKE);
        addAfter(ServiceInvokerInterceptor.class.getName());
    }

    public void handleMessage(Message msg) throws Fault {
        Exchange ex = msg.getExchange();
        if (ex.isOneWay()) {
            return;
        }

        Bus bus = ex.get(Bus.class);
        SortedSet<Phase> phases = new TreeSet<Phase>(bus.getExtension(PhaseManager.class).getOutPhases());

        //TODO Set Coloc FaultObserver chain
        ColocUtil.setPhases(phases, Phase.SETUP, Phase.USER_LOGICAL);
        InterceptorChain chain = ColocUtil.getOutInterceptorChain(ex, phases);

        if (LOG.isLoggable(Level.FINER)) {
            LOG.finer("Processing Message at collocated endpoint.  Response message: " + msg);
        }

        //Initiate OutBound Processing
        BindingOperationInfo boi = ex.get(BindingOperationInfo.class);
        Message outBound = ex.getOutMessage();
        if (boi != null) {
            outBound.put(MessageInfo.class, 
                         boi.getOperationInfo().getOutput());
        }

        outBound.put(Message.INBOUND_MESSAGE, Boolean.FALSE);
        outBound.setInterceptorChain(chain);
        chain.doIntercept(outBound);
    }
}
