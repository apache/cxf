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

package org.apache.cxf.systest.sts.batch;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;
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
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ModCountCopyOnWriteArrayList;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.databinding.source.SourceDataBinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.ws.addressing.VersionTransformer;
import org.apache.cxf.ws.policy.EffectivePolicy;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.cxf.ws.security.trust.TrustException;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.derivedKey.P_SHA1;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.token.Reference;
import org.apache.wss4j.common.util.DateUtil;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.processor.EncryptedKeyProcessor;
import org.apache.wss4j.dom.util.X509Util;
import org.apache.wss4j.policy.model.AbstractBinding;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.AlgorithmSuite.AlgorithmSuiteType;
import org.apache.wss4j.policy.model.Trust10;
import org.apache.wss4j.policy.model.Trust13;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.content.X509Data;
import org.apache.xml.security.keys.content.keyvalues.DSAKeyValue;
import org.apache.xml.security.keys.content.keyvalues.RSAKeyValue;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;

/**
 * A primitive STSClient for batch tokens. Note that this contains a number of hacks and should NOT be
 * used for production use.
 */
public class SimpleBatchSTSClient implements Configurable, InterceptorProvider {
    private static final Logger LOG = LogUtils.getL7dLogger(SimpleBatchSTSClient.class);

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
    protected Element claims;
    protected AlgorithmSuite algorithmSuite;
    protected String namespace = STSUtils.WST_NS_05_12;
    protected String addressingNamespace;
    protected Object onBehalfOf;
    protected boolean enableAppliesTo = true;

    protected boolean useCertificateForConfirmationKeyInfo;
    protected boolean isSecureConv;
    protected boolean isSpnego;
    protected boolean enableLifetime;
    protected int ttl = 300;
    protected boolean allowRenewing = true;
    protected boolean allowRenewingAfterExpiry;

    protected Object actAs;
    protected String tokenType;
    protected String keyType;
    protected boolean sendKeyType = true;
    protected Message message;
    protected String context;

    protected Map<String, Object> ctx = new HashMap<>();

    protected List<Interceptor<? extends Message>> in
        = new ModCountCopyOnWriteArrayList<>();
    protected List<Interceptor<? extends Message>> out
        = new ModCountCopyOnWriteArrayList<>();
    protected List<Interceptor<? extends Message>> outFault
        = new ModCountCopyOnWriteArrayList<>();
    protected List<Interceptor<? extends Message>> inFault
        = new ModCountCopyOnWriteArrayList<>();
    protected List<AbstractFeature> features;

    public SimpleBatchSTSClient(Bus b) {
        bus = b;
    }

    public String getBeanName() {
        return name;
    }

    public void setBeanName(String s) {
        name = s;
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
            namespace = STSUtils.WST_NS_05_02;
            requiresEntropy = trust.isRequireClientEntropy();
        }
    }

    public void setTrust(Trust13 trust) {
        if (trust != null) {
            namespace = STSUtils.WST_NS_05_12;
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

    public Client getClient()  throws BusException, EndpointException {
        if (client == null) {
            createClient();
        }
        return client;
    }

    protected String findMEXLocation(EndpointReferenceType ref) {
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
        return EndpointReferenceUtils.getAddress(ref);
    }
    protected String findMEXLocation(Element ref) {
        Element el = DOMUtils.getFirstElement(ref);
        while (el != null) {
            if ("Address".equals(el.getLocalName())
                && VersionTransformer.isSupported(el.getNamespaceURI())
                && "MetadataReference".equals(ref.getLocalName())) {
                return DOMUtils.getContent(el);
            }
            String ad = findMEXLocation(el);
            if (ad != null) {
                return ad;
            }
            el = DOMUtils.getNextElement(el);
        }
        return null;
    }
    protected void createClient() throws BusException, EndpointException {
        if (client != null) {
            return;
        }
        bus.getExtension(Configurer.class).configureBean(name, this);

        if (wsdlLocation != null) {
            WSDLServiceFactory factory = new WSDLServiceFactory(bus, wsdlLocation, serviceName);
            SourceDataBinding dataBinding = new SourceDataBinding();
            factory.setDataBinding(dataBinding);
            Service service = factory.create();
            service.setDataBinding(dataBinding);
            EndpointInfo ei = service.getEndpointInfo(endpointName);
            Endpoint endpoint = new EndpointImpl(bus, service, ei);
            client = new ClientImpl(bus, endpoint);
        } else {
            Endpoint endpoint = STSUtils.createSTSEndpoint(bus, namespace, null, location, soapVersion,
                                                           policy, endpointName);

            client = new ClientImpl(bus, endpoint);
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
            for (AbstractFeature f : features) {
                f.initialize(client, bus);
            }
        }
    }

    protected BindingOperationInfo findOperation(String suffix) {
        BindingInfo bi = client.getEndpoint().getBinding().getBindingInfo();
        for (BindingOperationInfo boi : bi.getOperations()) {
            SoapOperationInfo soi = boi.getExtensor(SoapOperationInfo.class);
            if (soi != null && soi.getAction() != null && soi.getAction().endsWith(suffix)) {
                PolicyEngine pe = bus.getExtension(PolicyEngine.class);
                Conduit conduit = client.getConduit();
                EffectivePolicy effectivePolicy = pe.getEffectiveClientRequestPolicy(client.getEndpoint()
                    .getEndpointInfo(), boi, conduit, PhaseInterceptorChain.getCurrentMessage());
                setPolicyInternal(effectivePolicy.getPolicy());
                return boi;
            }
        }
        //operation is not correct as the Action is not set correctly.   Let's see if
        //we can at least find it by name and then set the action and such manually later.
        for (BindingOperationInfo boi : bi.getOperations()) {
            if (boi.getInput().getMessageInfo().getMessageParts().size() > 0) {
                MessagePartInfo mpi = boi.getInput().getMessageInfo().getMessagePart(0);
                if ("RequestSecurityToken".equals(mpi.getConcreteName().getLocalPart())) {
                    return boi;
                }
            }
        }
        return null;
    }

    public List<SecurityToken> requestBatchSecurityTokens(
        List<BatchRequest> batchRequestList, String action, String requestType
    ) throws Exception {
        createClient();
        BindingOperationInfo boi = findOperation("/BatchIssue");

        client.getRequestContext().putAll(ctx);
        client.getRequestContext().put(SoapBindingConstants.SOAP_ACTION, action);

        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityTokenCollection", namespace);
        writer.writeNamespace("wst", namespace);

        for (BatchRequest batchRequest : batchRequestList) {
            writer.writeStartElement("wst", "RequestSecurityToken", namespace);
            writer.writeNamespace("wst", namespace);

            addRequestType(requestType, writer);
            if (enableAppliesTo) {
                addAppliesTo(writer, batchRequest.getAppliesTo());
            }

            writeKeyType(writer, batchRequest.getKeyType());

            addLifetime(writer);

            addTokenType(writer, batchRequest.getTokenType());

            writer.writeEndElement();
        }
        writer.writeEndElement();

        Object[] obj = client.invoke(boi, new DOMSource(writer.getDocument().getDocumentElement()));

        Element responseCollection = getDocumentElement((DOMSource)obj[0]);
        Node child = responseCollection.getFirstChild();
        List<SecurityToken> tokens = new ArrayList<>();
        while (child != null) {
            if (child instanceof Element
                && "RequestSecurityTokenResponse".equals(((Element)child).getLocalName())) {
                SecurityToken token =
                    createSecurityToken((Element)child, null);
                tokens.add(token);
            }
            child = child.getNextSibling();
        }

        return tokens;
    }

    protected List<SecurityToken> validateBatchSecurityTokens(
        List<BatchRequest> batchRequestList, String action, String requestType
    ) throws Exception {
        createClient();
        BindingOperationInfo boi = findOperation("/BatchValidate");

        client.getRequestContext().putAll(ctx);
        client.getRequestContext().put(SoapBindingConstants.SOAP_ACTION, action);

        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityTokenCollection", namespace);
        writer.writeNamespace("wst", namespace);

        for (BatchRequest batchRequest : batchRequestList) {
            writer.writeStartElement("wst", "RequestSecurityToken", namespace);
            writer.writeNamespace("wst", namespace);

            addRequestType(requestType, writer);

            addTokenType(writer, batchRequest.getTokenType());

            writer.writeStartElement("wst", "ValidateTarget", namespace);

            Element el = batchRequest.getValidateTarget();
            StaxUtils.copy(el, writer);

            writer.writeEndElement();

            writer.writeEndElement();
        }
        writer.writeEndElement();

        Object[] obj = client.invoke(boi, new DOMSource(writer.getDocument().getDocumentElement()));

        Element responseCollection = getDocumentElement((DOMSource)obj[0]);
        Node child = responseCollection.getFirstChild();
        List<SecurityToken> tokens = new ArrayList<>();
        while (child != null) {
            if (child instanceof Element
                && "RequestSecurityTokenResponse".equals(((Element)child).getLocalName())) {
                Element rstrChild = DOMUtils.getFirstElement(child);
                while (rstrChild != null) {
                    if ("Status".equals(rstrChild.getLocalName())) {
                        Element e2 =
                            DOMUtils.getFirstChildWithName(rstrChild, rstrChild.getNamespaceURI(), "Code");
                        String s = DOMUtils.getContent(e2);
                        if (!s.endsWith("/status/valid")) {
                            throw new TrustException(LOG, "VALIDATION_FAILED");
                        }

                    } else if ("RequestedSecurityToken".equals(rstrChild.getLocalName())) {
                        Element requestedSecurityTokenElement = DOMUtils.getFirstElement(rstrChild);
                        String id = findID(null, null, requestedSecurityTokenElement);
                        if (StringUtils.isEmpty(id)) {
                            throw new TrustException("NO_ID", LOG);
                        }
                        SecurityToken requestedSecurityToken = new SecurityToken(id);
                        requestedSecurityToken.setToken(requestedSecurityTokenElement);
                        tokens.add(requestedSecurityToken);
                    }
                    rstrChild = DOMUtils.getNextElement(rstrChild);
                }
            }
            child = child.getNextSibling();
        }

        return tokens;
    }

    protected byte[] writeElementsForRSTSymmetricKey(W3CDOMStreamWriter writer,
            boolean wroteKeySize) throws Exception {
        byte[] requestorEntropy = null;

        if (!wroteKeySize && (!isSecureConv || keySize != 256)) {
            addKeySize(keySize, writer);
        }

        if (requiresEntropy) {
            writer.writeStartElement("wst", "Entropy", namespace);
            writer.writeStartElement("wst", "BinarySecret", namespace);
            writer.writeAttribute("Type", namespace + "/Nonce");

            try {
                if (algorithmSuite == null) {
                    requestorEntropy = XMLSecurityConstants.generateBytes(keySize / 8);
                } else {
                    AlgorithmSuiteType algType = algorithmSuite.getAlgorithmSuiteType();
                    requestorEntropy = XMLSecurityConstants.generateBytes(algType.getMaximumSymmetricKeyLength() / 8);
                }
            } catch (XMLSecurityException e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
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
        writer.writeStartElement("dsig", "KeyInfo", "http://www.w3.org/2000/09/xmldsig#");
        writer.writeNamespace("dsig", "http://www.w3.org/2000/09/xmldsig#");

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
            writer.writeStartElement("dsig", "KeyValue", "http://www.w3.org/2000/09/xmldsig#");
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
        writer.writeAttribute("EncodingType", WSS4JConstants.BASE64_ENCODING);
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
        writer.writeCharacters(requestType);
        writer.writeEndElement();
    }

    protected Element getDocumentElement(DOMSource ds) {
        Node nd = ds.getNode();
        if (nd instanceof Document) {
            nd = ((Document)nd).getDocumentElement();
        }
        return (Element)nd;
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
        String alias = (String)getProperty(SecurityConstants.STS_TOKEN_USERNAME);
        if (alias == null) {
            alias = crypto.getDefaultX509Identifier();
        }
        if (alias == null) {
            throw new Fault("No alias specified for retrieving PublicKey", LOG);
        }
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(alias);

        X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
        if (certs == null || certs.length == 0) {
            throw new Fault("Could not get X509Certificate for alias " + alias, LOG);
        }
        return certs[0];
    }

    protected void addLifetime(XMLStreamWriter writer) throws XMLStreamException {
        Instant creationTime = Instant.now();
        Instant expirationTime = creationTime.plusSeconds(ttl);

        writer.writeStartElement("wst", "Lifetime", namespace);
        writer.writeNamespace("wsu", WSS4JConstants.WSU_NS);
        writer.writeStartElement("wsu", "Created", WSS4JConstants.WSU_NS);
        writer.writeCharacters(creationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        writer.writeEndElement();

        writer.writeStartElement("wsu", "Expires", WSS4JConstants.WSU_NS);
        writer.writeCharacters(expirationTime.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        writer.writeEndElement();
        writer.writeEndElement();
    }

    protected void addAppliesTo(XMLStreamWriter writer, String appliesTo) throws XMLStreamException {
        if (appliesTo != null && addressingNamespace != null) {
            writer.writeStartElement("wsp", "AppliesTo", "http://schemas.xmlsoap.org/ws/2004/09/policy");
            writer.writeNamespace("wsp", "http://schemas.xmlsoap.org/ws/2004/09/policy");
            writer.writeStartElement("wsa", "EndpointReference", addressingNamespace);
            writer.writeNamespace("wsa", addressingNamespace);
            writer.writeStartElement("wsa", "Address", addressingNamespace);
            writer.writeCharacters(appliesTo);
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }

    protected void addTokenType(XMLStreamWriter writer, String token) throws XMLStreamException {
        if (token != null) {
            writer.writeStartElement("wst", "TokenType", namespace);
            writer.writeCharacters(token);
            writer.writeEndElement();
        }
    }

    protected void addClaims(XMLStreamWriter writer) throws XMLStreamException {
        if (claims != null) {
            StaxUtils.copy(claims, writer);
        }
    }

    protected SecurityToken createSecurityToken(Element el, byte[] requestorEntropy)
        throws WSSecurityException {

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

        while (el != null) {
            String ln = el.getLocalName();
            if (namespace.equals(el.getNamespaceURI())) {
                if ("Lifetime".equals(ln)) {
                    lte = el;
                } else if ("RequestedSecurityToken".equals(ln)) {
                    rst = DOMUtils.getFirstElement(el);
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

        byte[] secret = null;

        if (rpt != null) {
            Element child = DOMUtils.getFirstElement(rpt);
            QName childQname = DOMUtils.getElementQName(child);
            if (childQname.equals(new QName(namespace, "BinarySecret"))) {
                // First check for the binary secret
                String b64Secret = DOMUtils.getContent(child);
                secret = Base64.getMimeDecoder().decode(b64Secret);
            } else if (childQname.equals(new QName(WSS4JConstants.ENC_NS, WSS4JConstants.ENC_KEY_LN))) {
                secret = decryptKey(child);
            } else if (childQname.equals(new QName(namespace, "ComputedKey"))) {
                // Handle the computed key
                Element computedKeyChild = entropy == null ? null : DOMUtils.getFirstElement(entropy);
                byte[] serviceEntr = null;

                if (computedKeyChild != null) {
                    QName computedKeyChildQName = DOMUtils.getElementQName(computedKeyChild);
                    if (computedKeyChildQName.equals(new QName(WSS4JConstants.ENC_NS, WSS4JConstants.ENC_KEY_LN))) {
                        serviceEntr = decryptKey(computedKeyChild);
                    } else if (computedKeyChildQName.equals(new QName(namespace, "BinarySecret"))) {
                        String content = DOMUtils.getContent(computedKeyChild);
                        serviceEntr = Base64.getMimeDecoder().decode(content);
                    }
                }

                if (serviceEntr != null) {
                    // Right now we only use PSHA1 as the computed key algo
                    P_SHA1 psha1 = new P_SHA1();

                    int length = (keySize > 0) ? keySize : 256;
                    if (algorithmSuite != null) {
                        AlgorithmSuiteType algType = algorithmSuite.getAlgorithmSuiteType();
                        length = (keySize > 0) ? keySize : algType.getMaximumSymmetricKeyLength();
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

    protected byte[] decryptKey(Element child) throws TrustException, WSSecurityException {
        String encryptionAlgorithm = X509Util.getEncAlgo(child);
        // For the SPNEGO case just return the decoded cipher value and decrypt it later
        if (encryptionAlgorithm != null && encryptionAlgorithm.endsWith("spnego#GSS_Wrap")) {
            // Get the CipherValue
            Element tmpE =
                XMLUtils.getDirectChildElement(child, "CipherData", WSS4JConstants.ENC_NS);
            byte[] cipherValue = null;
            if (tmpE != null) {
                tmpE =
                    XMLUtils.getDirectChildElement(tmpE, "CipherValue", WSS4JConstants.ENC_NS);
                if (tmpE != null) {
                    String content = DOMUtils.getContent(tmpE);
                    cipherValue = Base64.getMimeDecoder().decode(content);
                }
            }
            if (cipherValue == null) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY, "noCipher");
            }
            return cipherValue;
        }
        try {
            EncryptedKeyProcessor proc = new EncryptedKeyProcessor();
            RequestData data = new RequestData();
            data.setWssConfig(WSSConfig.getNewInstance());
            data.setDecCrypto(createCrypto(true));
            data.setCallbackHandler(createHandler());

            WSDocInfo docInfo = new WSDocInfo(child.getOwnerDocument());
            data.setWsDocInfo(docInfo);

            List<WSSecurityEngineResult> result = proc.handleToken(child, data);
            return
                (byte[])result.get(0).get(
                    WSSecurityEngineResult.TAG_SECRET
                );
        } catch (IOException e) {
            throw new TrustException("ENCRYPTED_KEY_ERROR", e, LOG);
        }
    }

    protected CallbackHandler createHandler() {
        Object o = getProperty(SecurityConstants.CALLBACK_HANDLER);
        if (o instanceof String) {
            try {
                Class<?> cls = ClassLoaderUtils.loadClass((String)o, this.getClass());
                o = cls.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new Fault(e);
            }
        }
        return (CallbackHandler)o;
    }

    protected Object getProperty(String s) {
        Object o = ctx.get(s);
        if (o == null) {
            o = client.getEndpoint().getEndpointInfo().getProperty(s);
        }
        if (o == null) {
            o = client.getEndpoint().getEndpointInfo().getBinding().getProperty(s);
        }
        if (o == null) {
            o = client.getEndpoint().getService().get(s);
        }
        return o;
    }

    protected Crypto createCrypto(boolean decrypt) throws IOException, WSSecurityException {
        Crypto crypto = (Crypto)getProperty(SecurityConstants.STS_TOKEN_CRYPTO + (decrypt ? ".decrypt" : ""));
        if (crypto != null) {
            return crypto;
        }

        Object o = getProperty(SecurityConstants.STS_TOKEN_PROPERTIES + (decrypt ? ".decrypt" : ""));
        Properties properties = null;
        if (o instanceof Properties) {
            properties = (Properties)o;
        } else if (o instanceof String) {
            ResourceManager rm = bus.getExtension(ResourceManager.class);
            URL url = rm.resolveResource((String)o, URL.class);
            if (url == null) {
                url = ClassLoaderUtils.getResource((String)o, this.getClass());
            }
            if (url != null) {
                properties = new Properties();
                InputStream ins = url.openStream();
                properties.load(ins);
                ins.close();
            } else {
                throw new Fault("Could not find properties file " + (String)o, LOG);
            }
        } else if (o instanceof URL) {
            properties = new Properties();
            InputStream ins = ((URL)o).openStream();
            properties.load(ins);
            ins.close();
        }

        if (properties != null) {
            return CryptoFactory.getInstance(properties);
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
            if (elName.equals(new QName(WSS4JConstants.SAML_NS, "Assertion"))
                && rst.hasAttributeNS(null, "AssertionID")) {
                id = rst.getAttributeNS(null, "AssertionID");
            } else if (elName.equals(new QName(WSS4JConstants.SAML2_NS, "Assertion"))
                && rst.hasAttributeNS(null, "ID")) {
                id = rst.getAttributeNS(null, "ID");
            }
            if (id == null) {
                id = this.getIDFromSTR(rst);
            }
        }
        if (id == null && rar != null) {
            id = this.getIDFromSTR(rar);
        }
        if (id == null && rur != null) {
            id = this.getIDFromSTR(rur);
        }
        if (id == null && rst != null) {
            id = rst.getAttributeNS(WSS4JConstants.WSU_NS, "Id");
        }
        return id;
    }

    protected String getIDFromSTR(Element el) {
        Element child = DOMUtils.getFirstElement(el);
        if (child == null) {
            return null;
        }
        QName elName = DOMUtils.getElementQName(child);
        if (elName.equals(new QName(WSS4JConstants.SIG_NS, "KeyInfo"))
            || elName.equals(new QName(WSS4JConstants.WSSE_NS, "KeyIdentifier"))) {
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

    public void setClaims(Element rstClaims) {
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

    public void setFeatures(List<AbstractFeature> f) {
        features = f;
    }
    public List<AbstractFeature> getFeatures() {
        return features;
    }
}
