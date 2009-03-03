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
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
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
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.ws.policy.EffectivePolicy;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;
import org.apache.cxf.ws.security.policy.model.Binding;
import org.apache.cxf.ws.security.policy.model.Trust10;
import org.apache.cxf.ws.security.policy.model.Trust13;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.conversation.ConversationException;
import org.apache.ws.security.conversation.dkalgo.P_SHA1;
import org.apache.ws.security.message.token.Reference;
import org.apache.ws.security.processor.EncryptedDataProcessor;
import org.apache.ws.security.processor.EncryptedKeyProcessor;
import org.apache.ws.security.util.Base64;
import org.apache.ws.security.util.WSSecurityUtil;
import org.apache.ws.security.util.XmlSchemaDateFormat;

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
    Trust10 trust10;
    Trust13 trust13;
    Element template;
    AlgorithmSuite algorithmSuite;
    String namespace = STSUtils.WST_NS_05_02;
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
        }
        trust10 = trust;
    }
    public void setTrust(Trust13 trust) {
        if (trust != null) {
            namespace = STSUtils.WST_NS_05_12;
        }
        trust13 = trust;        
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
    public void setServiceName(QName qn) {
        serviceName = qn;
    }
    public void setServiceName(String qn) {
        serviceName = QName.valueOf(qn);
    }
    public void setEndpointName(QName qn) {
        endpointName = qn;
    }
    public void setEndpointName(String qn) {
        endpointName = QName.valueOf(qn);
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
            Endpoint endpoint = STSUtils.createSTSEndpoint(bus, 
                                                           namespace,
                                                           null,
                                                           location,
                                                           soapVersion, 
                                                           policy,
                                                           endpointName);
            
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
                EffectivePolicy effectivePolicy 
                    = pe.getEffectiveClientRequestPolicy(client.getEndpoint().getEndpointInfo(),
                                                         boi, conduit);
                setPolicy(effectivePolicy.getPolicy());
                return boi;
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
    
    public SecurityToken requestSecurityToken(String appliesTo,
                                              String action,
                                              String requestType,
                                              SecurityToken target) throws Exception {
        createClient();
        BindingOperationInfo boi = findOperation("/RST/Issue");
        
        client.getRequestContext().putAll(ctx);
        if (action != null) {
            client.getRequestContext().put(SoapBindingConstants.SOAP_ACTION,
                                           action);
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
        
        byte[] requestorEntropy = null;
        
        if (keyType.endsWith("SymmetricKey")) {
            if (!wroteKeySize && !isSecureConv) {
                writer.writeStartElement("wst", "KeySize", namespace);
                writer.writeCharacters(Integer.toString(keySize));
                writer.writeEndElement();
            }
        
            if ((trust10 != null && trust10.isRequireClientEntropy())
                || (trust13 != null && trust13.isRequireClientEntropy())) {
                writer.writeStartElement("wst", "Entropy", namespace);
                writer.writeStartElement("wst", "BinarySecret", namespace);
                writer.writeAttribute("Type", namespace + "/Nonce");
                requestorEntropy =
                    WSSecurityUtil.generateNonce(algorithmSuite.getMaximumSymmetricKeyLength() / 8);
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
            
            /*
            //REVISIT - KeyValueToken support - how to get the key?
            RSAPublicKey key = getPublicKey();
            
            RSAKeyValue value = new RSAKeyValue(writer.getDocument(), key);
            StaxUtils.copy(value.getElement(), writer);
            */
            
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
        
        Object obj[] = client.invoke(boi,
                                     new DOMSource(writer.getDocument().getDocumentElement()));
        
        return createSecurityToken((Document)((DOMSource)obj[0]).getNode(), requestorEntropy);
    }
    public void renewSecurityToken(SecurityToken tok) throws Exception {
        String action = null;
        if (isSecureConv) {
            action = namespace + "/RST/SCT/Renew";
        }
        requestSecurityToken(tok.getIssuerAddress(), action, "/Renew", tok);
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

    private SecurityToken createSecurityToken(Document document, byte[] requestorEntropy) 
        throws WSSecurityException {
        
        Element el = document.getDocumentElement();
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
        try {
            rstDec = decrypt(rst);
        } catch (IOException e1) {
            throw new TrustException(e1);
        }
        
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
                //First check for the binary secret
                String b64Secret = DOMUtils.getContent(child);
                secret = Base64.decode(b64Secret);
            } else if (childQname.equals(new QName(namespace, WSConstants.ENC_KEY_LN))) {
                try {


                    EncryptedKeyProcessor processor = new EncryptedKeyProcessor();

                    processor.handleToken(child, null, createCrypto(),
                                          createHandler(), null, new Vector(),
                                          null);

                    secret = processor.getDecryptedBytes();
                } catch (IOException e) {
                    throw new TrustException(new Message("ENCRYPTED_KEY_ERROR", LOG), e);
                }
            } else if (childQname.equals(new QName(namespace, "ComputedKey"))) {
                //Handle the computed key
                Element binSecElem = entropy == null ? null 
                    : DOMUtils.getFirstElement(entropy);
                String content = binSecElem == null ? null
                    : DOMUtils.getContent(binSecElem);
                if (content != null && !StringUtils.isEmpty(content.trim())) {

                    byte[] serviceEntr = Base64.decode(content);

                    //Right now we only use PSHA1 as the computed key algo                    
                    P_SHA1 psha1 = new P_SHA1();

                    int length = (keySize > 0) ? keySize
                                 : algorithmSuite
                                     .getMaximumSymmetricKeyLength();
                    try {
                        secret = psha1.createKey(requestorEntropy, serviceEntr, 0, length / 8);
                    } catch (ConversationException e) {
                        throw new TrustException(new Message("DERIVED_KEY_ERROR", LOG), e);
                    }
                } else {
                    //Service entropy missing
                    throw new TrustException(new Message("NO_ENTROPY", LOG));
                }
            }
        } else if (requestorEntropy != null) {
            //Use requester entropy as the key
            secret = requestorEntropy;
        }
        token.setSecret(secret);
        
        return token;
    }

    protected Element decrypt(Element firstElement) throws IOException {
        if ("EncryptedData".equals(firstElement.getLocalName())
            && "http://www.w3.org/2001/04/xmlenc#".equals(firstElement.getNamespaceURI())) {
            
            Node parent = firstElement.getParentNode();
            Node prev = firstElement.getPreviousSibling();
            
            //encrypted even more.  WCF seems to do this periodically
            EncryptedDataProcessor processor = new EncryptedDataProcessor();

            processor.handleToken(firstElement, null, createCrypto(),
                                  createHandler(), null, new Vector(),
                                  null);
            
            if (prev == null) {
                firstElement = (Element)parent.getFirstChild();
            } else {
                firstElement = (Element)prev.getNextSibling();
            }

        }
        return firstElement;
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
            o = client.getEndpoint()
                .getEndpointInfo().getProperty(s);
        }
        if (o == null) {
            o = client.getEndpoint().getEndpointInfo().getBinding().getProperty(s);
        }
        if (o == null) {
            o = client.getEndpoint().getService().get(s);
        }
        return o;
    }
    
    private Crypto createCrypto() throws IOException {
        Crypto crypto = (Crypto)getProperty(SecurityConstants.STS_TOKEN_CRYPTO);
        if (crypto != null) {
            return crypto;
        }
        
        
        Object o = getProperty(SecurityConstants.STS_TOKEN_PROPERTIES); 
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
                properties.load(url.openStream());
            } else {
                throw new Fault("Could not find properties file " + url, LOG);
            }
        } else if (o instanceof URL) {
            properties = new Properties();
            properties.load(((URL)o).openStream());
        }
        
        if (properties != null) {
            return CryptoFactory.getInstance(properties);
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
