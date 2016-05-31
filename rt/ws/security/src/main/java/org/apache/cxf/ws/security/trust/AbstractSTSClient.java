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

package org.apache.cxf.ws.security.trust;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.wsdl.Definition;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
//import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ModCountCopyOnWriteArrayList;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.databinding.source.SourceDataBinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.ws.addressing.VersionTransformer;
import org.apache.cxf.ws.mex.MetadataExchange;
import org.apache.cxf.ws.mex.model._2004_09.Metadata;
import org.apache.cxf.ws.mex.model._2004_09.MetadataSection;
import org.apache.cxf.ws.policy.EffectivePolicy;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.attachment.reference.ReferenceResolver;
import org.apache.cxf.ws.policy.attachment.reference.RemoteReferenceResolver;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.claims.ClaimsCallback;
import org.apache.cxf.ws.security.trust.delegation.DelegationCallback;
import org.apache.cxf.ws.security.wss4j.WSS4JUtils;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.apache.neethi.All;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.neethi.PolicyRegistry;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.derivedKey.P_SHA1;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.token.Reference;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.processor.EncryptedKeyProcessor;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.dom.util.X509Util;
import org.apache.wss4j.dom.util.XmlSchemaDateFormat;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.SPConstants.SPVersion;
import org.apache.wss4j.policy.model.AbstractBinding;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.AlgorithmSuite.AlgorithmSuiteType;
import org.apache.wss4j.policy.model.Header;
import org.apache.wss4j.policy.model.ProtectionToken;
import org.apache.wss4j.policy.model.SecureConversationToken;
import org.apache.wss4j.policy.model.SignedParts;
import org.apache.wss4j.policy.model.Trust10;
import org.apache.wss4j.policy.model.Trust13;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.keys.content.X509Data;
import org.apache.xml.security.keys.content.keyvalues.DSAKeyValue;
import org.apache.xml.security.keys.content.keyvalues.RSAKeyValue;

/**
 * An abstract class with some functionality to invoke on a SecurityTokenService (STS) via the
 * WS-Trust protocol.
 */
public abstract class AbstractSTSClient implements Configurable, InterceptorProvider {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractSTSClient.class);
    
    protected Bus bus;
    protected String name = "default.sts-client";
    protected Client client;
    protected String location;

    protected String wsdlLocation;
    protected QName serviceName;
    protected QName endpointName;

    protected Policy policy;
    protected String soapVersion = SoapBindingConstants.SOAP11_BINDING_ID;
    protected int keySize = 256;
    protected boolean requiresEntropy = true;
    protected Element template;
    protected Object claims;
    protected CallbackHandler claimsCallbackHandler;
    protected AlgorithmSuite algorithmSuite;
    protected String namespace = STSUtils.WST_NS_05_12;
    protected String addressingNamespace = "http://www.w3.org/2005/08/addressing";
    protected String wspNamespace = "http://www.w3.org/ns/ws-policy";
    protected Object onBehalfOf;
    protected boolean enableAppliesTo = true;

    protected boolean useCertificateForConfirmationKeyInfo;
    protected boolean isSecureConv;
    protected boolean isSpnego;
    protected boolean enableLifetime;
    protected int ttl = 300;
    protected boolean sendRenewing = true;
    protected boolean allowRenewing = true;
    protected boolean allowRenewingAfterExpiry;
    
    protected Object actAs;
    protected String tokenType;
    protected String keyType;
    protected boolean sendKeyType = true;
    protected Message message;
    protected String context;
    protected X509Certificate useKeyCertificate;

    protected Map<String, Object> ctx = new HashMap<>();
    
    protected List<Interceptor<? extends Message>> in = new ModCountCopyOnWriteArrayList<>();
    protected List<Interceptor<? extends Message>> out = new ModCountCopyOnWriteArrayList<>();
    protected List<Interceptor<? extends Message>> outFault = new ModCountCopyOnWriteArrayList<>();
    protected List<Interceptor<? extends Message>> inFault = new ModCountCopyOnWriteArrayList<>();
    protected List<Feature> features;

    public AbstractSTSClient(Bus b) {
        bus = b;
    }

    public String getBeanName() {
        return name;
    }

    public void setBeanName(String s) {
        name = s;
    }
    
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
    
    public void setMessage(Message message) {
        this.message = message;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }
    
    public void setEnableLifetime(boolean enableLifetime) {
        this.enableLifetime = enableLifetime;
    }
    
    public void setSendRenewing(boolean sendRenewing) {
        this.sendRenewing = sendRenewing;
    }
    
    /**
     * Sets the WS-P policy that is applied to communications between this client and the remote server
     * if no value is supplied for {@link #setWsdlLocation(String)}.
     * <p/>
     * Accepts {@link Policy} or {@link Element} as input.
     *
     * @param newPolicy the policy object
     *
     * @throws IllegalArgumentException if {@code newPolicy} is not one of the supported types.
     */
    public void setPolicy(Object newPolicy) {
        if (newPolicy instanceof Policy) {
            this.setPolicyInternal((Policy) newPolicy);
        } else if (newPolicy instanceof Element) {
            this.setPolicyInternal((Element) newPolicy);    
        } else if (newPolicy instanceof String) {
            this.setPolicyInternal((String) newPolicy);    
        } else {
            throw new IllegalArgumentException("Unsupported policy object.  Type must be "
                       + "org.apache.neethi.Policy or org.w3c.dom.Element.");
        }
    }

    public void setSoap12() {
        soapVersion = SoapBindingConstants.SOAP12_BINDING_ID;
    }

    public void setSoap11() {
        soapVersion = SoapBindingConstants.SOAP11_BINDING_ID;
    }

    public void setSoap11(boolean b) {
        if (b) {
            setSoap11();
        } else {
            setSoap12();
        }
    }

    public void setAddressingNamespace(String ad) {
        addressingNamespace = ad;
    }

    public void setTrust(Trust10 trust) {
        if (trust != null) {
            if (trust instanceof Trust13) {
                namespace = STSUtils.WST_NS_05_12;
            } else {
                namespace = STSUtils.WST_NS_05_02;
            }
            requiresEntropy = trust.isRequireClientEntropy();
        }
    }

    public boolean isRequiresEntropy() {
        return requiresEntropy;
    }

    public void setRequiresEntropy(boolean requiresEntropy) {
        this.requiresEntropy = requiresEntropy;
    }

    public boolean isSecureConv() {
        return isSecureConv;
    }

    public void setSecureConv(boolean secureConv) {
        this.isSecureConv = secureConv;
    }
    
    public boolean isSpnego() {
        return isSpnego;
    }

    public void setSpnego(boolean spnego) {
        this.isSpnego = spnego;
    }
    
    public boolean isAllowRenewing() {
        return allowRenewing;
    }

    public void setAllowRenewing(boolean allowRenewing) {
        this.allowRenewing = allowRenewing;
    }

    public boolean isAllowRenewingAfterExpiry() {
        return allowRenewingAfterExpiry;
    }

    public void setAllowRenewingAfterExpiry(boolean allowRenewingAfterExpiry) {
        this.allowRenewingAfterExpiry = allowRenewingAfterExpiry;
    }
    
    public boolean isEnableAppliesTo() {
        return enableAppliesTo;
    }
    
    public void setEnableAppliesTo(boolean enableAppliesTo) {
        this.enableAppliesTo = enableAppliesTo;
    }
    
    public String getContext() {
        return context;
    }
    
    public void setContext(String context) {
        this.context = context;
    }

    public void setAlgorithmSuite(AlgorithmSuite ag) {
        algorithmSuite = ag;
    }

    public Map<String, Object> getRequestContext() {
        return ctx;
    }

    public void setProperties(Map<String, Object> p) {
        ctx.putAll(p);
    }

    public Map<String, Object> getProperties() {
        return ctx;
    }

    public void setWsdlLocation(String wsdl) {
        wsdlLocation = wsdl;
    }
    public String getWsdlLocation() {
        return wsdlLocation;
    }

    public void setServiceName(String qn) {
        serviceName = QName.valueOf(qn);
    }

    public void setEndpointName(String qn) {
        endpointName = QName.valueOf(qn);
    }
    
    public void setServiceQName(QName qn) {
        serviceName = qn;
    }
    public QName getServiceQName() {
        return serviceName;
    }

    public void setEndpointQName(QName qn) {
        endpointName = qn;
    }
    public QName getEndpointQName() {
        return endpointName;
    }
    
    public void setActAs(Object actAs) {
        this.actAs = actAs;
    }
    
    public void setKeySize(int i) {
        keySize = i;
    }
    
    public int getKeySize() {
        return keySize;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setSendKeyType(boolean sendKeyType) {
        this.sendKeyType = sendKeyType;
    }
    
    public void setKeyType(String keyType) {
        this.keyType = keyType;
    }
    
    @Deprecated
    public void setOnBehalfOfElement(Element onBehalfOfElement) {
        this.onBehalfOf = onBehalfOfElement;
    }

    public void setOnBehalfOf(Object onBehalfOf) {
        this.onBehalfOf = onBehalfOf;
    }
    
    /**
     * Indicate whether to use the signer's public X509 certificate for the subject confirmation key info 
     * when creating a RequestsSecurityToken message. If the property is set to 'false', only the public key 
     * value will be provided in the request. If the property is set to 'true' the complete certificate will 
     * be sent in the request.
     * 
     * Note: this setting is only applicable for assertions that use an asymmetric proof key
     */
    public void setUseCertificateForConfirmationKeyInfo(boolean useCertificate) {
        this.useCertificateForConfirmationKeyInfo = useCertificate;
    }
    
    public boolean isUseCertificateForConfirmationKeyInfo() {
        return useCertificateForConfirmationKeyInfo;
    }
    
    protected void setPolicyInternal(Policy newPolicy) {
        this.policy = newPolicy;
        if (algorithmSuite == null) {
            Iterator<?> i = policy.getAlternatives();
            while (i.hasNext() && algorithmSuite == null) {
                List<PolicyComponent> p = CastUtils.cast((List<?>)i.next());
                for (PolicyComponent p2 : p) {
                    if (p2 instanceof AbstractBinding) {
                        algorithmSuite = ((AbstractBinding)p2).getAlgorithmSuite();
                    }
                }
            }
        }
    }
    
    protected void setPolicyInternal(Element newPolicy) {
        this.setPolicyInternal(bus.getExtension(PolicyBuilder.class).getPolicy(newPolicy));
    }
    
    protected void setPolicyInternal(String policyReference) {
        PolicyBuilder builder = bus.getExtension(PolicyBuilder.class);
        ReferenceResolver resolver = new RemoteReferenceResolver(null, builder);
        PolicyRegistry registry = bus.getExtension(PolicyEngine.class).getRegistry();
        Policy resolved = registry.lookup(policyReference);
        if (null != resolved) {
            this.setPolicyInternal(resolved);
        } else {
            this.setPolicyInternal(resolver.resolveReference(policyReference));
        }
    }

    public Client getClient()  throws BusException, EndpointException {
        if (client == null) {
            createClient();
        }
        return client;
    }
    
    public void configureViaEPR(EndpointReferenceType ref, boolean useEPRWSAAddrAsMEXLocation) {
        if (client != null) {
            return;
        }
        location = EndpointReferenceUtils.getAddress(ref);
        if (location != null) {
            location = location.trim();
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("EPR address: " + location);
        }
        
        final QName sName = EndpointReferenceUtils.getServiceName(ref, bus);
        if (sName != null) {
            serviceName = sName;
            final QName epName = EndpointReferenceUtils.getPortQName(ref, bus);
            if (epName != null) {
                endpointName = epName;
            }
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("EPR endpoint: " + serviceName + " " + endpointName);
            }
        }
        final String wsdlLoc = EndpointReferenceUtils.getWSDLLocation(ref);
        if (wsdlLoc != null) {
            wsdlLocation = wsdlLoc;
        }
        
        String mexLoc = findMEXLocation(ref, useEPRWSAAddrAsMEXLocation);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("WS-MEX location: " + mexLoc);
        }
        if (mexLoc != null) {
            try {
                JaxWsProxyFactoryBean proxyFac = new JaxWsProxyFactoryBean();
                proxyFac.setBindingId(soapVersion);
                proxyFac.setAddress(mexLoc);
                MetadataExchange exc = proxyFac.create(MetadataExchange.class);
                Metadata metadata = exc.get2004();
                
                Definition definition = null;
                List<Schema> schemas = new ArrayList<Schema>();
                // Parse the MetadataSections into WSDL definition + associated schemas
                for (MetadataSection s : metadata.getMetadataSection()) {
                    if ("http://schemas.xmlsoap.org/wsdl/".equals(s.getDialect())) {
                        definition = 
                            bus.getExtension(WSDLManager.class).getDefinition((Element)s.getAny());
                    } else if ("http://www.w3.org/2001/XMLSchema".equals(s.getDialect())) {
                        Element schemaElement = (Element)s.getAny();
                        if (schemaElement ==  null) {
                            String schemaLocation = s.getLocation();
                            LOG.info("XSD schema location: " + schemaLocation);
                            schemaElement = downloadSchema(schemaLocation);
                        }
                        QName schemaName = 
                            new QName(schemaElement.getNamespaceURI(), schemaElement.getLocalName());
                        WSDLManager wsdlManager = bus.getExtension(WSDLManager.class);
                        ExtensibilityElement
                            exElement = wsdlManager.getExtensionRegistry().createExtension(Types.class, schemaName);
                        ((Schema)exElement).setElement(schemaElement);
                        schemas.add((Schema)exElement);
                    }
                }
                
                if (definition != null) {
                    // Add any extra schemas to the WSDL definition
                    for (Schema schema : schemas) {
                        definition.getTypes().addExtensibilityElement(schema);
                    }
                    
                    WSDLServiceFactory factory = new WSDLServiceFactory(bus, definition);
                    SourceDataBinding dataBinding = new SourceDataBinding();
                    factory.setDataBinding(dataBinding);
                    Service service = factory.create();
                    service.setDataBinding(dataBinding);

                    // Get the endpoint + service names by matching the 'location' to the
                    // address in the WSDL. If the 'location' is 'anonymous' then just fall
                    // back to the first service + endpoint name in the WSDL, if the endpoint
                    // name is not defined in the Metadata
                    List<ServiceInfo> services = service.getServiceInfos();
                    String anonymousAddress = "http://www.w3.org/2005/08/addressing/anonymous";

                    if (!anonymousAddress.equals(location)) {
                        for (ServiceInfo serv : services) {
                            for (EndpointInfo ei : serv.getEndpoints()) {
                                if (ei.getAddress().equals(location)) {
                                    endpointName = ei.getName();
                                    serviceName = serv.getName();
                                    LOG.fine("Matched endpoint to location");
                                }
                            }
                        }
                    }

                    EndpointInfo ei = service.getEndpointInfo(endpointName);
                    if (ei == null && anonymousAddress.equals(location)
                        && !services.isEmpty() && !services.get(0).getEndpoints().isEmpty()) {
                        LOG.fine("Anonymous location so taking first endpoint");
                        serviceName = services.get(0).getName();
                        endpointName = services.get(0).getEndpoints().iterator().next().getName();
                        ei = service.getEndpointInfo(endpointName);
                    }
                    
                    if (ei == null) {
                        throw new TrustException(LOG, "ADDRESS_NOT_MATCHED", location);
                    }

                    if (location != null && !anonymousAddress.equals(location)) {
                        ei.setAddress(location);
                    }
                    Endpoint endpoint = new EndpointImpl(bus, service, ei);
                    client = new ClientImpl(bus, endpoint);
                }
            } catch (Exception ex) {
                throw new TrustException("WS_MEX_ERROR", ex, LOG);
            }
        }
    }
    
    private Element downloadSchema(String schemaLocation) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        
        DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
        Document document = documentBuilder.parse(schemaLocation);
        return document.getDocumentElement();
    }
    
    protected String findMEXLocation(EndpointReferenceType ref, boolean useEPRWSAAddrAsMEXLocation) {
        if (ref.getMetadata() != null && ref.getMetadata().getAny() != null) {
            for (Object any : ref.getMetadata().getAny()) {
                if (any instanceof Element) {
                    String addr = findMEXLocation((Element)any);
                    if (addr != null) {
                        return addr;
                    }
                }
            }
        }
        return useEPRWSAAddrAsMEXLocation ? EndpointReferenceUtils.getAddress(ref) : null;
    }
    
    protected String findMEXLocation(Element ref) {
        Element el = DOMUtils.getFirstElement(ref);
        while (el != null) {
            if (el.getLocalName().equals("Address")
                && VersionTransformer.isSupported(el.getNamespaceURI())
                && "MetadataReference".equals(ref.getLocalName())) {
                return DOMUtils.getContent(el);
            } else {
                String ad = findMEXLocation(el);
                if (ad != null) {
                    return ad;
                }
            }
            el = DOMUtils.getNextElement(el);
        }
        return null;
    }
    
    protected void createClient() throws BusException, EndpointException {
        if (client != null) {
            return;
        }

        if (wsdlLocation != null) {
            WSDLServiceFactory factory = new WSDLServiceFactory(bus, wsdlLocation, serviceName);
            SourceDataBinding dataBinding = new SourceDataBinding();
            factory.setDataBinding(dataBinding);
            Service service = factory.create();
            service.setDataBinding(dataBinding);
            EndpointInfo ei = service.getEndpointInfo(endpointName);
            Endpoint endpoint = new EndpointImpl(bus, service, ei);
            client = new ClientImpl(bus, endpoint);
        } else if (location != null) {
            Endpoint endpoint = STSUtils.createSTSEndpoint(bus, namespace, null, location, soapVersion,
                                                           policy, endpointName);

            client = new ClientImpl(bus, endpoint);
        } else {
            throw new TrustException(LOG, "NO_LOCATION");
        }
        
        client.getInFaultInterceptors().addAll(inFault);
        client.getInInterceptors().addAll(in);
        client.getOutInterceptors().addAll(out);
        client.getOutFaultInterceptors().addAll(outFault);
        in = null;
        out = null;
        inFault = null;
        outFault = null;
        if (features != null) {
            for (Feature f : features) {
                f.initialize(client, bus);
            }
        }
    }

    protected BindingOperationInfo findOperation(String suffix) {
        BindingInfo bi = client.getEndpoint().getBinding().getBindingInfo();
        for (BindingOperationInfo boi : bi.getOperations()) {
            SoapOperationInfo soi = boi.getExtensor(SoapOperationInfo.class);
            String soapAction = soi != null ? soi.getAction() : null;
            Object o = boi.getOperationInfo().getInput()
                    .getExtensionAttribute(new QName("http://www.w3.org/2007/05/addressing/metadata",
                                                     "Action"));
            if (o instanceof QName) {
                o = ((QName)o).getLocalPart();
            }
            String wsamAction = o == null ? null : o.toString();
            
            if ((soapAction != null && soapAction.endsWith(suffix))
                || (wsamAction != null && wsamAction.endsWith(suffix))) {
                PolicyEngine pe = bus.getExtension(PolicyEngine.class);
                Conduit conduit = client.getConduit();
                EffectivePolicy effectivePolicy 
                    = pe.getEffectiveClientRequestPolicy(client.getEndpoint().getEndpointInfo(),
                                                         boi, conduit,
                                                         PhaseInterceptorChain.getCurrentMessage());
                setPolicyInternal(effectivePolicy.getPolicy());
                return boi;
            }
        }
        //operation is not correct as the Action is not set correctly.   Let's see if
        //we can at least find it by name and then set the action and such manually later.
        for (BindingOperationInfo boi : bi.getOperations()) {
            if (suffix.endsWith(boi.getName().getLocalPart())) {
                return boi;
            }
        }        
        //Still didn't find anything useful
        for (BindingOperationInfo boi : bi.getOperations()) {
            if (boi.getInput().getMessageInfo().getMessagePartsNumber() > 0) {
                MessagePartInfo mpi = boi.getInput().getMessageInfo().getFirstMessagePart();
                if ("RequestSecurityToken".equals(mpi.getConcreteName().getLocalPart())) {
                    return boi;
                }
            }
        }
        return null;
    }

    /**
     * Make an "Issue" invocation and return the response as a STSResponse Object
     */
    protected STSResponse issue(
        String appliesTo, String action, String requestType, String binaryExchange
    ) throws Exception {
        createClient();
        BindingOperationInfo boi = findOperation("/RST/Issue");

        client.getRequestContext().putAll(ctx);
        if (action != null) {
            client.getRequestContext().put(SoapBindingConstants.SOAP_ACTION, action);
        } else if (isSecureConv) {
            client.getRequestContext().put(SoapBindingConstants.SOAP_ACTION, 
                                           namespace + "/RST/SCT");
        } else {
            client.getRequestContext().put(SoapBindingConstants.SOAP_ACTION, 
                                           namespace + "/RST/Issue");
        }

        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityToken", namespace);
        writer.writeNamespace("wst", namespace);
        if (context != null) {
            writer.writeAttribute(null, "Context", context);
        }
        
        boolean wroteKeySize = false;
        String keyTypeTemplate = null;
        String sptt = null;
        
        if (template != null && DOMUtils.getFirstElement(template) != null) {
            if (this.useSecondaryParameters()) {
                writer.writeStartElement("wst", "SecondaryParameters", namespace);
            }
            
            Element tl = DOMUtils.getFirstElement(template);
            while (tl != null) {
                StaxUtils.copy(tl, writer);
                if ("KeyType".equals(tl.getLocalName())) {
                    keyTypeTemplate = DOMUtils.getContent(tl);
                } else if ("KeySize".equals(tl.getLocalName())) {
                    wroteKeySize = true;
                    keySize = Integer.parseInt(DOMUtils.getContent(tl));
                } else if ("TokenType".equals(tl.getLocalName())) {
                    sptt = DOMUtils.getContent(tl);
                }
                tl = DOMUtils.getNextElement(tl);
            }
            
            if (this.useSecondaryParameters()) {
                writer.writeEndElement();
            }
        }
        
        if (isSpnego) {
            tokenType = STSUtils.getTokenTypeSCT(namespace);
            sendKeyType = false;
        }

        addRequestType(requestType, writer);
        if (enableAppliesTo) {
            addAppliesTo(writer, appliesTo);
        }
        
        addClaims(writer);
        
        Element onBehalfOfToken = getOnBehalfOfToken();
        if (onBehalfOfToken != null) {
            writer.writeStartElement("wst", "OnBehalfOf", namespace);
            StaxUtils.copy(onBehalfOfToken, writer);
            writer.writeEndElement();
        }
        if (sptt == null) {
            addTokenType(writer);
        }
        if (isSecureConv || enableLifetime) {
            addLifetime(writer);
        }
        if (keyTypeTemplate == null) {
            keyTypeTemplate = writeKeyType(writer, keyType);
        }

        byte[] requestorEntropy = null;
        X509Certificate cert = null;
        Crypto crypto = null;

        if (keySize <= 0) {
            keySize = 256;
        }
        if (keyTypeTemplate != null && keyTypeTemplate.endsWith("SymmetricKey")) {
            requestorEntropy = writeElementsForRSTSymmetricKey(writer, wroteKeySize);
        } else if (keyTypeTemplate != null && keyTypeTemplate.endsWith("PublicKey")) {
            // Use the given cert, or else get it from a Crypto instance
            if (useKeyCertificate != null) {
                cert = useKeyCertificate;
            } else {
                crypto = createCrypto(false);
                cert = getCert(crypto);
            }
            writeElementsForRSTPublicKey(writer, cert);
        } else if (isSpnego || isSecureConv) {
            addKeySize(keySize, writer);
        }
        
        if (binaryExchange != null) {
            addBinaryExchange(binaryExchange, writer);
        }

        Element actAsSecurityToken = getActAsToken();
        if (actAsSecurityToken != null) {
            writer.writeStartElement(STSUtils.WST_NS_08_02, "ActAs");
            StaxUtils.copy(actAsSecurityToken, writer);
            writer.writeEndElement();
        }
        
        // Write out renewal semantics
        writeRenewalSemantics(writer);
        
        writer.writeEndElement();

        Object obj[] = client.invoke(boi, new DOMSource(writer.getDocument().getDocumentElement()));

        return new STSResponse((DOMSource)obj[0], requestorEntropy, cert, crypto);
    }
    
    /**
     * Get the "OnBehalfOf" element to be sent to the STS.
     */
    public Element getOnBehalfOfToken() throws Exception {
        return getDelegationSecurityToken(this.onBehalfOf);
    }
    
    /**
     * Get the "ActAs" element to be sent to the STS.
     */
    public Element getActAsToken() throws Exception {
        return getDelegationSecurityToken(this.actAs);
    }
    
    protected Element getDelegationSecurityToken(Object delegationObject) throws Exception {
        if (delegationObject != null) {
            final boolean isString = delegationObject instanceof String;
            final boolean isElement = delegationObject instanceof Element; 
            final boolean isCallbackHandler = delegationObject instanceof CallbackHandler;
            if (isString || isElement || isCallbackHandler) {
                if (isString) {
                    final Document doc =
                        StaxUtils.read(new StringReader((String) delegationObject));
                    return doc.getDocumentElement();
                } else if (isElement) {
                    return (Element) delegationObject;
                } else {
                    DelegationCallback callback = new DelegationCallback(message);
                    ((CallbackHandler)delegationObject).handle(new Callback[]{callback});
                    return callback.getToken();
                }
            }
        }
        return null;
    }
    
    protected byte[] writeElementsForRSTSymmetricKey(W3CDOMStreamWriter writer,
            boolean wroteKeySize) throws Exception {
        byte[] requestorEntropy = null;

        if (!wroteKeySize) {
            addKeySize(keySize, writer);
        }

        if (requiresEntropy) {
            writer.writeStartElement("wst", "Entropy", namespace);
            writer.writeStartElement("wst", "BinarySecret", namespace);
            writer.writeAttribute("Type", namespace + "/Nonce");
            if (algorithmSuite == null) {
                requestorEntropy = WSSecurityUtil.generateNonce(keySize / 8);
            } else {
                AlgorithmSuiteType algType = algorithmSuite.getAlgorithmSuiteType();
                requestorEntropy = WSSecurityUtil
                    .generateNonce(algType.getMaximumSymmetricKeyLength() / 8);
            }
            writer.writeCharacters(Base64.getMimeEncoder().encodeToString(requestorEntropy));

            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeStartElement("wst", "ComputedKeyAlgorithm", namespace);
            writer.writeCharacters(namespace + "/CK/PSHA1");
            writer.writeEndElement();
        }
        return requestorEntropy;
    }


    protected void writeElementsForRSTPublicKey(W3CDOMStreamWriter writer,
            X509Certificate cert) throws Exception {
        writer.writeStartElement("wst", "UseKey", namespace);
        writer.writeStartElement("ds", "KeyInfo", "http://www.w3.org/2000/09/xmldsig#");
        writer.writeNamespace("ds", "http://www.w3.org/2000/09/xmldsig#");

        boolean useCert = useCertificateForConfirmationKeyInfo;
        String useCertStr = (String)getProperty(SecurityConstants.STS_TOKEN_USE_CERT_FOR_KEYINFO);
        if (useCertStr != null) {
            useCert = Boolean.parseBoolean(useCertStr);
        }
        if (useCert) {
            X509Data certElem = new X509Data(writer.getDocument());
            certElem.addCertificate(cert);
            writer.getCurrentNode().appendChild(certElem.getElement());
        } else {
            writer.writeStartElement("ds", "KeyValue", "http://www.w3.org/2000/09/xmldsig#");
            PublicKey key = cert.getPublicKey();
            String pubKeyAlgo = key.getAlgorithm();
            if ("DSA".equalsIgnoreCase(pubKeyAlgo)) {
                DSAKeyValue dsaKeyValue = new DSAKeyValue(writer.getDocument(), key);
                writer.getCurrentNode().appendChild(dsaKeyValue.getElement());
            } else if ("RSA".equalsIgnoreCase(pubKeyAlgo)) {
                RSAKeyValue rsaKeyValue = new RSAKeyValue(writer.getDocument(), key);
                writer.getCurrentNode().appendChild(rsaKeyValue.getElement());
            }
            writer.writeEndElement();
        }

        writer.writeEndElement();
        writer.writeEndElement();
    }
    
    protected void addBinaryExchange(
        String binaryExchange, 
        W3CDOMStreamWriter writer
    ) throws XMLStreamException {
        writer.writeStartElement("wst", "BinaryExchange", namespace);
        writer.writeAttribute("EncodingType", WSConstants.BASE64_ENCODING);
        writer.writeAttribute("ValueType", namespace + "/spnego");
        writer.writeCharacters(binaryExchange);
        writer.writeEndElement();
    }
    
    protected void addKeySize(int keysize, W3CDOMStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("wst", "KeySize", namespace);
        writer.writeCharacters(Integer.toString(keysize));
        writer.writeEndElement();
    }

    protected void addRequestType(String requestType, W3CDOMStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("wst", "RequestType", namespace);
        writer.writeCharacters(namespace + requestType);
        writer.writeEndElement();
    }
    
    protected Element getDocumentElement(DOMSource ds) {
        Node nd = ds.getNode();
        if (nd instanceof Document) {
            nd = ((Document)nd).getDocumentElement();
        }
        return (Element)nd;
    }

    /**
     * Make an "Renew" invocation and return the response as a STSResponse Object
     */
    public STSResponse renew(SecurityToken tok) throws Exception {
        createClient();
        BindingOperationInfo boi = findOperation("/RST/Renew");

        client.getRequestContext().putAll(ctx);
        client.getRequestContext().remove(SecurityConstants.TOKEN_ID);
        if (isSecureConv) {
            client.getRequestContext().put(SoapBindingConstants.SOAP_ACTION, namespace + "/RST/SCT/Renew");
        } else {
            client.getRequestContext().put(SoapBindingConstants.SOAP_ACTION, namespace + "/RST/Renew");
        }

        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityToken", namespace);
        writer.writeNamespace("wst", namespace);
        if (context != null) {
            writer.writeAttribute(null, "Context", context);
        }
        
        String sptt = null;
        if (template != null && DOMUtils.getFirstElement(template) != null) {
            if (this.useSecondaryParameters()) {
                writer.writeStartElement("wst", "SecondaryParameters", namespace);
            }
            
            Element tl = DOMUtils.getFirstElement(template);
            while (tl != null) {
                StaxUtils.copy(tl, writer);
                if ("TokenType".equals(tl.getLocalName())) {
                    sptt = DOMUtils.getContent(tl);
                }
                tl = DOMUtils.getNextElement(tl);
            }
            
            if (this.useSecondaryParameters()) {
                writer.writeEndElement();
            }
        }
        
        if (isSpnego) {
            tokenType = STSUtils.getTokenTypeSCT(namespace);
        }

        addRequestType("/Renew", writer);
        if (enableAppliesTo) {
            addAppliesTo(writer, tok.getIssuerAddress());
        }
        
        if (sptt == null) {
            addTokenType(writer);
        }
        if (isSecureConv || enableLifetime) {
            addLifetime(writer);
        }

        writer.writeStartElement("wst", "RenewTarget", namespace);
        StaxUtils.copy(tok.getToken(), writer);
        writer.writeEndElement();
        
        // Write out renewal semantics
        writeRenewalSemantics(writer);
        
        writer.writeEndElement();

        Object obj[] = client.invoke(boi, new DOMSource(writer.getDocument().getDocumentElement()));

        return new STSResponse((DOMSource)obj[0], null);
    }

    protected PrimitiveAssertion getAddressingAssertion() {
        String ns = "http://schemas.xmlsoap.org/ws/2004/08/addressing/policy";
        String local = "UsingAddressing";
        if ("http://www.w3.org/2005/08/addressing".equals(addressingNamespace)) {
            ns = "http://www.w3.org/2007/02/addressing/metadata";
            local = "Addressing";
        }
        return new PrimitiveAssertion(new QName(ns, local), true);
    }
    
    /**
     * Make an "Validate" invocation and return the response as a STSResponse Object
     */
    protected STSResponse validate(SecurityToken tok, String tokentype) 
        throws Exception {
        createClient();
        
        if (tokentype == null) {
            tokentype = tokenType;
        }
        if (tokentype == null) {
            tokentype = namespace + "/RSTR/Status";
        }

        Policy validatePolicy = new Policy();
        ExactlyOne one = new ExactlyOne();
        validatePolicy.addPolicyComponent(one);
        All all = new All();
        one.addPolicyComponent(all);
        all.addAssertion(getAddressingAssertion());

        client.getRequestContext().clear();
        client.getRequestContext().putAll(ctx);
        client.getRequestContext().put(SecurityConstants.TOKEN, tok);
        BindingOperationInfo boi = findOperation("/RST/Validate");
        if (boi == null) {
            boi = findOperation("/RST/Issue");
            client.getRequestContext().put(PolicyConstants.POLICY_OVERRIDE, validatePolicy);
        }
        
        client.getRequestContext().put(SoapBindingConstants.SOAP_ACTION, 
                                       namespace + "/RST/Validate");

        
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityToken", namespace);
        writer.writeNamespace("wst", namespace);
        writer.writeStartElement("wst", "RequestType", namespace);
        writer.writeCharacters(namespace + "/Validate");
        writer.writeEndElement();

        writer.writeStartElement("wst", "TokenType", namespace);
        writer.writeCharacters(tokentype);
        writer.writeEndElement();
        
        if (tokentype.endsWith("/RSTR/Status")) {
            addClaims(writer);

            writer.writeStartElement("wst", "ValidateTarget", namespace);

            Element el = tok.getToken();
            if (el != null) {
                StaxUtils.copy(el, writer);
            }

            writer.writeEndElement();
            writer.writeEndElement();

            Object o[] = client.invoke(boi, new DOMSource(writer.getDocument().getDocumentElement()));
            
            return new STSResponse((DOMSource)o[0], null);
        } else {
            if (enableLifetime) {
                addLifetime(writer);
            }
            
            // Default to Bearer KeyType
            String keyTypeTemplate = keyType;
            if (keyTypeTemplate == null) {
                keyTypeTemplate = namespace + "/Bearer";
            }
            keyTypeTemplate = writeKeyType(writer, keyTypeTemplate);

            byte[] requestorEntropy = null;
            X509Certificate cert = null;
            Crypto crypto = null;

            if (keySize <= 0) {
                keySize = 256;
            }
            if (keyTypeTemplate != null && keyTypeTemplate.endsWith("SymmetricKey")) {
                requestorEntropy = writeElementsForRSTSymmetricKey(writer, false);
            } else if (keyTypeTemplate != null && keyTypeTemplate.endsWith("PublicKey")) {
                // Use the given cert, or else get it from a Crypto instance
                if (useKeyCertificate != null) {
                    cert = useKeyCertificate;
                } else {
                    crypto = createCrypto(false);
                    cert = getCert(crypto);
                }
                writeElementsForRSTPublicKey(writer, cert);
            }

            writeRenewalSemantics(writer);
            
            addClaims(writer);

            writer.writeStartElement("wst", "ValidateTarget", namespace);

            Element el = tok.getToken();
            StaxUtils.copy(el, writer);

            writer.writeEndElement();
            writer.writeEndElement();

            Object o[] = client.invoke(boi, new DOMSource(writer.getDocument().getDocumentElement()));
            
            return new STSResponse((DOMSource)o[0], requestorEntropy, cert, crypto);
        }
    }
    
    private void writeRenewalSemantics(XMLStreamWriter writer) throws XMLStreamException {
        // Write out renewal semantics
        if (sendRenewing) {
            writer.writeStartElement("wst", "Renewing", namespace);
            if (!allowRenewing) {
                writer.writeAttribute(null, "Allow", "false");
            }
            if (allowRenewing && allowRenewingAfterExpiry) {
                writer.writeAttribute(null, "OK", "true");
            }
            writer.writeEndElement();
        }    
    }

    /**
     * Make an "Cancel" invocation and return the response as a STSResponse Object
     */
    protected STSResponse cancel(SecurityToken token) throws Exception {
        createClient();

        client.getRequestContext().clear();
        client.getRequestContext().putAll(ctx);
        client.getRequestContext().put(SecurityConstants.TOKEN, token);
        
        BindingOperationInfo boi = findOperation("/RST/Cancel");
        boolean attachTokenDirectly = true;
        if (boi == null) {
            attachTokenDirectly = false;
            boi = findOperation("/RST/Issue");
            
            Policy cancelPolicy = new Policy();
            ExactlyOne one = new ExactlyOne();
            cancelPolicy.addPolicyComponent(one);
            All all = new All();
            one.addPolicyComponent(all);
            all.addAssertion(getAddressingAssertion());

            final SecureConversationToken secureConversationToken = 
                new SecureConversationToken(
                    SPConstants.SPVersion.SP12,
                    SPConstants.IncludeTokenType.INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT,
                    null,
                    null,
                    null,
                    null
                );
            secureConversationToken.setOptional(true);
            
            class InternalProtectionToken extends ProtectionToken {
                InternalProtectionToken(SPVersion version, Policy nestedPolicy) {
                    super(version, nestedPolicy);
                    super.setToken(secureConversationToken);
                }
            }
            
            DefaultSymmetricBinding binding = 
                new DefaultSymmetricBinding(SPConstants.SPVersion.SP12, new Policy());
            all.addAssertion(binding);
            all.addAssertion(getAddressingAssertion());
            binding.setProtectionToken(
                new InternalProtectionToken(SPConstants.SPVersion.SP12, new Policy())
            );
            binding.setIncludeTimestamp(true);
            binding.setOnlySignEntireHeadersAndBody(true);
            binding.setProtectTokens(false);
            
            String addrNamespace = addressingNamespace;
            if (addrNamespace == null) {
                addrNamespace = "http://www.w3.org/2005/08/addressing";
            }
            
            List<Header> headers = new ArrayList<Header>();
            headers.add(new Header("To", addrNamespace));
            headers.add(new Header("From", addrNamespace));
            headers.add(new Header("FaultTo", addrNamespace));
            headers.add(new Header("ReplyTo", addrNamespace));
            headers.add(new Header("Action", addrNamespace));
            headers.add(new Header("MessageID", addrNamespace));
            headers.add(new Header("RelatesTo", addrNamespace));
            
            SignedParts parts = new SignedParts(SPConstants.SPVersion.SP12, true, null, headers, false);
            parts.setOptional(true);
            all.addPolicyComponent(parts);
            
            client.getRequestContext().put(PolicyConstants.POLICY_OVERRIDE, cancelPolicy);
        }
        
        if (isSecureConv) {
            client.getRequestContext().put(SoapBindingConstants.SOAP_ACTION,
                                           namespace + "/RST/SCT/Cancel");
        } else {
            client.getRequestContext().put(SoapBindingConstants.SOAP_ACTION, 
                                           namespace + "/RST/Cancel");            
        }

        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityToken", namespace);
        writer.writeNamespace("wst", namespace);
        writer.writeStartElement("wst", "RequestType", namespace);
        writer.writeCharacters(namespace + "/Cancel");
        writer.writeEndElement();

        writer.writeStartElement("wst", "CancelTarget", namespace);
        Element el = null;
        if (attachTokenDirectly) {
            el = token.getToken();
        } else {
            el = token.getUnattachedReference();
            if (el == null) {
                el = token.getAttachedReference();
            }
        }
        StaxUtils.copy(el, writer);

        writer.writeEndElement();
        writer.writeEndElement();

        Object[] obj = client.invoke(boi, new DOMSource(writer.getDocument().getDocumentElement()));
        return new STSResponse((DOMSource)obj[0], null);
    }
    
    protected boolean useSecondaryParameters() {
        return !STSUtils.WST_NS_05_02.equals(namespace);
    }

    protected String writeKeyType(W3CDOMStreamWriter writer, String keyTypeToWrite) 
        throws XMLStreamException {
        if (isSecureConv) {
            if (keyTypeToWrite == null) {
                writer.writeStartElement("wst", "TokenType", namespace);
                writer.writeCharacters(STSUtils.getTokenTypeSCT(namespace));
                writer.writeEndElement();
                keyTypeToWrite = namespace + "/SymmetricKey";
            }
        } else if (keyTypeToWrite == null && sendKeyType) {
            writer.writeStartElement("wst", "KeyType", namespace);
            writer.writeCharacters(namespace + "/SymmetricKey");
            writer.writeEndElement();
            keyTypeToWrite = namespace + "/SymmetricKey";
        } else if (keyTypeToWrite != null) {
            writer.writeStartElement("wst", "KeyType", namespace);
            writer.writeCharacters(keyTypeToWrite);
            writer.writeEndElement();
        }
        return keyTypeToWrite;
    }

    protected X509Certificate getCert(Crypto crypto) throws Exception {
        if (crypto == null) {
            throw new Fault("No Crypto token properties are available to retrieve a certificate", 
                            LOG);
        }
        
        String alias = (String)getProperty(SecurityConstants.STS_TOKEN_USERNAME);
        if (alias == null) {
            alias = crypto.getDefaultX509Identifier();
        }
        if (alias == null) {
            throw new Fault("No alias specified for retrieving PublicKey", LOG);
        }
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(alias);
        
        X509Certificate certs[] = crypto.getX509Certificates(cryptoType);
        if (certs == null || certs.length == 0) {
            throw new Fault("Could not get X509Certificate for alias " + alias, LOG);
        }
        return certs[0];
    }

    protected void addLifetime(XMLStreamWriter writer) throws XMLStreamException {
        Date creationTime = new Date();
        Date expirationTime = new Date();
        expirationTime.setTime(creationTime.getTime() + ((long)ttl * 1000L));

        XmlSchemaDateFormat fmt = new XmlSchemaDateFormat();
        writer.writeStartElement("wst", "Lifetime", namespace);
        writer.writeNamespace("wsu", WSConstants.WSU_NS);
        writer.writeStartElement("wsu", "Created", WSConstants.WSU_NS);
        writer.writeCharacters(fmt.format(creationTime));
        writer.writeEndElement();

        writer.writeStartElement("wsu", "Expires", WSConstants.WSU_NS);
        writer.writeCharacters(fmt.format(expirationTime));
        writer.writeEndElement();
        writer.writeEndElement();
    }

    protected void addAppliesTo(XMLStreamWriter writer, String appliesTo) throws XMLStreamException {
        if (appliesTo != null && addressingNamespace != null) {
            String policyNS = wspNamespace;
            if (policyNS == null) {
                policyNS = "http://schemas.xmlsoap.org/ws/2004/09/policy";
            }
            writer.writeStartElement("wsp", "AppliesTo", policyNS);
            writer.writeNamespace("wsp", policyNS);
            writer.writeStartElement("wsa", "EndpointReference", addressingNamespace);
            writer.writeNamespace("wsa", addressingNamespace);
            writer.writeStartElement("wsa", "Address", addressingNamespace);
            writer.writeCharacters(appliesTo);
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }

    protected void addTokenType(XMLStreamWriter writer) throws XMLStreamException {
        if (tokenType != null) {
            writer.writeStartElement("wst", "TokenType", namespace);
            writer.writeCharacters(tokenType);
            writer.writeEndElement();
        }
    }
    
    protected void addClaims(XMLStreamWriter writer) throws Exception {
        Object claimsToSerialize = claims;
        if (claimsToSerialize == null && claimsCallbackHandler != null) {
            ClaimsCallback callback = new ClaimsCallback(message);
            claimsCallbackHandler.handle(new Callback[]{callback});
            claimsToSerialize = callback.getClaims();
        }
        
        if (claimsToSerialize instanceof Element) {
            StaxUtils.copy((Element)claimsToSerialize, writer);
        } else if (claimsToSerialize instanceof ClaimCollection) {
            ClaimCollection claimCollection = (ClaimCollection)claims;
            claimCollection.serialize(writer, "wst", namespace);
        }
    }

    protected SecurityToken createSecurityToken(Element el, byte[] requestorEntropy)
        throws WSSecurityException, Base64DecodingException {

        if ("RequestSecurityTokenResponseCollection".equals(el.getLocalName())) {
            el = DOMUtils.getFirstElement(el);
        }
        if (!"RequestSecurityTokenResponse".equals(el.getLocalName())) {
            throw new Fault("Unexpected element " + el.getLocalName(), LOG);
        }
        el = DOMUtils.getFirstElement(el);
        Element rst = null;
        Element rar = null;
        Element rur = null;
        Element rpt = null;
        Element lte = null;
        Element entropy = null;
        String tt = null;
        String retKeySize = null;
        String tokenData = null;
        
        while (el != null) {
            String ln = el.getLocalName();
            if (namespace.equals(el.getNamespaceURI())) {
                if ("Lifetime".equals(ln)) {
                    lte = el;
                } else if ("RequestedSecurityToken".equals(ln)) {
                    rst = DOMUtils.getFirstElement(el);
                    if (rst == null) {
                        tokenData = el.getTextContent();
                    }
                } else if ("RequestedAttachedReference".equals(ln)) {
                    rar = DOMUtils.getFirstElement(el);
                } else if ("RequestedUnattachedReference".equals(ln)) {
                    rur = DOMUtils.getFirstElement(el);
                } else if ("RequestedProofToken".equals(ln)) {
                    rpt = el;
                } else if ("Entropy".equals(ln)) {
                    entropy = el;
                } else if ("TokenType".equals(ln)) {
                    tt = DOMUtils.getContent(el);
                } else if ("KeySize".equals(ln)) {
                    retKeySize = DOMUtils.getContent(el);
                }
            }
            el = DOMUtils.getNextElement(el);
        }
        Element rstDec = rst;
        String id = findID(rar, rur, rstDec);
        if (StringUtils.isEmpty(id)) {
            throw new TrustException("NO_ID", LOG);
        }
        SecurityToken token = new SecurityToken(id, rstDec, lte);
        token.setAttachedReference(rar);
        token.setUnattachedReference(rur);
        token.setIssuerAddress(location);
        token.setTokenType(tt);
        if (tokenData != null) {
            token.setData(tokenData.getBytes());
        }

        byte[] secret = null;

        if (rpt != null) {
            Element child = DOMUtils.getFirstElement(rpt);
            QName childQname = DOMUtils.getElementQName(child);
            if (childQname.equals(new QName(namespace, "BinarySecret"))) {
                // First check for the binary secret
                String b64Secret = DOMUtils.getContent(child);
                secret = Base64.getMimeDecoder().decode(b64Secret);
            } else if (childQname.equals(new QName(WSConstants.ENC_NS, WSConstants.ENC_KEY_LN))) {
                secret = decryptKey(child);
            } else if (childQname.equals(new QName(namespace, "ComputedKey"))) {
                // Handle the computed key
                Element computedKeyChild = entropy == null ? null : DOMUtils.getFirstElement(entropy);
                byte[] serviceEntr = null;

                if (computedKeyChild != null) {
                    QName computedKeyChildQName = DOMUtils.getElementQName(computedKeyChild);
                    if (computedKeyChildQName.equals(new QName(WSConstants.ENC_NS, WSConstants.ENC_KEY_LN))) {
                        serviceEntr = decryptKey(computedKeyChild);
                    } else if (computedKeyChildQName.equals(new QName(namespace, "BinarySecret"))) {
                        String content = DOMUtils.getContent(computedKeyChild);
                        serviceEntr = Base64.getMimeDecoder().decode(content);
                    }
                }
                
                if (serviceEntr != null) {
                    // Right now we only use PSHA1 as the computed key algo
                    P_SHA1 psha1 = new P_SHA1();

                    int length = 0;
                    if (retKeySize != null) {
                        try {
                            length = Integer.parseInt(retKeySize);
                        } catch (NumberFormatException ex) {
                            // do nothing
                        }
                    } else {
                        length = keySize;
                    }
                    if (length <= 0) {
                        length = 256;
                    }
                    try {
                        secret = psha1.createKey(requestorEntropy, serviceEntr, 0, length / 8);
                    } catch (WSSecurityException e) {
                        throw new TrustException("DERIVED_KEY_ERROR", e, LOG);
                    }
                } else {
                    // Service entropy missing
                    throw new TrustException("NO_ENTROPY", LOG);
                }
            }
        } else if (requestorEntropy != null) {
            // Use requester entropy as the key
            secret = requestorEntropy;
        }
        token.setSecret(secret);

        return token;
    }
    
    protected byte[] decryptKey(Element child) throws TrustException, WSSecurityException, Base64DecodingException {
        String encryptionAlgorithm = X509Util.getEncAlgo(child);
        // For the SPNEGO case just return the decoded cipher value and decrypt it later
        if (encryptionAlgorithm != null && encryptionAlgorithm.endsWith("spnego#GSS_Wrap")) {
            // Get the CipherValue
            Element tmpE = 
                XMLUtils.getDirectChildElement(child, "CipherData", WSConstants.ENC_NS);
            byte[] cipherValue = null;
            if (tmpE != null) {
                tmpE = 
                    XMLUtils.getDirectChildElement(tmpE, "CipherValue", WSConstants.ENC_NS);
                if (tmpE != null) {
                    String content = DOMUtils.getContent(tmpE);
                    cipherValue = Base64.getMimeDecoder().decode(content);
                }
            }
            if (cipherValue == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "noCipher");
            }
            return cipherValue;
        } else {
            try {
                EncryptedKeyProcessor proc = new EncryptedKeyProcessor();
                WSDocInfo docInfo = new WSDocInfo(child.getOwnerDocument());
                RequestData data = new RequestData();
                data.setWssConfig(WSSConfig.getNewInstance());
                data.setDecCrypto(createCrypto(true));
                data.setCallbackHandler(createHandler());
                List<WSSecurityEngineResult> result =
                    proc.handleToken(child, data, docInfo);
                return 
                    (byte[])result.get(0).get(
                        WSSecurityEngineResult.TAG_SECRET
                    );
            } catch (IOException e) {
                throw new TrustException("ENCRYPTED_KEY_ERROR", e, LOG);
            }
        }
    }

    protected CallbackHandler createHandler() {
        Object o = getProperty(SecurityConstants.CALLBACK_HANDLER);
        try {
            return SecurityUtils.getCallbackHandler(o);
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    protected Object getProperty(String s) {
        String key = s;
        
        Object o = ctx.get(key);
        if (o == null) {
            o = client.getEndpoint().getEndpointInfo().getProperty(key);
        }
        if (o == null) {
            o = client.getEndpoint().getEndpointInfo().getBinding().getProperty(key);
        }
        if (o == null) {
            o = client.getEndpoint().getService().get(key);
        }
        
        key = "ws-" + s;
        if (o == null) {
            o = ctx.get(key);
        }
        if (o == null) {
            o = client.getEndpoint().getEndpointInfo().getProperty(key);
        }
        if (o == null) {
            o = client.getEndpoint().getEndpointInfo().getBinding().getProperty(key);
        }
        if (o == null) {
            o = client.getEndpoint().getService().get(key);
        }
        
        return o;
    }
    
    protected Crypto createCrypto(boolean decrypt) throws IOException, WSSecurityException {
        Crypto crypto = (Crypto)getProperty(SecurityConstants.STS_TOKEN_CRYPTO + (decrypt ? ".decrypt" : ""));
        if (crypto != null) {
            return crypto;
        }

        Object o = getProperty(SecurityConstants.STS_TOKEN_PROPERTIES + (decrypt ? ".decrypt" : ""));
        
        URL propsURL = SecurityUtils.loadResource(message, o);
        Properties properties = WSS4JUtils.getProps(o, propsURL);
        
        if (properties != null) {
            PasswordEncryptor passwordEncryptor = WSS4JUtils.getPasswordEncryptor(message);
            return CryptoFactory.getInstance(properties, this.getClass().getClassLoader(), passwordEncryptor);
        }
        if (decrypt) {
            return createCrypto(false);
        }
        return null;
    }

    protected String findID(Element rar, Element rur, Element rst) {
        String id = null;
        if (rst != null) {
            QName elName = DOMUtils.getElementQName(rst);
            if (elName.equals(new QName(WSConstants.SAML_NS, "Assertion"))
                && rst.hasAttributeNS(null, "AssertionID")) {
                id = rst.getAttributeNS(null, "AssertionID");
            } else if (elName.equals(new QName(WSConstants.SAML2_NS, "Assertion"))
                && rst.hasAttributeNS(null, "ID")) {
                id = rst.getAttributeNS(null, "ID");
            }
            if (id == null || "".equals(id)) {
                id = this.getIDFromSTR(rst);
            }
        }
        if ((id == null || "".equals(id)) && rar != null) {
            id = this.getIDFromSTR(rar);
        }
        if ((id == null || "".equals(id)) && rur != null) {
            id = this.getIDFromSTR(rur);
        }
        if ((id == null || "".equals(id)) && rst != null) {
            id = rst.getAttributeNS(WSConstants.WSU_NS, "Id");
            if (id == null || "".equals(id)) {
                QName elName = DOMUtils.getElementQName(rst);
                if (elName.equals(new QName(WSConstants.SAML2_NS, "EncryptedAssertion"))) {
                    Element child = DOMUtils.getFirstElement(rst);
                    if (child != null) {
                        id = child.getAttributeNS(WSConstants.WSU_NS, "Id");
                    }
                }
            }
        }
        return id;
    }

    protected String getIDFromSTR(Element el) {
        Element child = DOMUtils.getFirstElement(el);
        if (child == null) {
            return null;
        }
        QName elName = DOMUtils.getElementQName(child);
        if (elName.equals(new QName(WSConstants.SIG_NS, "KeyInfo"))
            || elName.equals(new QName(WSConstants.WSSE_NS, "KeyIdentifier"))) {
            return DOMUtils.getContent(child);
        } else if (elName.equals(Reference.TOKEN)) {
            return child.getAttributeNS(null, "URI");
        } else if (elName.equals(new QName(STSUtils.SCT_NS_05_02, "Identifier"))
                   || elName.equals(new QName(STSUtils.SCT_NS_05_12, "Identifier"))) {
            return DOMUtils.getContent(child);
        }
        return null;
    }

    public void setTemplate(Element rstTemplate) {
        template = rstTemplate;
    }

    /**
     * Set a Claims Object to be included in the request. This Object can be either a DOM Element, 
     * which will be copied "as is" into the request, or else a 
     * org.apache.cxf.rt.security.claims.ClaimCollection Object.
     */
    public void setClaims(Object rstClaims) {
        claims = rstClaims;
    }
    
    public List<Interceptor<? extends Message>> getOutFaultInterceptors() {
        if (client != null) {
            return client.getOutFaultInterceptors();
        }
        return outFault;
    }

    public List<Interceptor<? extends Message>> getInFaultInterceptors() {
        if (client != null) {
            return client.getInFaultInterceptors();
        }
        return inFault;
    }

    public List<Interceptor<? extends Message>> getInInterceptors() {
        if (client != null) {
            return client.getInInterceptors();
        }
        return in;
    }

    public List<Interceptor<? extends Message>> getOutInterceptors() {
        if (client != null) {
            return client.getOutInterceptors();
        }
        return out;
    }

    public void setInInterceptors(List<Interceptor<? extends Message>> interceptors) {
        getInInterceptors().addAll(interceptors);
    }

    public void setInFaultInterceptors(List<Interceptor<? extends Message>> interceptors) {
        getInFaultInterceptors().addAll(interceptors);
    }

    public void setOutInterceptors(List<Interceptor<? extends Message>> interceptors) {
        getOutInterceptors().addAll(interceptors);
    }

    public void setOutFaultInterceptors(List<Interceptor<? extends Message>> interceptors) {
        getOutFaultInterceptors().addAll(interceptors);
    }
        
    public void setFeatures(List<? extends Feature> f) {
        features = CastUtils.cast(f);
    }
    public List<Feature> getFeatures() {
        return features;
    }

    public CallbackHandler getClaimsCallbackHandler() {
        return claimsCallbackHandler;
    }

    public void setClaimsCallbackHandler(CallbackHandler claimsCallbackHandler) {
        this.claimsCallbackHandler = claimsCallbackHandler;
    }
    
    protected static class STSResponse {
        private final DOMSource response;
        private final byte[] entropy;
        private final X509Certificate cert;
        private final Crypto crypto;
        
        public STSResponse(DOMSource response, byte[] entropy) {
            this(response, entropy, null, null);
        }
        
        public STSResponse(DOMSource response, byte[] entropy, X509Certificate cert, Crypto crypto) {
            this.response = response;
            this.entropy = entropy;
            this.cert = cert;
            this.crypto = crypto;
        }
        
        public DOMSource getResponse() {
            return response;
        }
        
        public byte[] getEntropy() {
            return entropy;
        }
        
        public X509Certificate getCert() {
            return cert;
        }
        
        public Crypto getCrypto() {
            return crypto;
        }
    }

    public String getWspNamespace() {
        return wspNamespace;
    }

    public void setWspNamespace(String wspNamespace) {
        this.wspNamespace = wspNamespace;
    }

    public X509Certificate getUseKeyCertificate() {
        return useKeyCertificate;
    }

    public void setUseKeyCertificate(X509Certificate useKeyCertificate) {
        this.useKeyCertificate = useKeyCertificate;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
