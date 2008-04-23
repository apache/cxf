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

import java.util.ResourceBundle;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;

public class ObjectDispatchInInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(ObjectDispatchInInterceptor.class);
    
    public ObjectDispatchInInterceptor() {
        super(Phase.READ);
    }

    public void handleMessage(Message message) throws Fault {
        if (Boolean.TRUE.equals(message.get(Message.REQUESTOR_ROLE))) {
            return;
        }
        
        QName opName = (QName) message.get(ObjectBinding.OPERATION);
        QName bindingName = (QName) message.get(ObjectBinding.BINDING);

        if (opName == null) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("NO_OPERATION", BUNDLE));
        }

        Endpoint ep = message.getExchange().get(Endpoint.class);
        
        BindingInfo binding = null;
        
        if (bindingName == null) {
            binding = ep.getEndpointInfo().getBinding(); 
        } else {
            binding = ep.getEndpointInfo().getService().getBinding(bindingName);
        }
        
        BindingOperationInfo bop = binding.getOperation(opName);
        
        message.getExchange().put(BindingOperationInfo.class, bop);
        
    }

}
