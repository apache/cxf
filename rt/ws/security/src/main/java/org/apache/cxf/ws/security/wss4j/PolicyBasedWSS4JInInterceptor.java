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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.MapNamespaceContext;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.ContentEncryptedElements;
import org.apache.cxf.ws.security.policy.model.Header;
import org.apache.cxf.ws.security.policy.model.RequiredElements;
import org.apache.cxf.ws.security.policy.model.RequiredParts;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedElements;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedParts;
import org.apache.cxf.ws.security.policy.model.Wss11;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageScope;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.apache.cxf.ws.security.wss4j.policyvalidators.AsymmetricBindingPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.EncryptedTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.EndorsingEncryptedTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.EndorsingTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SamlTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SecurityContextTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SignedEncryptedTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SignedEndorsingEncryptedTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SignedEndorsingTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SignedTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SymmetricBindingPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.TransportBindingPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.UsernameTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.WSS11PolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.X509TokenPolicyValidator;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * 
 */
public class PolicyBasedWSS4JInInterceptor extends WSS4JInInterceptor {
    public static final String PROPERTIES_CACHE = "ws-security.properties.cache";
    public static final PolicyBasedWSS4JInInterceptor INSTANCE 
        = new PolicyBasedWSS4JInInterceptor();

    /**
     * 
     */
    public PolicyBasedWSS4JInInterceptor() {
        super(true);
    }
    
    protected static Map<Object, Properties> getPropertiesCache(SoapMessage message) {
        EndpointInfo info = message.getExchange().get(Endpoint.class).getEndpointInfo();
        synchronized (info) {
            Map<Object, Properties> o = 
                CastUtils.cast((Map<?, ?>)message.getContextualProperty(PROPERTIES_CACHE));
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
            URL url = null;
            ResourceManager rm = message.getExchange().get(Bus.class).getExtension(ResourceManager.class);
            url = rm.resolveResource((String)o, URL.class);
            try {
                if (url == null) {
                    url = ClassLoaderUtils.getResource((String)o, AbstractWSS4JInterceptor.class);
                }
                if (url == null) {
                    url = new URL((String)o);
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
    
    private void handleWSS11(AssertionInfoMap aim, SoapMessage message) {
        if (isRequestor(message)) {
            message.put(WSHandlerConstants.ENABLE_SIGNATURE_CONFIRMATION, "false");
            Collection<AssertionInfo> ais = aim.get(SP12Constants.WSS11);
            if (ais != null) {
                for (AssertionInfo ai : ais) {
                    Wss11 wss11 = (Wss11)ai.getAssertion();
                    if (wss11.isRequireSignatureConfirmation()) {
                        message.put(WSHandlerConstants.ENABLE_SIGNATURE_CONFIRMATION, "true");
                        break;
                    }
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

    private String checkAsymmetricBinding(
        AssertionInfoMap aim, String action, SoapMessage message
    ) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.ASYMMETRIC_BINDING);
        if (ais == null || ais.isEmpty()) {
            return action;
        }
        
        action = addToAction(action, "Signature", true);
        action = addToAction(action, "Encrypt", true);
        Object s = message.getContextualProperty(SecurityConstants.SIGNATURE_PROPERTIES);
        Object e = message.getContextualProperty(SecurityConstants.ENCRYPT_PROPERTIES);
        if (s != null) {
            message.put(WSHandlerConstants.DEC_PROP_REF_ID, "RefId-" + s.toString());
            message.put("RefId-" + s.toString(), getProps(s, message));
            if (e == null) {
                e = s;
            }
        }
        if (e != null) {
            message.put(WSHandlerConstants.SIG_PROP_REF_ID, "RefId-" + e.toString());
            message.put("RefId-" + e.toString(), getProps(e, message));
        }
     
        return action;
    }
    
    private String checkTransportBinding(
        AssertionInfoMap aim, String action, SoapMessage message
    ) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.TRANSPORT_BINDING);
        if (ais == null || ais.isEmpty()) {
            return action;
        }
        
        action = addToAction(action, "Signature", true);
        action = addToAction(action, "Encrypt", true);
        Object s = message.getContextualProperty(SecurityConstants.SIGNATURE_PROPERTIES);
        Object e = message.getContextualProperty(SecurityConstants.ENCRYPT_PROPERTIES);
        if (s != null) {
            message.put(WSHandlerConstants.DEC_PROP_REF_ID, "RefId-" + s.toString());
            message.put("RefId-" + s.toString(), getProps(s, message));
            if (e == null) {
                e = s;
            }
        }
        if (e != null) {
            message.put(WSHandlerConstants.SIG_PROP_REF_ID, "RefId-" + e.toString());
            message.put("RefId-" + e.toString(), getProps(e, message));
        }

        return action;
    }
    
    private String checkSymmetricBinding(
        AssertionInfoMap aim, String action, SoapMessage message
    ) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.SYMMETRIC_BINDING);
        if (ais == null || ais.isEmpty()) {
            return action;
        }
        
        action = addToAction(action, "Signature", true);
        action = addToAction(action, "Encrypt", true);
        Object s = message.getContextualProperty(SecurityConstants.SIGNATURE_PROPERTIES);
        Object e = message.getContextualProperty(SecurityConstants.ENCRYPT_PROPERTIES);
        if (e != null && s == null) {
            s = e;
        } else if (s != null && e == null) {
            e = s;
        }
        
        if (isRequestor(message)) {
            if (e != null) {
                message.put(WSHandlerConstants.SIG_PROP_REF_ID, "RefId-" + e.toString());
                message.put("RefId-" + e.toString(), getProps(e, message));
            }
            if (s != null) {
                message.put(WSHandlerConstants.DEC_PROP_REF_ID, "RefId-" + s.toString());
                message.put("RefId-" + s.toString(), getProps(s, message));
            }
        } else {
            if (s != null) {
                message.put(WSHandlerConstants.SIG_PROP_REF_ID, "RefId-" + s.toString());
                message.put("RefId-" + s.toString(), getProps(s, message));
            }
            if (e != null) {
                message.put(WSHandlerConstants.DEC_PROP_REF_ID, "RefId-" + e.toString());
                message.put("RefId-" + e.toString(), getProps(e, message));
            }
        }
        
        return action;
    }
    
    private boolean assertXPathTokens(AssertionInfoMap aim, 
                                   QName name, 
                                   Collection<WSDataRef> refs,
                                   SoapMessage msg,
                                   Element soapEnvelope,
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
                            CryptoCoverageUtil.checkCoverage(soapEnvelope, refs,
                                    namespaces, xPath, type, scope);
                        } catch (WSSecurityException e) {
                            ai.setNotAsserted("No " + type 
                                    + " element found matching XPath " + xPath);
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    
    private boolean assertTokens(AssertionInfoMap aim, 
                              QName name, 
                              Collection<WSDataRef> signed,
                              SoapMessage msg,
                              Element soapHeader,
                              Element soapBody,
                              CoverageType type) throws SOAPException {
        Collection<AssertionInfo> ais = aim.get(name);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
                SignedEncryptedParts p = (SignedEncryptedParts)ai.getAssertion();
                
                if (p.isBody()) {
                    try {
                        if (CoverageType.SIGNED.equals(type)) {
                            CryptoCoverageUtil.checkBodyCoverage(
                                soapBody, signed, type, CoverageScope.ELEMENT
                            );
                        } else {
                            CryptoCoverageUtil.checkBodyCoverage(
                                soapBody, signed, type, CoverageScope.CONTENT
                            );
                        }
                    } catch (WSSecurityException e) {
                        ai.setNotAsserted(msg.getVersion().getBody() + " not " + type);
                        return false;
                    }
                }
                
                for (Header h : p.getHeaders()) {
                    try {
                        CryptoCoverageUtil.checkHeaderCoverage(soapHeader, signed, h
                                .getNamespace(), h.getName(), type,
                                CoverageScope.ELEMENT);
                    } catch (WSSecurityException e) {
                        ai.setNotAsserted(h.getQName() + " not + " + type);
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    protected void computeAction(SoapMessage message, RequestData data) {
        String action = getString(WSHandlerConstants.ACTION, message);
        if (action == null) {
            action = "";
        }
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        if (aim != null) {
            //things that DO impact setup
            handleWSS11(aim, message);
            action = checkAsymmetricBinding(aim, action, message);
            action = checkSymmetricBinding(aim, action, message);
            action = checkTransportBinding(aim, action, message);
            
            // stuff we can default to asserted and un-assert if a condition isn't met
            assertPolicy(aim, SP12Constants.KEYVALUE_TOKEN);

            message.put(WSHandlerConstants.ACTION, action.trim());
        }
    }
    
    @Override
    protected void doResults(
        SoapMessage msg, 
        String actor,
        Element soapHeader,
        Element soapBody,
        List<WSSecurityEngineResult> results, 
        boolean utWithCallbacks
    ) throws SOAPException, XMLStreamException, WSSecurityException {
        AssertionInfoMap aim = msg.get(AssertionInfoMap.class);
        Collection<WSDataRef> signed = new HashSet<WSDataRef>();
        Collection<WSDataRef> encrypted = new HashSet<WSDataRef>();
        
        //
        // Pre-fetch various results
        //
        List<WSSecurityEngineResult> signedResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.SIGN, signedResults);
        for (WSSecurityEngineResult result : signedResults) {
            List<WSDataRef> sl = 
                CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
            if (sl != null) {
                for (WSDataRef r : sl) {
                    signed.add(r);
                }
            }
        }
        
        List<WSSecurityEngineResult> encryptResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.ENCR, encryptResults);
        for (WSSecurityEngineResult result : encryptResults) {
            List<WSDataRef> sl = 
                CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
            if (sl != null) {
                for (WSDataRef r : sl) {
                    encrypted.add(r);
                }
            }
        }
        
        /*
        WSSecurityEngineResult tsResult = WSSecurityUtil.fetchActionResult(results, WSConstants.TS);
        Element timestamp = null;
        if (tsResult != null) {
            Timestamp ts = (Timestamp)tsResult.get(WSSecurityEngineResult.TAG_TIMESTAMP);
            timestamp = ts.getElement();
        }
        */
        
        //
        // Check policies
        //
        boolean check = checkSignedEncryptedCoverage(aim, msg, soapHeader, soapBody, signed, encrypted);
        
        if (check) {
            check = checkTokenCoverage(aim, msg, soapBody, results, signedResults, utWithCallbacks);
        }
        
        if (check) {
            check = checkBindingCoverage(aim, msg, results, signedResults);
        }

        if (check) {
            check = checkSupportingTokenCoverage(aim, msg, results, signedResults, utWithCallbacks);
        }
        
        // The supporting tokens are already validated
        assertPolicy(aim, SP12Constants.SUPPORTING_TOKENS);
        
        // relatively irrelevant stuff from a verification standpoint
        assertPolicy(aim, SP12Constants.LAYOUT);
        assertPolicy(aim, SP12Constants.WSS10);
        assertPolicy(aim, SP12Constants.TRUST_13);
        assertPolicy(aim, SP11Constants.TRUST_10);
        
        super.doResults(msg, actor, soapHeader, soapBody, results, utWithCallbacks);
    }
    
    /**
     * Check SignedParts, EncryptedParts, SignedElements, EncryptedElements, RequiredParts, etc.
     */
    private boolean checkSignedEncryptedCoverage(
        AssertionInfoMap aim,
        SoapMessage msg,
        Element soapHeader,
        Element soapBody,
        Collection<WSDataRef> signed, 
        Collection<WSDataRef> encrypted
    ) throws SOAPException {
        CryptoCoverageUtil.reconcileEncryptedSignedRefs(signed, encrypted);
        //
        // SIGNED_PARTS and ENCRYPTED_PARTS only apply to non-Transport bindings
        //
        boolean check = true;
        if (!isTransportBinding(aim)) {
            check &= assertTokens(
                aim, SP12Constants.SIGNED_PARTS, signed, msg, soapHeader, soapBody, CoverageType.SIGNED
            );
            check &= assertTokens(
                aim, SP12Constants.ENCRYPTED_PARTS, encrypted, msg, soapHeader, soapBody, 
                CoverageType.ENCRYPTED
            );
        }
        Element soapEnvelope = soapHeader.getOwnerDocument().getDocumentElement();
        check &= assertXPathTokens(aim, SP12Constants.SIGNED_ELEMENTS, signed, msg, soapEnvelope,
                CoverageType.SIGNED, CoverageScope.ELEMENT);
        check &= assertXPathTokens(aim, SP12Constants.ENCRYPTED_ELEMENTS, encrypted, msg, soapEnvelope,
                CoverageType.ENCRYPTED, CoverageScope.ELEMENT);
        check &= assertXPathTokens(aim, SP12Constants.CONTENT_ENCRYPTED_ELEMENTS, encrypted, msg, 
                soapEnvelope, CoverageType.ENCRYPTED, CoverageScope.CONTENT);
        
        check &= assertHeadersExists(aim, msg, soapHeader);
        return check;
    }
    
    /**
     * Check the token coverage
     */
    private boolean checkTokenCoverage(
        AssertionInfoMap aim,
        SoapMessage msg,
        Element soapBody,
        List<WSSecurityEngineResult> results, 
        List<WSSecurityEngineResult> signedResults, 
        boolean utWithCallbacks
    ) {
        List<WSSecurityEngineResult> utResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.UT, utResults);
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.UT_NOPASSWORD, utResults);
        
        List<WSSecurityEngineResult> samlResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.ST_SIGNED, samlResults);
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.ST_UNSIGNED, samlResults);
        
        boolean check = true;
        X509TokenPolicyValidator x509Validator = new X509TokenPolicyValidator(msg, results);
        check &= x509Validator.validatePolicy(aim);
        
        if (utWithCallbacks) {
            UsernameTokenPolicyValidator utValidator = 
                new UsernameTokenPolicyValidator(msg, results);
            check &= utValidator.validatePolicy(aim);
        } else {
            Collection<AssertionInfo> ais = aim.get(SP12Constants.USERNAME_TOKEN);
            if (ais != null) {
                for (AssertionInfo ai : ais) {
                    ai.setAsserted(true);
                }
            }
        }
        
        SamlTokenPolicyValidator samlValidator = 
            new SamlTokenPolicyValidator(soapBody, signedResults, msg, results);
        check &= samlValidator.validatePolicy(aim);
        
        SecurityContextTokenPolicyValidator sctValidator = 
            new SecurityContextTokenPolicyValidator(msg, results);
        check &= sctValidator.validatePolicy(aim);
        
        WSS11PolicyValidator wss11Validator = new WSS11PolicyValidator(msg, results);
        check &= wss11Validator.validatePolicy(aim);
        
        return check;
    }
    
    /**
     * Check the binding coverage
     */
    private boolean checkBindingCoverage(
        AssertionInfoMap aim, 
        SoapMessage msg,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults
    ) {
        boolean check = true;
        
        TransportBindingPolicyValidator transportValidator = 
            new TransportBindingPolicyValidator(msg, results, signedResults);
        check &= transportValidator.validatePolicy(aim);
            
        SymmetricBindingPolicyValidator symmetricValidator = 
            new SymmetricBindingPolicyValidator(msg, results, signedResults);
        check &= symmetricValidator.validatePolicy(aim);

        AsymmetricBindingPolicyValidator asymmetricValidator = 
            new AsymmetricBindingPolicyValidator(msg, results, signedResults);
        check &= asymmetricValidator.validatePolicy(aim);
        
        return check;
    }
    
    /**
     * Check the supporting token coverage
     */
    private boolean checkSupportingTokenCoverage(
        AssertionInfoMap aim,
        SoapMessage msg,
        List<WSSecurityEngineResult> results, 
        List<WSSecurityEngineResult> signedResults, 
        boolean utWithCallbacks
    ) {
        boolean check = true;
        
        SignedTokenPolicyValidator suppValidator = 
            new SignedTokenPolicyValidator(msg, results, signedResults);
        suppValidator.setValidateUsernameToken(utWithCallbacks);
        check &= suppValidator.validatePolicy(aim);

        EndorsingTokenPolicyValidator endorsingValidator = 
            new EndorsingTokenPolicyValidator(msg, results, signedResults);
        check &= endorsingValidator.validatePolicy(aim);

        SignedEndorsingTokenPolicyValidator signedEdorsingValidator = 
            new SignedEndorsingTokenPolicyValidator(msg, results, signedResults);
        check &= signedEdorsingValidator.validatePolicy(aim);

        SignedEncryptedTokenPolicyValidator signedEncryptedValidator = 
            new SignedEncryptedTokenPolicyValidator(msg, results, signedResults);
        signedEncryptedValidator.setValidateUsernameToken(utWithCallbacks);
        check &= signedEncryptedValidator.validatePolicy(aim);

        EncryptedTokenPolicyValidator encryptedValidator = 
            new EncryptedTokenPolicyValidator(msg, results, signedResults);
        encryptedValidator.setValidateUsernameToken(utWithCallbacks);
        check &= encryptedValidator.validatePolicy(aim);

        EndorsingEncryptedTokenPolicyValidator endorsingEncryptedValidator = 
            new EndorsingEncryptedTokenPolicyValidator(msg, results, signedResults);
        endorsingEncryptedValidator.setValidateUsernameToken(utWithCallbacks);
        check &= endorsingEncryptedValidator.validatePolicy(aim);

        SignedEndorsingEncryptedTokenPolicyValidator signedEndorsingEncryptedValidator = 
            new SignedEndorsingEncryptedTokenPolicyValidator(msg, results, signedResults);
        check &= signedEndorsingEncryptedValidator.validatePolicy(aim);
        
        return check;
    }
    
    private boolean assertHeadersExists(AssertionInfoMap aim, SoapMessage msg, Node header) 
        throws SOAPException {
        
        Collection<AssertionInfo> ais = aim.get(SP12Constants.REQUIRED_PARTS);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                RequiredParts rp = (RequiredParts)ai.getAssertion();
                ai.setAsserted(true);
                for (Header h : rp.getHeaders()) {
                    if (header == null 
                        || DOMUtils.getFirstChildWithName((Element)header, h.getQName()) == null) {
                        ai.setNotAsserted("No header element of name " + h.getQName() + " found.");
                        return false;
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
                            return false;
                        }
                    } catch (XPathExpressionException e) {
                        ai.setNotAsserted("Invalid XPath expression " + expression + " " + e.getMessage());
                        return false;
                    }
                }
            }
        }
        
        return true;
    }

    private boolean isTransportBinding(AssertionInfoMap aim) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.TRANSPORT_BINDING);
        if (ais != null && ais.size() > 0) {
            ais = aim.get(SP12Constants.SYMMETRIC_BINDING);
            if (ais != null && ais.size() > 0) {
                return false;
            }
            ais = aim.get(SP12Constants.ASYMMETRIC_BINDING);
            if (ais != null && ais.size() > 0) {
                return false;
            }
            return true;
        }
        return false;
    }

}
