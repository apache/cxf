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
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.DefaultSecurityContext;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.SamlToken;
import org.apache.cxf.ws.security.policy.model.Token;

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.components.crypto.CryptoFactory;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.processor.SAMLTokenProcessor;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.SAMLParms;
import org.apache.ws.security.validate.Validator;

import org.opensaml.common.SAMLVersion;

/**
 * An interceptor to create and add a SAML token to the security header of an outbound
 * request, and to process a SAML Token on an inbound request.
 */
public class SamlTokenInterceptor extends AbstractTokenInterceptor {

    public SamlTokenInterceptor() {
        super();
    }
    
    protected void processToken(SoapMessage message) {
        Header h = findSecurityHeader(message, false);
        if (h == null) {
            return;
        }
        Element el = (Element)h.getObject();
        Element child = DOMUtils.getFirstElement(el);
        while (child != null) {
            if ("Assertion".equals(child.getLocalName())
                && (WSConstants.SAML_NS.equals(child.getNamespaceURI())
                    || WSConstants.SAML2_NS.equals(child.getNamespaceURI()))) {
                try {
                    List<WSSecurityEngineResult> samlResults = processToken(child, message);
                    if (samlResults != null) {
                        List<WSHandlerResult> results = CastUtils.cast((List<?>)message
                                .get(WSHandlerConstants.RECV_RESULTS));
                        if (results == null) {
                            results = new ArrayList<WSHandlerResult>();
                            message.put(WSHandlerConstants.RECV_RESULTS, results);
                        }
                        WSHandlerResult rResult = new WSHandlerResult(null, samlResults);
                        results.add(0, rResult);

                        boolean signed = false;
                        for (WSSecurityEngineResult result : samlResults) {
                            AssertionWrapper wrapper = 
                                (AssertionWrapper)result.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
                            if (wrapper.isSigned()) {
                                signed = true;
                                break;
                            }
                        }
                        assertTokens(message, SP12Constants.SAML_TOKEN, signed);
                        
                        // Check version against policy
                        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
                        for (AssertionInfo ai : aim.getAssertionInfo(SP12Constants.SAML_TOKEN)) {
                            SamlToken samlToken = (SamlToken)ai.getAssertion();
                            for (WSSecurityEngineResult result : samlResults) {
                                AssertionWrapper assertionWrapper = 
                                    (AssertionWrapper)result.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);

                                if (!checkVersion(samlToken, assertionWrapper)) {
                                    ai.setNotAsserted("Wrong SAML Version");
                                }
                                
                                TLSSessionInfo tlsInfo = message.get(TLSSessionInfo.class);
                                Certificate[] tlsCerts = null;
                                if (tlsInfo != null) {
                                    tlsCerts = tlsInfo.getPeerCertificates();
                                }
                                if (!SAMLUtils.checkHolderOfKey(assertionWrapper, null, tlsCerts)) {
                                    ai.setNotAsserted("Assertion fails holder-of-key requirements");
                                    continue;
                                }
                                if (!SAMLUtils.checkSenderVouches(assertionWrapper, tlsCerts, null, null)) {
                                    ai.setNotAsserted("Assertion fails sender-vouches requirements");
                                    continue;
                                }
                            }
                        }
                        
                        if (signed) {
                            Principal principal = 
                                (Principal)samlResults.get(0).get(WSSecurityEngineResult.TAG_PRINCIPAL);
                            message.put(WSS4JInInterceptor.PRINCIPAL_RESULT, principal);                   
                            
                            SecurityContext sc = message.get(SecurityContext.class);
                            if (sc == null || sc.getUserPrincipal() == null) {
                                message.put(SecurityContext.class, new DefaultSecurityContext(principal, null));
                            }
                        }
                    }
                } catch (WSSecurityException ex) {
                    throw new Fault(ex);
                }
            }
            child = DOMUtils.getNextElement(child);
        }
    }

    private List<WSSecurityEngineResult> processToken(Element tokenElement, final SoapMessage message)
        throws WSSecurityException {
        WSDocInfo wsDocInfo = new WSDocInfo(tokenElement.getOwnerDocument());
        RequestData data = new RequestData() {
            public CallbackHandler getCallbackHandler() {
                return getCallback(message);
            }
            public Validator getValidator(QName qName) throws WSSecurityException {
                String key = null;
                if (WSSecurityEngine.SAML_TOKEN.equals(qName)) {
                    key = SecurityConstants.SAML1_TOKEN_VALIDATOR;
                } else if (WSSecurityEngine.SAML2_TOKEN.equals(qName)) {
                    key = SecurityConstants.SAML2_TOKEN_VALIDATOR;
                } 
                if (key != null) {
                    Object o = message.getContextualProperty(key);
                    try {
                        if (o instanceof Validator) {
                            return (Validator)o;
                        } else if (o instanceof Class) {
                            return (Validator)((Class<?>)o).newInstance();
                        } else if (o instanceof String) {
                            return (Validator)ClassLoaderUtils.loadClass(o.toString(),
                                                                         SamlTokenInterceptor.class)
                                                                         .newInstance();
                        }
                    } catch (RuntimeException t) {
                        throw t;
                    } catch (Throwable t) {
                        throw new WSSecurityException(t.getMessage(), t);
                    }
                }
                return super.getValidator(qName);
            }
        };
        data.setWssConfig(WSSConfig.getNewInstance());
        
        data.setSigCrypto(getCrypto(null, SecurityConstants.SIGNATURE_CRYPTO,
                                     SecurityConstants.SIGNATURE_PROPERTIES, message));
        
        SAMLTokenProcessor p = new SAMLTokenProcessor();
        List<WSSecurityEngineResult> results = 
            p.handleToken(tokenElement, data, wsDocInfo);
        return results;
    }

    protected Token assertTokens(SoapMessage message) {
        return assertTokens(message, SP12Constants.SAML_TOKEN, true);
    }

    protected void addToken(SoapMessage message) {
        WSSConfig.init();
        SamlToken tok = (SamlToken)assertTokens(message);

        Header h = findSecurityHeader(message, true);
        try {
            AssertionWrapper wrapper = addSamlToken(tok, message);
            if (wrapper == null) {
                AssertionInfoMap aim = message.get(AssertionInfoMap.class);
                Collection<AssertionInfo> ais = aim.getAssertionInfo(SP12Constants.SAML_TOKEN);
                for (AssertionInfo ai : ais) {
                    if (ai.isAsserted()) {
                        ai.setAsserted(false);
                    }
                }
                return;
            }
            Element el = (Element)h.getObject();
            el.appendChild(wrapper.toDOM(el.getOwnerDocument()));
        } catch (WSSecurityException ex) {
            policyNotAsserted(tok, ex.getMessage(), message);
        }
    }

    
    private AssertionWrapper addSamlToken(
        SamlToken token, SoapMessage message
    ) throws WSSecurityException {
        //
        // Get the SAML CallbackHandler
        //
        Object o = message.getContextualProperty(SecurityConstants.SAML_CALLBACK_HANDLER);

        CallbackHandler handler = null;
        if (o instanceof CallbackHandler) {
            handler = (CallbackHandler)o;
        } else if (o instanceof String) {
            try {
                handler = (CallbackHandler)ClassLoaderUtils
                    .loadClass((String)o, this.getClass()).newInstance();
            } catch (Exception e) {
                handler = null;
            }
        }
        if (handler == null) {
            return null;
        }

        SAMLParms samlParms = new SAMLParms();
        samlParms.setCallbackHandler(handler);
        if (token.isUseSamlVersion11Profile10() || token.isUseSamlVersion11Profile11()) {
            samlParms.setSAMLVersion(SAMLVersion.VERSION_11);
        } else if (token.isUseSamlVersion20Profile11()) {
            samlParms.setSAMLVersion(SAMLVersion.VERSION_20);
        }
        AssertionWrapper assertion = new AssertionWrapper(samlParms);

        boolean selfSignAssertion = 
            MessageUtils.getContextualBoolean(
                message, SecurityConstants.SELF_SIGN_SAML_ASSERTION, false
            );
        if (selfSignAssertion) {
            Crypto crypto = 
                getCrypto(
                    token, SecurityConstants.SIGNATURE_CRYPTO,
                    SecurityConstants.SIGNATURE_PROPERTIES, message
                );

            String userNameKey = SecurityConstants.SIGNATURE_USERNAME;
            String user = (String)message.getContextualProperty(userNameKey);
            if (crypto != null && StringUtils.isEmpty(user)) {
                try {
                    user = crypto.getDefaultX509Identifier();
                } catch (WSSecurityException e1) {
                    throw new Fault(e1);
                }
            }
            if (StringUtils.isEmpty(user)) {
                return null;
            }

            String password = (String)message.getContextualProperty(SecurityConstants.PASSWORD);
            if (StringUtils.isEmpty(password)) {
                password = getPassword(user, token, WSPasswordCallback.SIGNATURE, message);
            }

            // TODO configure using a KeyValue here
            assertion.signAssertion(user, password, crypto, false);
        }

        return assertion;
    }

    private Crypto getCrypto(
        SamlToken samlToken, 
        String cryptoKey, 
        String propKey,
        SoapMessage message
    ) throws WSSecurityException {
        Crypto crypto = (Crypto)message.getContextualProperty(cryptoKey);
        if (crypto != null) {
            return crypto;
        }

        Object o = message.getContextualProperty(propKey);
        if (o == null) {
            return null;
        }

        Properties properties = null;
        if (o instanceof Properties) {
            properties = (Properties)o;
        } else if (o instanceof String) {
            ResourceManager rm = message.getExchange().get(Bus.class).getExtension(ResourceManager.class);
            URL url = rm.resolveResource((String)o, URL.class);
            try {
                if (url == null) {
                    url = ClassLoaderUtils.getResource((String)o, this.getClass());
                }
                if (url == null) {
                    try {
                        url = new URL((String)o);
                    } catch (Exception ex) {
                        //ignore
                    }
                }
                if (url != null) {
                    InputStream ins = url.openStream();
                    properties = new Properties();
                    properties.load(ins);
                    ins.close();
                } else if (samlToken != null) {
                    policyNotAsserted(samlToken, "Could not find properties file " + o, message);
                }
            } catch (IOException e) {
                if (samlToken != null) {
                    policyNotAsserted(samlToken, e.getMessage(), message);
                }
            }
        } else if (o instanceof URL) {
            properties = new Properties();
            try {
                InputStream ins = ((URL)o).openStream();
                properties.load(ins);
                ins.close();
            } catch (IOException e) {
                if (samlToken != null) {
                    policyNotAsserted(samlToken, e.getMessage(), message);
                }
            }            
        }

        if (properties != null) {
            crypto = CryptoFactory.getInstance(properties);
        }
        return crypto;
    }

    /**
     * Check the policy version against the received assertion
     */
    private boolean checkVersion(SamlToken samlToken, AssertionWrapper assertionWrapper) {
        if ((samlToken.isUseSamlVersion11Profile10()
            || samlToken.isUseSamlVersion11Profile11())
            && assertionWrapper.getSamlVersion() != SAMLVersion.VERSION_11) {
            return false;
        } else if (samlToken.isUseSamlVersion20Profile11()
            && assertionWrapper.getSamlVersion() != SAMLVersion.VERSION_20) {
            return false;
        }
        return true;
    }
    
}
