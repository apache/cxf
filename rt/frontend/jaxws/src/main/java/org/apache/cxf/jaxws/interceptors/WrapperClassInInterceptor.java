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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.WrapperCapableDatabinding;
import org.apache.cxf.databinding.WrapperHelper;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceModelUtil;

public class WrapperClassInInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getL7dLogger(WrapperClassInInterceptor.class);
    
    public WrapperClassInInterceptor() {
        super(Phase.POST_LOGICAL);
    }

    public void handleMessage(Message message) throws Fault {
        Exchange ex = message.getExchange();
        BindingOperationInfo boi = ex.get(BindingOperationInfo.class);
        if (boi == null) {
            return;
        }
               
        Method method = ex.get(Method.class);

        if (method != null && method.getName().endsWith("Async")) {
            Class<?> retType = method.getReturnType();
            if (retType.getName().equals("java.util.concurrent.Future") 
                || retType.getName().equals("javax.xml.ws.Response")) {
                return;
            }
        }        

        
        if (boi != null && boi.isUnwrappedCapable()) {
            BindingOperationInfo boi2 = boi.getUnwrappedOperation();
            OperationInfo op = boi2.getOperationInfo();
            BindingMessageInfo bmi;
            
            MessageInfo wrappedMessageInfo = message.get(MessageInfo.class);
            MessageInfo messageInfo;
            if (wrappedMessageInfo == boi.getOperationInfo().getInput()) {
                messageInfo = op.getInput();
                bmi = boi2.getInput();
            } else {
                messageInfo = op.getOutput();
                bmi = boi2.getOutput();
            }
            
            // Sometimes, an operation can be unwrapped according to WSDLServiceFactory,
            // but not according to JAX-WS. We should unify these at some point, but
            // for now check for the wrapper class.
            MessageContentsList lst = MessageContentsList.getContentsList(message);
            if (lst == null) {
                return;
            }
            if (lst != null) {
                message.put(MessageInfo.class, messageInfo);
                message.put(BindingMessageInfo.class, bmi);
                ex.put(BindingOperationInfo.class, boi2);
                ex.put(OperationInfo.class, op);
            }
            
            if (isGET(message)) {
                LOG.fine("WrapperClassInInterceptor skipped in HTTP GET method");
                return;
            }
            
            MessagePartInfo wrapperPart = wrappedMessageInfo.getMessagePart(0);
            Class<?> wrapperClass = wrapperPart.getTypeClass();
            Object wrappedObject = lst.get(wrapperPart.getIndex());
            if (!wrapperClass.isInstance(wrappedObject)) {
                wrappedObject = null;
                wrapperPart = null;
                wrapperClass = null;
            }
            if (wrapperClass == null || wrappedObject == null) {
                return;
            }
            
            WrapperHelper helper = wrapperPart.getProperty("WRAPPER_CLASS", WrapperHelper.class);
            if (helper == null) {
                Service service = ServiceModelUtil.getService(message.getExchange());
                DataBinding dataBinding = service.getDataBinding();
                if (dataBinding instanceof WrapperCapableDatabinding) {
                    helper = createWrapperHelper((WrapperCapableDatabinding)dataBinding,
                                                 messageInfo, wrappedMessageInfo, wrapperClass);
                    wrapperPart.setProperty("WRAPPER_CLASS", helper);
                } else {
                    return;
                }                
            }            
            
            MessageContentsList newParams;
            try {
                newParams = new MessageContentsList(helper.getWrapperParts(wrappedObject));
                
                List<Integer> removes = null;
                int count = 0;
                for (MessagePartInfo part : messageInfo.getMessageParts()) {
                    if (Boolean.TRUE.equals(part.getProperty(ReflectionServiceFactoryBean.HEADER))) {
                        MessagePartInfo mpi = wrappedMessageInfo.getMessagePart(part.getName());
                        if (lst.hasValue(mpi)) {
                            count++;
                            newParams.put(part, lst.get(mpi));
                        } else if (mpi.getTypeClass() == null) {
                            //header, but not mapped to a param on the method
                            if (removes == null) {
                                removes = new ArrayList<Integer>();
                            }
                            removes.add(part.getIndex());
                        }
                    } else {
                        ++count;
                    }
                }
                if (count == 0) {
                    newParams.clear();
                } else if (removes != null) {
                    Collections.sort(removes, Collections.reverseOrder());
                    for (Integer i : removes) {
                        if (i < newParams.size()) {
                            newParams.remove(i.intValue());
                        }
                    }
                }
                
            } catch (Exception e) {
                throw new Fault(e);
            }
            
            message.setContent(List.class, newParams);
        }
    }
    
    private WrapperHelper createWrapperHelper(WrapperCapableDatabinding dataBinding, 
                                              MessageInfo messageInfo,
                                              MessageInfo wrappedMessageInfo,
                                              Class<?> wrapperClass) {
        List<String> partNames = new ArrayList<String>();
        List<String> elTypeNames = new ArrayList<String>();
        List<Class<?>> partClasses = new ArrayList<Class<?>>();
        
        for (MessagePartInfo p : messageInfo.getMessageParts()) {
            if (Boolean.TRUE.equals(p.getProperty(ReflectionServiceFactoryBean.HEADER))) {
                if (p.getTypeClass() != null) {
                    int idx = p.getIndex();
                    ensureSize(elTypeNames, idx);
                    ensureSize(partClasses, idx);
                    ensureSize(partNames, idx);
                    elTypeNames.set(idx, null);
                    partClasses.set(idx, null);
                    partNames.set(idx, null);
                }
            } else {
                String elementType = null;
                if (p.getTypeQName() == null) {
                    // handling anonymous complex type
                    elementType = null;
                } else {
                    elementType = p.getTypeQName().getLocalPart();
                }
                int idx = p.getIndex();
                ensureSize(elTypeNames, idx);
                ensureSize(partClasses, idx);
                ensureSize(partNames, idx);
                
                elTypeNames.set(idx, elementType);
                partClasses.set(idx, p.getTypeClass());
                partNames.set(idx, p.getName().getLocalPart());
            }
        }
        return dataBinding.createWrapperHelper(wrapperClass,
                                                  partNames,
                                                  elTypeNames,
                                                  partClasses);
    }
    private void ensureSize(List<?> lst, int idx) {
        while (idx >= lst.size()) {
            lst.add(null);
        }
    }
}
