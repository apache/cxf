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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.interceptor.MessageSenderInterceptor.MessageSenderEndingInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.policy.PolicyVerificationOutInterceptor;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceType;
import org.apache.cxf.ws.rm.v200702.TerminateSequenceType;
import org.apache.cxf.ws.security.SecurityConstants;

/**
 * 
 */
public class RMCaptureOutInterceptor extends AbstractRMInterceptor<Message>  {
    
    private static final Logger LOG = LogUtils.getL7dLogger(RMCaptureOutInterceptor.class);
 
    public RMCaptureOutInterceptor() {
        super(Phase.PRE_STREAM);
        addBefore(AttachmentOutInterceptor.class.getName());
        addBefore(LoggingOutInterceptor.class.getName());
    }
    
    protected void handle(Message msg) throws SequenceFault, RMException {  
        AddressingProperties maps = ContextUtils.retrieveMAPs(msg, false, true,  false);
        if (null == maps) {
            LogUtils.log(LOG, Level.WARNING, "MAPS_RETRIEVAL_FAILURE_MSG");
            return;
        }
        if (Boolean.TRUE.equals(msg.get(RMMessageConstants.RM_RETRANSMISSION))) {
            return;
        }
        if (isRuntimeFault(msg)) {
            LogUtils.log(LOG, Level.WARNING, "RUNTIME_FAULT_MSG");
            // in case of a SequenceFault or other WS-RM related fault, set action appropriately.
            // the received inbound maps is available to extract some values in case if needed.
            Throwable cause = msg.getContent(Exception.class).getCause();
            if (cause instanceof SequenceFault || cause instanceof RMException) {
                maps.getAction().setValue(getAddressingNamespace(maps) + "/fault");
            }
            return;
        }

        Source source = getManager().getSource(msg);
        
        RMConfiguration config = getManager().getEffectiveConfiguration(msg);
        String wsaNamespace = config.getAddressingNamespace();
        String rmNamespace = config.getRMNamespace();
        ProtocolVariation protocol = ProtocolVariation.findVariant(rmNamespace, wsaNamespace);
        RMContextUtils.setProtocolVariation(msg, protocol);
        maps.exposeAs(wsaNamespace);

        String action = null;
        if (null != maps.getAction()) {
            action = maps.getAction().getValue();
        }
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Action: " + action);
        }

        boolean isApplicationMessage = !RMContextUtils.isRMProtocolMessage(action);
        boolean isPartialResponse = MessageUtils.isPartialResponse(msg);
        RMConstants constants = protocol.getConstants();
        boolean isLastMessage = constants.getCloseSequenceAction().equals(action);
        
        RMProperties rmpsOut = RMContextUtils.retrieveRMProperties(msg, true);
        if (null == rmpsOut) {
            rmpsOut = new RMProperties();
            rmpsOut.exposeAs(protocol.getWSRMNamespace());
            RMContextUtils.storeRMProperties(msg, rmpsOut, true);
        }
        
        // Activate process response for oneWay
        if (msg.getExchange().isOneWay()) {
            msg.getExchange().put(Message.PROCESS_ONEWAY_RESPONSE, true);
        }
        
        RMProperties rmpsIn = null;
        Identifier inSeqId = null;
        long inMessageNumber = 0;
        
        if (isApplicationMessage) {
            rmpsIn = RMContextUtils.retrieveRMProperties(msg, false);
            if (null != rmpsIn && null != rmpsIn.getSequence()) {
                inSeqId = rmpsIn.getSequence().getIdentifier();
                inMessageNumber = rmpsIn.getSequence().getMessageNumber();
            }
            ContextUtils.storeDeferUncorrelatedMessageAbort(msg);
        }
        
        Map<?, ?> invocationContext = (Map<?, ?>)msg.get(Message.INVOCATION_CONTEXT);
        if ((isApplicationMessage || (isLastMessage && invocationContext != null)) && !isPartialResponse) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("inbound sequence: " + (null == inSeqId ? "null" : inSeqId.getValue()));
            }
            
            // get the current sequence, requesting the creation of a new one if necessary
            synchronized (source) {
                SourceSequence seq = null;
                if (isLastMessage) {
                    seq = (SourceSequence)invocationContext.get(SourceSequence.class.getName());
                } else {
                    seq = getManager().getSequence(inSeqId, msg, maps);
                }
                assert null != seq;

                // increase message number and store a sequence type object in
                // context
                seq.nextMessageNumber(inSeqId, inMessageNumber, isLastMessage);
                
                if (Boolean.TRUE.equals(msg.getContextualProperty(RMManager.WSRM_LAST_MESSAGE_PROPERTY))) {
                    // mark the message as the last one
                    seq.setLastMessage(true);
                }
                
                rmpsOut.setSequence(seq);

                // if this was the last message in the sequence, reset the
                // current sequence so that a new one will be created next
                // time the handler is invoked

                if (seq.isLastMessage()) {
                    source.setCurrent(null);
                }
            }
        } else if (!MessageUtils.isRequestor(msg) && constants.getCreateSequenceAction().equals(action)) {
            maps.getAction().setValue(constants.getCreateSequenceResponseAction());
        } else if (isPartialResponse && action == null
            && isResponseToAction(msg, constants.getSequenceAckAction())) {
            Collection<SequenceAcknowledgement> acks = rmpsIn.getAcks();
            if (acks.size() == 1) {
                SourceSequence ss = source.getSequence(acks.iterator().next().getIdentifier());
                if (ss != null && ss.allAcknowledged()) {
                    setAction(maps, constants.getTerminateSequenceAction());
                    setTerminateSequence(msg, ss.getIdentifier(), protocol);
                    msg.remove(Message.EMPTY_PARTIAL_RESPONSE_MESSAGE);
                    // removing this sequence now. See the comment in SourceSequence.setAcknowledged()
                    source.removeSequence(ss);
                }
            }
        }
        
        // capture message if retranmission possible
        if (isApplicationMessage && !isPartialResponse) {
            getManager().initializeInterceptorChain(msg);
            captureMessage(msg);
        }
    }

    private void captureMessage(Message message) {
        Message capture = new MessageImpl();
        capture.setId(message.getId());
        capture.put(RMMessageConstants.MESSAGE_CAPTURE_CHAIN, Boolean.TRUE);
        Iterator<Class<?>> citer = message.getContentFormats().iterator();
        while (citer.hasNext()) {
            Class<?> clas = citer.next();
            if (OutputStream.class != clas) {
                
                // clone contents list so changes won't effect original message
                Object content = message.getContent(clas);
                if (content instanceof MessageContentsList) {
                    content = new MessageContentsList((MessageContentsList)content);
                }
                capture.setContent(clas, content);
            }
        }
        Iterator<String> kiter = message.keySet().iterator();
        while (kiter.hasNext()) {
            String key = kiter.next();
            capture.put(key, message.get(key));
        }
        kiter = message.getContextualPropertyKeys().iterator();
        while (kiter.hasNext()) {
            String key = kiter.next();
            capture.setContextualProperty(key, message.getContextualProperty(key));
        }
        if (message instanceof SoapMessage) {
            capture = new SoapMessage(capture);
            ((SoapMessage)capture).setVersion(((SoapMessage)message).getVersion());
        }
        
        // eliminate all other RM interceptors, along with attachment and security and message loss interceptors, from
        //  capture chain
        PhaseInterceptorChain chain = (PhaseInterceptorChain)message.getInterceptorChain();
        PhaseInterceptorChain cchain = chain.cloneChain();
        ListIterator<Interceptor<? extends Message>> iterator = cchain.getIterator();
        boolean past = false;
        boolean ending = false;
        while (iterator.hasNext()) {
            PhaseInterceptor<? extends Message> intercept = (PhaseInterceptor<? extends Message>)iterator.next();
            String id = intercept.getId();
            if (RMCaptureOutInterceptor.class.getName().equals(id)) {
                past = true;
            } else if (past && id != null) {
                if ((id.startsWith(RMCaptureOutInterceptor.class.getPackage().getName())
                    && !(id.equals(RetransmissionInterceptor.class.getName())))
                    || id.startsWith(SecurityConstants.class.getPackage().getName())
                    || PolicyVerificationOutInterceptor.class.getName().equals(id)
                    || AttachmentOutInterceptor.class.getName().equals(id)
                    || LoggingOutInterceptor.class.getName().equals(id)
                    || "org.apache.cxf.systest.ws.rm.MessageLossSimulator$MessageLossEndingInterceptor".equals(id)) {
                    cchain.remove(intercept);
                } else if (MessageSenderEndingInterceptor.class.getName().equals(id)) {
                    ending = true;
                }
            }
        }
        if (!ending) {
            
            // add normal ending interceptor back in, in case removed by MessageLossSimulator
            cchain.add(new MessageSenderEndingInterceptor());
        }
        capture.setInterceptorChain(cchain);
        LoadingByteArrayOutputStream bos = new LoadingByteArrayOutputStream();
        capture.setContent(OutputStream.class, bos);
        ExchangeImpl captureExchange = new ExchangeImpl((ExchangeImpl)message.getExchange());
        capture.setExchange(captureExchange);
        captureExchange.setOutMessage(capture);
        captureExchange.setConduit(new AbstractConduit(captureExchange.getConduit(capture).getTarget()) {
            
            @Override
            public void prepare(Message message) throws IOException {
            }
            
            @Override
            protected Logger getLogger() {
                return null;
            }
            
        });
        cchain.doInterceptStartingAfter(capture, RMCaptureOutInterceptor.class.getName());
        try {
            
            RMProperties rmps = RMContextUtils.retrieveRMProperties(message, true);
            SequenceType sequence = rmps.getSequence();
            Long number = sequence.getMessageNumber();
            Identifier sid = sequence.getIdentifier();
            if (LOG.isLoggable(Level.INFO)) {
                LOG.log(Level.INFO, "Captured message " + number + " in sequence " + sid.getValue());
            }
            
            // save message for potential retransmission
            ByteArrayInputStream bis = bos.createInputStream();
            message.put(RMMessageConstants.SAVED_CONTENT, RewindableInputStream.makeRewindable(bis));
            RMManager manager = getManager();
            manager.getRetransmissionQueue().start();
            manager.getRetransmissionQueue().addUnacknowledged(message);
            RMStore store = manager.getStore();
            if (null != store) {
                
                // persist message to store
                Source s = manager.getSource(message);
                SourceSequence ss = s.getSequence(sid);
                RMMessage msg = new RMMessage();
                msg.setMessageNumber(number);
                if (!MessageUtils.isRequestor(message)) {
                    AddressingProperties maps = RMContextUtils.retrieveMAPs(message, false, true);
                    if (null != maps && null != maps.getTo()) {
                        msg.setTo(maps.getTo().getValue());
                    }
                }
                msg.setContent(bis);
                store.persistOutgoing(ss, msg);
            }
                
        } catch (RMException e) {
            // ignore
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Error persisting message", e);
        } 
    }

    private String getAddressingNamespace(AddressingProperties maps) {
        String wsaNamespace = maps.getNamespaceURI();
        if (wsaNamespace == null) {
            getManager().getConfiguration().getAddressingNamespace();
        }
        return wsaNamespace;
    }
    
    boolean isRuntimeFault(Message message) {
        FaultMode mode = MessageUtils.getFaultMode(message);
        if (null == mode) {
            return false;
        }
        return FaultMode.CHECKED_APPLICATION_FAULT != mode;
    }

    private boolean isResponseToAction(Message msg, String action) {
        AddressingProperties inMaps = RMContextUtils.retrieveMAPs(msg, false, false);
        String inAction = null;
        if (null != inMaps.getAction()) {
            inAction = inMaps.getAction().getValue();
        }
        return action.equals(inAction);
    }
    
    private void setTerminateSequence(Message msg, Identifier identifier, ProtocolVariation protocol) 
        throws RMException {
        TerminateSequenceType ts = new TerminateSequenceType();
        ts.setIdentifier(identifier);
        MessageContentsList contents = 
            new MessageContentsList(new Object[]{protocol.getCodec().convertToSend(ts)});
        msg.setContent(List.class, contents);

        // create a new exchange for this output-only exchange
        Exchange newex = new ExchangeImpl();
        Exchange oldex = msg.getExchange();
        
        newex.put(Bus.class, oldex.getBus());
        newex.put(Endpoint.class, oldex.getEndpoint());
        newex.put(Service.class, oldex.getEndpoint().getService());
        newex.put(Binding.class, oldex.getEndpoint().getBinding());
        newex.setConduit(oldex.getConduit(msg));
        newex.setDestination(oldex.getDestination());
        
        //Setup the BindingOperationInfo
        RMEndpoint rmep = getManager().getReliableEndpoint(msg);
        OperationInfo oi = rmep.getEndpoint(protocol).getEndpointInfo().getService().getInterface()
            .getOperation(protocol.getConstants().getTerminateSequenceAnonymousOperationName());
        BindingInfo bi = rmep.getBindingInfo(protocol);
        BindingOperationInfo boi = bi.getOperation(oi);
        
        newex.put(BindingInfo.class, bi);
        newex.put(BindingOperationInfo.class, boi);
        newex.put(OperationInfo.class, boi.getOperationInfo());
        
        msg.setExchange(newex);
        newex.setOutMessage(msg);
    }

    private static void setAction(AddressingProperties maps, String action) {
        AttributedURIType actionURI = new AttributedURIType();
        actionURI.setValue(action);
        maps.setAction(actionURI);
    }
}
