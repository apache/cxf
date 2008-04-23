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
import org.apache.cxf.interceptor.OutFaultChainInitiatorObserver;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseManager;

public class ColocOutFaultObserver extends OutFaultChainInitiatorObserver {

    SortedSet<Phase> list;
    public ColocOutFaultObserver(Bus bus) {
        super(bus);
        list = new TreeSet<Phase>(bus.getExtension(PhaseManager.class).getOutPhases());
        ColocUtil.setPhases(list, Phase.SETUP, Phase.USER_LOGICAL);        
    }

    protected SortedSet<Phase> getPhases() {
        return list;
    }
}
