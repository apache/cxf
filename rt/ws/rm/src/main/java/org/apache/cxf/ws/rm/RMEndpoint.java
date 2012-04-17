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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.policy.EffectivePolicy;
import org.apache.cxf.ws.policy.EndpointPolicy;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.PolicyInterceptorProviderRegistry;
import org.apache.cxf.ws.rm.manager.SequenceTerminationPolicyType;
import org.apache.cxf.ws.rm.manager.SourcePolicyType;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;

public class RMEndpoint {

    private static final Logger LOG = LogUtils.getL7dLogger(RMEndpoint.class);
    
    private static final String SERVICE_NAME = "SequenceAbstractService";
    private static final String INTERFACE_NAME = "SequenceAbstractPortType";
    private static final String BINDING_NAME = "SequenceAbstractSoapBinding";

    private static final String CREATE_PART_NAME = "create";
    private static final String CREATE_RESPONSE_PART_NAME = "createResponse";
    private static final String TERMINATE_PART_NAME = "terminate";
    
    private static Schema rmSchema;

    private RMManager manager;
    private Endpoint applicationEndpoint;
    private Conduit conduit;
    private ProtocolVariation protocol;
    private EndpointReferenceType replyTo;
    private Source source;
    private Destination destination;
    private WrappedService service;
    private Endpoint endpoint;
    private Proxy proxy;
    private Servant servant;
    private QName serviceQName;
    private QName bindingQName;
    private QName interfaceQName;
    private long lastApplicationMessage;
    private long lastControlMessage;
    private InstrumentationManager instrumentationManager;
    private ManagedRMEndpoint managedEndpoint;
    
    /**
     * Constructor.
     * 
     * @param m
     * @param ae
     * @param pv
     */
    public RMEndpoint(RMManager m, Endpoint ae, ProtocolVariation pv) {
        manager = m;
        applicationEndpoint = ae;
        source = new Source(this);
        destination = new Destination(this);
        proxy = new Proxy(this);
        servant = new Servant(this);
        protocol = pv;
    }

    /**
     * @return Returns the bus.
     */
    public RMManager getManager() {
        return manager;
    }

    /**
     * @return Returns the application endpoint.
     */
    public Endpoint getApplicationEndpoint() {
        return applicationEndpoint;
    }

    /**
     * @return Returns the RM protocol endpoint.
     */
    public Endpoint getEndpoint() {
        return endpoint;
    }

    public ProtocolVariation getProtocol() {
        return protocol;
    }

    /**
     * Set the protocol used by this endpoint. This method is only intended for use in testing; all normal use
     * uses the constructor to set the value.
     * 
     * @param protocol
     */
    public void setProtocol(ProtocolVariation protocol) {
        this.protocol = protocol;
    }

    /**
     * @return Returns the RM protocol service.
     */
    public Service getService() {
        return service;
    }

    /**
     * @return Returns the RM protocol binding info.
     */
    public BindingInfo getBindingInfo() {
        return service.getServiceInfo().getBinding(bindingQName);
    }

    /**
     * @return Returns the proxy.
     */
    public Proxy getProxy() {
        return proxy;
    }

    /**
     * @return Returns the servant.
     */
    public Servant getServant() {
        return servant;
    }

    /**
     * @return Returns the destination.
     */
    public Destination getDestination() {
        return destination;
    }

    /**
     * @param destination The destination to set.
     */
    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    /**
     * @return Returns the source.
     */
    public Source getSource() {
        return source;
    }

    /**
     * @param source The source to set.
     */
    public void setSource(Source source) {
        this.source = source;
    }

    /**
     * @return The time when last application message was received.
     */
    public long getLastApplicationMessage() {
        return lastApplicationMessage;
    }

    /**
     * Indicates that an application message has been received.
     */
    public void receivedApplicationMessage() {
        lastApplicationMessage = System.currentTimeMillis();
    }

    /**
     * @return The time when last RM protocol message was received.
     */
    public long getLastControlMessage() {
        return lastControlMessage;
    }

    /**
     * Indicates that an RM protocol message has been received.
     */
    public void receivedControlMessage() {
        lastControlMessage = System.currentTimeMillis();
    }

    /**
     * @return Returns the conduit.
     */
    public Conduit getConduit() {
        return conduit;
    }

    /**
     * Returns the replyTo address of the first application request, i.e. the
     * target address to which to send CreateSequence, CreateSequenceResponse
     * and TerminateSequence messages originating from the from the server.
     * 
     * @return the replyTo address
     */
    EndpointReferenceType getReplyTo() {
        return replyTo;
    }

    void initialise(Conduit c, 
                    EndpointReferenceType r,
                    org.apache.cxf.transport.Destination d) {
        conduit = c;
        replyTo = r;
        createService();
        createEndpoint(d);
        setPolicies();
        // CXF-4218 requires a change in the DB schema, which is not practical
        // for the released 2.5.x. Thus, this is a workaround for 2.5.x to avoid getting 
        // the duplicate jmx registration error.
        if (!ProtocolVariation.RM10WSA200408.equals(protocol)) {
            LOG.log(Level.INFO, "Skip monitoring for protocol: " + protocol);
            return;
        }
        if (manager != null && manager.getBus() != null) {
            managedEndpoint = new ManagedRMEndpoint(this);
            instrumentationManager = manager.getBus().getExtension(InstrumentationManager.class);        
            if (instrumentationManager != null) {   
                try {
                    instrumentationManager.register(managedEndpoint);
                } catch (JMException jmex) {
                    LOG.log(Level.WARNING, "Registering ManagedRMEndpoint failed.", jmex);
                }
            }
        }
    }

    void createService() {
        ServiceInfo si = new ServiceInfo();
        si.setProperty(Schema.class.getName(), getSchema());
        serviceQName = new QName(protocol.getWSRMNamespace(), SERVICE_NAME);
        si.setName(serviceQName);
        
        buildInterfaceInfo(si);

        service = new WrappedService(applicationEndpoint.getService(), serviceQName, si);

        DataBinding dataBinding = null;
        Class create = protocol.getCodec().getCreateSequenceType();
        try {
            JAXBContext ctx =
                JAXBContext.newInstance(PackageUtils.getPackageName(create), create.getClassLoader());
            dataBinding = new JAXBDataBinding(ctx);
        } catch (JAXBException e) {
            throw new ServiceConstructionException(e);
        }
        service.setDataBinding(dataBinding);
        service.setInvoker(servant);
    }

    private static synchronized Schema getSchema() {
        if (rmSchema == null) {
            try {
                SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                javax.xml.transform.Source ad = new StreamSource(RMEndpoint.class
                                             .getResource("/schemas/wsdl/addressing.xsd")
                                             .openStream(),
                                             "http://schemas.xmlsoap.org/ws/2004/08/addressing");
                javax.xml.transform.Source rm = new StreamSource(RMEndpoint.class
                                                                 .getResource("/schemas/wsdl/wsrm.xsd")
                                                                 .openStream());
                
                javax.xml.transform.Source schemas[] = new javax.xml.transform.Source[] {ad, rm};
                rmSchema = factory.newSchema(schemas);
            } catch (Exception ex) {
                //ignore
            }
        }
        return rmSchema;
    }

    void createEndpoint(org.apache.cxf.transport.Destination d) {
        ServiceInfo si = service.getServiceInfo();
        buildBindingInfo(si);
        EndpointInfo aei = applicationEndpoint.getEndpointInfo();
        String transportId = aei.getTransportId();
        EndpointInfo ei = new EndpointInfo(si, transportId);
        if (d != null) {
            ei.setProperty(MAPAggregator.DECOUPLED_DESTINATION, d);
        }

        ei.setAddress(aei.getAddress());
        
        ei.setName(RMUtils.getConstants(protocol.getWSRMNamespace()).getPortName());
        ei.setBinding(si.getBinding(bindingQName));

        // if addressing was enabled on the application endpoint by means
        // of the UsingAddressing element extensor, use this for the
        // RM endpoint also

        Object ua = getUsingAddressing(aei);
        if (null != ua) {
            ei.addExtensor(ua);
        }
        si.addEndpoint(ei);

        endpoint = new WrappedEndpoint(applicationEndpoint, ei, service);
        service.setEndpoint(endpoint);
    }

    void setPolicies() {
        // use same WS-policies as for application endpoint
        PolicyEngine engine = manager.getBus().getExtension(PolicyEngine.class);
        if (null == engine || !engine.isEnabled()) {
            return;
        }

        EndpointInfo ei = getEndpoint().getEndpointInfo();

        PolicyInterceptorProviderRegistry reg = manager.getBus()
            .getExtension(PolicyInterceptorProviderRegistry.class);
        EndpointPolicy ep = null == conduit ? engine.getServerEndpointPolicy(applicationEndpoint
            .getEndpointInfo(), null) : engine.getClientEndpointPolicy(applicationEndpoint.getEndpointInfo(),
                                                                       conduit);

        if (conduit != null) {
            engine.setClientEndpointPolicy(ei, ep);
        } else {
            engine.setServerEndpointPolicy(ei, ep);
        }

        EffectivePolicy effectiveOutbound = new EffectivePolicyImpl(ep, reg, true, false);
        EffectivePolicy effectiveInbound = new EffectivePolicyImpl(ep, reg, false, false);

        BindingInfo bi = ei.getBinding();
        Collection<BindingOperationInfo> bois = bi.getOperations();

        for (BindingOperationInfo boi : bois) {
            engine.setEffectiveServerRequestPolicy(ei, boi, effectiveInbound);
            engine.setEffectiveServerResponsePolicy(ei, boi, effectiveOutbound);

            engine.setEffectiveClientRequestPolicy(ei, boi, effectiveOutbound);
            engine.setEffectiveClientResponsePolicy(ei, boi, effectiveInbound);
        }

        // TODO: FaultPolicy (SequenceFault)
    }

    void buildInterfaceInfo(ServiceInfo si) {
        interfaceQName = new QName(protocol.getWSRMNamespace(), INTERFACE_NAME);
        InterfaceInfo ii = new InterfaceInfo(si, interfaceQName);
        buildOperationInfo(ii);
    }

    void buildOperationInfo(InterfaceInfo ii) {
        buildCreateSequenceOperationInfo(ii);
        buildTerminateSequenceOperationInfo(ii);
        buildSequenceAckOperationInfo(ii);
        buildCloseSequenceOperationInfo(ii);
        buildAckRequestedOperationInfo(ii);

        // TODO: FaultInfo (SequenceFault)
    }

    void buildCreateSequenceOperationInfo(InterfaceInfo ii) {

        OperationInfo operationInfo = null;
        MessagePartInfo partInfo = null;
        MessageInfo messageInfo = null;
        RMConstants consts = protocol.getConstants();
        operationInfo = ii.addOperation(consts.getCreateSequenceOperationName());
        messageInfo = operationInfo.createMessage(consts.getCreateSequenceOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
        partInfo = messageInfo.addMessagePart(CREATE_PART_NAME);
        partInfo.setElementQName(consts.getCreateSequenceOperationName());
        partInfo.setElement(true);
        partInfo.setTypeClass(protocol.getCodec().getCreateSequenceType());

        messageInfo = operationInfo.createMessage(consts.getCreateSequenceResponseOperationName(),
                                                  MessageInfo.Type.OUTPUT);
        operationInfo.setOutput(messageInfo.getName().getLocalPart(), messageInfo);
        partInfo = messageInfo.addMessagePart(CREATE_RESPONSE_PART_NAME);
        partInfo.setElementQName(consts.getCreateSequenceResponseOperationName());
        partInfo.setElement(true);
        partInfo.setTypeClass(protocol.getCodec().getCreateSequenceResponseType());
        partInfo.setIndex(0);

        operationInfo = ii.addOperation(consts.getCreateSequenceOnewayOperationName());
        messageInfo = operationInfo.createMessage(consts.getCreateSequenceOnewayOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
        partInfo = messageInfo.addMessagePart(CREATE_PART_NAME);
        partInfo.setElementQName(consts.getCreateSequenceOnewayOperationName());
        partInfo.setElement(true);
        partInfo.setTypeClass(protocol.getCodec().getCreateSequenceType());

        operationInfo = ii.addOperation(consts.getCreateSequenceResponseOnewayOperationName());
        messageInfo = operationInfo.createMessage(consts.getCreateSequenceResponseOnewayOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
        partInfo = messageInfo.addMessagePart(CREATE_RESPONSE_PART_NAME);
        partInfo.setElementQName(consts.getCreateSequenceResponseOnewayOperationName());
        partInfo.setElement(true);
        partInfo.setTypeClass(protocol.getCodec().getCreateSequenceResponseType());
    }

    void buildTerminateSequenceOperationInfo(InterfaceInfo ii) {

        OperationInfo operationInfo = null;
        MessagePartInfo partInfo = null;
        MessageInfo messageInfo = null;

        RMConstants consts = protocol.getConstants();
        operationInfo = ii.addOperation(consts.getTerminateSequenceOperationName());
        messageInfo = operationInfo.createMessage(consts.getTerminateSequenceOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
        partInfo = messageInfo.addMessagePart(TERMINATE_PART_NAME);
        partInfo.setElementQName(consts.getTerminateSequenceOperationName());
        partInfo.setElement(true);
        partInfo.setTypeClass(protocol.getCodec().getTerminateSequenceType());
        
        // for the TerminateSequence operation to an anonymous endpoint
        operationInfo = ii.addOperation(consts.getTerminateSequenceAnonymousOperationName());
        messageInfo = operationInfo.createMessage(consts.getTerminateSequenceAnonymousOperationName(),
                                                  MessageInfo.Type.OUTPUT);
        operationInfo.setOutput(messageInfo.getName().getLocalPart(), messageInfo);
        partInfo = messageInfo.addMessagePart(TERMINATE_PART_NAME);
        partInfo.setElementQName(consts.getTerminateSequenceOperationName());
        partInfo.setElement(true);
        partInfo.setTypeClass(protocol.getCodec().getTerminateSequenceType());
        
    }

    void buildSequenceAckOperationInfo(InterfaceInfo ii) {

        OperationInfo operationInfo = null;
        MessageInfo messageInfo = null;

        RMConstants consts = protocol.getConstants();
        operationInfo = ii.addOperation(consts.getSequenceAckOperationName());
        messageInfo = operationInfo.createMessage(consts.getSequenceAckOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
    }

    void buildCloseSequenceOperationInfo(InterfaceInfo ii) {

        OperationInfo operationInfo = null;
        MessageInfo messageInfo = null;

        RMConstants consts = protocol.getConstants();
        operationInfo = ii.addOperation(consts.getCloseSequenceOperationName());
        messageInfo = operationInfo.createMessage(consts.getCloseSequenceOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
    }

    void buildAckRequestedOperationInfo(InterfaceInfo ii) {

        OperationInfo operationInfo = null;
        MessageInfo messageInfo = null;

        RMConstants consts = protocol.getConstants();
        operationInfo = ii.addOperation(consts.getAckRequestedOperationName());
        messageInfo = operationInfo.createMessage(consts.getAckRequestedOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
    }

    void buildBindingInfo(ServiceInfo si) {
        // use same binding id as for application endpoint
        // also, to workaround the problem that it may not be possible to determine
        // the soap version depending on the bindingId, speciffy the soap version
        // explicitly
        if (null != applicationEndpoint) {
            SoapBindingInfo sbi = (SoapBindingInfo)applicationEndpoint.getEndpointInfo().getBinding();
            SoapVersion sv = sbi.getSoapVersion();
            String bindingId = sbi.getBindingId();
            SoapBindingInfo bi = new SoapBindingInfo(si, bindingId, sv);
            bindingQName = new QName(protocol.getWSRMNamespace(), BINDING_NAME);
            bi.setName(bindingQName);
            BindingOperationInfo boi = null;

            RMConstants consts = protocol.getConstants();
            
            boi = bi.buildOperation(consts.getCreateSequenceOperationName(),
                                    consts.getCreateSequenceOperationName().getLocalPart(), null);
            addAction(boi, consts.getCreateSequenceAction(), consts.getCreateSequenceResponseAction());
            bi.addOperation(boi);

            boi = bi.buildOperation(consts.getTerminateSequenceOperationName(),
                                    consts.getTerminateSequenceOperationName().getLocalPart(), null);
            addAction(boi, consts.getTerminateSequenceAction());
            bi.addOperation(boi);

            boi = bi.buildOperation(consts.getTerminateSequenceAnonymousOperationName(),
                                    null, consts.getTerminateSequenceAnonymousOperationName().getLocalPart());
            addAction(boi, consts.getTerminateSequenceAction());
            bi.addOperation(boi);

            boi = bi.buildOperation(consts.getSequenceAckOperationName(), null, null);
            addAction(boi, consts.getSequenceAckAction());
            bi.addOperation(boi);

            boi = bi.buildOperation(consts.getCloseSequenceOperationName(), null, null);
            addAction(boi, consts.getCloseSequenceAction());
            bi.addOperation(boi);

            boi = bi.buildOperation(consts.getAckRequestedOperationName(), null, null);
            addAction(boi, consts.getAckRequestedAction());
            bi.addOperation(boi);

            boi = bi.buildOperation(consts.getCreateSequenceOnewayOperationName(),
                                    consts.getCreateSequenceOnewayOperationName().getLocalPart(), null);
            addAction(boi, consts.getCreateSequenceAction());
            bi.addOperation(boi);

            boi = bi.buildOperation(consts.getCreateSequenceResponseOnewayOperationName(),
                                    consts.getCreateSequenceResponseOnewayOperationName().getLocalPart(),
                                    null);
            addAction(boi, consts.getCreateSequenceResponseAction());
            bi.addOperation(boi);

            si.addBinding(bi);
        }

        // TODO: BindingFaultInfo (SequenceFault)
    }

    private void addAction(BindingOperationInfo boi, String action) {
        addAction(boi, action, action);
    }
    private void addAction(BindingOperationInfo boi, String action, String outputAction) {
        SoapOperationInfo soi = new SoapOperationInfo();
        soi.setAction(action);
        boi.addExtensor(soi);

        MessageInfo info = boi.getOperationInfo().getInput();
        if (info != null) {
            info.addExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME, action);
        }
        
        info = boi.getOperationInfo().getOutput();
        if (info != null) {
            info.addExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME, outputAction);
        }
    }

    Object getUsingAddressing(EndpointInfo endpointInfo) {
        if (null == endpointInfo) {
            return null;
        }
        Object ua = null;
        List<ExtensibilityElement> exts = endpointInfo.getExtensors(ExtensibilityElement.class);
        ua = getUsingAddressing(exts);
        if (null != ua) {
            return ua;
        }
        exts = endpointInfo.getBinding() != null ? endpointInfo.getBinding()
            .getExtensors(ExtensibilityElement.class) : null;
        ua = getUsingAddressing(exts);
        if (null != ua) {
            return ua;
        }
        exts = endpointInfo.getService() != null ? endpointInfo.getService()
            .getExtensors(ExtensibilityElement.class) : null;
        ua = getUsingAddressing(exts);
        if (null != ua) {
            return ua;
        }
        return ua;
    }

    Object getUsingAddressing(List<ExtensibilityElement> exts) {
        Object ua = null;
        if (exts != null) {
            for (ExtensibilityElement ext : exts) {
                if (Names.WSAW_USING_ADDRESSING_QNAME.equals(ext.getElementType())) {
                    ua = ext;
                }
            }
        }
        return ua;
    }

    void setAplicationEndpoint(Endpoint ae) {
        applicationEndpoint = ae;
    }

    void setManager(RMManager m) {
        manager = m;
    }

    void shutdown() {
        // cancel outstanding timer tasks (deferred acknowledgements)
        // and scheduled termination for all
        // destination sequences of this endpoint

        for (DestinationSequence ds : getDestination().getAllSequences()) {
            ds.cancelDeferredAcknowledgments();
            ds.cancelTermination();
        }

        // try terminating sequences
        SourcePolicyType sp = manager.getSourcePolicy();
        SequenceTerminationPolicyType stp = null;
        if (null != sp) {
            stp = sp.getSequenceTerminationPolicy();
        }
        if (null != stp && stp.isTerminateOnShutdown()) {

            Collection<SourceSequence> seqs = source.getAllUnacknowledgedSequences();
            LOG.log(Level.FINE, "Trying to terminate {0} sequences", seqs.size());
            for (SourceSequence seq : seqs) {
                try {
                    // destination MUST respond with a
                    // sequence acknowledgement
                    if (seq.isLastMessage()) {
                        // REVISIT: this may be non-standard
                        // getProxy().ackRequested(seq);
                    } else {

                        getProxy().lastMessage(seq);
                    }
                } catch (RMException ex) {
                    // already logged
                }
            }
        }

        // cancel outstanding resends for all source sequences
        // of this endpoint

        for (SourceSequence ss : getSource().getAllSequences()) {
            manager.getRetransmissionQueue().stop(ss);
        }

        // unregistering of this managed bean from the server is done by the bus itself
    }

    class EffectivePolicyImpl implements EffectivePolicy {

        private EndpointPolicy endpointPolicy;
        private List<Interceptor<? extends Message>> interceptors;

        EffectivePolicyImpl(EndpointPolicy ep, PolicyInterceptorProviderRegistry reg, boolean outbound,
                            boolean fault) {
            endpointPolicy = ep;
            interceptors = reg.getInterceptors(endpointPolicy.getChosenAlternative(), outbound, fault);
        }

        public Collection<Assertion> getChosenAlternative() {
            return endpointPolicy.getChosenAlternative();
        }

        public List<Interceptor<? extends Message>> getInterceptors() {
            return interceptors;
        }

        public Policy getPolicy() {
            return endpointPolicy.getPolicy();
        }
    }
}
