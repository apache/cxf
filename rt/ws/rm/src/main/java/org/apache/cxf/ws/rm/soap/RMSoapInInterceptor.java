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

package org.apache.cxf.ws.rm.soap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.soap.MAPCodec;
import org.apache.cxf.ws.rm.AbstractRMInterceptor;
import org.apache.cxf.ws.rm.EncoderDecoder;
import org.apache.cxf.ws.rm.ProtocolVariation;
import org.apache.cxf.ws.rm.RM10Constants;
import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rm.RMConfiguration;
import org.apache.cxf.ws.rm.RMConstants;
import org.apache.cxf.ws.rm.RMContextUtils;
import org.apache.cxf.ws.rm.RMEndpoint;
import org.apache.cxf.ws.rm.RMException;
import org.apache.cxf.ws.rm.RMManager;
import org.apache.cxf.ws.rm.RMMessageConstants;
import org.apache.cxf.ws.rm.RMProperties;
import org.apache.cxf.ws.rm.v200702.AckRequestedType;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.wsdl.interceptors.BareInInterceptor;

/**
 * Protocol Handler responsible for {en|de}coding the RM
 * Properties for {outgo|incom}ing messages.
 */
public class RMSoapInInterceptor extends AbstractSoapInterceptor {

    protected static JAXBContext jaxbContext;

    private static final Set<QName> HEADERS;
    static {
        Set<QName> set = new HashSet<>();
        set.addAll(RM10Constants.HEADERS);
        set.addAll(RM11Constants.HEADERS);
        HEADERS = set;
    }

    private static final Logger LOG = LogUtils.getL7dLogger(RMSoapInInterceptor.class);

    /**
     * Constructor.
     */
    public RMSoapInInterceptor() {
        super(Phase.PRE_PROTOCOL);

        addAfter(MAPCodec.class.getName());
    }

    // AbstractSoapInterceptor interface

    /**
     * @return the set of SOAP headers understood by this handler
     */
    public Set<QName> getUnderstoodHeaders() {
        return HEADERS;
    }

    // Interceptor interface

    /* (non-Javadoc)
     * @see org.apache.cxf.interceptor.Interceptor#handleMessage(org.apache.cxf.message.Message)
     */
    public void handleMessage(SoapMessage message) throws Fault {
        decode(message);
        updateServiceModelInfo(message);
    }

    /**
     * Decode the RM properties from protocol-specific headers
     * and store them in the message.
     *
     * @param message the SOAP mesage
     */
    void decode(SoapMessage message) {
        RMProperties rmps = unmarshalRMProperties(message);
        RMContextUtils.storeRMProperties(message, rmps, false);
        // TODO: decode SequenceFault ?
    }

    /**
     * Decode the RM properties from protocol-specific headers.
     *
     * @param message the SOAP message
     * @return the RM properties
     */
    public RMProperties unmarshalRMProperties(SoapMessage message) {
        RMProperties rmps = (RMProperties)message.get(RMContextUtils.getRMPropertiesKey(false));
        if (rmps == null) {
            rmps = new RMProperties();
        }
        List<Header> headers = message.getHeaders();
        if (headers != null) {
            decodeHeaders(message, headers, rmps);
        }
        return rmps;
    }

    public void decodeHeaders(SoapMessage message, List<Header> headers, RMProperties rmps) {
        try {
            Collection<SequenceAcknowledgement> acks = new ArrayList<>();
            Collection<AckRequestedType> requested = new ArrayList<>();

            String rmUri = null;
            EncoderDecoder codec = null;
            Iterator<Header> iter = headers.iterator();
            while (iter.hasNext()) {
                Object node = iter.next().getObject();
                if (node instanceof Element) {
                    Element elem = (Element) node;
                    if (Node.ELEMENT_NODE != elem.getNodeType()) {
                        continue;
                    }
                    String ns = elem.getNamespaceURI();
                    if (rmUri == null && (RM10Constants.NAMESPACE_URI.equals(ns)
                        || RM11Constants.NAMESPACE_URI.equals(ns))) {
                        LOG.log(Level.FINE, "set RM namespace {0}", ns);
                        rmUri = ns;
                        rmps.exposeAs(rmUri);
                    }
                    if (rmUri != null && rmUri.equals(ns)) {
                        if (codec == null) {
                            final String wsauri;
                            AddressingProperties maps = ContextUtils.retrieveMAPs(message, false, false, false);
                            if (maps == null) {
                                RMConfiguration config = getManager(message).getEffectiveConfiguration(message);
                                wsauri = config.getAddressingNamespace();
                            } else {
                                wsauri = maps.getNamespaceURI();
                            }
                            ProtocolVariation protocol = ProtocolVariation.findVariant(rmUri, wsauri);
                            if (protocol == null) {
                                LOG.log(Level.WARNING, "NAMESPACE_ERROR_MSG", wsauri);
                                break;
                            }
                            codec = protocol.getCodec();
                        }
                        String localName = elem.getLocalName();
                        LOG.log(Level.FINE, "decoding RM header {0}", localName);
                        if (RMConstants.SEQUENCE_NAME.equals(localName)) {
                            rmps.setSequence(codec.decodeSequenceType(elem));
                            rmps.setCloseSequence(codec.decodeSequenceTypeCloseSequence(elem));
                        } else if (RMConstants.SEQUENCE_ACK_NAME.equals(localName)) {
                            acks.add(codec.decodeSequenceAcknowledgement(elem));
                        } else if (RMConstants.ACK_REQUESTED_NAME.equals(localName)) {
                            requested.add(codec.decodeAckRequestedType(elem));
                        }
                    }
                }
            }
            if (!acks.isEmpty()) {
                rmps.setAcks(acks);
            }
            if (!requested.isEmpty()) {
                rmps.setAcksRequested(requested);
            }
        } catch (JAXBException ex) {
            LOG.log(Level.WARNING, "SOAP_HEADER_DECODE_FAILURE_MSG", ex);
        }
    }

    /**
     * When invoked inbound, check if the action indicates that this is one of the
     * RM protocol messages (CreateSequence, CreateSequenceResponse, TerminateSequence)
     * and if so, replace references to the application service model with references to
     * the RM service model.
     * The addressing protocol handler must have extracted the action beforehand.
     * @see org.apache.cxf.transport.ChainInitiationObserver
     *
     * @param message the message
     */
    private void updateServiceModelInfo(SoapMessage message) throws Fault {

        AddressingProperties maps = ContextUtils.retrieveMAPs(message, false, false, false);
        AttributedURIType actionURI = null == maps ? null : maps.getAction();
        String action = null == actionURI ? null : actionURI.getValue().trim();

        LOG.fine("action: " + action);
        RMConstants consts;
        if (RM10Constants.ACTIONS.contains(action)) {
            consts = RM10Constants.INSTANCE;
        } else if (RM11Constants.ACTIONS.contains(action)) {
            consts = RM11Constants.INSTANCE;
        } else {
            return;
        }
        RMProperties rmps = RMContextUtils.retrieveRMProperties(message, false);
        rmps.exposeAs(consts.getWSRMNamespace());
        ProtocolVariation protocol =
            ProtocolVariation.findVariant(consts.getWSRMNamespace(), maps.getNamespaceURI());

        LOG.info("Updating service model info in exchange");

        RMManager manager = getManager(message);
        assert manager != null;

        final RMEndpoint rme;
        try {
            rme = manager.getReliableEndpoint(message);
        } catch (RMException e) {
            throw new SoapFault(new org.apache.cxf.common.i18n.Message("CANNOT_PROCESS", LOG), e,
                message.getVersion().getSender());
        }

        Exchange exchange = message.getExchange();
        Endpoint ep = rme.getEndpoint(protocol);
        exchange.put(Endpoint.class, ep);
        exchange.put(Service.class, ep.getService());
        exchange.put(Binding.class, ep.getBinding());

        // Also set BindingOperationInfo as some operations (SequenceAcknowledgment) have
        // neither in nor out messages, and thus the WrappedInInterceptor cannot
        // determine the operation name.

        BindingInfo bi = ep.getEndpointInfo().getBinding();
        BindingOperationInfo boi = null;
        boolean isOneway = true;
        if (consts.getCreateSequenceAction().equals(action)) {
            if (RMContextUtils.isServerSide(message)) {
                boi = bi.getOperation(consts.getCreateSequenceOperationName());
                isOneway = false;
            } else {
                boi = bi.getOperation(consts.getCreateSequenceOnewayOperationName());
            }
        } else if (consts.getCreateSequenceResponseAction().equals(action)) {
            if (RMContextUtils.isServerSide(message)) {
                boi = bi.getOperation(consts.getCreateSequenceResponseOnewayOperationName());
            } else {
                boi = bi.getOperation(consts.getCreateSequenceOperationName());
                isOneway = false;
            }
        } else if (consts.getSequenceAckAction().equals(action)) {
            boi = bi.getOperation(consts.getSequenceAckOperationName());
        } else if (consts.getAckRequestedAction().equals(action)) {
            boi = bi.getOperation(consts.getAckRequestedOperationName());
        } else if (consts.getTerminateSequenceAction().equals(action)) {
            boi = bi.getOperation(consts.getTerminateSequenceOperationName());
        } else if (RM11Constants.INSTANCE.getTerminateSequenceResponseAction().equals(action)) {
            //TODO add server-side TSR handling
            boi = bi.getOperation(RM11Constants.INSTANCE.getTerminateSequenceOperationName());
            isOneway = false;
        } else if (consts.getCloseSequenceAction().equals(action)) {
            boi = bi.getOperation(consts.getCloseSequenceOperationName());
        } else if (RM11Constants.INSTANCE.getCloseSequenceResponseAction().equals(action)) {
            boi = bi.getOperation(RM11Constants.INSTANCE.getCloseSequenceOperationName());
            isOneway = false;
        }

        // make sure the binding information has been set
        if (boi == null) {
            LOG.fine("No BindingInfo for action " + action);
        } else {
            exchange.put(BindingOperationInfo.class, boi);
            exchange.setOneWay(isOneway);
        }

        // Fix requestor role (as the client side message observer always sets it to TRUE)
        // to allow unmarshalling the body of a server originated TerminateSequence request.
        // In the logical RM interceptor set it back to what it was so that the logical
        // addressing interceptor does not try to send a partial response to
        // server originated oneway RM protocol messages.
        //

        if (!consts.getCreateSequenceResponseAction().equals(action)
            && !consts.getSequenceAckAction().equals(action)
            && !RM11Constants.INSTANCE.getTerminateSequenceResponseAction().equals(action)
            && !RM11Constants.INSTANCE.getCloseSequenceResponseAction().equals(action)) {
            LOG.fine("Changing requestor role from " + message.get(Message.REQUESTOR_ROLE)
                     + " to false");
            Object originalRequestorRole = message.get(Message.REQUESTOR_ROLE);
            if (null != originalRequestorRole) {
                message.put(RMMessageConstants.ORIGINAL_REQUESTOR_ROLE, originalRequestorRole);
            }
            message.put(Message.REQUESTOR_ROLE, Boolean.FALSE);
        }

        // replace WrappedInInterceptor with BareInInterceptor if necessary
        // as RM protocol messages use parameter style BARE

        InterceptorChain chain = message.getInterceptorChain();
        ListIterator<Interceptor<? extends Message>> it = chain.getIterator();
        boolean bareIn = false;
        boolean wrappedIn = false;
        while (it.hasNext() && !wrappedIn && !bareIn) {
            PhaseInterceptor<? extends Message> pi = (PhaseInterceptor<? extends Message>)it.next();
            if (BareInInterceptor.class.getName().equals(pi.getId())) {
                bareIn = true;
            }

        }
        if (!bareIn) {
            chain.add(new BareInInterceptor());
            LOG.fine("Added BareInInterceptor to chain.");
        }
    }

    private RMManager getManager(SoapMessage message) {
        InterceptorChain chain = message.getInterceptorChain();
        ListIterator<Interceptor<? extends Message>> it = chain.getIterator();
        while (it.hasNext()) {
            Interceptor<? extends Message> i = it.next();
            if (i instanceof AbstractRMInterceptor) {
                return ((AbstractRMInterceptor<? extends Message>)i).getManager();
            }
        }
        return null;
    }
}