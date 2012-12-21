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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

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
import org.apache.cxf.common.logging.LogUtils;
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
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.Binding;
import org.apache.cxf.ws.security.policy.model.ContentEncryptedElements;
import org.apache.cxf.ws.security.policy.model.Header;
import org.apache.cxf.ws.security.policy.model.RequiredElements;
import org.apache.cxf.ws.security.policy.model.RequiredParts;
import org.apache.cxf.ws.security.policy.model.SamlToken;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedElements;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedParts;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.cxf.ws.security.policy.model.Wss11;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageScope;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.apache.cxf.ws.security.wss4j.policyvalidators.AsymmetricBindingPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.BindingPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.ConcreteSupportingTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.EncryptedTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.EndorsingEncryptedTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.EndorsingTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SamlTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SecurityContextTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SignedEncryptedTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SignedEndorsingEncryptedTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SignedEndorsingTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SignedTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SupportingTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SymmetricBindingPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.TokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.TransportBindingPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.UsernameTokenPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.WSS11PolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.X509TokenPolicyValidator;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.AlgorithmSuite;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.message.token.Timestamp;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * 
 */
public class PolicyBasedWSS4JInInterceptor extends WSS4JInInterceptor {
    public static final PolicyBasedWSS4JInInterceptor INSTANCE 
        = new PolicyBasedWSS4JInInterceptor();
    private static final Logger LOG = LogUtils.getL7dLogger(PolicyBasedWSS4JInInterceptor.class);

    /**
     * 
     */
    public PolicyBasedWSS4JInInterceptor() {
        super(true);
    }
    
    private static Properties getProps(Object o, String propsKey, URL propsURL, SoapMessage message) {
        Properties properties = null;
        if (o instanceof Properties) {
            properties = (Properties)o;
        } else if (propsURL != null) {
            try {
                properties = new Properties();
                InputStream ins = propsURL.openStream();
                properties.load(ins);
                ins.close();
            } catch (IOException e) {
                properties = null;
            }
        }
        
        return properties;
    }
    
    private URL getPropertiesFileURL(Object o, SoapMessage message) {
        if (o instanceof String) {
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
                return url;
            } catch (IOException e) {
                // Do nothing
            }
        } else if (o instanceof URL) {
            return (URL)o;        
        }
        return null;
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
    ) throws WSSecurityException {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.ASYMMETRIC_BINDING);
        if (ais == null || ais.isEmpty()) {
            return action;
        }
        
        action = addToAction(action, "Signature", true);
        action = addToAction(action, "Encrypt", true);
        Object s = message.getContextualProperty(SecurityConstants.SIGNATURE_CRYPTO);
        if (s == null) {
            s = message.getContextualProperty(SecurityConstants.SIGNATURE_PROPERTIES);
        }
        Object e = message.getContextualProperty(SecurityConstants.ENCRYPT_CRYPTO);
        if (e == null) {
            e = message.getContextualProperty(SecurityConstants.ENCRYPT_PROPERTIES);
        }
        
        Crypto encrCrypto = getEncryptionCrypto(e, message);
        Crypto signCrypto = null;
        if (e != null && e.equals(s)) {
            signCrypto = encrCrypto;
        } else {
            signCrypto = getSignatureCrypto(s, message);
        }
        
        if (signCrypto != null) {
            message.put(WSHandlerConstants.DEC_PROP_REF_ID, "RefId-" + signCrypto.hashCode());
            message.put("RefId-" + signCrypto.hashCode(), signCrypto);
        }
        
        if (encrCrypto != null) {
            message.put(WSHandlerConstants.SIG_PROP_REF_ID, "RefId-" + encrCrypto.hashCode());
            message.put("RefId-" + encrCrypto.hashCode(), (Crypto)encrCrypto);
        } else if (signCrypto != null) {
            message.put(WSHandlerConstants.SIG_PROP_REF_ID, "RefId-" + signCrypto.hashCode());
            message.put("RefId-" + signCrypto.hashCode(), (Crypto)signCrypto);
        }
     
        return action;
    }
    
    private String checkTransportBinding(
        AssertionInfoMap aim, String action, SoapMessage message
    ) throws WSSecurityException {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.TRANSPORT_BINDING);
        if (ais == null || ais.isEmpty()) {
            return action;
        }
        
        action = addToAction(action, "Signature", true);
        action = addToAction(action, "Encrypt", true);
        Object s = message.getContextualProperty(SecurityConstants.SIGNATURE_CRYPTO);
        if (s == null) {
            s = message.getContextualProperty(SecurityConstants.SIGNATURE_PROPERTIES);
        }
        Object e = message.getContextualProperty(SecurityConstants.ENCRYPT_CRYPTO);
        if (e == null) {
            e = message.getContextualProperty(SecurityConstants.ENCRYPT_PROPERTIES);
        }
        
        Crypto encrCrypto = getEncryptionCrypto(e, message);
        Crypto signCrypto = null;
        if (e != null && e.equals(s)) {
            signCrypto = encrCrypto;
        } else {
            signCrypto = getSignatureCrypto(s, message);
        }
        
        if (signCrypto != null) {
            message.put(WSHandlerConstants.DEC_PROP_REF_ID, "RefId-" + signCrypto.hashCode());
            message.put("RefId-" + signCrypto.hashCode(), signCrypto);
        }
        
        if (encrCrypto != null) {
            message.put(WSHandlerConstants.SIG_PROP_REF_ID, "RefId-" + encrCrypto.hashCode());
            message.put("RefId-" + encrCrypto.hashCode(), (Crypto)encrCrypto);
        } else if (signCrypto != null) {
            message.put(WSHandlerConstants.SIG_PROP_REF_ID, "RefId-" + signCrypto.hashCode());
            message.put("RefId-" + signCrypto.hashCode(), (Crypto)signCrypto);
        }

        return action;
    }
    
    private String checkSymmetricBinding(
        AssertionInfoMap aim, String action, SoapMessage message
    ) throws WSSecurityException {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.SYMMETRIC_BINDING);
        if (ais == null || ais.isEmpty()) {
            return action;
        }
        
        action = addToAction(action, "Signature", true);
        action = addToAction(action, "Encrypt", true);
        Object s = message.getContextualProperty(SecurityConstants.SIGNATURE_CRYPTO);
        if (s == null) {
            s = message.getContextualProperty(SecurityConstants.SIGNATURE_PROPERTIES);
        }
        Object e = message.getContextualProperty(SecurityConstants.ENCRYPT_CRYPTO);
        if (e == null) {
            e = message.getContextualProperty(SecurityConstants.ENCRYPT_PROPERTIES);
        }
        
        Crypto encrCrypto = getEncryptionCrypto(e, message);
        Crypto signCrypto = null;
        if (e != null && e.equals(s)) {
            signCrypto = encrCrypto;
        } else {
            signCrypto = getSignatureCrypto(s, message);
        }
        
        if (isRequestor(message)) {
            Crypto crypto = encrCrypto;
            if (crypto == null) {
                crypto = signCrypto;
            }
            if (crypto != null) {
                message.put(WSHandlerConstants.SIG_PROP_REF_ID, "RefId-" + crypto.hashCode());
                message.put("RefId-" + crypto.hashCode(), crypto);
            }
            
            crypto = signCrypto;
            if (crypto == null) {
                crypto = encrCrypto;
            }
            if (crypto != null) {
                message.put(WSHandlerConstants.DEC_PROP_REF_ID, "RefId-" + crypto.hashCode());
                message.put("RefId-" + crypto.hashCode(), crypto);
            }
        } else {
            Crypto crypto = signCrypto;
            if (crypto == null) {
                crypto = encrCrypto;
            }
            if (crypto != null) {
                message.put(WSHandlerConstants.SIG_PROP_REF_ID, "RefId-" + crypto.hashCode());
                message.put("RefId-" + crypto.hashCode(), crypto);
            }
            
            crypto = encrCrypto;
            if (crypto == null) {
                crypto = signCrypto;
            }
            if (crypto != null) {
                message.put(WSHandlerConstants.DEC_PROP_REF_ID, "RefId-" + crypto.hashCode());
                message.put("RefId-" + crypto.hashCode(), crypto);
            }
        }
        
        return action;
    }
    
    private Crypto getEncryptionCrypto(Object e, SoapMessage message) throws WSSecurityException {
        Crypto encrCrypto = null;
        if (e instanceof Crypto) {
            encrCrypto = (Crypto)e;
        } else if (e != null) {
            URL propsURL = getPropertiesFileURL(e, message);
            String propsKey = e.toString();
            if (propsURL != null) {
                propsKey = propsURL.getPath();
            }
            Properties props = getProps(e, propsKey, propsURL, message);
            encrCrypto = CryptoFactory.getInstance(props);
            
            EndpointInfo info = message.getExchange().get(Endpoint.class).getEndpointInfo();
            synchronized (info) {
                info.setProperty(SecurityConstants.ENCRYPT_CRYPTO, encrCrypto);
            }
        }
        return encrCrypto;
    }
    
    private Crypto getSignatureCrypto(Object s, SoapMessage message) throws WSSecurityException {
        Crypto signCrypto = null;
        if (s instanceof Crypto) {
            signCrypto = (Crypto)s;
        } else if (s != null) {
            URL propsURL = getPropertiesFileURL(s, message);
            String propsKey = s.toString();
            if (propsURL != null) {
                propsKey = propsURL.getPath();
            }
            Properties props = getProps(s, propsKey, propsURL, message);
            signCrypto = CryptoFactory.getInstance(props);
            
            EndpointInfo info = message.getExchange().get(Endpoint.class).getEndpointInfo();
            synchronized (info) {
                info.setProperty(SecurityConstants.SIGNATURE_CRYPTO, signCrypto);
            }
        }
        return signCrypto;
    }
    
    private boolean assertXPathTokens(AssertionInfoMap aim, 
                                   QName name, 
                                   Collection<WSDataRef> refs,
                                   Element soapEnvelope,
                                   CoverageType type,
                                   CoverageScope scope,
                                   final XPath xpath) throws SOAPException {
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
                    if (namespaces != null) {
                        xpath.setNamespaceContext(new MapNamespaceContext(namespaces));
                    }
                    try {
                        CryptoCoverageUtil.checkCoverage(soapEnvelope, refs,
                                xpath, xpaths, type, scope);
                    } catch (WSSecurityException e) {
                        ai.setNotAsserted("No " + type 
                                + " element found matching one of the XPaths " 
                                + Arrays.toString(xpaths.toArray()));
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
                        continue;
                    }
                }
                
                for (Header h : p.getHeaders()) {
                    try {
                        CryptoCoverageUtil.checkHeaderCoverage(soapHeader, signed, h
                                .getNamespace(), h.getName(), type,
                                CoverageScope.ELEMENT);
                    } catch (WSSecurityException e) {
                        ai.setNotAsserted(h.getQName() + " not + " + type);
                    }
                }
            }
        }
        return true;
    }
    
    
    /**
     * Set a WSS4J AlgorithmSuite object on the RequestData context, to restrict the
     * algorithms that are allowed for encryption, signature, etc.
     */
    protected void setAlgorithmSuites(SoapMessage message, RequestData data) throws WSSecurityException {
        Binding binding = getBinding(message);
        if (binding != null && binding.getAlgorithmSuite() != null) {
            // Translate into WSS4J's AlgorithmSuite class
            AlgorithmSuite algorithmSuite = translateAlgorithmSuite(binding.getAlgorithmSuite());
            data.setAlgorithmSuite(algorithmSuite);
        }

        // Now look for an AlgorithmSuite for a SAML Assertion
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        if (aim != null) {
            Collection<AssertionInfo> ais = aim.get(SP12Constants.SAML_TOKEN);
            if (ais != null && !ais.isEmpty()) {
                for (AssertionInfo ai : ais) {
                    SamlToken samlToken = (SamlToken)ai.getAssertion();
                    SupportingToken supportingToken = samlToken.getSupportingToken();
                    if (supportingToken != null && supportingToken.getAlgorithmSuite() != null) {
                        AlgorithmSuite algorithmSuite = 
                            translateAlgorithmSuite(supportingToken.getAlgorithmSuite());
                        data.setSamlAlgorithmSuite(algorithmSuite);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Translate a CXF AlgorithmSuite object into WSS4J's AlgorithmSuite object
     */
    private AlgorithmSuite translateAlgorithmSuite(
        org.apache.cxf.ws.security.policy.model.AlgorithmSuite cxfAlgorithmSuite
    ) {
        // Translate into WSS4J's AlgorithmSuite class
        AlgorithmSuite algorithmSuite = new AlgorithmSuite();
        algorithmSuite.setEncryptionDerivedKeyLength(
            cxfAlgorithmSuite.getEncryptionDerivedKeyLength());
        algorithmSuite.setSignatureDerivedKeyLength(
            cxfAlgorithmSuite.getSignatureDerivedKeyLength());
        algorithmSuite.setMaximumAsymmetricKeyLength(
            cxfAlgorithmSuite.getMaximumAsymmetricKeyLength());
        algorithmSuite.setMinimumAsymmetricKeyLength(
            cxfAlgorithmSuite.getMinimumAsymmetricKeyLength());
        algorithmSuite.setMaximumSymmetricKeyLength(
            cxfAlgorithmSuite.getMaximumSymmetricKeyLength());
        algorithmSuite.setMinimumSymmetricKeyLength(
            cxfAlgorithmSuite.getMinimumSymmetricKeyLength());

        algorithmSuite.addEncryptionMethod(cxfAlgorithmSuite.getEncryption());
        algorithmSuite.addKeyWrapAlgorithm(cxfAlgorithmSuite.getSymmetricKeyWrap());
        algorithmSuite.addKeyWrapAlgorithm(cxfAlgorithmSuite.getAsymmetricKeyWrap());

        algorithmSuite.addSignatureMethod(cxfAlgorithmSuite.getAsymmetricSignature());
        algorithmSuite.addSignatureMethod(cxfAlgorithmSuite.getSymmetricSignature());
        algorithmSuite.addDigestAlgorithm(cxfAlgorithmSuite.getDigest());
        algorithmSuite.addC14nAlgorithm(cxfAlgorithmSuite.getInclusiveC14n());

        algorithmSuite.addTransformAlgorithm(cxfAlgorithmSuite.getInclusiveC14n());
        algorithmSuite.addTransformAlgorithm(SPConstants.STRT10);
        algorithmSuite.addTransformAlgorithm(WSConstants.NS_XMLDSIG_ENVELOPED_SIGNATURE);

        algorithmSuite.addDerivedKeyAlgorithm(SPConstants.P_SHA1);
        algorithmSuite.addDerivedKeyAlgorithm(SPConstants.P_SHA1_L128);

        return algorithmSuite;
    }

    /**
     * Get the WS-SecurityPolicy Binding that is in operation
     */
    private Binding getBinding(SoapMessage message) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        if (aim != null) {
            Collection<AssertionInfo> ais = aim.get(SP12Constants.TRANSPORT_BINDING);
            if (ais != null && !ais.isEmpty()) {     
                AssertionInfo ai = ais.iterator().next();
                return (Binding)ai.getAssertion();
            }
            ais = aim.get(SP12Constants.ASYMMETRIC_BINDING);
            if (ais != null && !ais.isEmpty()) {     
                AssertionInfo ai = ais.iterator().next();
                return (Binding)ai.getAssertion();
            }
            ais = aim.get(SP12Constants.SYMMETRIC_BINDING);
            if (ais != null && !ais.isEmpty()) {     
                AssertionInfo ai = ais.iterator().next();
                return (Binding)ai.getAssertion();
            }
        }
        return null;
    }
    
    protected void computeAction(SoapMessage message, RequestData data) throws WSSecurityException {
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
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.UT_SIGN, signedResults);
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
        
        //
        // Check policies
        //
        if (!checkSignedEncryptedCoverage(aim, msg, soapHeader, soapBody, signed, encrypted)) {
            LOG.fine("Incoming request failed signed-encrypted policy validation");
        }
        
        if (!checkTokenCoverage(aim, msg, soapBody, results, signedResults)) {
            LOG.fine("Incoming request failed token policy validation");
        }
        
        if (!checkBindingCoverage(aim, msg, soapBody, results, signedResults, encryptResults)) {
            LOG.fine("Incoming request failed binding policy validation");
        }

        if (!checkSupportingTokenCoverage(aim, msg, results, signedResults, 
            encryptResults, utWithCallbacks)) {
            LOG.fine("Incoming request failed supporting token policy validation");
        }
        
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
        if (containsXPathPolicy(aim)) {
            // XPathFactory and XPath are not thread-safe so we must recreate them
            // each request.
            final XPathFactory factory = XPathFactory.newInstance();
            final XPath xpath = factory.newXPath();
            
            check &= assertXPathTokens(aim, SP12Constants.SIGNED_ELEMENTS, signed, soapEnvelope,
                    CoverageType.SIGNED, CoverageScope.ELEMENT, xpath);
            check &= assertXPathTokens(aim, SP12Constants.ENCRYPTED_ELEMENTS, encrypted, soapEnvelope,
                    CoverageType.ENCRYPTED, CoverageScope.ELEMENT, xpath);
            check &= assertXPathTokens(aim, SP12Constants.CONTENT_ENCRYPTED_ELEMENTS, encrypted, 
                    soapEnvelope, CoverageType.ENCRYPTED, CoverageScope.CONTENT, xpath);
        }
        
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
        List<WSSecurityEngineResult> signedResults
    ) {
        boolean check = true;
        TokenPolicyValidator x509Validator = new X509TokenPolicyValidator();
        check &= x509Validator.validatePolicy(aim, msg, soapBody, results, signedResults);
        
        TokenPolicyValidator utValidator = new UsernameTokenPolicyValidator();
        check &= utValidator.validatePolicy(aim, msg, soapBody, results, signedResults);
        
        TokenPolicyValidator samlValidator = new SamlTokenPolicyValidator();
        check &= samlValidator.validatePolicy(aim, msg, soapBody, results, signedResults);
        
        TokenPolicyValidator sctValidator = new SecurityContextTokenPolicyValidator();
        check &= sctValidator.validatePolicy(aim, msg, soapBody, results, signedResults);
        
        TokenPolicyValidator wss11Validator = new WSS11PolicyValidator();
        check &= wss11Validator.validatePolicy(aim, msg, soapBody, results, signedResults);
        
        return check;
    }
    
    /**
     * Check the binding coverage
     */
    private boolean checkBindingCoverage(
        AssertionInfoMap aim, 
        SoapMessage msg,
        Element soapBody,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults,
        List<WSSecurityEngineResult> encryptedResults
    ) {
        boolean check = true;
        
        BindingPolicyValidator transportValidator = new TransportBindingPolicyValidator();
        check &= 
            transportValidator.validatePolicy(
                aim, msg, soapBody, results, signedResults, encryptedResults
            );
            
        BindingPolicyValidator symmetricValidator = new SymmetricBindingPolicyValidator();
        check &= 
            symmetricValidator.validatePolicy(
                aim, msg, soapBody, results, signedResults, encryptedResults
            );

        BindingPolicyValidator asymmetricValidator = new AsymmetricBindingPolicyValidator();
        check &= 
            asymmetricValidator.validatePolicy(
                aim, msg, soapBody, results, signedResults, encryptedResults
            );
        
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
        List<WSSecurityEngineResult> encryptedResults,
        boolean utWithCallbacks
    ) {
        List<WSSecurityEngineResult> utResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.UT, utResults);
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.UT_NOPASSWORD, utResults);
        
        List<WSSecurityEngineResult> samlResults = new ArrayList<WSSecurityEngineResult>();
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.ST_SIGNED, samlResults);
        WSSecurityUtil.fetchAllActionResults(results, WSConstants.ST_UNSIGNED, samlResults);
        
        // Store the timestamp element
        WSSecurityEngineResult tsResult = WSSecurityUtil.fetchActionResult(results, WSConstants.TS);
        Element timestamp = null;
        if (tsResult != null) {
            Timestamp ts = (Timestamp)tsResult.get(WSSecurityEngineResult.TAG_TIMESTAMP);
            timestamp = ts.getElement();
        }
        
        boolean check = true;
        
        SupportingTokenPolicyValidator validator = new ConcreteSupportingTokenPolicyValidator();
        validator.setUsernameTokenResults(utResults, utWithCallbacks);
        validator.setSAMLTokenResults(samlResults);
        validator.setTimestampElement(timestamp);
        check &= validator.validatePolicy(aim, msg, results, signedResults, encryptedResults);
        
        validator = new SignedTokenPolicyValidator();
        validator.setUsernameTokenResults(utResults, utWithCallbacks);
        validator.setSAMLTokenResults(samlResults);
        validator.setTimestampElement(timestamp);
        check &= validator.validatePolicy(aim, msg, results, signedResults, encryptedResults);

        validator = new EndorsingTokenPolicyValidator();
        validator.setUsernameTokenResults(utResults, utWithCallbacks);
        validator.setSAMLTokenResults(samlResults);
        validator.setTimestampElement(timestamp);
        check &= validator.validatePolicy(aim, msg, results, signedResults, encryptedResults);

        validator = new SignedEndorsingTokenPolicyValidator();
        validator.setUsernameTokenResults(utResults, utWithCallbacks);
        validator.setSAMLTokenResults(samlResults);
        validator.setTimestampElement(timestamp);
        check &= validator.validatePolicy(aim, msg, results, signedResults, encryptedResults);

        validator = new SignedEncryptedTokenPolicyValidator();
        validator.setUsernameTokenResults(utResults, utWithCallbacks);
        validator.setSAMLTokenResults(samlResults);
        validator.setTimestampElement(timestamp);
        check &= validator.validatePolicy(aim, msg, results, signedResults, encryptedResults);

        validator = new EncryptedTokenPolicyValidator();
        validator.setUsernameTokenResults(utResults, utWithCallbacks);
        validator.setSAMLTokenResults(samlResults);
        validator.setTimestampElement(timestamp);
        check &= validator.validatePolicy(aim, msg, results, signedResults, encryptedResults);

        validator = new EndorsingEncryptedTokenPolicyValidator();
        validator.setUsernameTokenResults(utResults, utWithCallbacks);
        validator.setSAMLTokenResults(samlResults);
        validator.setTimestampElement(timestamp);
        check &= validator.validatePolicy(aim, msg, results, signedResults, encryptedResults);

        validator = new SignedEndorsingEncryptedTokenPolicyValidator();
        validator.setUsernameTokenResults(utResults, utWithCallbacks);
        validator.setSAMLTokenResults(samlResults);
        validator.setTimestampElement(timestamp);
        check &= validator.validatePolicy(aim, msg, results, signedResults, encryptedResults);
        
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
    
    private boolean containsXPathPolicy(AssertionInfoMap aim) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.SIGNED_ELEMENTS);
        if (ais != null && ais.size() > 0) {
            return true;
        }
        ais = aim.get(SP12Constants.ENCRYPTED_ELEMENTS);
        if (ais != null && ais.size() > 0) {
            return true;
        }
        ais = aim.get(SP12Constants.CONTENT_ENCRYPTED_ELEMENTS);
        if (ais != null && ais.size() > 0) {
            return true;
        }
        return false;
    }

}
