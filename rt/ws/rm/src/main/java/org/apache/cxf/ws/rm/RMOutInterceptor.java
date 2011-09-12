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

import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;

/**
 * 
 */
public class RMOutInterceptor extends AbstractRMInterceptor<Message>  {
    
    private static final Logger LOG = LogUtils.getL7dLogger(RMOutInterceptor.class);
 
    public RMOutInterceptor() {
        addAfter(MAPAggregator.class.getName());
    }
    
    protected void handle(Message msg) throws SequenceFault, RMException {  
        if (isRuntimeFault(msg)) {
            LogUtils.log(LOG, Level.WARNING, "RUNTIME_FAULT_MSG");
            // TODO: in case of a SequenceFault need to set action
            // to http://schemas.xmlsoap.org/ws/2004/a08/addressing/fault
            // but: need to defer propagation of received MAPS to outbound chain first           
            return;
        }
       
        AddressingPropertiesImpl maps = RMContextUtils.retrieveMAPs(msg, false, true);
        if (null == maps) {
            LogUtils.log(LOG, Level.WARNING, "MAPS_RETRIEVAL_FAILURE_MSG");
            return;
        }
        
        Source source = getManager().getSource(msg);
        ProtocolVariation protocol = source.getReliableEndpoint().getProtocol();
        maps.exposeAs(protocol.getWSANamespace());
        Destination destination = getManager().getDestination(msg);

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
        
        if (isApplicationMessage && !isPartialResponse) {
            RetransmissionInterceptor ri = new RetransmissionInterceptor();
            ri.setManager(getManager());
            // TODO:
            // On the server side: If a fault occurs after this interceptor we will switch 
            // interceptor chains (if this is not already a fault message) and therefore need to 
            // make sure the retransmission interceptor is added to the fault chain
            // 
            msg.getInterceptorChain().add(ri);
            LOG.fine("Added RetransmissionInterceptor to chain.");
            
            getManager().getRetransmissionQueue().start();
        }
        
        RMProperties rmpsOut = RMContextUtils.retrieveRMProperties(msg, true);
        if (null == rmpsOut) {
            rmpsOut = new RMProperties();
            rmpsOut.exposeAs(protocol.getWSRMNamespace());
            RMContextUtils.storeRMProperties(msg, rmpsOut, true);
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
        
        if ((isApplicationMessage || isLastMessage)
            && !isPartialResponse) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("inbound sequence: " + (null == inSeqId ? "null" : inSeqId.getValue()));
            }
            
            // get the current sequence, requesting the creation of a new one if necessary
            
            synchronized (source) {
                SourceSequence seq = null;
                if (isLastMessage) {
                    Map<?, ?> invocationContext = (Map)msg.get(Message.INVOCATION_CONTEXT);
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
        }
        
        // add Acknowledgements (to application messages or explicitly 
        // created Acknowledgement messages only)
        if (isApplicationMessage || constants.getSequenceAckAction().equals(action)) {
            AttributedURIType to = maps.getTo();
            assert null != to;
            addAcknowledgements(destination, rmpsOut, inSeqId, to);
            if (isPartialResponse && rmpsOut.getAcks() != null && rmpsOut.getAcks().size() > 0) {
                AttributedURIType actionURI = new AttributedURIType();
                actionURI.setValue(constants.getSequenceAckAction());
                maps.setAction(actionURI);
                msg.remove(Message.EMPTY_PARTIAL_RESPONSE_MESSAGE);
            }
        } 
        
        if (constants.getSequenceAckAction().equals(action)
            || constants.getTerminateSequenceAction().equals(action)) {
            maps.setReplyTo(RMUtils.createNoneReference());
        }
        
        assertReliability(msg);
    }
    
    void addAcknowledgements(Destination destination, 
                             RMProperties rmpsOut, 
                             Identifier inSeqId, 
                             AttributedURIType to) {
        for (DestinationSequence seq : destination.getAllSequences()) {
            if (!seq.sendAcknowledgement()) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("no need to add acknowledgements for sequence "
                        + seq.getIdentifier().getValue()); 
                }
                continue;
            }
            String address = seq.getAcksTo().getAddress().getValue();
            if (!to.getValue().equals(address)) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("sequences acksTo address (" + address
                        + ") does not match to address (" + to.getValue() + ")");
                }
                continue;
            }
            // there may be multiple sources with anonymous acksTo 
            if (RMUtils.getAddressingConstants().getAnonymousURI().equals(address)
                && !AbstractSequence.identifierEquals(seq.getIdentifier(), inSeqId)) {                
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("sequence identifier does not match inbound sequence identifier");
                }
                continue;
            }
            rmpsOut.addAck(seq);
        }

        if (LOG.isLoggable(Level.FINE)) {
            Collection<SequenceAcknowledgement> acks = rmpsOut.getAcks();
            if (null == acks) {
                LOG.fine("No acknowledgements added.");
            } else {
                LOG.fine("Added " + acks.size() + " acknowledgements.");
            }
        }
    }
    
    boolean isRuntimeFault(Message message) {
        FaultMode mode = MessageUtils.getFaultMode(message);
        if (null == mode) {
            return false;
        }
        return FaultMode.CHECKED_APPLICATION_FAULT != mode;
    }
}
