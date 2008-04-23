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

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.InFaultChainInitiatorObserver;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;

public class ColocInFaultObserver extends InFaultChainInitiatorObserver {

    private SortedSet<Phase> list;
    public ColocInFaultObserver(Bus bus) {
        super(bus);
        list = new TreeSet<Phase>(bus.getExtension(PhaseManager.class).getInPhases());
        ColocUtil.setPhases(list, Phase.PRE_LOGICAL, Phase.PRE_INVOKE);
    }

    protected void initializeInterceptors(Exchange ex, PhaseInterceptorChain chain) {
        super.initializeInterceptors(ex, chain);
        chain.add(new WebFaultInInterceptor());
    }

    protected SortedSet<Phase> getPhases() {
        return list;
    }
}
