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

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.rm.manager.AckRequestModeType;
import org.apache.cxf.ws.rm.manager.SourcePolicyType;
import org.apache.cxf.ws.rm.v200702.AckRequestedType;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceType;

/**
 *
 */
public class RMOutInterceptor extends AbstractRMInterceptor<Message>  {

    private static final Logger LOG = LogUtils.getL7dLogger(RMOutInterceptor.class);

    public RMOutInterceptor() {
        super(Phase.PRE_STREAM);
        addAfter(RMCaptureOutInterceptor.class.getName());
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
            return;
        }

        RMConfiguration config = getManager().getEffectiveConfiguration(msg);
        String wsaNamespace = config.getAddressingNamespace();
        String rmNamespace = config.getRMNamespace();
        ProtocolVariation protocol = ProtocolVariation.findVariant(rmNamespace, wsaNamespace);
        RMContextUtils.setProtocolVariation(msg, protocol);
        Destination destination = getManager().getDestination(msg);

        String action = null;
        if (null != maps.getAction()) {
            action = maps.getAction().getValue();
        }
        //make sure we use the appropriate namespace
        maps.exposeAs(wsaNamespace);

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Action: " + action);
        }

        boolean isApplicationMessage = !RMContextUtils.isRMProtocolMessage(action);
        boolean isPartialResponse = MessageUtils.isPartialResponse(msg);
        RMConstants constants = protocol.getConstants();
        RMProperties rmpsOut = RMContextUtils.retrieveRMProperties(msg, true);

        if (isApplicationMessage && !isPartialResponse) {
            addRetransmissionInterceptor(msg);
        }

        Identifier inSeqId = null;
        if (isApplicationMessage) {
            RMProperties rmpsIn = RMContextUtils.retrieveRMProperties(msg, false);
            if (null != rmpsIn && null != rmpsIn.getSequence()) {
                inSeqId = rmpsIn.getSequence().getIdentifier();

                SourceSequence seq = rmpsIn.getSourceSequence();
                SequenceType sequence = rmpsIn.getSequence();
                if (seq == null || sequence == null) {
                    LOG.warning("sequence not set for outbound message, skipped acknowledgement request");
                } else {
                    addAckRequest(msg, rmpsIn, seq, sequence);
                }
            }
        }

        // add Acknowledgements (to application messages or explicitly created Acknowledgement messages only)
        boolean isAck = constants.getSequenceAckAction().equals(action);
        boolean isClose = constants.getCloseSequenceAction().equals(action);
        boolean isTerminate = constants.getTerminateSequenceAction().equals(action);
        if (isApplicationMessage || isAck || isClose || isTerminate) {
            AttributedURIType to = maps.getTo();
            assert null != to;
            addAcknowledgements(destination, rmpsOut, inSeqId, to);
            if (isPartialResponse && rmpsOut.getAcks() != null && rmpsOut.getAcks().size() > 0) {
                setAction(maps, constants.getSequenceAckAction());
                msg.remove(Message.EMPTY_PARTIAL_RESPONSE_MESSAGE);
                isAck = true;
            }
        }
        if (isAck || (isTerminate
                && RM10Constants.NAMESPACE_URI.equals(rmNamespace))) {
            maps.setReplyTo(RMUtils.createNoneReference());
        }

        assertReliability(msg);
    }

    /**
     * Add AcknowledgementRequested to message if needed. The AckRequest mode set either in the message
     * properties or in the source policy is used to determine whether AcknowledgementRequested is always
     * added, never added, or added only when we're waiting for the acknowledgement to a previously-sent
     * message.
     *
     * @param msg
     * @param rmpsIn
     * @param seq
     * @param sequence
     */
    protected void addAckRequest(Message msg, RMProperties rmpsIn, SourceSequence seq, SequenceType sequence) {
        AckRequestModeType mode = (AckRequestModeType)msg.get(RMMessageConstants.ACK_REQUEST_MODE);
        if (mode == null) {
            mode = AckRequestModeType.PENDING;
            SourcePolicyType policy = getManager().getSourcePolicy();
            if (policy.isSetAckRequestMode()) {
                mode = policy.getAckRequestMode();
            }
        }
        if (AckRequestModeType.ALWAYS == mode
            || (mode == AckRequestModeType.PENDING && seq.needAcknowledge(rmpsIn.getMessageNumber()))) {
            Collection<AckRequestedType> reqs = rmpsIn.getAcksRequested();
            if (reqs == null) {
                reqs = new ArrayList<>();
            }
            Identifier identifier = new Identifier();
            identifier.setValue(sequence.getIdentifier().getValue());
            AckRequestedType ackRequest = new AckRequestedType();
            ackRequest.setIdentifier(identifier);
            reqs.add(ackRequest);
            rmpsIn.setAcksRequested(reqs);
        }
    }

    void addAcknowledgements(Destination destination, RMProperties rmpsOut, Identifier inSeqId,  AttributedURIType to) {
        if (destination == null) {
            return;
        }
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

    private void addRetransmissionInterceptor(Message msg) {
        RetransmissionInterceptor ri = new RetransmissionInterceptor();
        ri.setManager(getManager());
        // TODO:
        // On the server side: If a fault occurs after this interceptor we will switch
        // interceptor chains (if this is not already a fault message) and therefore need to
        // make sure the retransmission interceptor is added to the fault chain
        //
        msg.getInterceptorChain().add(ri);
        LOG.fine("Added RetransmissionInterceptor to chain.");

        RetransmissionQueue queue = getManager().getRetransmissionQueue();
        if (queue != null) {
            queue.start();
        }
    }

    boolean isRuntimeFault(Message message) {
        FaultMode mode = MessageUtils.getFaultMode(message);
        if (null == mode) {
            return false;
        }
        return FaultMode.CHECKED_APPLICATION_FAULT != mode;
    }

    private static void setAction(AddressingProperties maps, String action) {
        AttributedURIType actionURI = new AttributedURIType();
        actionURI.setValue(action);
        maps.setAction(actionURI);
    }
}
