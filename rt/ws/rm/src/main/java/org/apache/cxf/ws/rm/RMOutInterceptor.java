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

import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.addressing.VersionTransformer;
import org.apache.cxf.ws.addressing.v200408.AttributedURI;

/**
 * 
 */
public class RMOutInterceptor extends AbstractRMInterceptor {
    
    private static final Logger LOG = LogUtils.getL7dLogger(RMOutInterceptor.class);
 
    public RMOutInterceptor() {
        addAfter(MAPAggregator.class.getName());
    }
    
    protected void handle(Message message) throws SequenceFault, RMException {  
        if (isRuntimeFault(message)) {
            LogUtils.log(LOG, Level.WARNING, "RUNTIME_FAULT_MSG");
            // TODO: in case of a SequenceFault need to set action
            // to http://schemas.xmlsoap.org/ws/2004/a08/addressing/fault
            // but: need to defer propagation of received MAPS to outbound chain first           
            return;
        }
       
        AddressingProperties maps =
            RMContextUtils.retrieveMAPs(message, false, true);
        if (null == maps) {
            LogUtils.log(LOG, Level.WARNING, "MAPS_RETRIEVAL_FAILURE_MSG");
            return;
        }
        
        RMContextUtils.ensureExposedVersion(maps);
        
        Source source = getManager().getSource(message);
        Destination destination = getManager().getDestination(message);

        String action = null;
        if (maps != null && null != maps.getAction()) {
            action = maps.getAction().getValue();
        }
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Action: " + action);
        }

        boolean isApplicationMessage = !RMContextUtils.isRMProtocolMessage(action);
        boolean isPartialResponse = MessageUtils.isPartialResponse(message);
        boolean isLastMessage = RMConstants.getLastMessageAction().equals(action);
        
        if (isApplicationMessage && !isPartialResponse) {
            RetransmissionInterceptor ri = new RetransmissionInterceptor();
            ri.setManager(getManager());
            // TODO:
            // On the server side: If a fault occurs after this interceptor we will switch 
            // interceptor chains (if this is not already a fault message) and therefore need to 
            // make sure the retransmission interceptor is added to the fault chain
            // 
            message.getInterceptorChain().add(ri);
            LOG.fine("Added RetransmissionInterceptor to chain.");
            
            getManager().getRetransmissionQueue().start();
        }
        
        RMProperties rmpsOut = (RMProperties)RMContextUtils.retrieveRMProperties(message, true);
        if (null == rmpsOut) {
            rmpsOut = new RMProperties();
            RMContextUtils.storeRMProperties(message, rmpsOut, true);
        }
        
        RMProperties rmpsIn = null;
        Identifier inSeqId = null;
        BigInteger inMessageNumber = null;
        
        if (isApplicationMessage) {
            rmpsIn = (RMProperties)RMContextUtils.retrieveRMProperties(message, false);
            if (null != rmpsIn && null != rmpsIn.getSequence()) {
                inSeqId = rmpsIn.getSequence().getIdentifier();
                inMessageNumber = rmpsIn.getSequence().getMessageNumber();
            }
            ContextUtils.storeDeferUncorrelatedMessageAbort(message);
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
                    Map<?, ?> invocationContext = (Map)message.get(Message.INVOCATION_CONTEXT);
                    seq = (SourceSequence)invocationContext.get(SourceSequence.class.getName());
                } else {
                    seq = getManager().getSequence(inSeqId, message, maps);
                }
                assert null != seq;

                // increase message number and store a sequence type object in
                // context

                seq.nextMessageNumber(inSeqId, inMessageNumber, isLastMessage);
                
                rmpsOut.setSequence(seq);

                // if this was the last message in the sequence, reset the
                // current sequence so that a new one will be created next
                // time the handler is invoked

                if (seq.isLastMessage()) {
                    source.setCurrent(null);
                }
            }
        } else {
            if (!MessageUtils.isRequestor(message)
                && RMConstants.getCreateSequenceAction().equals(action)) {
                maps.getAction().setValue(RMConstants.getCreateSequenceResponseAction());
            }
        }
        
        // add Acknowledgements (to application messages or explicitly 
        // created Acknowledgement messages only)

        if (isApplicationMessage 
            || RMConstants.getSequenceAcknowledgmentAction().equals(action)) {
            AttributedURI to = VersionTransformer.convert(maps.getTo());
            assert null != to;
            addAcknowledgements(destination, rmpsOut, inSeqId, to);
            if (isPartialResponse && rmpsOut.getAcks() != null && rmpsOut.getAcks().size() > 0) {
                AttributedURIType actionURI = new AttributedURIType();
                actionURI.setValue(RMConstants.getSequenceAcknowledgmentAction());
                maps.setAction(actionURI);
            }
        } 
        
        if (RMConstants.getSequenceAckAction().equals(action)
            || RMConstants.getTerminateSequenceAction().equals(action)) {
            maps.setReplyTo(RMUtils.createNoneReference());
        }
        
        assertReliability(message);
    }
    
    void addAcknowledgements(Destination destination, 
                             RMProperties rmpsOut, 
                             Identifier inSeqId, 
                             AttributedURI to) {
        for (DestinationSequence seq : destination.getAllSequences()) {
            if (!seq.sendAcknowledgement()) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("no need to add acknowledgements for sequence "
                        + seq.getIdentifier().getValue()); 
                }
                continue;
            }
            if (!to.getValue().equals(seq.getAcksTo().getAddress().getValue())) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("sequences acksTo address (" + seq.getAcksTo().getAddress().getValue()
                        + ") does not match to address (" + to.getValue() + ")");
                }
                continue;
            }
            // there may be multiple sources with anonymous acksTo 
            if (RMConstants.getAnonymousAddress().equals(seq.getAcksTo().getAddress().getValue())
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
