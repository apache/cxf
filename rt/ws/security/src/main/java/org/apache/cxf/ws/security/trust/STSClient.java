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
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
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
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.databinding.source.SourceDataBinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
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
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.apache.neethi.All;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.conversation.ConversationException;
import org.apache.ws.security.conversation.dkalgo.P_SHA1;
import org.apache.ws.security.message.token.Reference;
import org.apache.ws.security.processor.EncryptedKeyProcessor;
import org.apache.ws.security.util.Base64;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.ws.security.util.XmlSchemaDateFormat;
import org.apache.xml.security.keys.content.keyvalues.DSAKeyValue;
import org.apache.xml.security.keys.content.keyvalues.RSAKeyValue;

/**
 * 
 */
public class STSClient implements Configurable {
    private static final Logger LOG = LogUtils.getL7dLogger(STSClient.class);

    Bus bus;
    String name = "default.sts-client";
    Client client;
    String location;

    String wsdlLocation;
    QName serviceName;
    QName endpointName;

    Policy policy;
    String soapVersion = SoapBindingConstants.SOAP11_BINDING_ID;
    int keySize = 256;
    boolean requiresEntropy = true;
    Element template;
    AlgorithmSuite algorithmSuite;
    String namespace = STSUtils.WST_NS_05_12;
    String addressingNamespace;

    boolean isSecureConv;
    int ttl = 300;

    Map<String, Object> ctx = new HashMap<String, Object>();

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

    public void setPolicy(Policy policy) {
        this.policy = policy;
        if (algorithmSuite == null) {
            Iterator i = policy.getAlternatives();
            while (i.hasNext() && algorithmSuite == null) {
                List<PolicyComponent> p = CastUtils.cast((List)i.next());
                for (PolicyComponent p2 : p) {
                    if (p2 instanceof Binding) {
                        algorithmSuite = ((Binding)p2).getAlgorithmSuite();
                    }
                }
            }
        }
    }

    public void setPolicy(Element policy) {
        setPolicy(bus.getExtension(PolicyBuilder.class).getPolicy(policy));
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

    public void setServiceName(String qn) {
        serviceName = QName.valueOf(qn);
    }

    public void setEndpointName(String qn) {
        endpointName = QName.valueOf(qn);
    }
    
    public void setServiceQName(QName qn) {
        serviceName = qn;
    }

    public void setEndpointQName(QName qn) {
        endpointName = qn;
    }


    private void createClient() throws BusException, EndpointException {
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
    }

    private BindingOperationInfo findOperation(String suffix) {
        BindingInfo bi = client.getEndpoint().getBinding().getBindingInfo();
        for (BindingOperationInfo boi : bi.getOperations()) {
            SoapOperationInfo soi = boi.getExtensor(SoapOperationInfo.class);
            if (soi != null && soi.getAction() != null && soi.getAction().endsWith(suffix)) {
                PolicyEngine pe = bus.getExtension(PolicyEngine.class);
                Conduit conduit = client.getConduit();
                EffectivePolicy effectivePolicy = pe.getEffectiveClientRequestPolicy(client.getEndpoint()
                    .getEndpointInfo(), boi, conduit);
                setPolicy(effectivePolicy.getPolicy());
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
        String action = null;
        if (isSecureConv) {
            action = namespace + "/RST/SCT";
        }
        return requestSecurityToken(appliesTo, action, "/Issue", null);
    }

    public SecurityToken requestSecurityToken(String appliesTo, String action, String requestType,
                                              SecurityToken target) throws Exception {
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
        boolean wroteKeySize = false;
        String keyType = null;
        if (template != null) {
            Element tl = DOMUtils.getFirstElement(template);
            while (tl != null) {
                StaxUtils.copy(tl, writer);
                if ("KeyType".equals(tl.getLocalName())) {
                    keyType = DOMUtils.getContent(tl);
                } else if ("KeySize".equals(tl.getLocalName())) {
                    wroteKeySize = true;
                    keySize = Integer.parseInt(DOMUtils.getContent(tl));
                }
                tl = DOMUtils.getNextElement(tl);
            }
        }

        writer.writeStartElement("wst", "RequestType", namespace);
        writer.writeCharacters(namespace + requestType);
        writer.writeEndElement();
        addAppliesTo(writer, appliesTo);
        keyType = writeKeyType(writer, keyType);

        byte[] requestorEntropy = null;
        X509Certificate cert = null;
        Crypto crypto = null;

        if (keyType.endsWith("SymmetricKey")) {
            if (!wroteKeySize && !isSecureConv) {
                writer.writeStartElement("wst", "KeySize", namespace);
                writer.writeCharacters(Integer.toString(keySize));
                writer.writeEndElement();
            }

            if (requiresEntropy) {
                writer.writeStartElement("wst", "Entropy", namespace);
                writer.writeStartElement("wst", "BinarySecret", namespace);
                writer.writeAttribute("Type", namespace + "/Nonce");
                requestorEntropy = WSSecurityUtil
                    .generateNonce(algorithmSuite.getMaximumSymmetricKeyLength() / 8);
                writer.writeCharacters(Base64.encode(requestorEntropy));

                writer.writeEndElement();
                writer.writeEndElement();
                writer.writeStartElement("wst", "ComputedKeyAlgorithm", namespace);
                writer.writeCharacters(namespace + "/CK/PSHA1");
                writer.writeEndElement();
            }
        } else if (keyType.endsWith("PublicKey")) {
            writer.writeStartElement("wst", "UseKey", namespace);
            writer.writeStartElement("http://www.w3.org/2000/09/xmldsig#", "KeyInfo");
            writer.writeStartElement("http://www.w3.org/2000/09/xmldsig#", "KeyValue");
            crypto = createCrypto(false);
            cert = getCert(crypto);
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
            writer.writeEndElement();
            writer.writeEndElement();
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
        writer.writeEndElement();

        Object obj[] = client.invoke(boi, new DOMSource(writer.getDocument().getDocumentElement()));

        SecurityToken token = createSecurityToken(getDocumentElement((DOMSource)obj[0]), requestorEntropy);
        if (cert != null) {
            token.setX509Certificate(cert, crypto);
        }
        return token;
    }
    
    private Element getDocumentElement(DOMSource ds) {
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

    private PrimitiveAssertion getAddressingAssertion() {
        String ns = "http://schemas.xmlsoap.org/ws/2004/08/addressing/policy";
        return new PrimitiveAssertion(new QName(ns, "UsingAddressing"));
    }
    public boolean validateSecurityToken(SecurityToken tok) throws Exception {
        return validateSecurityToken(tok,
                                     namespace + "/RSTR/Status");
    }
    private boolean validateSecurityToken(SecurityToken tok, String string) 
        throws Exception {
        createClient();

        if (addressingNamespace == null) {
            addressingNamespace = "http://www.w3.org/2005/08/addressing";
        }

        Policy validatePolicy = new Policy();
        ExactlyOne one = new ExactlyOne();
        validatePolicy.addPolicyComponent(one);
        All all = new All();
        SymmetricBinding binding = new SymmetricBinding();
        all.addAssertion(binding);
        one.addPolicyComponent(all);
        all.addAssertion(getAddressingAssertion());
        ProtectionToken ptoken = new ProtectionToken();
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
        parts.setBody(true);
        parts.addHeader(new Header("To", addressingNamespace));
        parts.addHeader(new Header("From", addressingNamespace));
        parts.addHeader(new Header("FaultTo", addressingNamespace));
        parts.addHeader(new Header("ReplyTo", addressingNamespace));
        parts.addHeader(new Header("Action", addressingNamespace));
        parts.addHeader(new Header("MessageID", addressingNamespace));
        parts.addHeader(new Header("RelatesTo", addressingNamespace));
        all.addPolicyComponent(parts);
        
        client.getRequestContext().putAll(ctx);
        client.getRequestContext().put(PolicyConstants.POLICY_OVERRIDE, validatePolicy);
        client.getRequestContext().put(SecurityConstants.TOKEN, tok);
        BindingOperationInfo boi = findOperation("/RST/Validate");
        
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityToken", namespace);
        writer.writeStartElement("wst", "RequestType", namespace);
        writer.writeCharacters(namespace + "/Validate");
        writer.writeEndElement();

        writer.writeStartElement("wst", "ValidateTarget", namespace);
        Element el = tok.getUnattachedReference();
        if (el == null) {
            el = tok.getAttachedReference();
        }
        StaxUtils.copy(el, writer);

        writer.writeEndElement();
        writer.writeEndElement();

        client.invoke(boi, new DOMSource(writer.getDocument().getDocumentElement()));
        
        return false;
    }

    public void cancelSecurityToken(SecurityToken token) throws Exception {
        createClient();

        if (isSecureConv) {
            client.getRequestContext().put(SoapBindingConstants.SOAP_ACTION,
                                           namespace + "/RST/SCT/Cancel");
        }

        if (addressingNamespace == null) {
            addressingNamespace = "http://www.w3.org/2005/08/addressing";
        }
        Policy cancelPolicy = new Policy();
        ExactlyOne one = new ExactlyOne();
        cancelPolicy.addPolicyComponent(one);
        All all = new All();
        one.addPolicyComponent(all);
        SymmetricBinding binding = new SymmetricBinding();
        all.addAssertion(binding);
        all.addAssertion(getAddressingAssertion());
        ProtectionToken ptoken = new ProtectionToken();
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
        parts.setBody(true);
        parts.addHeader(new Header("To", addressingNamespace));
        parts.addHeader(new Header("From", addressingNamespace));
        parts.addHeader(new Header("FaultTo", addressingNamespace));
        parts.addHeader(new Header("ReplyTo", addressingNamespace));
        parts.addHeader(new Header("Action", addressingNamespace));
        parts.addHeader(new Header("MessageID", addressingNamespace));
        parts.addHeader(new Header("RelatesTo", addressingNamespace));
        all.addPolicyComponent(parts);
        

        client.getRequestContext().putAll(ctx);
        client.getRequestContext().put(PolicyConstants.POLICY_OVERRIDE, cancelPolicy);
        client.getRequestContext().put(SecurityConstants.TOKEN, token);
        BindingOperationInfo boi = findOperation("/RST/Cancel");

        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        writer.writeStartElement("wst", "RequestSecurityToken", namespace);
        writer.writeStartElement("wst", "RequestType", namespace);
        writer.writeCharacters(namespace + "/Cancel");
        writer.writeEndElement();

        writer.writeStartElement("wst", "CancelTarget", namespace);
        Element el = token.getUnattachedReference();
        if (el == null) {
            el = token.getAttachedReference();
        }
        StaxUtils.copy(el, writer);

        writer.writeEndElement();
        writer.writeEndElement();

        try {
            client.invoke(boi, new DOMSource(writer.getDocument().getDocumentElement()));
            token.setState(SecurityToken.State.CANCELLED);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Problem cancelling token", ex);
        }
    }

    private String writeKeyType(W3CDOMStreamWriter writer, String keyType) throws XMLStreamException {
        if (isSecureConv) {
            addLifetime(writer);
            if (keyType == null) {
                writer.writeStartElement("wst", "TokenType", namespace);
                writer.writeCharacters(STSUtils.getTokenTypeSCT(namespace));
                writer.writeEndElement();
                keyType = namespace + "/SymmetricKey";
            }
        } else if (keyType == null) {
            writer.writeStartElement("wst", "KeyType", namespace);
            writer.writeCharacters(namespace + "/SymmetricKey");
            writer.writeEndElement();
            keyType = namespace + "/SymmetricKey";
        }
        return keyType;
    }

    private X509Certificate getCert(Crypto crypto) throws Exception {
        String alias = (String)getProperty(SecurityConstants.STS_TOKEN_USERNAME);
        if (alias == null) {
            alias = crypto.getDefaultX509Alias();
        }
        if (alias == null) {
            Enumeration<String> as = crypto.getKeyStore().aliases();
            if (as.hasMoreElements()) {
                alias = as.nextElement();
            }
            if (as.hasMoreElements()) {
                throw new Fault("No alias specified for retrieving PublicKey", LOG);
            }
        }
        return crypto.getCertificates(alias)[0];
    }

    private void addLifetime(XMLStreamWriter writer) throws XMLStreamException {
        Date creationTime = new Date();
        Date expirationTime = new Date();
        expirationTime.setTime(creationTime.getTime() + (ttl * 1000));

        XmlSchemaDateFormat fmt = new XmlSchemaDateFormat();
        writer.writeStartElement("wst", "Lifetime", namespace);
        writer.writeStartElement("wsu", "Created", WSConstants.WSU_NS);
        writer.writeCharacters(fmt.format(creationTime));
        writer.writeEndElement();

        writer.writeStartElement("wsu", "Expires", WSConstants.WSU_NS);
        writer.writeCharacters(fmt.format(expirationTime));
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private void addAppliesTo(XMLStreamWriter writer, String appliesTo) throws XMLStreamException {
        if (appliesTo != null && addressingNamespace != null) {
            writer.writeStartElement("wsp", "AppliesTo", "http://schemas.xmlsoap.org/ws/2004/09/policy");
            writer.writeStartElement("wsa", "EndpointReference", addressingNamespace);
            writer.writeStartElement("wsa", "Address", addressingNamespace);
            writer.writeCharacters(appliesTo);
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndElement();
        }
    }

    private SecurityToken createSecurityToken(Element el, byte[] requestorEntropy)
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
                }
            }
            el = DOMUtils.getNextElement(el);
        }
        Element rstDec = rst;
        String id = findID(rar, rur, rstDec);
        if (StringUtils.isEmpty(id)) {
            throw new TrustException(new Message("NO_ID", LOG));
        }
        SecurityToken token = new SecurityToken(id, rstDec, lte);
        token.setAttachedReference(rar);
        token.setUnattachedReference(rur);
        token.setIssuerAddress(location);

        byte[] secret = null;

        if (rpt != null) {
            Element child = DOMUtils.getFirstElement(rpt);
            QName childQname = DOMUtils.getElementQName(child);
            if (childQname.equals(new QName(namespace, "BinarySecret"))) {
                // First check for the binary secret
                String b64Secret = DOMUtils.getContent(child);
                secret = Base64.decode(b64Secret);
            } else if (childQname.equals(new QName(namespace, WSConstants.ENC_KEY_LN))) {
                try {

                    EncryptedKeyProcessor processor = new EncryptedKeyProcessor();

                    processor.handleToken(child, null, createCrypto(true), createHandler(), null,
                                          new Vector(), null);

                    secret = processor.getDecryptedBytes();
                } catch (IOException e) {
                    throw new TrustException(new Message("ENCRYPTED_KEY_ERROR", LOG), e);
                }
            } else if (childQname.equals(new QName(namespace, "ComputedKey"))) {
                // Handle the computed key
                Element binSecElem = entropy == null ? null : DOMUtils.getFirstElement(entropy);
                String content = binSecElem == null ? null : DOMUtils.getContent(binSecElem);
                if (content != null && !StringUtils.isEmpty(content.trim())) {

                    byte[] serviceEntr = Base64.decode(content);

                    // Right now we only use PSHA1 as the computed key algo
                    P_SHA1 psha1 = new P_SHA1();

                    int length = (keySize > 0) ? keySize : algorithmSuite.getMaximumSymmetricKeyLength();
                    try {
                        secret = psha1.createKey(requestorEntropy, serviceEntr, 0, length / 8);
                    } catch (ConversationException e) {
                        throw new TrustException(new Message("DERIVED_KEY_ERROR", LOG), e);
                    }
                } else {
                    // Service entropy missing
                    throw new TrustException(new Message("NO_ENTROPY", LOG));
                }
            }
        } else if (requestorEntropy != null) {
            // Use requester entropy as the key
            secret = requestorEntropy;
        }
        token.setSecret(secret);

        return token;
    }

    private CallbackHandler createHandler() {
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

    private Object getProperty(String s) {
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

    private Crypto createCrypto(boolean decrypt) throws IOException {
        WSSConfig.getDefaultWSConfig();
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
                throw new Fault("Could not find properties file " + url, LOG);
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

    private String findID(Element rar, Element rur, Element rst) {
        String id = null;
        if (rst != null) {
            id = this.getIDFromSTR(rst);
        }
        if (id == null && rar != null) {
            id = this.getIDFromSTR(rar);
        }
        if (id == null && rur != null) {
            id = this.getIDFromSTR(rur);
        }
        if (id == null) {
            id = rst.getAttributeNS(WSConstants.WSU_NS, "Id");
        }
        return id;
    }

    private String getIDFromSTR(Element el) {
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

}
