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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
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
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.workqueue.SynchronousExecutor;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.RelatesToType;
import org.apache.cxf.ws.rm.manager.SourcePolicyType;
import org.apache.cxf.ws.rm.v200702.CloseSequenceType;
import org.apache.cxf.ws.rm.v200702.CreateSequenceResponseType;
import org.apache.cxf.ws.rm.v200702.CreateSequenceType;
import org.apache.cxf.ws.rm.v200702.Expires;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.OfferType;
import org.apache.cxf.ws.rm.v200702.TerminateSequenceType;

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
        final ProtocolVariation protocol = ds.getProtocol(); 
        String address = ds.getAcksTo().getAddress().getValue();
        if (RMUtils.getAddressingConstants().getAnonymousURI().equals(address)) {
            LOG.log(Level.WARNING, "STANDALONE_ANON_ACKS_NOT_SUPPORTED");
            return;
        }
        RMConstants constants = protocol.getConstants();
        OperationInfo oi = reliableEndpoint.getEndpoint(protocol).getEndpointInfo()
            .getService().getInterface().getOperation(constants.getSequenceAckOperationName());
        invoke(oi, protocol, new Object[] {ds});
    }
    
    void terminate(SourceSequence ss) throws RMException {
        ProtocolVariation protocol = ss.getProtocol(); 
        RMConstants constants = protocol.getConstants();
        OperationInfo oi = reliableEndpoint.getEndpoint(protocol).getEndpointInfo()
            .getService().getInterface().getOperation(constants.getTerminateSequenceOperationName());
        
        TerminateSequenceType ts = new TerminateSequenceType();
        ts.setIdentifier(ss.getIdentifier());
        ts.setLastMsgNumber(ss.getCurrentMessageNr());
        EncoderDecoder codec = protocol.getCodec();
        invoke(oi, protocol, new Object[] {codec.convertToSend(ts)});
    }
    
    void terminate(DestinationSequence ds) throws RMException {
        ProtocolVariation protocol = ds.getProtocol(); 
        RMConstants constants = protocol.getConstants();
        OperationInfo oi = reliableEndpoint.getEndpoint(protocol).getEndpointInfo()
            .getService().getInterface().getOperation(constants.getTerminateSequenceOperationName());
        
        TerminateSequenceType ts = new TerminateSequenceType();
        ts.setIdentifier(ds.getIdentifier());
        ts.setLastMsgNumber(ds.getLastMessageNumber());
        EncoderDecoder codec = protocol.getCodec();
        invoke(oi, protocol, new Object[] {codec.convertToSend(ts)});
    }
    
    void createSequenceResponse(final Object createResponse, ProtocolVariation protocol) throws RMException {
        LOG.fine("sending CreateSequenceResponse from client side");
        RMConstants constants = protocol.getConstants();
        final OperationInfo oi = reliableEndpoint.getEndpoint(protocol).getEndpointInfo().getService()
            .getInterface().getOperation(constants.getCreateSequenceResponseOnewayOperationName());
        
        // TODO: need to set relatesTo

        invoke(oi, protocol, new Object[] {createResponse});
       
    }

    public CreateSequenceResponseType createSequence(EndpointReferenceType defaultAcksTo, RelatesToType relatesTo, 
             boolean isServer, final ProtocolVariation protocol, final Exchange exchange, Map<String, Object> context) 
        throws RMException {
        SourcePolicyType sp = reliableEndpoint.getManager().getSourcePolicy();
        CreateSequenceType create = new CreateSequenceType();        

        String address = sp.getAcksTo();
        EndpointReferenceType acksTo = null;
        if (null != address) {
            acksTo = RMUtils.createReference(address);
        } else {
            acksTo = defaultAcksTo; 
        }
        create.setAcksTo(acksTo);

        Duration d = sp.getSequenceExpiration();
        if (null != d) {
            Expires expires = new Expires();
            expires.setValue(d);  
            create.setExpires(expires);
        }
        
        if (sp.isIncludeOffer()) {
            OfferType offer = new OfferType();
            d = sp.getOfferedSequenceExpiration();
            if (null != d) {
                Expires expires = new Expires();
                expires.setValue(d);  
                offer.setExpires(expires);
            }
            offer.setIdentifier(reliableEndpoint.getSource().generateSequenceIdentifier());
            offer.setEndpoint(acksTo);
            create.setOffer(offer);
            setOfferedIdentifier(offer);
        }
        
        InterfaceInfo ii = reliableEndpoint.getEndpoint(protocol).getEndpointInfo()
            .getService().getInterface();
        
        EncoderDecoder codec = protocol.getCodec();
        RMConstants constants = codec.getConstants();
        final OperationInfo oi = isServer 
            ? ii.getOperation(constants.getCreateSequenceOnewayOperationName())
            : ii.getOperation(constants.getCreateSequenceOperationName());
        final Object send = codec.convertToSend(create);
            
        // tried using separate thread - did not help either
        
        if (isServer) {
            LOG.fine("sending CreateSequenceRequest from server side");
            Runnable r = new Runnable() {
                public void run() {
                    try {
                        invoke(oi, protocol, new Object[] {send}, null, exchange);
                    } catch (RMException ex) {
                        // already logged
                    }
                }
            };
            Executor ex = reliableEndpoint.getApplicationEndpoint().getExecutor();
            if (ex == null) {
                ex = SynchronousExecutor.getInstance();
            }
            ex.execute(r);
            return null;
        }
        
        
        Object resp = invoke(oi, protocol, new Object[] {send}, context, exchange);
        return codec.convertReceivedCreateSequenceResponse(resp);
    }
    
    void lastMessage(SourceSequence s) throws RMException {
        final ProtocolVariation protocol = s.getProtocol();
        EndpointReferenceType target = s.getTarget();
        AttributedURIType uri = null;
        if (null != target) {
            uri = target.getAddress();
        }
        String addr = null;
        if (null != uri) {
            addr = uri.getValue();
        }
        
        if (addr == null) {
            LOG.log(Level.WARNING, "STANDALONE_CLOSE_SEQUENCE_NO_TARGET_MSG");
            return;
        }
        
        if (RMUtils.getAddressingConstants().getAnonymousURI().equals(addr)) {
            LOG.log(Level.WARNING, "STANDALONE_CLOSE_SEQUENCE_ANON_TARGET_MSG");
            return; 
        }
        RMConstants constants = protocol.getConstants();
        OperationInfo oi = reliableEndpoint.getEndpoint(protocol).getEndpointInfo().getService()
            .getInterface().getOperation(constants.getCloseSequenceOperationName());
        // pass reference to source sequence in invocation context
        Map<String, Object> context = new HashMap<String, Object>(
                Collections.singletonMap(SourceSequence.class.getName(), 
                                         (Object)s));

        if (constants instanceof RM11Constants) {
            CloseSequenceType csr = new CloseSequenceType();
            csr.setIdentifier(s.getIdentifier());
            csr.setLastMsgNumber(s.getCurrentMessageNr());
            invoke(oi, protocol, new Object[] {csr}, context);
        } else {
            invoke(oi, protocol, new Object[] {}, context);
        }
    }
    
    void ackRequested(SourceSequence s) throws RMException {
        final ProtocolVariation protocol = s.getProtocol();
        EndpointReferenceType target = s.getTarget();
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
        
        RMConstants constants = protocol.getConstants();
        OperationInfo oi = reliableEndpoint.getEndpoint(protocol).getEndpointInfo().getService()
            .getInterface().getOperation(constants.getAckRequestedOperationName());
        invoke(oi, protocol, new Object[] {});
    }
        
    Identifier getOfferedIdentifier() {
        return offeredIdentifier;    
    }
    
    void setOfferedIdentifier(OfferType offer) { 
        if (offer != null) {
            offeredIdentifier = offer.getIdentifier();
        }
    }
       
    Object invoke(OperationInfo oi, ProtocolVariation protocol, 
                  Object[] params, Map<String, Object> context, Exchange exchange) throws RMException {
        
        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "Sending out-of-band RM protocol message {0}.", 
                    oi == null ? null : oi.getName());
        }
        
        RMManager manager = reliableEndpoint.getManager();
        Bus bus = manager.getBus();
        Endpoint endpoint = reliableEndpoint.getEndpoint(protocol);
        BindingInfo bi = reliableEndpoint.getBindingInfo(protocol);
        Conduit c = reliableEndpoint.getConduit();
        Client client = null;
        if (params.length > 0 && params[0] instanceof DestinationSequence) {
            EndpointReferenceType acksTo = ((DestinationSequence)params[0]).getAcksTo();
            String acksAddress = acksTo.getAddress().getValue();
            AttributedURIType attrURIType =  new AttributedURIType();
            attrURIType.setValue(acksAddress);
            EndpointReferenceType acks =  new EndpointReferenceType();
            acks.setAddress(attrURIType);
            client = createClient(bus, endpoint, protocol, c, acks);
            params = new Object[] {};
        } else {
            EndpointReferenceType replyTo = reliableEndpoint.getReplyTo();
            client = createClient(bus, endpoint, protocol, c, replyTo);
        }
        
        BindingOperationInfo boi = bi.getOperation(oi);
        try {
            if (context != null) {
                client.getRequestContext().putAll(context);
            }
            Object[] result = client.invoke(boi, params, context, exchange);
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
    
    Object invoke(OperationInfo oi, ProtocolVariation protocol, Object[] params, Map<String, Object> context)
        throws RMException {
        return invoke(oi, protocol, params, context, new ExchangeImpl());
    }
    
    Object invoke(OperationInfo oi, ProtocolVariation protocol, Object[] params) throws RMException {
        return invoke(oi, protocol, params, null);
    }
    
    protected Client createClient(Bus bus, Endpoint endpoint, final ProtocolVariation protocol, 
                                  Conduit conduit, final EndpointReferenceType address) {
        ConduitSelector cs = new DeferredConduitSelector(conduit) {
            @Override
            public synchronized Conduit selectConduit(Message message) {
                Conduit conduit = null;
                EndpointInfo endpointInfo = getEndpoint().getEndpointInfo();
                EndpointReferenceType original = 
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
        RMClient client = new RMClient(bus, endpoint, cs);
        Map<String, Object> context = client.getRequestContext();
        context.put(RMManager.WSRM_VERSION_PROPERTY, protocol.getWSRMNamespace());
        context.put(RMManager.WSRM_WSA_VERSION_PROPERTY, protocol.getWSANamespace());
        return client;
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
