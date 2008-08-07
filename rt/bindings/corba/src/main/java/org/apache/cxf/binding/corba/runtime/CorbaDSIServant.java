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
package org.apache.cxf.binding.corba.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.CorbaBindingException;
import org.apache.cxf.binding.corba.CorbaDestination;
import org.apache.cxf.binding.corba.CorbaMessage;
import org.apache.cxf.binding.corba.CorbaTypeMap;
import org.apache.cxf.binding.corba.wsdl.BindingType;
import org.apache.cxf.binding.corba.wsdl.OperationType;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.transport.MessageObserver;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ServerRequest;
import org.omg.PortableServer.DynamicImplementation;
import org.omg.PortableServer.POA;

public class CorbaDSIServant extends DynamicImplementation {

    private static final Logger LOG = LogUtils.getL7dLogger(CorbaDSIServant.class);
    private ORB orb;
    private POA servantPOA;    
    private List<String> interfaces;
    private MessageObserver incomingObserver;
    private CorbaDestination destination;
    private Map<String, QName> operationMap;
    private CorbaTypeMap typeMap;
    
    public CorbaDSIServant() {
        //Complete
    }
    
    public void init(ORB theOrb,
                     POA poa,
                     CorbaDestination dest,
                     MessageObserver observer) {
        init(theOrb, poa, dest, observer, null);
    }

    public void init(ORB theOrb,
                     POA poa,
                     CorbaDestination dest,
                     MessageObserver observer,
                     CorbaTypeMap map) {
        orb = theOrb;
        servantPOA = poa;
        destination = dest;
        incomingObserver = observer;
        typeMap = map;
       
        // Get the list of interfaces that this servant will support
        try {                
            BindingType bindType = destination.getBindingInfo().getExtensor(BindingType.class);            
            if (bindType == null) {
                throw new CorbaBindingException("Unable to determine corba binding information");
            }

            List<String> bases = bindType.getBases();
            interfaces = new ArrayList<String>();
            interfaces.add(bindType.getRepositoryID());
            for (Iterator<String> iter = bases.iterator(); iter.hasNext();) {
                interfaces.add(iter.next());
            }
        } catch (java.lang.Exception ex) {
            LOG.log(Level.SEVERE, "Couldn't initialize the corba DSI servant");
            throw new CorbaBindingException(ex);
        }

        // Build the list of CORBA operations and the WSDL operations they map to.  Note that
        // the WSDL operation name may not always match the CORBA operation name.
        BindingInfo bInfo = destination.getBindingInfo();
        Iterator i = bInfo.getOperations().iterator();
        
        operationMap = new HashMap<String, QName>(bInfo.getOperations().size());

        while (i.hasNext()) {
            BindingOperationInfo bopInfo = (BindingOperationInfo)i.next();
            OperationType opType = bopInfo.getExtensor(OperationType.class);
            if (opType != null) {
                operationMap.put(opType.getName(), bopInfo.getName());
            }
        }
    }

    public MessageObserver getObserver() {
        return incomingObserver;
    }
    
    public void setObserver(MessageObserver observer) {
        incomingObserver = observer;
    }
    
    public ORB getOrb() {
        return orb;
    }

    public CorbaDestination getDestination() {
        return destination;
    }

    public Map<String, QName> getOperationMapping() {
        return operationMap;
    }

    public void setOperationMapping(Map<String, QName> map) {
        operationMap = map;
    }

    public void setCorbaTypeMap(CorbaTypeMap map) {
        typeMap = map;
    }
    
    public void invoke(ServerRequest request) throws CorbaBindingException {
        String opName = request.operation();
        QName requestOperation = operationMap.get(opName);
        
        MessageImpl msgImpl = new MessageImpl();
        msgImpl.setDestination(getDestination());
        Exchange exg = new ExchangeImpl();
        exg.put(String.class, requestOperation.getLocalPart());
        exg.put(ORB.class, getOrb());
        exg.put(ServerRequest.class, request);
        msgImpl.setExchange(exg);
        CorbaMessage msg = new CorbaMessage(msgImpl);
        msg.setCorbaTypeMap(typeMap);
        
        // If there's no output message part in our operation then it's a oneway op
        BindingMessageInfo bindingMsgOutputInfo = null;
        BindingOperationInfo bindingOpInfo = null;
        try {
            bindingOpInfo = this.destination.getEndPointInfo().getBinding().getOperation(requestOperation);
        } catch (Exception ex) {
            throw new CorbaBindingException("Invalid Request. Operation unknown: " + opName);
        }
        if (bindingOpInfo != null) {
            bindingMsgOutputInfo = bindingOpInfo.getOutput();
            if (bindingMsgOutputInfo == null) {
                exg.setOneWay(true);
            } 
        }
        
        // invokes the interceptors
        getObserver().onMessage(msg);
    }
       
    public String[] _all_interfaces(POA poa, byte[] objectId) {
        return interfaces.toArray(new String[interfaces.size()]);
    }

    public POA _default_POA() {
        return servantPOA;
    }
}
