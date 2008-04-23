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

import javax.xml.namespace.QName;
import javax.xml.ws.WebFault;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.UnwrappedOperationInfo;

public class WebFaultInInterceptor extends AbstractPhaseInterceptor<Message> {

    public WebFaultInInterceptor() {
        super(Phase.PRE_LOGICAL);
    }

    public void handleMessage(Message message) throws Fault {
        Exception ex = message.getContent(Exception.class);
        if (ex != null) {
            message.put(Message.RESPONSE_CODE, Integer.valueOf(500));
        }
        
        if (ex instanceof Fault) {
            Fault f = (Fault) ex;
            ex = (Exception) f.getCause();
        }
        if (ex == null) { 
            return;
        }


        QName faultName = this.getFaultName(ex);
        if (faultName == null) {
            return;
        }

        BindingOperationInfo boi = message.getExchange().get(BindingOperationInfo.class);
        MessagePartInfo part = getFaultMessagePart(faultName, boi.getOperationInfo());

        if (part != null) {
            message.setContent(Exception.class, ex);
        }
    }

    private QName getFaultName(Exception webFault) {
        QName faultName = null;
        WebFault wf = webFault.getClass().getAnnotation(WebFault.class);
        if (wf != null) {
            faultName = new QName(wf.targetNamespace(), wf.name());
        }
            
        return faultName;
    }
    
    private MessagePartInfo getFaultMessagePart(QName qname, OperationInfo op) {
        if (op.isUnwrapped()) {
            op = ((UnwrappedOperationInfo)op).getWrappedOperation();
        }
        
        for (FaultInfo faultInfo : op.getFaults()) {
            for (MessagePartInfo mpi : faultInfo.getMessageParts()) {
                String ns = null;
                if (mpi.isElement()) {
                    ns = mpi.getElementQName().getNamespaceURI();
                } else {
                    ns = mpi.getTypeQName().getNamespaceURI();
                }
                if (qname.getLocalPart().equals(mpi.getConcreteName().getLocalPart()) 
                        && qname.getNamespaceURI().equals(ns)) {
                    return mpi;
                }
            }
            
        }
        return null;
    }    
}
