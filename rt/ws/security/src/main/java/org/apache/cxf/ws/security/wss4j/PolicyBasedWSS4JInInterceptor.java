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

package org.apache.cxf.ws.security.wss4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NodeList;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.MapNamespaceContext;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.AsymmetricBinding;
import org.apache.cxf.ws.security.policy.model.ContentEncryptedElements;
import org.apache.cxf.ws.security.policy.model.Header;
import org.apache.cxf.ws.security.policy.model.RequiredElements;
import org.apache.cxf.ws.security.policy.model.RequiredParts;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedElements;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedParts;
import org.apache.cxf.ws.security.policy.model.SymmetricBinding;
import org.apache.cxf.ws.security.policy.model.Token;
import org.apache.cxf.ws.security.policy.model.UsernameToken;
import org.apache.cxf.ws.security.policy.model.Wss11;
import org.apache.cxf.ws.security.policy.model.X509Token;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageScope;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.WSUsernameTokenPrincipal;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandlerConstants;

/**
 * 
 */
public class PolicyBasedWSS4JInInterceptor extends WSS4JInInterceptor {
    public static final String PROPERTIES_CACHE = "ws-security.properties.cache";

    /**
     * 
     */
    public PolicyBasedWSS4JInInterceptor() {
        super(true);
    }
    
    protected static Map<Object, Properties> getPropertiesCache(SoapMessage message) {
        EndpointInfo info = message.getExchange().get(Endpoint.class).getEndpointInfo();
        synchronized (info) {
            Map<Object, Properties> o = CastUtils.cast((Map<?, ?>)message
                                                       .getContextualProperty(PROPERTIES_CACHE));
            if (o == null) {
                o = new ConcurrentHashMap<Object, Properties>();
                info.setProperty(PROPERTIES_CACHE, o);
            }
            return o;
        }
    }

    private static Properties getProps(Object o, SoapMessage message) {
        Properties properties = getPropertiesCache(message).get(o);
        if (properties != null) {
            return properties;
        }
        if (o instanceof Properties) {
            properties = (Properties)o;
        } else if (o instanceof String) {
            ResourceManager rm = message.getExchange().get(Bus.class).getExtension(ResourceManager.class);
            URL url = rm.resolveResource((String)o, URL.class);
            try {
                if (url == null) {
                    url = ClassLoaderUtils.getResource((String)o, AbstractWSS4JInterceptor.class);
                }
                if (url != null) {
                    properties = new Properties();
                    InputStream ins = url.openStream();
                    properties.load(ins);
                    ins.close();
                }
            } catch (IOException e) {
                properties = null;
            }
        } else if (o instanceof URL) {
            properties = new Properties();
            try {
                InputStream ins = ((URL)o).openStream();
                properties.load(ins);
                ins.close();
            } catch (IOException e) {
                properties = null;
            }            
        }
        if (properties != null) {
            getPropertiesCache(message).put(o, properties);
        }
        return properties;
    }
    
    private boolean containsPolicy(AssertionInfoMap aim, 
                                     QName n) {
        Collection<AssertionInfo> ais = aim.getAssertionInfo(n);
        return ais != null && !ais.isEmpty();
    }
    private void handleWSS11(AssertionInfoMap aim, SoapMessage message) {
        if (!isRequestor(message)) {
            assertPolicy(aim, SP12Constants.WSS11);
            return;
        }
        message.put(WSHandlerConstants.ENABLE_SIGNATURE_CONFIRMATION, "false");
        Collection<AssertionInfo> ais = aim.get(SP12Constants.WSS11);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                Wss11 wss11 = (Wss11)ai.getAssertion();
                if (wss11.isRequireSignatureConfirmation()) {
                    message.put(WSHandlerConstants.ENABLE_SIGNATURE_CONFIRMATION,
                                "true");
                } else {
                    ai.setAsserted(true);
                }
            }
        }
    }

    private String addToAction(String action, String val, boolean pre) {
        if (action.contains(val)) {
            return action;
        }
        if (pre) {
            return val + " " + action; 
        } 
        return action + " " + val;
    }
    private boolean assertPolicy(AssertionInfoMap aim, QName q) {
        Collection<AssertionInfo> ais = aim.get(q);
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }    
            return true;
        }
        return false;
    }
    private void assertPolicy(AssertionInfoMap aim, Token token, boolean derived) {
        if (!derived && token instanceof X509Token && token.isDerivedKeys()) {
            notAssertPolicy(aim, token, "No derived keys found.");
        }
    }
    private void assertPolicy(AssertionInfoMap aim, PolicyAssertion token) {
        Collection<AssertionInfo> ais = aim.get(token.getName());
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == token) {
                    ai.setAsserted(true);
                }
            }    
        }
    }
    private void notAssertPolicy(AssertionInfoMap aim, PolicyAssertion token, String msg) {
        Collection<AssertionInfo> ais = aim.get(token.getName());
        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == token) {
                    ai.setNotAsserted(msg);
                }
            }    
        }
    }

    private String checkAsymetricBinding(AssertionInfoMap aim, 
                                 String action, 
                                 SoapMessage message) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.ASYMMETRIC_BINDING);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                AsymmetricBinding abinding = (AsymmetricBinding)ai.getAssertion();
                if (abinding.getProtectionOrder() == SPConstants.ProtectionOrder.EncryptBeforeSigning) {
                    action = addToAction(action, "Signature", true);
                    action = addToAction(action, "Encrypt", true);
                } else {
                    action = addToAction(action, "Encrypt", true);
                    action = addToAction(action, "Signature", true);
                }
                Object s = message.getContextualProperty(SecurityConstants.SIGNATURE_PROPERTIES);
                Object e = message.getContextualProperty(SecurityConstants.ENCRYPT_PROPERTIES);
                if (s != null) {
                    message.put("decryptionPropRefId", "RefId-" + s.toString());
                    message.put("RefId-" + s.toString(), getProps(s, message));
                    if (e == null) {
                        e = s;
                    }
                }
                if (e != null) {
                    message.put("SignaturePropRefId", "RefId-" + e.toString());
                    message.put("RefId-" + e.toString(), getProps(e, message));
                }
            }
        }
     
        return action;
    }
    private String checkTransportBinding(AssertionInfoMap aim, 
                                         String action, 
                                         SoapMessage message) {
        if (isRequestor(message) && StringUtils.isEmpty(action)) {
            //for a TransportBinding, these won't come back in the response
            assertPolicy(aim, SP12Constants.TRANSPORT_BINDING);
            assertPolicy(aim, SP12Constants.TRANSPORT_TOKEN);
            assertPolicy(aim, SP12Constants.SUPPORTING_TOKENS);
        }
        return action;
    }
    private String checkSymetricBinding(AssertionInfoMap aim, 
                                String action, 
                                SoapMessage message) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.SYMMETRIC_BINDING);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                SymmetricBinding abinding = (SymmetricBinding)ai.getAssertion();
                if (abinding.getProtectionOrder() == SPConstants.ProtectionOrder.EncryptBeforeSigning) {
                    action = addToAction(action, "Signature", true);
                    action = addToAction(action, "Encrypt", true);
                } else {
                    action = addToAction(action, "Encrypt", true);
                    action = addToAction(action, "Signature", true);
                }
                Object s = message.getContextualProperty(SecurityConstants.SIGNATURE_PROPERTIES);
                Object e = message.getContextualProperty(SecurityConstants.ENCRYPT_PROPERTIES);
                if (abinding.getProtectionToken() != null) {
                    if (e != null && s == null) {
                        s = e;
                    } else if (s != null && e == null) {
                        e = s;
                    }
                }
                if (isRequestor(message)) {
                    if (e != null) {
                        message.put("SignaturePropRefId", "RefId-" + e.toString());
                        message.put("RefId-" + e.toString(), getProps(e, message));
                    }
                    if (s != null) {
                        message.put("decryptionPropRefId", "RefId-" + s.toString());
                        message.put("RefId-" + s.toString(), getProps(s, message));
                    }
                } else {
                    if (s != null) {
                        message.put("SignaturePropRefId", "RefId-" + s.toString());
                        message.put("RefId-" + s.toString(), getProps(s, message));
                    }
                    if (e != null) {
                        message.put("decryptionPropRefId", "RefId-" + e.toString());
                        message.put("RefId-" + e.toString(), getProps(e, message));
                    }
                }
            }
        }
        return action;
    }
    
    private void assertXPathTokens(AssertionInfoMap aim, 
                                   QName name, 
                                   Collection<WSDataRef> refs,
                                   SoapMessage msg,
                                   SOAPMessage doc,
                                   CoverageType type,
                                   CoverageScope scope) throws SOAPException {
        Collection<AssertionInfo> ais = aim.get(name);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
                Map<String, String> namespaces = null;
                List<String> xpaths = null;
                if (CoverageScope.CONTENT.equals(scope)) {
                    ContentEncryptedElements p = (ContentEncryptedElements)ai.getAssertion();
                    namespaces = p.getDeclaredNamespaces();
                    xpaths = p.getXPathExpressions();
                } else {
                    SignedEncryptedElements p = (SignedEncryptedElements)ai.getAssertion();
                    namespaces = p.getDeclaredNamespaces();
                    xpaths = p.getXPathExpressions();
                }
                
                if (xpaths != null) {
                    for (String xPath : xpaths) {
                        try {
                            CryptoCoverageUtil.checkCoverage(doc, refs,
                                    namespaces, xPath, type, scope);
                        } catch (WSSecurityException e) {
                            ai.setNotAsserted("No " + type 
                                    + " element found matching XPath " + xPath);
                        }
                    }
                }
            }
        }
    }

    
    private void assertTokens(AssertionInfoMap aim, 
                              QName name, 
                              Collection<WSDataRef> signed,
                              SoapMessage msg,
                              SOAPMessage doc,
                              CoverageType type) throws SOAPException {
        Collection<AssertionInfo> ais = aim.get(name);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
                SignedEncryptedParts p = (SignedEncryptedParts)ai.getAssertion();
                
                if (p.isBody()) {
                    try {
                        if (CoverageType.SIGNED.equals(type)) {
                            CryptoCoverageUtil.checkBodyCoverage(doc, signed, type, CoverageScope.ELEMENT);
                        } else {
                            CryptoCoverageUtil.checkBodyCoverage(doc, signed, type, CoverageScope.CONTENT);
                        }
                    } catch (WSSecurityException e) {
                        ai.setNotAsserted(msg.getVersion().getBody() + " not " + type);
                    }
                }
                
                for (Header h : p.getHeaders()) {
                    try {
                        CryptoCoverageUtil.checkHeaderCoverage(doc, signed, h
                                .getNamespace(), h.getName(), type,
                                CoverageScope.ELEMENT);
                    } catch (WSSecurityException e) {
                        ai.setNotAsserted(h.getQName() + " not + " + type);
                    }
                }
            }
        }
    }
    
    protected void computeAction(SoapMessage message, RequestData data) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        // extract Assertion information
        String action = getString(WSHandlerConstants.ACTION, message);
        if (action == null) {
            action = "";
        }
        if (aim != null) {
            if (containsPolicy(aim, SP12Constants.INCLUDE_TIMESTAMP)) {
                action = addToAction(action, WSHandlerConstants.TIMESTAMP, true);
            }
            if (containsPolicy(aim, SP12Constants.USERNAME_TOKEN)) {
                if (isRequestor(message)) {
                    assertPolicy(aim, SP12Constants.USERNAME_TOKEN);
                } else {
                    action = addToAction(action, WSHandlerConstants.USERNAME_TOKEN, true);
                }
            }
            
            //relatively irrelevant stuff from a verification standpoint
            assertPolicy(aim, SP12Constants.LAYOUT);
            assertPolicy(aim, SP12Constants.WSS10);
            assertPolicy(aim, SP12Constants.TRUST_13);
            assertPolicy(aim, SP11Constants.TRUST_10);
            
            //things that DO impact setup
            handleWSS11(aim, message);
            action = checkAsymetricBinding(aim, action, message);
            action = checkSymetricBinding(aim, action, message);
            action = checkTransportBinding(aim, action, message);
            
            //stuff we can default to asserted and un-assert if a condition isn't met
            assertPolicy(aim, SP12Constants.KEYVALUE_TOKEN);
            assertPolicy(aim, SP12Constants.X509_TOKEN);

            message.put(WSHandlerConstants.ACTION, action.trim());
        }
    }
    
    enum Protections {
        NONE,
        SIGN,
        ENCRYPT,
        SIGN_ENCRYPT,
        ENCRYPT_SIGN,
        ENCRYPT_SIGN_PROTECT,
    };
    private Protections addSign(Protections prots) {
        if (prots == Protections.NONE) {
            return Protections.SIGN;
        }
        if (prots == Protections.ENCRYPT) {
            return Protections.ENCRYPT_SIGN;
        }
        return prots;
    }
    private Protections addEncrypt(Protections prots) {
        if (prots == Protections.NONE) {
            return Protections.ENCRYPT;
        }
        if (prots == Protections.SIGN) {
            return Protections.SIGN_ENCRYPT;
        }
        if (prots == Protections.ENCRYPT_SIGN
            || prots == Protections.SIGN_ENCRYPT) {
            return Protections.ENCRYPT_SIGN_PROTECT;
        }
        return prots;
    }
    
    protected void doResults(SoapMessage msg, String actor, 
                             SOAPMessage doc, Vector results) 
        throws SOAPException, XMLStreamException, WSSecurityException {
        
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        Collection<WSDataRef> signed = new HashSet<WSDataRef>();
        Collection<WSDataRef> encrypted = new HashSet<WSDataRef>();
        boolean hasDerivedKeys = false;
        boolean hasEndorsement = false;
        Protections prots = Protections.NONE;
        
        for (int j = 0; j < results.size(); j++) {
            WSSecurityEngineResult wser =
                    (WSSecurityEngineResult) results.get(j);
            Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
            switch (actInt.intValue()) {                    
            case WSConstants.SIGN:
                List<WSDataRef> sl = CastUtils.cast((List<?>)wser
                                                       .get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
                if (sl != null) {
                    if (sl.size() == 1
                        && sl.get(0).getName().equals(new QName(WSConstants.SIG_NS, WSConstants.SIG_LN))) {
                        //endorsing the signature
                        hasEndorsement = true;
                        break;
                    }
                    for (WSDataRef r : sl) {
                        signed.add(r);
                    }
                    prots = addSign(prots);
                }
                break;
            case WSConstants.ENCR:
                List<WSDataRef> el = CastUtils.cast((List<?>)wser
                                                       .get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
                if (el != null) {
                    for (WSDataRef r : el) {
                        encrypted.add(r);
                    }
                    prots = addEncrypt(prots);
                }
                break;
            case WSConstants.UT:
                Collection<AssertionInfo> ais = aim.get(SP12Constants.USERNAME_TOKEN);
                if (ais != null) {
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }
                    WSUsernameTokenPrincipal princ 
                        = (WSUsernameTokenPrincipal)wser.get(WSSecurityEngineResult.TAG_PRINCIPAL);
                    for (AssertionInfo ai : ais) {
                        UsernameToken tok = (UsernameToken)ai.getAssertion();
                        if (tok.isHashPassword() != princ.isPasswordDigest()) {
                            ai.setNotAsserted("Password hashing policy not enforced");
                        }
                    }
                }
                break;
            case WSConstants.TS:
                assertPolicy(aim, SP12Constants.INCLUDE_TIMESTAMP);
                break;
            case WSConstants.DKT:
                hasDerivedKeys = true;
                break;
            case WSConstants.SC:
                assertPolicy(aim, SP12Constants.WSS11);
                break;
            default:
                //System.out.println(actInt);
                //anything else to process?  Maybe check tokens for BKT requirements?
            }                        
        }
        
        CryptoCoverageUtil.reconcileEncryptedSignedRefs(signed, encrypted);
        
        assertTokens(aim, SP12Constants.SIGNED_PARTS, signed, msg, doc, CoverageType.SIGNED);
        assertTokens(aim, SP12Constants.ENCRYPTED_PARTS, encrypted, msg, doc, CoverageType.ENCRYPTED);
        assertXPathTokens(aim, SP12Constants.SIGNED_ELEMENTS, signed, msg, doc,
                CoverageType.SIGNED, CoverageScope.ELEMENT);
        assertXPathTokens(aim, SP12Constants.ENCRYPTED_ELEMENTS, encrypted, msg, doc,
                CoverageType.ENCRYPTED, CoverageScope.ELEMENT);
        assertXPathTokens(aim, SP12Constants.CONTENT_ENCRYPTED_ELEMENTS, encrypted, msg, doc,
                CoverageType.ENCRYPTED, CoverageScope.CONTENT);
        
        assertHeadersExists(aim, msg, doc);

        assertAsymetricBinding(aim, msg, doc, prots, hasDerivedKeys);
        assertSymetricBinding(aim, msg, doc, prots, hasDerivedKeys);
        assertTransportBinding(aim);
        
        
        //REVISIT - probably can verify some of these like if UT is encrypted and/or signed, etc...
        assertPolicy(aim, SP12Constants.SIGNED_SUPPORTING_TOKENS);
        assertPolicy(aim, SP12Constants.SIGNED_ENCRYPTED_SUPPORTING_TOKENS);
        assertPolicy(aim, SP12Constants.SUPPORTING_TOKENS);
        assertPolicy(aim, SP12Constants.ENCRYPTED_SUPPORTING_TOKENS);
        if (hasEndorsement || isRequestor(msg)) {
            assertPolicy(aim, SP12Constants.ENDORSING_SUPPORTING_TOKENS);
            assertPolicy(aim, SP12Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS);
            assertPolicy(aim, SP12Constants.ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
            assertPolicy(aim, SP12Constants.SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS);
        }
        
        super.doResults(msg, actor, doc, results);
    }
    private void assertHeadersExists(AssertionInfoMap aim, SoapMessage msg, SOAPMessage doc) 
        throws SOAPException {
        
        SOAPHeader header = doc.getSOAPHeader();
        Collection<AssertionInfo> ais = aim.get(SP12Constants.REQUIRED_PARTS);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                RequiredParts rp = (RequiredParts)ai.getAssertion();
                ai.setAsserted(true);
                for (Header h : rp.getHeaders()) {
                    if (header == null || DOMUtils.getFirstChildWithName(header, h.getQName()) == null) {
                        ai.setNotAsserted("No header element of name " + h.getQName() + " found.");
                    }
                }
            }
        }
        ais = aim.get(SP12Constants.REQUIRED_ELEMENTS);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                RequiredElements rp = (RequiredElements)ai.getAssertion();
                ai.setAsserted(true);
                Map<String, String> namespaces = rp.getDeclaredNamespaces();
                XPathFactory factory = XPathFactory.newInstance();
                for (String expression : rp.getXPathExpressions()) {
                    XPath xpath = factory.newXPath();
                    if (namespaces != null) {
                        xpath.setNamespaceContext(new MapNamespaceContext(namespaces));
                    }
                    NodeList list;
                    try {
                        list = (NodeList)xpath.evaluate(expression, 
                                                                 header,
                                                                 XPathConstants.NODESET);
                        if (list.getLength() == 0) {
                            ai.setNotAsserted("No header element matching XPath " + expression + " found.");
                        }
                    } catch (XPathExpressionException e) {
                        ai.setNotAsserted("Invalid XPath expression " + expression + " " + e.getMessage());
                    }
                }
            }
        }
        
    }

    private boolean assertSymetricBinding(AssertionInfoMap aim, 
                                           SoapMessage message,
                                           SOAPMessage doc,
                                           Protections prots,
                                           boolean derived) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.SYMMETRIC_BINDING);
        if (ais == null) {
            return true;
        }
        
        for (AssertionInfo ai : ais) {
            SymmetricBinding abinding = (SymmetricBinding)ai.getAssertion();
            ai.setAsserted(true);
            if (abinding.getProtectionOrder() == SPConstants.ProtectionOrder.EncryptBeforeSigning) {
                if (abinding.isSignatureProtection()) {
                    if (prots == Protections.ENCRYPT_SIGN
                        || prots == Protections.SIGN_ENCRYPT) {
                        ai.setNotAsserted("Not encrypted before signed and then protected");
                    }
                } else if (prots == Protections.SIGN_ENCRYPT) {
                    ai.setNotAsserted("Not encrypted before signed");                    
                }
            } else if (prots == Protections.ENCRYPT_SIGN) {
                ai.setNotAsserted("Not signed before encrypted");                                    
            }
            
            if (abinding.getEncryptionToken() != null) {
                assertPolicy(aim, abinding.getEncryptionToken());
                assertPolicy(aim, abinding.getEncryptionToken().getToken(), derived);
            }
            if (abinding.getSignatureToken() != null) {
                assertPolicy(aim, abinding.getSignatureToken());
                assertPolicy(aim, abinding.getSignatureToken().getToken(), derived);
            }
            if (abinding.getProtectionToken() != null) {
                assertPolicy(aim, abinding.getProtectionToken());
                assertPolicy(aim, abinding.getProtectionToken().getToken(), derived);
            }
        }
        return true;
    }
    private boolean assertAsymetricBinding(AssertionInfoMap aim, 
                                           SoapMessage message,
                                           SOAPMessage doc,
                                           Protections prots,
                                           boolean derived) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.ASYMMETRIC_BINDING);
        if (ais == null) {                       
            return true;
        }
        for (AssertionInfo ai : ais) {
            AsymmetricBinding abinding = (AsymmetricBinding)ai.getAssertion();
            ai.setAsserted(true);
            if (abinding.getProtectionOrder() == SPConstants.ProtectionOrder.EncryptBeforeSigning) {
                if (abinding.isSignatureProtection()) {
                    if (prots == Protections.ENCRYPT_SIGN
                        || prots == Protections.SIGN_ENCRYPT) {
                        ai.setNotAsserted("Not encrypted before signed and then protected");
                    }
                } else if (prots == Protections.SIGN_ENCRYPT) {
                    ai.setNotAsserted("Not encrypted before signed");                    
                }
            } else if (prots == Protections.ENCRYPT_SIGN) {
                ai.setNotAsserted("Not signed before encrypted");                                    
            }
            assertPolicy(aim, abinding.getInitiatorToken());
            assertPolicy(aim, abinding.getRecipientToken());
            assertPolicy(aim, abinding.getInitiatorToken().getToken(), derived);
            assertPolicy(aim, abinding.getRecipientToken().getToken(), derived);
        }
        return true;
    }
    private boolean assertTransportBinding(AssertionInfoMap aim) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.TRANSPORT_BINDING);
        if (ais == null) {                       
            return true;
        }
        assertPolicy(aim, SP12Constants.TRANSPORT_TOKEN);
        assertPolicy(aim, SP12Constants.ENCRYPTED_PARTS);
        assertPolicy(aim, SP12Constants.SIGNED_PARTS);
        return !assertPolicy(aim, SP12Constants.TRANSPORT_BINDING);
    }

}
