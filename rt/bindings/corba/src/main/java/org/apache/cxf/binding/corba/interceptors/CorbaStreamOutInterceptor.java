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

package org.apache.cxf.binding.corba.interceptors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.binding.corba.CorbaMessage;
import org.apache.cxf.binding.corba.CorbaTypeMap;
import org.apache.cxf.binding.corba.runtime.CorbaStreamWriter;
import org.apache.cxf.binding.corba.utils.ContextUtils;
import org.apache.cxf.binding.corba.wsdl.ArgType;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.ModeType;
import org.apache.cxf.binding.corba.wsdl.OperationType;
import org.apache.cxf.binding.corba.wsdl.ParamType;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.ServiceInfo;


public class CorbaStreamOutInterceptor extends AbstractPhaseInterceptor<Message> {

    private org.omg.CORBA.ORB orb;
    private CorbaTypeMap typeMap;
    private ServiceInfo service;

    public CorbaStreamOutInterceptor() {
        super(Phase.PRE_STREAM);
    }

    public void handleMessage(Message msg) {
        CorbaMessage message = (CorbaMessage) msg;
        orb = (org.omg.CORBA.ORB) message.get(CorbaConstants.ORB);
        Exchange exchange = message.getExchange();
        service = exchange.get(ServiceInfo.class);
        typeMap = message.getCorbaTypeMap();
        BindingOperationInfo boi = exchange.get(BindingOperationInfo.class);
        if (ContextUtils.isRequestor(message)) {
            handleOutBoundMessage(message, boi);
        } else {
            handleInBoundMessage(message, boi);
        }
        message.getInterceptorChain().add(new CorbaStreamOutEndingInterceptor());
    }

    private void handleOutBoundMessage(CorbaMessage message, BindingOperationInfo boi) {
        boolean wrap = false;
        if (boi.isUnwrappedCapable()) {
            wrap = true;
        }
        OperationType opType = boi.getExtensor(OperationType.class);
        List<ParamType> paramTypes = opType.getParam();
        List<ArgType> params = new ArrayList<ArgType>();
        for (Iterator<ParamType> iter = paramTypes.iterator(); iter.hasNext();) {
            ParamType param = iter.next();
            if (!param.getMode().equals(ModeType.OUT)) {
                params.add((ArgType)param);
            }
        }
        CorbaStreamWriter writer = new CorbaStreamWriter(orb, params, typeMap, service, wrap);
        message.setContent(XMLStreamWriter.class, writer);
    }

    private void handleInBoundMessage(CorbaMessage message, BindingOperationInfo boi) {
        boolean wrap = false;
        if (boi.isUnwrappedCapable()) {
            wrap = true;
        }
        OperationType opType = boi.getExtensor(OperationType.class);

        ArgType returnParam = opType.getReturn();
        List<ParamType> paramTypes = opType.getParam();
        List<ArgType> params = new ArrayList<ArgType>();
        if (returnParam != null) {
            params.add(returnParam);
        }
        for (Iterator<ParamType> iter = paramTypes.iterator(); iter.hasNext();) {
            ParamType param = iter.next();
            if (!param.getMode().equals(ModeType.IN)) {
                params.add((ArgType)param);
            }
        }
        CorbaStreamWriter writer = new CorbaStreamWriter(orb, params, typeMap, service, wrap);
        message.setContent(XMLStreamWriter.class, writer);      
    }

    

}
