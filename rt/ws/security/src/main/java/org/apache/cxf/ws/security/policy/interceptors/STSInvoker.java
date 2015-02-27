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

package org.apache.cxf.ws.security.policy.interceptors;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.wss4j.common.derivedKey.P_SHA1;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.bsp.BSPEnforcer;
import org.apache.wss4j.dom.message.token.Reference;
import org.apache.wss4j.dom.message.token.SecurityContextToken;
import org.apache.wss4j.dom.message.token.SecurityTokenReference;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.dom.util.XmlSchemaDateFormat;
import org.apache.xml.security.utils.Base64;

/**
 * An abstract Invoker used by the Spnego and SecureConversationInInterceptors.
 */
abstract class STSInvoker implements Invoker {
    
    private static final Logger LOG = LogUtils.getL7dLogger(STSInvoker.class);
    
    public Object invoke(Exchange exchange, Object o) {
        AddressingProperties inProps = (AddressingProperties)exchange.getInMessage()
                .getContextualProperty(JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND);
        if (inProps != null) {
            AddressingProperties props = inProps.createCompatibleResponseProperties();
            AttributedURIType action = new AttributedURIType();
            action.setValue(inProps.getAction().getValue().replace("/RST/", "/RSTR/"));
            props.setAction(action);
            exchange.getOutMessage().put(JAXWSAConstants.ADDRESSING_PROPERTIES_OUTBOUND, props);
        }

        MessageContentsList lst = (MessageContentsList)o;
        DOMSource src = (DOMSource)lst.get(0);
        Node nd = src.getNode();
        Element requestEl = null;
        if (nd instanceof Document) {
            requestEl = ((Document)nd).getDocumentElement();
        } else {
            requestEl = (Element)nd;
        }
        String namespace = requestEl.getNamespaceURI();
        String prefix = requestEl.getPrefix();
        SecurityToken cancelOrRenewToken = null;
        if ("RequestSecurityToken".equals(requestEl.getLocalName())) {
            try {
                String requestType = null;
                Element binaryExchange = null;
                String tokenType = null;
                Element el = DOMUtils.getFirstElement(requestEl);
                while (el != null) {
                    String localName = el.getLocalName();
                    if (namespace.equals(el.getNamespaceURI())) {
                        if ("RequestType".equals(localName)) {
                            requestType = el.getTextContent();
                        } else if ("CancelTarget".equals(localName) || "RenewTarget".equals(localName)) {
                            cancelOrRenewToken = findCancelOrRenewToken(exchange, el);
                        } else if ("BinaryExchange".equals(localName)) {
                            binaryExchange = el;
                        } else if ("TokenType".equals(localName)) {
                            tokenType = DOMUtils.getContent(el);
                        } 
                    }

                    el = DOMUtils.getNextElement(el);
                }
                if (requestType == null) {
                    requestType = "/Issue";
                }
                if (requestType.endsWith("/Issue") 
                    && !STSUtils.getTokenTypeSCT(namespace).equals(tokenType)) {
                    throw new Exception("Unknown token type: " + tokenType);
                }
                
                W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
                writer.setNsRepairing(true);

                if (requestType.endsWith("/Issue")) { 
                    doIssue(requestEl, exchange, binaryExchange, writer, prefix, namespace);
                } else if (requestType.endsWith("/Cancel")) {
                    doCancel(exchange, cancelOrRenewToken, writer, prefix, namespace);
                } else if (requestType.endsWith("/Renew")) {
                    doRenew(requestEl, exchange, cancelOrRenewToken, binaryExchange, writer, prefix, namespace);
                }

                return new MessageContentsList(new DOMSource(writer.getDocument()));
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new Fault(ex);
            }
        } else {
            throw new Fault("Unknown SecureConversation element: " + requestEl.getLocalName(), LOG);
        }
    }
    
    abstract void doIssue(
        Element requestEl,
        Exchange exchange,
        Element binaryExchange,
        W3CDOMStreamWriter writer,
        String prefix, 
        String namespace
    ) throws Exception;

    abstract void doRenew(
            Element requestEl,
            Exchange exchange,
            SecurityToken renewToken,
            Element binaryExchange,
            W3CDOMStreamWriter writer,
            String prefix,
            String namespace
    ) throws Exception;

    private void doCancel(
        Exchange exchange, 
        SecurityToken cancelToken,
        W3CDOMStreamWriter writer,
        String prefix, 
        String namespace
    ) throws Exception {
        if (STSUtils.WST_NS_05_12.equals(namespace)) {
            writer.writeStartElement(prefix, "RequestSecurityTokenResponseCollection", namespace);
        }
        writer.writeStartElement(prefix, "RequestSecurityTokenResponse", namespace);
        
        TokenStore store = (TokenStore)exchange.get(Endpoint.class).getEndpointInfo()
                .getProperty(TokenStore.class.getName());
        store.remove(cancelToken.getId());
        // Put the token on the out message so that we can sign the response
        exchange.put(SecurityConstants.TOKEN, cancelToken);
        writer.writeEmptyElement(prefix, "RequestedTokenCancelled", namespace);
        
        writer.writeEndElement();
        if (STSUtils.WST_NS_05_12.equals(namespace)) {
            writer.writeEndElement();
        }
    }

    private SecurityToken findCancelOrRenewToken(Exchange exchange, Element el) throws WSSecurityException {
        Element childElement = DOMUtils.getFirstElement(el);
        String uri = "";
        if ("SecurityContextToken".equals(childElement.getLocalName())) {
            SecurityContextToken sct = new SecurityContextToken(childElement);
            uri = sct.getIdentifier();
        } else {
            SecurityTokenReference ref = 
                new SecurityTokenReference(childElement, new BSPEnforcer());
            uri = ref.getReference().getURI();
        }
        TokenStore store = (TokenStore)exchange.get(Endpoint.class).getEndpointInfo()
                .getProperty(TokenStore.class.getName());
        return store.getToken(uri);
    }
    
    byte[] writeProofToken(String prefix, 
        String namespace,
        W3CDOMStreamWriter writer,
        byte[] clientEntropy,
        int keySize
    ) throws NoSuchAlgorithmException, WSSecurityException, XMLStreamException {
        byte secret[] = null; 
        writer.writeStartElement(prefix, "RequestedProofToken", namespace);
        if (clientEntropy == null) {
            secret = WSSecurityUtil.generateNonce(keySize / 8);

            writer.writeStartElement(prefix, "BinarySecret", namespace);
            writer.writeAttribute("Type", namespace + "/Nonce");
            writer.writeCharacters(Base64.encode(secret));
            writer.writeEndElement();
        } else {
            byte entropy[] = WSSecurityUtil.generateNonce(keySize / 8);
            P_SHA1 psha1 = new P_SHA1();
            secret = psha1.createKey(clientEntropy, entropy, 0, keySize / 8);

            writer.writeStartElement(prefix, "ComputedKey", namespace);
            writer.writeCharacters(namespace + "/CK/PSHA1");            
            writer.writeEndElement();
            writer.writeEndElement();

            writer.writeStartElement(prefix, "Entropy", namespace);
            writer.writeStartElement(prefix, "BinarySecret", namespace);
            writer.writeAttribute("Type", namespace + "/Nonce");
            writer.writeCharacters(Base64.encode(entropy));
            writer.writeEndElement();

        }
        writer.writeEndElement();
        return secret;
    }
    
    Element writeSecurityTokenReference(
        W3CDOMStreamWriter writer,
        String id,
        String refValueType
    ) {
        Reference ref = new Reference(writer.getDocument());
        ref.setURI(id);
        if (refValueType != null) {
            ref.setValueType(refValueType);
        }
        SecurityTokenReference str = new SecurityTokenReference(writer.getDocument());
        str.addWSSENamespace();
        str.setReference(ref);

        writer.getCurrentNode().appendChild(str.getElement());
        return str.getElement();
    }
    
    void writeLifetime(
        W3CDOMStreamWriter writer,
        Date created,
        Date expires,
        String prefix,
        String namespace
    ) throws Exception {
        XmlSchemaDateFormat fmt = new XmlSchemaDateFormat();
        writer.writeStartElement(prefix, "Lifetime", namespace);
        writer.writeNamespace("wsu", WSConstants.WSU_NS);
        writer.writeStartElement("wsu", "Created", WSConstants.WSU_NS);
        writer.writeCharacters(fmt.format(created.getTime()));
        writer.writeEndElement();
        
        writer.writeStartElement("wsu", "Expires", WSConstants.WSU_NS);
        writer.writeCharacters(fmt.format(expires.getTime()));
        writer.writeEndElement();
        writer.writeEndElement();
    }

}

