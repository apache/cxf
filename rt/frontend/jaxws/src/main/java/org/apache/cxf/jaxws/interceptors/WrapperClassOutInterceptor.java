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

package org.apache.cxf.jaxws.interceptors;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;

public class WrapperClassOutInterceptor extends AbstractPhaseInterceptor<Message> {
    public WrapperClassOutInterceptor() {
        super(Phase.PRE_LOGICAL);
    }

    public void handleMessage(Message message) throws Fault {
        Exchange ex = message.getExchange();
        BindingOperationInfo bop = ex.get(BindingOperationInfo.class);

        MessageInfo messageInfo = message.get(MessageInfo.class);
        if (messageInfo == null || bop == null || !bop.isUnwrapped()) {
            return;
        }
        
        BindingOperationInfo newbop = bop.getWrappedOperation();
        MessageInfo wrappedMsgInfo;
        if (Boolean.TRUE.equals(message.get(Message.REQUESTOR_ROLE))) {
            wrappedMsgInfo = newbop.getInput().getMessageInfo();
        } else {
            wrappedMsgInfo = newbop.getOutput().getMessageInfo();
        }
             
        Class<?> wrapped = null;
        List<MessagePartInfo> parts = wrappedMsgInfo.getMessageParts();
        if (parts.size() > 0) {
            wrapped = parts.get(0).getTypeClass();
        }

        if (wrapped != null) {
            MessageContentsList objs = MessageContentsList.getContentsList(message);

            WrapperHelper helper = parts.get(0).getProperty("WRAPPER_CLASS", WrapperHelper.class);
            if (helper == null) {
                List<String> partNames = new ArrayList<String>();
                List<String> elTypeNames = new ArrayList<String>();
                List<Class<?>> partClasses = new ArrayList<Class<?>>();
                
                for (MessagePartInfo p : messageInfo.getMessageParts()) {
                    ensureSize(partNames, p.getIndex());
                    ensureSize(elTypeNames, p.getIndex());
                    ensureSize(partClasses, p.getIndex());
                    
                    partNames.set(p.getIndex(), p.getName().getLocalPart());
                    
                    String elementType = null;
                    if (p.getTypeQName() == null) {
                        // handling anonymous complex type
                        elementType = null;
                    } else {
                        elementType = p.getTypeQName().getLocalPart();
                    }
                    
                    elTypeNames.set(p.getIndex(), elementType);
                    partClasses.set(p.getIndex(), p.getTypeClass());
                }
                helper = WrapperHelper.createWrapperHelper(wrapped,
                                                           partNames,
                                                           elTypeNames,
                                                           partClasses);

                parts.get(0).setProperty("WRAPPER_CLASS", helper);
            }
            try {
                MessageContentsList newObjs = new MessageContentsList(); 
                Object o2 = helper.createWrapperObject(objs);
                newObjs.put(parts.get(0), o2);
                
                for (MessagePartInfo p : messageInfo.getMessageParts()) {
                    if (Boolean.TRUE.equals(p.getProperty(ReflectionServiceFactoryBean.HEADER))) {
                        MessagePartInfo mpi = wrappedMsgInfo.getMessagePart(p.getName());
                        if (objs.hasValue(p)) {
                            newObjs.put(mpi, objs.get(p));
                        }
                    }
                }

                message.setContent(List.class, newObjs);
            } catch (Exception e) {
                throw new Fault(e);
            }
            
            // we've now wrapped the object, so use the wrapped binding op
            ex.put(BindingOperationInfo.class, newbop);
            ex.put(OperationInfo.class, newbop.getOperationInfo());
            
            if (messageInfo == bop.getOperationInfo().getInput()) {
                message.put(MessageInfo.class, newbop.getOperationInfo().getInput());
                message.put(BindingMessageInfo.class, newbop.getInput());
            } else if (messageInfo == bop.getOperationInfo().getOutput()) {
                message.put(MessageInfo.class, newbop.getOperationInfo().getOutput());
                message.put(BindingMessageInfo.class, newbop.getOutput());
            }
        }
    }


    private void ensureSize(List<?> lst, int idx) {
        while (idx >= lst.size()) {
            lst.add(null);
        }
    }
}
