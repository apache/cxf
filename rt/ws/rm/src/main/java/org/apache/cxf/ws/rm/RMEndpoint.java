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
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.policy.EffectivePolicy;
import org.apache.cxf.ws.policy.EndpointPolicy;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.PolicyInterceptorProviderRegistry;
import org.apache.cxf.ws.rm.manager.SequenceTerminationPolicyType;
import org.apache.cxf.ws.rm.manager.SourcePolicyType;
import org.apache.neethi.Policy;

public class RMEndpoint {

    private static final Logger LOG = LogUtils.getL7dLogger(RMEndpoint.class);

    private static final QName SERVICE_NAME = new QName(RMConstants.getWsdlNamespace(),
                                                        "SequenceAbstractService");
    private static final QName INTERFACE_NAME = new QName(RMConstants.getWsdlNamespace(),
                                                          "SequenceAbstractPortType");
    private static final QName BINDING_NAME = new QName(RMConstants.getWsdlNamespace(),
                                                        "SequenceAbstractSoapBinding");

    private static final QName CREATE_PART_NAME = new QName(RMConstants.getWsdlNamespace(), "create");
    private static final QName CREATE_RESPONSE_PART_NAME = new QName(RMConstants.getWsdlNamespace(),
                                                                     "createResponse");
    private static final QName TERMINATE_PART_NAME = new QName(RMConstants.getWsdlNamespace(), "terminate");
    
    private static Schema rmSchema;

    private RMManager manager;
    private Endpoint applicationEndpoint;
    private Conduit conduit;
    private org.apache.cxf.ws.addressing.EndpointReferenceType replyTo;
    private Source source;
    private Destination destination;
    private WrappedService service;
    private Endpoint endpoint;
    private Proxy proxy;
    private Servant servant;
    private long lastApplicationMessage;
    private long lastControlMessage;

    public RMEndpoint(RMManager m, Endpoint ae) {
        manager = m;
        applicationEndpoint = ae;
        source = new Source(this);
        destination = new Destination(this);
        proxy = new Proxy(this);
        servant = new Servant(this);
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
        return service.getServiceInfo().getBinding(BINDING_NAME);
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
    org.apache.cxf.ws.addressing.EndpointReferenceType getReplyTo() {
        return replyTo;
    }

    void initialise(Conduit c, org.apache.cxf.ws.addressing.EndpointReferenceType r) {
        conduit = c;
        replyTo = r;
        createService();
        createEndpoint();
        setPolicies();
    }

    void createService() {
        ServiceInfo si = new ServiceInfo();
        si.setProperty(Schema.class.getName(), getSchema());
        
        si.setName(SERVICE_NAME);
        
        buildInterfaceInfo(si);

        service = new WrappedService(applicationEndpoint.getService(), SERVICE_NAME, si);

        DataBinding dataBinding = null;
        try {
            JAXBContext ctx =
                JAXBContext.newInstance(
                    PackageUtils.getPackageName(CreateSequenceType.class),
                    CreateSequenceType.class.getClassLoader());
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

    void createEndpoint() {
        ServiceInfo si = service.getServiceInfo();
        buildBindingInfo(si);
        EndpointInfo aei = applicationEndpoint.getEndpointInfo();
        String transportId = aei.getTransportId();
        EndpointInfo ei = new EndpointInfo(si, transportId);

        ei.setAddress(aei.getAddress());

        ei.setName(RMConstants.getPortName());
        ei.setBinding(si.getBinding(BINDING_NAME));

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
        InterfaceInfo ii = new InterfaceInfo(si, INTERFACE_NAME);
        buildOperationInfo(ii);
    }

    void buildOperationInfo(InterfaceInfo ii) {
        buildCreateSequenceOperationInfo(ii);
        buildTerminateSequenceOperationInfo(ii);
        buildSequenceAckOperationInfo(ii);
        buildLastMessageOperationInfo(ii);
        buildAckRequestedOperationInfo(ii);

        // TODO: FaultInfo (SequenceFault)
    }

    void buildCreateSequenceOperationInfo(InterfaceInfo ii) {

        OperationInfo operationInfo = null;
        MessagePartInfo partInfo = null;
        MessageInfo messageInfo = null;

        operationInfo = ii.addOperation(RMConstants.getCreateSequenceOperationName());
        messageInfo = operationInfo.createMessage(RMConstants.getCreateSequenceOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
        partInfo = messageInfo.addMessagePart(CREATE_PART_NAME);
        partInfo.setElementQName(RMConstants.getCreateSequenceOperationName());
        partInfo.setElement(true);
        partInfo.setTypeClass(CreateSequenceType.class);

        messageInfo = operationInfo.createMessage(RMConstants.getCreateSequenceResponseOperationName(),
                                                  MessageInfo.Type.OUTPUT);
        operationInfo.setOutput(messageInfo.getName().getLocalPart(), messageInfo);
        partInfo = messageInfo.addMessagePart(CREATE_RESPONSE_PART_NAME);
        partInfo.setElementQName(RMConstants.getCreateSequenceResponseOperationName());
        partInfo.setElement(true);
        partInfo.setTypeClass(CreateSequenceResponseType.class);
        partInfo.setIndex(0);

        operationInfo = ii.addOperation(RMConstants.getCreateSequenceOnewayOperationName());
        messageInfo = operationInfo.createMessage(RMConstants.getCreateSequenceOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
        partInfo = messageInfo.addMessagePart(CREATE_PART_NAME);
        partInfo.setElementQName(RMConstants.getCreateSequenceOperationName());
        partInfo.setElement(true);
        partInfo.setTypeClass(CreateSequenceType.class);

        operationInfo = ii.addOperation(RMConstants.getCreateSequenceResponseOnewayOperationName());
        messageInfo = operationInfo.createMessage(RMConstants.getCreateSequenceResponseOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
        partInfo = messageInfo.addMessagePart(CREATE_RESPONSE_PART_NAME);
        partInfo.setElementQName(RMConstants.getCreateSequenceResponseOperationName());
        partInfo.setElement(true);
        partInfo.setTypeClass(CreateSequenceResponseType.class);
    }

    void buildTerminateSequenceOperationInfo(InterfaceInfo ii) {

        OperationInfo operationInfo = null;
        MessagePartInfo partInfo = null;
        MessageInfo messageInfo = null;

        operationInfo = ii.addOperation(RMConstants.getTerminateSequenceOperationName());
        messageInfo = operationInfo.createMessage(RMConstants.getTerminateSequenceOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
        partInfo = messageInfo.addMessagePart(TERMINATE_PART_NAME);
        partInfo.setElementQName(RMConstants.getTerminateSequenceOperationName());
        partInfo.setElement(true);
        partInfo.setTypeClass(TerminateSequenceType.class);
    }

    void buildSequenceAckOperationInfo(InterfaceInfo ii) {

        OperationInfo operationInfo = null;
        MessageInfo messageInfo = null;

        operationInfo = ii.addOperation(RMConstants.getSequenceAckOperationName());
        messageInfo = operationInfo.createMessage(RMConstants.getSequenceAckOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
    }

    void buildLastMessageOperationInfo(InterfaceInfo ii) {

        OperationInfo operationInfo = null;
        MessageInfo messageInfo = null;

        operationInfo = ii.addOperation(RMConstants.getLastMessageOperationName());
        messageInfo = operationInfo.createMessage(RMConstants.getLastMessageOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
    }

    void buildAckRequestedOperationInfo(InterfaceInfo ii) {

        OperationInfo operationInfo = null;
        MessageInfo messageInfo = null;

        operationInfo = ii.addOperation(RMConstants.getAckRequestedOperationName());
        messageInfo = operationInfo.createMessage(RMConstants.getAckRequestedOperationName(),
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
            bi.setName(BINDING_NAME);
            BindingOperationInfo boi = null;
            SoapOperationInfo soi = null;

            boi = bi.buildOperation(RMConstants.getCreateSequenceOperationName(), RMConstants
                .getCreateSequenceOperationName().getLocalPart(), null);
            soi = new SoapOperationInfo();
            soi.setAction(RMConstants.getCreateSequenceAction());
            boi.addExtensor(soi);
            bi.addOperation(boi);

            boi = bi.buildOperation(RMConstants.getTerminateSequenceOperationName(), RMConstants
                .getTerminateSequenceOperationName().getLocalPart(), null);
            soi = new SoapOperationInfo();
            soi.setAction(RMConstants.getTerminateSequenceAction());
            boi.addExtensor(soi);
            bi.addOperation(boi);

            boi = bi.buildOperation(RMConstants.getSequenceAckOperationName(), null, null);
            assert null != boi;
            soi = new SoapOperationInfo();
            soi.setAction(RMConstants.getSequenceAckAction());
            boi.addExtensor(soi);
            bi.addOperation(boi);

            boi = bi.buildOperation(RMConstants.getLastMessageOperationName(), null, null);
            assert null != boi;
            soi = new SoapOperationInfo();
            soi.setAction(RMConstants.getLastMessageAction());
            boi.addExtensor(soi);
            bi.addOperation(boi);

            boi = bi.buildOperation(RMConstants.getAckRequestedOperationName(), null, null);
            assert null != boi;
            soi = new SoapOperationInfo();
            soi.setAction(RMConstants.getAckRequestedAction());
            boi.addExtensor(soi);
            bi.addOperation(boi);

            boi = bi.buildOperation(RMConstants.getCreateSequenceOnewayOperationName(), RMConstants
                .getCreateSequenceOperationName().getLocalPart(), null);
            soi = new SoapOperationInfo();
            soi.setAction(RMConstants.getCreateSequenceAction());
            boi.addExtensor(soi);
            bi.addOperation(boi);

            boi = bi.buildOperation(RMConstants.getCreateSequenceResponseOnewayOperationName(), RMConstants
                .getCreateSequenceResponseOperationName().getLocalPart(), null);
            soi = new SoapOperationInfo();
            soi.setAction(RMConstants.getCreateSequenceResponseAction());
            boi.addExtensor(soi);
            bi.addOperation(boi);

            si.addBinding(bi);
        }

        // TODO: BindingFaultInfo (SequenceFault)
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
    }

    class EffectivePolicyImpl implements EffectivePolicy {

        private EndpointPolicy endpointPolicy;
        private List<Interceptor> interceptors;

        EffectivePolicyImpl(EndpointPolicy ep, PolicyInterceptorProviderRegistry reg, boolean outbound,
                            boolean fault) {
            endpointPolicy = ep;
            interceptors = reg.getInterceptors(endpointPolicy.getChosenAlternative(), outbound, fault);
        }

        public Collection<PolicyAssertion> getChosenAlternative() {
            return endpointPolicy.getChosenAlternative();
        }

        public List<Interceptor> getInterceptors() {
            return interceptors;
        }

        public Policy getPolicy() {
            return endpointPolicy.getPolicy();
        }
    }
}
