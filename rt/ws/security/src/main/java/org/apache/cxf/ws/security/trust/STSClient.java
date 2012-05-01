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
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.Callback;
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
//import org.apache.cxf.common.i18n.Message;
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
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.policy.EffectivePolicy;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;
import org.apache.cxf.ws.security.policy.model.Binding;
import org.apache.cxf.ws.security.policy.model.Header;
import org.apache.cxf.ws.security.policy.model.ProtectionToken;
import org.apache.cxf.ws.security.policy.model.SecureConversationToken;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedParts;
import org.apache.cxf.ws.security.policy.model.SymmetricBinding;
import org.apache.cxf.ws.security.policy.model.Trust10;
import org.apache.cxf.ws.security.policy.model.Trust13;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.delegation.DelegationCallback;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.apache.neethi.All;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.components.crypto.CryptoType;
import org.apache.ws.security.conversation.ConversationException;
import org.apache.ws.security.conversation.dkalgo.P_SHA1;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.message.token.Reference;
import org.apache.ws.security.processor.EncryptedKeyProcessor;
import org.apache.ws.security.processor.X509Util;
import org.apache.ws.security.util.Base64;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.ws.security.util.XmlSchemaDateFormat;
import org.apache.xml.security.keys.content.X509Data;
import org.apache.xml.security.keys.content.keyvalues.DSAKeyValue;
import org.apache.xml.security.keys.content.keyvalues.RSAKeyValue;

/**
 * 
 */
public class STSClient implements Configurable, InterceptorProvider {
    private static final Logger LOG = LogUtils.getL7dLogger(STSClient.class);
    
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
    
    protected Object actAs;
    protected String tokenType;
    protected String keyType;
    protected boolean sendKeyType = true;
    protected Message message;
    protected String context;

    protected Map<String, Object> ctx = new HashMap<String, Object>();
    
    protected List<Interceptor<? extends Message>> in 
        = new ModCountCopyOnWriteArrayList<Interceptor<? extends Message>>();
    protected List<Interceptor<? extends Message>> out 
        = new ModCountCopyOnWriteArrayList<Interceptor<? extends Message>>();
    protected List<Interceptor<? extends Message>> outFault  
        = new ModCountCopyOnWriteArrayList<Interceptor<? extends Message>>();
    protected List<Interceptor<? extends Message>> inFault 
        = new ModCountCopyOnWriteArrayList<Interceptor<? extends Message>>();
    protected List<AbstractFeature> features;

    public STSClient(Bus b) {
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
                    if (p2 instanceof Binding) {
                        algorithmSuite = ((Binding)p2).getAlgorithmSuite();
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
                    .getEndpointInfo(), boi, conduit);
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

    public SecurityToken requestSecurityToken() throws Exception {
        return requestSecurityToken(null);
    }

    public SecurityToken requestSecurityToken(String appliesTo) throws Exception {
        return requestSecurityToken(appliesTo, null);
    }
    
    public SecurityToken requestSecurityToken(String appliesTo, String binaryExchange) throws Exception {
        String action = null;
        if (isSecureConv) {
            action = namespace + "/RST/SCT";
        }
        return requestSecurityToken(appliesTo, action, "/Issue", null, binaryExchange);
    }
    
    public SecurityToken requestSecurityToken(String appliesTo, String action, String requestType,
            SecurityToken target) throws Exception {
        return requestSecurityToken(appliesTo, action, requestType, target, null);
    }

    public SecurityToken requestSecurityToken(String appliesTo, String action, String requestType,
                                              SecurityToken target, String binaryExchange) throws Exception {
        createClient();
        BindingOperationInfo boi = findOperation("/RST/Issue");

        client.getRequestContext().putAll(ctx);
        if (action != null) {
            client.getRequestContext().put(SoapBindingConstants.SOAP_ACTION, action);
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
            crypto = createCrypto(false);
            cert = getCert(crypto);
            writeElementsForRSTPublicKey(writer, cert);
        } else if (isSpnego) {
            addKeySize(keySize, writer);
        }
        
        if (target != null) {
            writer.writeStartElement("wst", "RenewTarget", namespace);
            Element el = target.getUnattachedReference();
            if (el == null) {
                el = target.getAttachedReference();
            }
            StaxUtils.copy(el, writer);
            writer.writeEndElement();
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
        
        writer.writeEndElement();

        Object obj[] = client.invoke(boi, new DOMSource(writer.getDocument().getDocumentElement()));

        SecurityToken token = createSecurityToken(getDocumentElement((DOMSource)obj[0]), requestorEntropy);
        if (cert != null) {
            token.setX509Certificate(cert, crypto);
        }
        if (token.getTokenType() == null) {
            if (sptt != null) {
                token.setTokenType(sptt);
            } else if (tokenType != null) {
                token.setTokenType(tokenType);
            }
        }
        return token;
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
                        DOMUtils.readXml(new StringReader((String) delegationObject));
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

        if (!wroteKeySize && (!isSecureConv || keySize != 256)) {
            addKeySize(keySize, writer);
        }

        if (requiresEntropy) {
            writer.writeStartElement("wst", "Entropy", namespace);
            writer.writeStartElement("wst", "BinarySecret", namespace);
            writer.writeAttribute("Type", namespace + "/Nonce");
            if (algorithmSuite == null) {
                requestorEntropy = WSSecurityUtil.generateNonce(keySize / 8);
            } else {
                requestorEntropy = WSSecurityUtil
                    .generateNonce(algorithmSuite.getMaximumSymmetricKeyLength() / 8);
            }
            writer.writeCharacters(Base64.encode(requestorEntropy));

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
        writer.writeAttribute("EncodingType", BinarySecurity.BASE64_ENCODING);
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

    public void renewSecurityToken(SecurityToken tok) throws Exception {
        String action = null;
        if (isSecureConv) {
            action = namespace + "/RST/SCT/Renew";
        }
        requestSecurityToken(tok.getIssuerAddress(), action, "/Renew", tok);
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
    
    public List<SecurityToken> validateSecurityToken(SecurityToken tok) throws Exception {
        String validateTokenType = tokenType;
        if (validateTokenType == null) {
            validateTokenType = namespace + "/RSTR/Status";
        }
        return validateSecurityToken(tok, validateTokenType);
    }
    
    protected List<SecurityToken> validateSecurityToken(SecurityToken tok, String tokentype) 
        throws Exception {
        createClient();
        
        if (tokentype == null) {
            tokentype = tokenType;
        }
        if (tokentype == null) {
            tokentype = namespace + "/RSTR/Status";
        }

        if (addressingNamespace == null) {
            addressingNamespace = "http://www.w3.org/2005/08/addressing";
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

        writer.writeStartElement("wst", "ValidateTarget", namespace);

        Element el = tok.getToken();
        StaxUtils.copy(el, writer);

        writer.writeEndElement();
        writer.writeEndElement();

        Object o[] = client.invoke(boi, new DOMSource(writer.getDocument().getDocumentElement()));
        el = getDocumentElement((DOMSource)o[0]);
        if ("RequestSecurityTokenResponseCollection".equals(el.getLocalName())) {
            el = DOMUtils.getFirstElement(el);
        }
        if (!"RequestSecurityTokenResponse".equals(el.getLocalName())) {
            throw new Fault("Unexpected element " + el.getLocalName(), LOG);
        }
        el = DOMUtils.getFirstElement(el);
        String reason = null;
        boolean valid = false;
        List<SecurityToken> tokens = new LinkedList<SecurityToken>();
        while (el != null) {
            if ("Status".equals(el.getLocalName())) {
                Element e2 = DOMUtils.getFirstChildWithName(el, el.getNamespaceURI(), "Code");
                String s = DOMUtils.getContent(e2);
                valid = s.endsWith("/status/valid");
                
                e2 = DOMUtils.getFirstChildWithName(el, el.getNamespaceURI(), "Reason");
                if (e2 != null) {
                    reason = DOMUtils.getContent(e2);
                }
            } else if ("RequestedSecurityToken".equals(el.getLocalName())) {
                Element requestedSecurityTokenElement = DOMUtils.getFirstElement(el);
                String id = findID(null, null, requestedSecurityTokenElement);
                if (StringUtils.isEmpty(id)) {
                    throw new TrustException("NO_ID", LOG);
                }
                SecurityToken requestedSecurityToken = new SecurityToken(id);
                requestedSecurityToken.setToken(requestedSecurityTokenElement);
                tokens.add(requestedSecurityToken);
            }
            el = DOMUtils.getNextElement(el);
        }
        if (!valid) {
            throw new TrustException(LOG, "VALIDATION_FAILED", reason);
        }
        if (tokens.isEmpty()) {
            tokens.add(tok);
        }
        return tokens;
    }

    public boolean cancelSecurityToken(SecurityToken token) throws Exception {
        createClient();

        if (addressingNamespace == null) {
            addressingNamespace = "http://www.w3.org/2005/08/addressing";
        }

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
            
            PolicyBuilder pbuilder = bus.getExtension(PolicyBuilder.class);
            SymmetricBinding binding = new SymmetricBinding(pbuilder);
            all.addAssertion(binding);
            all.addAssertion(getAddressingAssertion());
            ProtectionToken ptoken = new ProtectionToken(pbuilder);
            binding.setProtectionToken(ptoken);
            binding.setIncludeTimestamp(true);
            binding.setEntireHeadersAndBodySignatures(true);
            binding.setTokenProtection(false);
            AlgorithmSuite suite = new AlgorithmSuite();
            binding.setAlgorithmSuite(suite);
            SecureConversationToken sct = new SecureConversationToken();
            sct.setOptional(true);
            ptoken.setToken(sct);
            
            SignedEncryptedParts parts = new SignedEncryptedParts(true);
            parts.setOptional(true);
            parts.setBody(true);
            parts.addHeader(new Header("To", addressingNamespace));
            parts.addHeader(new Header("From", addressingNamespace));
            parts.addHeader(new Header("FaultTo", addressingNamespace));
            parts.addHeader(new Header("ReplyTo", addressingNamespace));
            parts.addHeader(new Header("Action", addressingNamespace));
            parts.addHeader(new Header("MessageID", addressingNamespace));
            parts.addHeader(new Header("RelatesTo", addressingNamespace));
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

        try {
            client.invoke(boi, new DOMSource(writer.getDocument().getDocumentElement()));
            token.setState(SecurityToken.State.CANCELLED);
            return true;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Problem cancelling token", ex);
            return false;
        }
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
        
        X509Certificate certs[] = crypto.getX509Certificates(cryptoType);
        if (certs == null || certs.length == 0) {
            throw new Fault("Could not get X509Certificate for alias " + alias, LOG);
        }
        return certs[0];
    }

    protected void addLifetime(XMLStreamWriter writer) throws XMLStreamException {
        Date creationTime = new Date();
        Date expirationTime = new Date();
        expirationTime.setTime(creationTime.getTime() + (ttl * 1000L));

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

    protected void addTokenType(XMLStreamWriter writer) throws XMLStreamException {
        if (tokenType != null) {
            writer.writeStartElement("wst", "TokenType", namespace);
            writer.writeCharacters(tokenType);
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
        String retKeySize = null;

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

        byte[] secret = null;

        if (rpt != null) {
            Element child = DOMUtils.getFirstElement(rpt);
            QName childQname = DOMUtils.getElementQName(child);
            if (childQname.equals(new QName(namespace, "BinarySecret"))) {
                // First check for the binary secret
                String b64Secret = DOMUtils.getContent(child);
                secret = Base64.decode(b64Secret);
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
                        serviceEntr = Base64.decode(content);
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
                    } catch (ConversationException e) {
                        throw new TrustException("DERIVED_KEY_ERROR", LOG, e);
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
                WSSecurityUtil.getDirectChildElement(child, "CipherData", WSConstants.ENC_NS);
            byte[] cipherValue = null;
            if (tmpE != null) {
                tmpE = 
                    WSSecurityUtil.getDirectChildElement(tmpE, "CipherValue", WSConstants.ENC_NS);
                if (tmpE != null) {
                    String content = DOMUtils.getContent(tmpE);
                    cipherValue = Base64.decode(content);
                }
            }
            if (cipherValue == null) {
                throw new WSSecurityException(WSSecurityException.INVALID_SECURITY, "noCipher");
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
                throw new TrustException("ENCRYPTED_KEY_ERROR", LOG, e);
            }
        }
    }

    protected CallbackHandler createHandler() {
        Object o = getProperty(SecurityConstants.CALLBACK_HANDLER);
        if (o instanceof String) {
            try {
                Class<?> cls = ClassLoaderUtils.loadClass((String)o, this.getClass());
                o = cls.newInstance();
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
            if (elName.equals(new QName(WSConstants.SAML_NS, "Assertion"))
                && rst.hasAttributeNS(null, "AssertionID")) {
                id = rst.getAttributeNS(null, "AssertionID");
            } else if (elName.equals(new QName(WSConstants.SAML2_NS, "Assertion"))
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
            id = rst.getAttributeNS(WSConstants.WSU_NS, "Id");
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
            return child.getAttribute("URI");
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
