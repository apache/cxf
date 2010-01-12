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
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.datatype.Duration;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxb.DatatypeFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.VersionTransformer;
import org.apache.cxf.ws.addressing.v200408.AttributedURI;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;

/**
 * 
 */
public class Servant implements Invoker {

    private static final Logger LOG = LogUtils.getL7dLogger(Servant.class);
    private RMEndpoint reliableEndpoint;
    // REVISIT assumption there is only a single outstanding unattached Identifier
    private Identifier unattachedIdentifier;
 
    Servant(RMEndpoint rme) {
        reliableEndpoint = rme;
    }
    
    private void throwSequenceFault(SequenceFault sf, Exchange exchange) {
        Endpoint e = exchange.get(Endpoint.class);
        Binding b = null;
        if (null != e) {
            b = e.getBinding();
        }
        Bus bus = exchange.get(Bus.class);
        if (null != b && bus != null) {
            RMManager m = bus.getExtension(RMManager.class);
            LOG.fine("Manager: " + m);
            BindingFaultFactory bff = m.getBindingFaultFactory(b);
            Fault f = bff.createFault(sf);
            LogUtils.log(LOG, Level.SEVERE, "SEQ_FAULT_MSG", bff.toString(f));
            throw f;
        }
        throw new Fault(sf);
    }
    
    
    public Object invoke(Exchange exchange, Object o) {
        LOG.fine("Invoking on RM Endpoint");
        OperationInfo oi = exchange.get(OperationInfo.class);
        if (null == oi) {
            LOG.fine("No operation info."); 
            return null;
        }
        
        if (RMConstants.getCreateSequenceOperationName().equals(oi.getName())
            || RMConstants.getCreateSequenceOnewayOperationName().equals(oi.getName())) {
            try {
                return Collections.singletonList(createSequence(exchange.getInMessage()));
            } catch (SequenceFault ex) {
                throwSequenceFault(ex, exchange);
            } catch (Exception ex) {
                throw new Fault(ex);
            }
        } else if (RMConstants.getCreateSequenceResponseOnewayOperationName().equals(oi.getName())) {
            CreateSequenceResponseType createResponse = 
                (CreateSequenceResponseType)getParameter(exchange.getInMessage());
            try {
                createSequenceResponse(createResponse);
            } catch (SequenceFault ex) {
                throwSequenceFault(ex, exchange);
            }
        } else if (RMConstants.getTerminateSequenceOperationName().equals(oi.getName())) {            
            try {
                terminateSequence(exchange.getInMessage());
            } catch (SequenceFault ex) {
                throwSequenceFault(ex, exchange);
            } catch (RMException ex) {
                throw new Fault(ex);
            }
        }
        
        return null;
    }


    CreateSequenceResponseType createSequence(Message message) throws SequenceFault {
        LOG.fine("Creating sequence");
        
        AddressingProperties maps = RMContextUtils.retrieveMAPs(message, false, false);        
        Message outMessage = message.getExchange().getOutMessage();  
        if (null != outMessage) {
            RMContextUtils.storeMAPs(maps, outMessage, false, false);
        }
        
        CreateSequenceType create = (CreateSequenceType)getParameter(message);
        Destination destination = reliableEndpoint.getDestination();
        
        CreateSequenceResponseType createResponse = 
            RMUtils.getWSRMFactory().createCreateSequenceResponseType();        
        createResponse.setIdentifier(destination.generateSequenceIdentifier());
        
        DestinationPolicyType dp = reliableEndpoint.getManager().getDestinationPolicy();
        Duration supportedDuration = dp.getSequenceExpiration();
        if (null == supportedDuration) {
            supportedDuration = DatatypeFactory.PT0S;
        }
        Expires ex = create.getExpires();
        
        if (null != ex || supportedDuration.isShorterThan(DatatypeFactory.PT0S)) {
            Duration effectiveDuration = supportedDuration;
            if (null != ex && supportedDuration.isLongerThan(ex.getValue()))  {
                effectiveDuration = supportedDuration;
            }
            ex = RMUtils.getWSRMFactory().createExpires();
            ex.setValue(effectiveDuration);
            createResponse.setExpires(ex);
        }
        
        OfferType offer = create.getOffer();
        if (null != offer) {
            AcceptType accept = RMUtils.getWSRMFactory().createAcceptType();
            if (dp.isAcceptOffers()) {
                Source source = reliableEndpoint.getSource();
                LOG.fine("Accepting inbound sequence offer");
                // AddressingProperties maps = RMContextUtils.retrieveMAPs(message, false, false);
                AttributedURI to = VersionTransformer.convert(maps.getTo());
                accept.setAcksTo(RMUtils.createReference2004(to.getValue()));
                SourceSequence seq = new SourceSequence(offer.getIdentifier(), 
                                                                    null, 
                                                                    createResponse.getIdentifier());
                seq.setExpires(offer.getExpires());
                seq.setTarget(VersionTransformer.convert(create.getAcksTo()));
                source.addSequence(seq);
                source.setCurrent(createResponse.getIdentifier(), seq);  
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Making offered sequence the current sequence for responses to "
                             + createResponse.getIdentifier().getValue());
                }
            } else {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Refusing inbound sequence offer"); 
                }
                accept.setAcksTo(RMUtils.createNoneReference2004());
            }
            createResponse.setAccept(accept);
        }
        
        DestinationSequence seq = new DestinationSequence(createResponse.getIdentifier(),
                                                          create.getAcksTo(), destination);
        seq.setCorrelationID(maps.getMessageID().getValue());
        destination.addSequence(seq);
        LOG.fine("returning " + createResponse);
        return createResponse;
    }

    public void createSequenceResponse(CreateSequenceResponseType createResponse) throws SequenceFault {
        LOG.fine("Creating sequence response");
        
        SourceSequence seq = new SourceSequence(createResponse.getIdentifier());
        seq.setExpires(createResponse.getExpires());
        Source source  = reliableEndpoint.getSource();
        source.addSequence(seq);
        
        // the incoming sequence ID is either used as the requestor sequence
        // (signalled by null) or associated with a corresponding sequence 
        // identifier
        source.setCurrent(clearUnattachedIdentifier(), seq);

        // if a sequence was offered and accepted, then we can add this to
        // to the local destination sequence list, otherwise we have to wait for
        // and incoming CreateSequence request
        
        Identifier offeredId = reliableEndpoint.getProxy().getOfferedIdentifier();
        if (null != offeredId) {
            AcceptType accept = createResponse.getAccept();
            assert null != accept;
            Destination dest = reliableEndpoint.getDestination();
            String address = accept.getAcksTo().getAddress().getValue();
            if (!RMUtils.getAddressingConstants().getNoneURI().equals(address)) {
                DestinationSequence ds = 
                    new DestinationSequence(offeredId, accept.getAcksTo(), dest);
                dest.addSequence(ds);
            }
        }
    }

    public void terminateSequence(Message message) throws SequenceFault, RMException {
        LOG.fine("Terminating sequence");
        
        TerminateSequenceType terminate = (TerminateSequenceType)getParameter(message);
        
        // check if the terminated sequence was created in response to a a createSequence
        // request
        
        Destination destination = reliableEndpoint.getDestination();
        Identifier sid = terminate.getIdentifier();
        DestinationSequence terminatedSeq = destination.getSequence(sid);
        if (null == terminatedSeq) {
            //  TODO
            LOG.severe("No such sequence.");
            return;
        } 

        destination.removeSequence(terminatedSeq);
        
        // the following may be necessary if the last message for this sequence was a oneway
        // request and hence there was no response to which a last message could have been added
        
        // REVISIT: A last message for the correlated sequence should have been sent by the time
        // the last message for the underlying sequence was received.
        
        Source source = reliableEndpoint.getSource();
        
        for (SourceSequence outboundSeq : source.getAllSequences()) {
            if (outboundSeq.offeredBy(sid) && !outboundSeq.isLastMessage()) {
                
                if (BigInteger.ZERO.equals(outboundSeq.getCurrentMessageNr())) {
                    source.removeSequence(outboundSeq);
                }
                // send an out of band message with an empty body and a 
                // sequence header containing a lastMessage element.
               
                /*
                Proxy proxy = new Proxy(reliableEndpoint);
                try {
                    proxy.lastMessage(outboundSeq);
                } catch (RMException ex) {
                    LogUtils.log(LOG, Level.SEVERE, "CORRELATED_SEQ_TERMINATION_EXC", ex);
                }
                */
                
                break;
            }
        }
        
    }

    Object getParameter(Message message) {
        List resList = null;
        // assert message == message.getExchange().getInMessage();
        
        if (message != null) {
            resList = message.getContent(List.class);
        }

        if (resList != null) {
            return resList.get(0);
        }
        return null;
    }
    
    Identifier clearUnattachedIdentifier() {
        Identifier ret = unattachedIdentifier;
        unattachedIdentifier = null;
        return ret;
    }
    
    void setUnattachedIdentifier(Identifier i) { 
        unattachedIdentifier = i;
    }
}
