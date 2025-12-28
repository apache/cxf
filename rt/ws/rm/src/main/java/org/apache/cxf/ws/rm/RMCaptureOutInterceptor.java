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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.WriteOnCloseOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.rm.persistence.PersistenceUtils;
import org.apache.cxf.ws.rm.persistence.RMMessage;
import org.apache.cxf.ws.rm.persistence.RMStore;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceType;
import org.apache.cxf.ws.rm.v200702.TerminateSequenceType;

/**
 *
 */
public class RMCaptureOutInterceptor extends AbstractRMInterceptor<Message>  {

    private static final Logger LOG = LogUtils.getL7dLogger(RMCaptureOutInterceptor.class);

    public RMCaptureOutInterceptor() {
        super(Phase.PRE_STREAM);
        addBefore(AttachmentOutInterceptor.class.getName());
        addBefore("org.apache.cxf.ext.logging.LoggingOutInterceptor");
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
        //make sure we use the appropriate namespace
        maps.exposeAs(wsaNamespace);

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Action: " + action);
        }

        boolean isApplicationMessage = !RMContextUtils.isRMProtocolMessage(action);
        boolean isPartialResponse = MessageUtils.isPartialResponse(msg);
        RMConstants constants = protocol.getConstants();
        boolean isLastMessage = RM10Constants.CLOSE_SEQUENCE_ACTION.equals(action);

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
                final SourceSequence seq;
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
            Collection<SequenceAcknowledgement> acks =
                rmpsIn != null ? rmpsIn.getAcks() : null;
            if (acks != null && acks.size() == 1) {
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

        // capture message if retransmission possible
        if (isApplicationMessage && !isPartialResponse) {
            OutputStream os = msg.getContent(OutputStream.class);
            // We need to ensure that we have an output stream which won't start writing the
            // message until connection is setup
            if (!(os instanceof WriteOnCloseOutputStream)) {
                msg.setContent(OutputStream.class, new WriteOnCloseOutputStream(os));
            }
            getManager().initializeInterceptorChain(msg);
            //doneCaptureMessage(msg);
            captureMessage(msg);
        } else if (isLastMessage) {
            // got either the rm11 CS or the rm10 empty LM
            RMStore store = getManager().getStore();
            if (null != store) {
                store.persistOutgoing(rmpsOut.getSourceSequence(), null);
            }
        }
    }
    private void captureMessage(Message message) {
        message.put(RMMessageConstants.MESSAGE_CAPTURE, Boolean.TRUE);

        message.getInterceptorChain().add(new CaptureStart());
        message.getInterceptorChain().add(new CaptureEnd());
    }

    private static class CaptureStart extends AbstractPhaseInterceptor<Message> {
        CaptureStart() {
            super(Phase.PRE_PROTOCOL);
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
            message.put("RM_ORIGINAL_WRITER", writer);
            writer = new CapturingXMLWriter(writer);
            message.put("RM_CAPTURING_WRITER", writer);
            message.setContent(XMLStreamWriter.class, writer);
            message.put(AbstractOutDatabindingInterceptor.DISABLE_OUTPUTSTREAM_OPTIMIZATION, Boolean.TRUE);
        }
    }
    private class CaptureEnd extends AbstractPhaseInterceptor<Message> {
        CaptureEnd() {
            super(Phase.WRITE_ENDING);
            addAfter(SoapOutInterceptor.SoapOutEndingInterceptor.class.getName());
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            XMLStreamWriter w = (XMLStreamWriter)message.get("RM_ORIGINAL_WRITER");
            message.setContent(XMLStreamWriter.class, w);

            CapturingXMLWriter cw = (CapturingXMLWriter)message.get("RM_CAPTURING_WRITER");

            try {

                RMProperties rmps = RMContextUtils.retrieveRMProperties(message, true);
                SequenceType sequence = rmps.getSequence();
                Long number = sequence.getMessageNumber();
                Identifier sid = sequence.getIdentifier();
                if (LOG.isLoggable(Level.INFO)) {
                    LOG.log(Level.INFO, "Captured message " + number + " in sequence " + sid.getValue());
                }

                // save message for potential retransmission
                CachedOutputStream cos = new CachedOutputStream();
                IOUtils.copyAndCloseInput(cw.getOutputStream().createInputStream(), cos);
                cos.flush();
                InputStream is = cos.getInputStream();
                message.put(RMMessageConstants.SAVED_CONTENT, cos);
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
                    // serializes the message content and the attachments into
                    // the RMMessage content
                    msg.setCreatedTime(rmps.getCreatedTime());
                    PersistenceUtils.encodeRMContent(msg, message, is);
                    store.persistOutgoing(ss, msg);
                }

            } catch (RMException e) {
                // ignore
            } catch (XMLStreamException e) {
                LOG.log(Level.SEVERE, "Error persisting message", e);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Error persisting message", e);
            }
            if (cw.getThrowable() != null) {
                Throwable t = cw.getThrowable();
                final RuntimeException exception;
                if (t instanceof RuntimeException) {
                    exception = (RuntimeException)t;
                } else {
                    exception = new Fault(t);
                }
                throw exception;
            }
        }
    }

    private String getAddressingNamespace(AddressingProperties maps) {
        String wsaNamespace = maps.getNamespaceURI();
        if (wsaNamespace == null) {
            return getManager().getConfiguration().getAddressingNamespace();
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

        msg.setExchange(newex);
        newex.setOutMessage(msg);
    }

    private static void setAction(AddressingProperties maps, String action) {
        AttributedURIType actionURI = new AttributedURIType();
        actionURI.setValue(action);
        maps.setAction(actionURI);
    }
}
