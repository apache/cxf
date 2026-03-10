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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.RequiredModelMBean;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.jmx.export.runtime.ModelMBeanAssembler;
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
import org.apache.cxf.ws.policy.EffectivePolicyImpl;
import org.apache.cxf.ws.policy.EndpointPolicy;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.rm.manager.SequenceTerminationPolicyType;
import org.apache.cxf.ws.rm.manager.SourcePolicyType;
import org.apache.cxf.ws.rm.v200702.CloseSequenceResponseType;
import org.apache.cxf.ws.rm.v200702.CloseSequenceType;
import org.apache.cxf.ws.security.SecurityConstants;

public class RMEndpoint {

    private static final Logger LOG = LogUtils.getL7dLogger(RMEndpoint.class);

    private static final String SERVICE_NAME = "SequenceAbstractService";
    private static final String INTERFACE_NAME = "SequenceAbstractPortType";
    private static final String BINDING_NAME = "SequenceAbstractSoapBinding";

    private static final String CREATE_PART_NAME = "create";
    private static final String CREATE_RESPONSE_PART_NAME = "createResponse";
    private static final String TERMINATE_PART_NAME = "terminate";
    private static final String TERMINATE_RESPONSE_PART_NAME = "terminateResponse";
    private static final String CLOSE_PART_NAME = "close";
    private static final String CLOSE_RESPONSE_PART_NAME = "closeResponse";

    private static Schema rmSchema;

    private RMManager manager;
    private Endpoint applicationEndpoint;
    private Conduit conduit;
    private EndpointReferenceType replyTo;
    private Source source;
    private Destination destination;
    private final Map<ProtocolVariation, WrappedService> services = new EnumMap<>(ProtocolVariation.class);
    private final Map<ProtocolVariation, Endpoint> endpoints = new EnumMap<>(ProtocolVariation.class);
    private Object tokenStore;
    private Proxy proxy;
    private Servant servant;
    private long lastApplicationMessage;
    private long lastControlMessage;
    private final AtomicInteger applicationMessageCount = new AtomicInteger();
    private final AtomicInteger controlMessageCount = new AtomicInteger();
    private InstrumentationManager instrumentationManager;
    private RMConfiguration configuration;
    private ManagedRMEndpoint managedEndpoint;
    private RequiredModelMBean modelMBean;
    private final AtomicInteger acknowledgementSequence = new AtomicInteger();

    public RMEndpoint(RMManager m, Endpoint ae) {
        manager = m;
        applicationEndpoint = ae;
        source = new Source(this);
        destination = new Destination(this);
        proxy = new Proxy(this);
        servant = new Servant(this);
        tokenStore = ae.getEndpointInfo().getProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
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
    public Endpoint getEndpoint(ProtocolVariation protocol) {
        return endpoints.get(protocol);
    }

    /**
     * @return Returns the RM protocol service.
     */
    public Service getService(ProtocolVariation protocol) {
        return services.get(protocol);
    }

    /**
     * @return Returns the RM protocol binding info.
     */
    public BindingInfo getBindingInfo(ProtocolVariation protocol) {
        final QName bindingQName = new QName(protocol.getWSRMNamespace(), BINDING_NAME);
        return services.get(protocol).getServiceInfo().getBinding(bindingQName);
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
     * @return The number of times when last application message was received.
     */
    public int getApplicationMessageCount() {
        return applicationMessageCount.get();
    }

    /**
     * Indicates that an application message has been received.
     */
    public void receivedApplicationMessage() {
        lastApplicationMessage = System.currentTimeMillis();
        applicationMessageCount.incrementAndGet();
    }

    /**
     * @return The time when last RM protocol message was received.
     */
    public long getLastControlMessage() {
        return lastControlMessage;
    }

    /**
     * @return The number of times when RM protocol message was received.
     */
    public int getControlMessageCount() {
        return controlMessageCount.get();
    }

    /**
     * Indicates that an RM protocol message has been received.
     */
    public void receivedControlMessage() {
        lastControlMessage = System.currentTimeMillis();
        controlMessageCount.incrementAndGet();
    }

    /**
     * @return Returns the conduit.
     */
    public Conduit getConduit() {
        return conduit;
    }

    /**
     * Get the RM configuration applied to this endpoint.
     *
     * @return configuration
     */
    public RMConfiguration getConfiguration() {
        return configuration;
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

    /**
     * Handle message accepted for source sequence. This generates a callback if a receiver is set on the message.
     * @param ssid
     * @param number
     * @param msg
     */
    public void handleAccept(String ssid, long number, Message msg) {
        Object value = msg.get(RMMessageConstants.RM_CLIENT_CALLBACK);
        if (value instanceof MessageCallback) {
            ((MessageCallback)value).messageAccepted(ssid, number);
        }
    }

    /**
     * Handle message acknowledgment for source sequence. This generates a notification of the acknowledgment if JMX
     * is being used, and also generates a callback if a receiver is set on the message.
     *
     * @param ssid
     * @param number
     * @param msg
     */
    public void handleAcknowledgment(String ssid, long number, Message msg) {
        if (modelMBean != null) {
            int seq = acknowledgementSequence.incrementAndGet();
            try {
                modelMBean.sendNotification(new AcknowledgementNotification(this, seq, ssid, number));
            } catch (RuntimeOperationsException | MBeanException e) {
                LOG.log(Level.WARNING, "Error handling JMX notification", e);
            }
        }
        Object value = msg.get(RMMessageConstants.RM_CLIENT_CALLBACK);
        if (value instanceof MessageCallback) {
            ((MessageCallback)value).messageAcknowledged(ssid, number);
        }
    }

    void initialise(RMConfiguration config, Conduit c, EndpointReferenceType r,
        org.apache.cxf.transport.Destination d,
        Message message) {
        configuration = config;
        conduit = c;
        replyTo = r;
        createServices();
        createEndpoints(d);
        setPolicies(message);
        if (manager != null && manager.getBus() != null) {
            managedEndpoint = new ManagedRMEndpoint(this);
            instrumentationManager = manager.getBus().getExtension(InstrumentationManager.class);
            if (instrumentationManager != null) {
                ModelMBeanAssembler assembler = new ModelMBeanAssembler();
                ModelMBeanInfo mbi = assembler.getModelMbeanInfo(managedEndpoint.getClass());
                MBeanServer mbs = instrumentationManager.getMBeanServer();
                if (mbs == null) {
                    LOG.log(Level.WARNING, "MBeanServer not available.");
                } else {
                    try {
                        RequiredModelMBean rtMBean =
                            (RequiredModelMBean)mbs.instantiate("javax.management.modelmbean.RequiredModelMBean");
                        rtMBean.setModelMBeanInfo(mbi);
                        try {
                            rtMBean.setManagedResource(managedEndpoint, "ObjectReference");
                        } catch (InvalidTargetObjectTypeException itotex) {
                            throw new JMException(itotex.getMessage());
                        }
                        ObjectName name = managedEndpoint.getObjectName();
                        instrumentationManager.register(rtMBean, name);
                        modelMBean = rtMBean;
                    } catch (JMException jmex) {
                        LOG.log(Level.WARNING, "Registering ManagedRMEndpoint failed.", jmex);
                    }
                }
            }
        }
    }

    // internally we keep three services and three endpoints to support three protocol variations
    // using the specifically generated jaxb classes and operation names etc but this could probably
    // be simplified/unified.
    void createServices() {
        for (ProtocolVariation protocol : ProtocolVariation.values()) {
            createService(protocol);
        }
    }

    void createService(ProtocolVariation protocol) {
        ServiceInfo si = new ServiceInfo();
        si.setProperty(Schema.class.getName(), getSchema());
        QName serviceQName = new QName(protocol.getWSRMNamespace(), SERVICE_NAME);
        si.setName(serviceQName);

        buildInterfaceInfo(si, protocol);

        WrappedService service = new WrappedService(applicationEndpoint.getService(), serviceQName, si);

        Class<?> create = protocol.getCodec().getCreateSequenceType();
        try {
            JAXBContext ctx =
                JAXBContext.newInstance(PackageUtils.getPackageName(create), create.getClassLoader());
            service.setDataBinding(new JAXBDataBinding(ctx));
        } catch (JAXBException e) {
            throw new ServiceConstructionException(e);
        }
        service.setInvoker(servant);
        services.put(protocol, service);
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

                javax.xml.transform.Source[] schemas = new javax.xml.transform.Source[] {ad, rm};
                rmSchema = factory.newSchema(schemas);
            } catch (Exception ex) {
                //ignore
            }
        }
        return rmSchema;
    }

    void createEndpoints(org.apache.cxf.transport.Destination d) {
        for (ProtocolVariation protocol : ProtocolVariation.values()) {
            createEndpoint(d, protocol);
        }
    }

    void createEndpoint(org.apache.cxf.transport.Destination d, ProtocolVariation protocol) {
        final QName bindingQName = new QName(protocol.getWSRMNamespace(), BINDING_NAME);
        WrappedService service = services.get(protocol);
        ServiceInfo si = service.getServiceInfo();
        buildBindingInfo(si, protocol);
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
        ei.setProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, tokenStore);

        Endpoint endpoint = new WrappedEndpoint(applicationEndpoint, ei, service);
        if (applicationEndpoint.getEndpointInfo() != null
            && applicationEndpoint.getEndpointInfo().getProperties() != null) {
            for (String key : applicationEndpoint.getEndpointInfo().getProperties().keySet()) {
                endpoint.getEndpointInfo()
                    .setProperty(key, applicationEndpoint.getEndpointInfo().getProperty(key));
            }
        }
        service.setEndpoint(endpoint);
        endpoints.put(protocol, endpoint);
    }

    void setPolicies(Message message) {
        // use same WS-policies as for application endpoint
        PolicyEngine engine = manager.getBus().getExtension(PolicyEngine.class);
        if (null == engine || !engine.isEnabled()) {
            return;
        }

        for (Endpoint endpoint : endpoints.values()) {
            EndpointInfo ei = endpoint.getEndpointInfo();
            EndpointPolicy epi = null == conduit
                ? engine.getServerEndpointPolicy(applicationEndpoint.getEndpointInfo(), null, message)
                    : engine.getClientEndpointPolicy(applicationEndpoint.getEndpointInfo(), conduit, message);

            if (conduit != null) {
                engine.setClientEndpointPolicy(ei, epi);
            } else {
                engine.setServerEndpointPolicy(ei, epi);
            }
            EffectivePolicyImpl effectiveOutbound = new EffectivePolicyImpl();
            effectiveOutbound.initialise(epi, engine, false, false, message);
            EffectivePolicyImpl effectiveInbound = new EffectivePolicyImpl();
            effectiveInbound.initialise(epi, engine, true, false, message);

            BindingInfo bi = ei.getBinding();
            Collection<BindingOperationInfo> bois = bi.getOperations();

            for (BindingOperationInfo boi : bois) {
                engine.setEffectiveServerRequestPolicy(ei, boi, effectiveInbound);
                engine.setEffectiveServerResponsePolicy(ei, boi, effectiveOutbound);

                engine.setEffectiveClientRequestPolicy(ei, boi, effectiveOutbound);
                engine.setEffectiveClientResponsePolicy(ei, boi, effectiveInbound);
            }
        }

        // TODO: FaultPolicy (SequenceFault)
    }

    void buildInterfaceInfo(ServiceInfo si, ProtocolVariation protocol) {
        QName interfaceQName = new QName(protocol.getWSRMNamespace(), INTERFACE_NAME);
        InterfaceInfo ii = new InterfaceInfo(si, interfaceQName);
        buildOperationInfo(ii, protocol);
    }

    void buildOperationInfo(InterfaceInfo ii, ProtocolVariation protocol) {
        buildCreateSequenceOperationInfo(ii, protocol);
        buildTerminateSequenceOperationInfo(ii, protocol);
        buildSequenceAckOperationInfo(ii, protocol);
        buildCloseSequenceOperationInfo(ii, protocol);
        buildAckRequestedOperationInfo(ii, protocol);

        // TODO: FaultInfo (SequenceFault)
    }

    void buildCreateSequenceOperationInfo(InterfaceInfo ii, ProtocolVariation protocol) {
        RMConstants consts = protocol.getConstants();
        OperationInfo operationInfo = ii.addOperation(consts.getCreateSequenceOperationName());
        MessageInfo messageInfo = operationInfo.createMessage(consts.getCreateSequenceOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
        MessagePartInfo partInfo = messageInfo.addMessagePart(CREATE_PART_NAME);
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

    void buildTerminateSequenceOperationInfo(InterfaceInfo ii, ProtocolVariation protocol) {
        RMConstants consts = protocol.getConstants();
        OperationInfo operationInfo = ii.addOperation(consts.getTerminateSequenceOperationName());

        MessageInfo messageInfo = operationInfo.createMessage(consts.getTerminateSequenceOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
        MessagePartInfo partInfo = messageInfo.addMessagePart(TERMINATE_PART_NAME);
        partInfo.setElementQName(consts.getTerminateSequenceOperationName());
        partInfo.setElement(true);
        partInfo.setTypeClass(protocol.getCodec().getTerminateSequenceType());
        if (RM11Constants.NAMESPACE_URI.equals(protocol.getWSRMNamespace())) {
            messageInfo = operationInfo.createMessage(
                RM11Constants.INSTANCE.getTerminateSequenceResponseOperationName(),
                MessageInfo.Type.OUTPUT);
            operationInfo.setOutput(messageInfo.getName().getLocalPart(), messageInfo);
            partInfo = messageInfo.addMessagePart(TERMINATE_RESPONSE_PART_NAME);
            partInfo.setElementQName(RM11Constants.INSTANCE.getTerminateSequenceResponseOperationName());
            partInfo.setElement(true);
            partInfo.setTypeClass(protocol.getCodec().getTerminateSequenceResponseType());
            partInfo.setIndex(0);
        }

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

    void buildSequenceAckOperationInfo(InterfaceInfo ii, ProtocolVariation protocol) {
        RMConstants consts = protocol.getConstants();
        OperationInfo operationInfo = ii.addOperation(consts.getSequenceAckOperationName());
        MessageInfo messageInfo = operationInfo.createMessage(consts.getSequenceAckOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
    }

    void buildCloseSequenceOperationInfo(InterfaceInfo ii, ProtocolVariation protocol) {
        RMConstants consts = protocol.getConstants();
        OperationInfo operationInfo = ii.addOperation(consts.getCloseSequenceOperationName());
        MessageInfo messageInfo = operationInfo.createMessage(consts.getCloseSequenceOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
        if (RM11Constants.NAMESPACE_URI.equals(protocol.getWSRMNamespace())) {
            MessagePartInfo partInfo = messageInfo.addMessagePart(CLOSE_PART_NAME);
            partInfo.setElementQName(consts.getCloseSequenceOperationName());
            partInfo.setElement(true);
            partInfo.setTypeClass(CloseSequenceType.class);
            messageInfo = operationInfo.createMessage(
                RM11Constants.INSTANCE.getCloseSequenceResponseOperationName(),
                MessageInfo.Type.OUTPUT);
            operationInfo.setOutput(messageInfo.getName().getLocalPart(), messageInfo);
            partInfo = messageInfo.addMessagePart(CLOSE_RESPONSE_PART_NAME);
            partInfo.setElementQName(RM11Constants.INSTANCE.getCloseSequenceResponseOperationName());
            partInfo.setElement(true);
            partInfo.setTypeClass(CloseSequenceResponseType.class);
            partInfo.setIndex(0);
        }
    }

    void buildAckRequestedOperationInfo(InterfaceInfo ii, ProtocolVariation protocol) {
        RMConstants consts = protocol.getConstants();
        OperationInfo operationInfo = ii.addOperation(consts.getAckRequestedOperationName());
        MessageInfo messageInfo = operationInfo.createMessage(consts.getAckRequestedOperationName(),
                                                  MessageInfo.Type.INPUT);
        operationInfo.setInput(messageInfo.getName().getLocalPart(), messageInfo);
    }

    void buildBindingInfo(ServiceInfo si, ProtocolVariation protocol) {
        // use same binding id as for application endpoint
        // also, to workaround the problem that it may not be possible to determine
        // the soap version depending on the bindingId, speciffy the soap version
        // explicitly
        if (null != applicationEndpoint) {
            final QName bindingQName = new QName(protocol.getWSRMNamespace(), BINDING_NAME);
            SoapBindingInfo sbi = (SoapBindingInfo)applicationEndpoint.getEndpointInfo().getBinding();
            SoapVersion sv = sbi.getSoapVersion();
            String bindingId = sbi.getBindingId();
            SoapBindingInfo bi = new SoapBindingInfo(si, bindingId, sv);
            bi.setName(bindingQName);

            RMConstants consts = protocol.getConstants();

            BindingOperationInfo boi = bi.buildOperation(consts.getCreateSequenceOperationName(),
                                    consts.getCreateSequenceOperationName().getLocalPart(), null);
            addAction(boi, consts.getCreateSequenceAction(), consts.getCreateSequenceResponseAction());
            bi.addOperation(boi);

            boi = bi.buildOperation(consts.getTerminateSequenceOperationName(),
                                    consts.getTerminateSequenceOperationName().getLocalPart(), null);

            if (RM11Constants.NAMESPACE_URI.equals(protocol.getWSRMNamespace())) {
                addAction(boi, consts.getTerminateSequenceAction(),
                          RM11Constants.INSTANCE.getTerminateSequenceResponseAction());
            } else {
                addAction(boi, consts.getTerminateSequenceAction());
            }
            bi.addOperation(boi);

            boi = bi.buildOperation(consts.getTerminateSequenceAnonymousOperationName(),
                                    null, consts.getTerminateSequenceAnonymousOperationName().getLocalPart());
            addAction(boi, consts.getTerminateSequenceAction());
            bi.addOperation(boi);

            boi = bi.buildOperation(consts.getSequenceAckOperationName(), null, null);
            addAction(boi, consts.getSequenceAckAction());
            bi.addOperation(boi);

            boi = bi.buildOperation(consts.getCloseSequenceOperationName(), null, null);
            if (RM11Constants.NAMESPACE_URI.equals(protocol.getWSRMNamespace())) {
                addAction(boi, consts.getCloseSequenceAction(),
                          RM11Constants.INSTANCE.getCloseSequenceResponseAction());
            } else {
                addAction(boi, consts.getCloseSequenceAction());
            }
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
        List<ExtensibilityElement> exts = endpointInfo.getExtensors(ExtensibilityElement.class);
        Object ua = getUsingAddressing(exts);
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
        for (DestinationSequence ds : getDestination().getAllSequences()) {
            manager.getRedeliveryQueue().stop(ds);
        }

        // unregistering of this managed bean from the server is done by the bus itself
    }

    int getProcessingSourceSequenceCount() {
        return source != null ? source.getProcessingSequenceCount() : 0;
    }

    int getCompletedSourceSequenceCount() {
        return source != null ? source.getCompletedSequenceCount() : 0;
    }

    int getProcessingDestinationSequenceCount() {
        return destination != null ? destination.getProcessingSequenceCount() : 0;
    }

    int getCompletedDestinationSequenceCount() {
        return destination != null ? destination.getCompletedSequenceCount() : 0;
    }
}
