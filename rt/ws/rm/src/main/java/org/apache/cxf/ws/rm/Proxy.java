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

package org.apache.cxf.ws.rm;


import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.datatype.Duration;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.DeferredConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.RelatesToType;
import org.apache.cxf.ws.addressing.v200408.EndpointReferenceType;
import org.apache.cxf.ws.rm.manager.SourcePolicyType;


/**
 * 
 */
public class Proxy {

    private static final Logger LOG = LogUtils.getL7dLogger(Proxy.class);

    private RMEndpoint reliableEndpoint;
    // REVISIT assumption there is only a single outstanding offer
    private Identifier offeredIdentifier;
    

    public Proxy(RMEndpoint rme) {
        reliableEndpoint = rme;
    }

    RMEndpoint getReliableEndpoint() {
        return reliableEndpoint;
    }

    void acknowledge(DestinationSequence ds) throws RMException {        
        if (RMConstants.getAnonymousAddress().equals(ds.getAcksTo().getAddress().getValue())) {
            LOG.log(Level.WARNING, "STANDALONE_ANON_ACKS_NOT_SUPPORTED");
            return;
        }
        
        OperationInfo oi = reliableEndpoint.getEndpoint().getEndpointInfo().getService().getInterface()
            .getOperation(RMConstants.getSequenceAckOperationName());
        invoke(oi, new Object[] {ds}, null);
    }
    
    void terminate(SourceSequence ss) throws RMException {
        OperationInfo oi = reliableEndpoint.getEndpoint().getEndpointInfo().getService().getInterface()
            .getOperation(RMConstants.getTerminateSequenceOperationName());
        
        TerminateSequenceType ts = RMUtils.getWSRMFactory().createTerminateSequenceType();
        ts.setIdentifier(ss.getIdentifier());
        invoke(oi, new Object[] {ts}, null);
    }
    
    void createSequenceResponse(final CreateSequenceResponseType createResponse) throws RMException {
        LOG.fine("sending CreateSequenceResponse from client side");
        final OperationInfo oi = reliableEndpoint.getEndpoint().getEndpointInfo().getService().getInterface()
            .getOperation(RMConstants.getCreateSequenceResponseOnewayOperationName());
        
        // TODO: need to set relatesTo

        invoke(oi, new Object[] {createResponse}, null);
       
    }

    public CreateSequenceResponseType createSequence(
                        EndpointReferenceType defaultAcksTo,
                        RelatesToType relatesTo,
                        boolean isServer) throws RMException {
        
        SourcePolicyType sp = reliableEndpoint.getManager().getSourcePolicy();
        final CreateSequenceType create = RMUtils.getWSRMFactory().createCreateSequenceType();        

        String address = sp.getAcksTo();
        EndpointReferenceType acksTo = null;
        if (null != address) {
            acksTo = RMUtils.createReference2004(address);
        } else {
            acksTo = defaultAcksTo; 
        }
        create.setAcksTo(acksTo);

        Duration d = sp.getSequenceExpiration();
        if (null != d) {
            Expires expires = RMUtils.getWSRMFactory().createExpires();
            expires.setValue(d);  
            create.setExpires(expires);
        }
        
        if (sp.isIncludeOffer()) {
            OfferType offer = RMUtils.getWSRMFactory().createOfferType();
            d = sp.getOfferedSequenceExpiration();
            if (null != d) {
                Expires expires = RMUtils.getWSRMFactory().createExpires();
                expires.setValue(d);  
                offer.setExpires(expires);
            }
            offer.setIdentifier(reliableEndpoint.getSource().generateSequenceIdentifier());
            create.setOffer(offer);
            setOfferedIdentifier(offer);
        }
        
        InterfaceInfo ii = reliableEndpoint.getEndpoint().getEndpointInfo().getService().getInterface();
        
        final OperationInfo oi = isServer 
            ? ii.getOperation(RMConstants.getCreateSequenceOnewayOperationName())
            : ii.getOperation(RMConstants.getCreateSequenceOperationName());
        
        // tried using separate thread - did not help either
        
        if (isServer) {
            LOG.fine("sending CreateSequenceRequest from server side");
            Runnable r = new Runnable() {
                public void run() {
                    try {
                        invoke(oi, new Object[] {create}, null);
                    } catch (RMException ex) {
                        // already logged
                    }
                }
            };
            reliableEndpoint.getApplicationEndpoint().getExecutor().execute(r);
            return null;
        }
        
        
        return (CreateSequenceResponseType)invoke(oi, new Object[] {create}, null);
    }
    
    void lastMessage(SourceSequence s) throws RMException {
        org.apache.cxf.ws.addressing.EndpointReferenceType target = s.getTarget();
        AttributedURIType uri = null;
        if (null != target) {
            uri = target.getAddress();
        }
        String addr = null;
        if (null != uri) {
            addr = uri.getValue();
        }
        
        if (addr == null) {
            LOG.log(Level.WARNING, "STANDALONE_LAST_MESSAGE_NO_TARGET_MSG");
            return;
        }
        
        if (RMUtils.getAddressingConstants().getAnonymousURI().equals(addr)) {
            LOG.log(Level.WARNING, "STANDALONE_LAST_MESSAGE_ANON_TARGET_MSG");
            return; 
        }
        
        OperationInfo oi = reliableEndpoint.getEndpoint().getEndpointInfo().getService().getInterface()
            .getOperation(RMConstants.getLastMessageOperationName());
        // pass reference to source sequence in invocation context
        Map<String, Object> context = Collections.singletonMap(SourceSequence.class.getName(), (Object)s);

        invoke(oi, new Object[] {}, context);
    }
    
    void ackRequested(SourceSequence s) throws RMException {
        org.apache.cxf.ws.addressing.EndpointReferenceType target = s.getTarget();
        AttributedURIType uri = null;
        if (null != target) {
            uri = target.getAddress();
        }
        String addr = null;
        if (null != uri) {
            addr = uri.getValue();
        }
        
        if (addr == null) {
            LOG.log(Level.WARNING, "STANDALONE_ACK_REQUESTED_NO_TARGET_MSG");
            return;
        }
        
        if (RMUtils.getAddressingConstants().getAnonymousURI().equals(addr)) {
            LOG.log(Level.WARNING, "STANDALONE_ACK_REQUESTED_ANON_TARGET_MSG");
            return; 
        }
        
        OperationInfo oi = reliableEndpoint.getEndpoint().getEndpointInfo().getService().getInterface()
            .getOperation(RMConstants.getAckRequestedOperationName());
        invoke(oi, new Object[] {}, null);
    }
        
    Identifier getOfferedIdentifier() {
        return offeredIdentifier;    
    }
    
    void setOfferedIdentifier(OfferType offer) { 
        if (offer != null) {
            offeredIdentifier = offer.getIdentifier();
        }
    }
       
    Object invoke(OperationInfo oi, Object[] params, Map<String, Object> context) throws RMException {
        
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "Sending out-of-band RM protocol message {0}.", 
                    oi == null ? null : oi.getName());
        }
        
        RMManager manager = reliableEndpoint.getManager();
        Bus bus = manager.getBus();
        Endpoint endpoint = reliableEndpoint.getEndpoint();
        BindingInfo bi = reliableEndpoint.getBindingInfo();
        Conduit c = reliableEndpoint.getConduit();
        Client client = null;
        if (params.length > 0 && params[0] instanceof DestinationSequence) {
            EndpointReferenceType acksTo = 
                ((DestinationSequence)params[0]).getAcksTo();
            String acksAddress = acksTo.getAddress().getValue();
            org.apache.cxf.ws.addressing.AttributedURIType attrURIType = 
                new org.apache.cxf.ws.addressing.AttributedURIType();
            attrURIType.setValue(acksAddress);
            org.apache.cxf.ws.addressing.EndpointReferenceType acks = 
                new org.apache.cxf.ws.addressing.EndpointReferenceType();
            acks.setAddress(attrURIType);
            client = createClient(bus, endpoint, c, acks);
            params = new Object[] {};
        } else {
            org.apache.cxf.ws.addressing.EndpointReferenceType replyTo = reliableEndpoint.getReplyTo();
            client = createClient(bus, endpoint, c, replyTo);
        }
        
        BindingOperationInfo boi = bi.getOperation(oi);
        try {
            Object[] result = client.invoke(boi, params, context);
            if (result != null && result.length > 0) {
                return result[0];
            }
            
        } catch (Exception ex) {  
            org.apache.cxf.common.i18n.Message msg = 
                new org.apache.cxf.common.i18n.Message("SEND_PROTOCOL_MSG_FAILED_EXC", LOG, 
                                                       oi == null ? null : oi.getName());
            LOG.log(Level.SEVERE, msg.toString(), ex);
            throw new RMException(msg, ex);
        }
        return null;
    }
    
    protected Client createClient(Bus bus, Endpoint endpoint, Conduit conduit,
                                  final org.apache.cxf.ws.addressing.EndpointReferenceType address) {
        ConduitSelector cs = new DeferredConduitSelector(conduit) {
            @Override
            public synchronized Conduit selectConduit(Message message) {
                Conduit conduit = null;
                EndpointInfo endpointInfo = getEndpoint().getEndpointInfo();
                org.apache.cxf.ws.addressing.EndpointReferenceType original = 
                    endpointInfo.getTarget();
                try {
                    if (null != address) {
                        endpointInfo.setAddress(address);
                    }
                    conduit = super.selectConduit(message);
                } finally {
                    endpointInfo.setAddress(original);
                }
                return conduit;
            }
        };  
        return new RMClient(bus, endpoint, cs);
    }
    
    class RMClient extends ClientImpl {

        RMClient(Bus bus, Endpoint endpoint, ConduitSelector cs) {
            super(bus, endpoint, cs);  
        }

        @Override
        public void onMessage(Message m) {
            m.getExchange().put(Endpoint.class, Proxy.this.reliableEndpoint.getApplicationEndpoint());
            super.onMessage(m);
        }    
    }
    
    // for test
    
    void setReliableEndpoint(RMEndpoint rme) {
        reliableEndpoint = rme;
    }
    

}
