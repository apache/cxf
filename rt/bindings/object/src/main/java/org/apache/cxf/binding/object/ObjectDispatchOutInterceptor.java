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
package org.apache.cxf.binding.object;

import java.util.HashSet;
import java.util.Set;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.transport.local.LocalConduit;
import org.apache.cxf.transport.local.LocalTransportFactory;

public class ObjectDispatchOutInterceptor extends AbstractPhaseInterceptor<Message> {
    private Set<String> includes = new HashSet<String>();
    
    public ObjectDispatchOutInterceptor() {
        super(Phase.SETUP);
        includes.add(ObjectBinding.OPERATION);
        includes.add(ObjectBinding.BINDING);
    }

    public void handleMessage(Message message) throws Fault {
        Exchange ex = message.getExchange();
        message.put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        message.put(LocalTransportFactory.MESSAGE_INCLUDE_PROPERTIES,
                    includes);
        BindingOperationInfo bop = ex.get(BindingOperationInfo.class);
        
        message.put(ObjectBinding.OPERATION, bop.getName());
        message.put(ObjectBinding.BINDING, bop.getBinding().getName());
    }

}
