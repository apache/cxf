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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.DefaultSecurityContext;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.cxf.ws.security.policy.model.UsernameToken;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.WSUsernameTokenPrincipal;
import org.apache.ws.security.cache.ReplayCache;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.WSSecUsernameToken;
import org.apache.ws.security.processor.UsernameTokenProcessor;
import org.apache.ws.security.validate.Validator;

/**
 * 
 */
public class UsernameTokenInterceptor extends AbstractTokenInterceptor {

    public UsernameTokenInterceptor() {
        super();
    }

    protected void processToken(SoapMessage message) {
        Header h = findSecurityHeader(message, false);
        if (h == null) {
            return;
        }
        boolean utWithCallbacks = 
            MessageUtils.getContextualBoolean(message, SecurityConstants.VALIDATE_TOKEN, true);
        
        Element el = (Element)h.getObject();
        Element child = DOMUtils.getFirstElement(el);
        while (child != null) {
            if (SPConstants.USERNAME_TOKEN.equals(child.getLocalName())
                && WSConstants.WSSE_NS.equals(child.getNamespaceURI())) {
                try {
                    Principal principal = null;
                    Subject subject = null;
                    if (utWithCallbacks) {
                        final WSSecurityEngineResult result = validateToken(child, message);
                        principal = (Principal)result.get(WSSecurityEngineResult.TAG_PRINCIPAL);
                        subject = (Subject)result.get(WSSecurityEngineResult.TAG_SUBJECT);
                    } else {
                        boolean bspCompliant = isWsiBSPCompliant(message);
                        principal = parseTokenAndCreatePrincipal(child, bspCompliant);
                        WSS4JTokenConverter.convertToken(message, principal);
                    }
                    
                    SecurityContext sc = message.get(SecurityContext.class);
                    if (sc == null || sc.getUserPrincipal() == null) {
                        if (subject != null && principal != null) {
                            message.put(SecurityContext.class, 
                                    createSecurityContext(principal, subject));
                        } else if (principal instanceof WSUsernameTokenPrincipal) {
                            WSUsernameTokenPrincipal utPrincipal = (WSUsernameTokenPrincipal)principal;
                            subject = createSubject(utPrincipal.getName(), utPrincipal.getPassword(),
                                    utPrincipal.isPasswordDigest(), utPrincipal.getNonce(), 
                                    utPrincipal.getCreatedTime());
                            message.put(SecurityContext.class, 
                                    createSecurityContext(utPrincipal, subject));
                        }
                    }
                    
                    if (principal instanceof WSUsernameTokenPrincipal) {
                        storeResults((WSUsernameTokenPrincipal)principal, message);
                    }
                } catch (WSSecurityException ex) {
                    throw new Fault(ex);
                }
            }
            child = DOMUtils.getNextElement(child);
        }
    }
    
    private void storeResults(WSUsernameTokenPrincipal principal, SoapMessage message) {
        List<WSSecurityEngineResult> v = new ArrayList<WSSecurityEngineResult>();
        int action = WSConstants.UT;
        if (principal.getPassword() == null) {
            action = WSConstants.UT_NOPASSWORD;
        }
        v.add(0, new WSSecurityEngineResult(action, principal, null, null, null));
        List<WSHandlerResult> results = CastUtils.cast((List<?>)message
                                                  .get(WSHandlerConstants.RECV_RESULTS));
        if (results == null) {
            results = new ArrayList<WSHandlerResult>();
            message.put(WSHandlerConstants.RECV_RESULTS, results);
        }
        WSHandlerResult rResult = new WSHandlerResult(null, v);
        results.add(0, rResult);

        assertTokens(message, principal, false);
        message.put(WSS4JInInterceptor.PRINCIPAL_RESULT, principal);   
    }

    @Deprecated
    protected WSUsernameTokenPrincipal getPrincipal(Element tokenElement, final SoapMessage message)
        throws WSSecurityException {
        return null;
    }

    protected WSSecurityEngineResult validateToken(Element tokenElement, final SoapMessage message)
        throws WSSecurityException {
        UsernameTokenProcessor p = new UsernameTokenProcessor();
        WSDocInfo wsDocInfo = new WSDocInfo(tokenElement.getOwnerDocument());
        RequestData data = new RequestData() {
            public CallbackHandler getCallbackHandler() {
                return getCallback(message);
            }
            public Validator getValidator(QName qName) throws WSSecurityException {
                Object validator = 
                    message.getContextualProperty(SecurityConstants.USERNAME_TOKEN_VALIDATOR);
                if (validator == null) {
                    return super.getValidator(qName);
                }
                return (Validator)validator;
            }
        };
        
        // Configure replay caching
        ReplayCache nonceCache = 
            WSS4JUtils.getReplayCache(
                message, SecurityConstants.ENABLE_NONCE_CACHE, SecurityConstants.NONCE_CACHE_INSTANCE
            );
        data.setNonceReplayCache(nonceCache);
        
        WSSConfig config = WSSConfig.getNewInstance();
        boolean bspCompliant = isWsiBSPCompliant(message);
        boolean allowNoPassword = isAllowNoPassword(message.get(AssertionInfoMap.class));
        config.setWsiBSPCompliant(bspCompliant);
        config.setAllowUsernameTokenNoPassword(allowNoPassword);
        data.setWssConfig(config);
        List<WSSecurityEngineResult> results = 
            p.handleToken(tokenElement, data, wsDocInfo);
        
        return results.get(0);
    }
    
    protected WSUsernameTokenPrincipal parseTokenAndCreatePrincipal(Element tokenElement, boolean bspCompliant) 
        throws WSSecurityException {
        org.apache.ws.security.message.token.UsernameToken ut = 
            new org.apache.ws.security.message.token.UsernameToken(tokenElement, false, bspCompliant);
        
        WSUsernameTokenPrincipal principal = new WSUsernameTokenPrincipal(ut.getName(), ut.isHashed());
        principal.setNonce(ut.getNonce());
        principal.setPassword(ut.getPassword());
        principal.setCreatedTime(ut.getCreated());
        principal.setPasswordType(ut.getPasswordType());

        return principal;
    }
    
    protected boolean isWsiBSPCompliant(final SoapMessage message) {
        String bspc = (String)message.getContextualProperty(SecurityConstants.IS_BSP_COMPLIANT);
        // Default to WSI-BSP compliance enabled
        return !("false".equals(bspc) || "0".equals(bspc));
    }
    
    private boolean isAllowNoPassword(AssertionInfoMap aim) throws WSSecurityException {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.USERNAME_TOKEN);

        if (ais != null && !ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                UsernameToken policy = (UsernameToken)ai.getAssertion();
                if (policy.isNoPassword()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    protected SecurityContext createSecurityContext(final Principal p, Subject subject) {
        return new DefaultSecurityContext(p, subject);
    }
    
    /**
     * Create a Subject representing a current user and its roles. 
     * This Subject is expected to contain at least one Principal representing a user
     * and optionally followed by one or more principal Groups this user is a member of.
     * @param name username
     * @param password password
     * @param isDigest true if a password digest is used
     * @param nonce optional nonce
     * @param created optional timestamp
     * @return subject
     * @throws SecurityException
     */
    protected Subject createSubject(String name, 
                                    String password, 
                                    boolean isDigest,
                                    String nonce,
                                    String created) throws SecurityException {
        return null;
    }
    
    protected UsernameToken assertTokens(SoapMessage message) {
        return (UsernameToken)assertTokens(message, SP12Constants.USERNAME_TOKEN, true);
    }
    
    private UsernameToken assertTokens(
        SoapMessage message, 
        WSUsernameTokenPrincipal princ,
        boolean signed
    ) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = aim.getAssertionInfo(SP12Constants.USERNAME_TOKEN);
        UsernameToken tok = null;
        for (AssertionInfo ai : ais) {
            tok = (UsernameToken)ai.getAssertion();
            if (princ != null && tok.isHashPassword() != princ.isPasswordDigest()) {
                ai.setNotAsserted("Password hashing policy not enforced");
            } else if (princ != null && !tok.isNoPassword() && (princ.getPassword() == null)
                && isNonEndorsingSupportingToken(tok)) {
                ai.setNotAsserted("Username Token No Password supplied");
            } else {
                ai.setAsserted(true);         
            }
        }
        ais = aim.getAssertionInfo(SP12Constants.SUPPORTING_TOKENS);
        for (AssertionInfo ai : ais) {
            ai.setAsserted(true);
        }
        if (signed || isTLSInUse(message)) {
            ais = aim.getAssertionInfo(SP12Constants.SIGNED_SUPPORTING_TOKENS);
            for (AssertionInfo ai : ais) {
                ai.setAsserted(true);
            }
        }
        return tok;
    }
    
    /**
     * Return true if this UsernameToken policy is a (non-endorsing)SupportingToken. If this is
     * true then the corresponding UsernameToken must have a password element.
     */
    private boolean isNonEndorsingSupportingToken(
        org.apache.cxf.ws.security.policy.model.UsernameToken usernameTokenPolicy
    ) {
        SupportingToken supportingToken = usernameTokenPolicy.getSupportingToken();
        if (supportingToken != null) {
            SPConstants.SupportTokenType type = supportingToken.getTokenType();
            if (type == SPConstants.SupportTokenType.SUPPORTING_TOKEN_SUPPORTING
                || type == SPConstants.SupportTokenType.SUPPORTING_TOKEN_SIGNED
                || type == SPConstants.SupportTokenType.SUPPORTING_TOKEN_SIGNED_ENCRYPTED
                || type == SPConstants.SupportTokenType.SUPPORTING_TOKEN_ENCRYPTED) {
                return true;
            }
        }
        return false;
    }

    protected void addToken(SoapMessage message) {
        UsernameToken tok = assertTokens(message);

        Header h = findSecurityHeader(message, true);
        WSSecUsernameToken utBuilder = 
            addUsernameToken(message, tok);
        if (utBuilder == null) {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            Collection<AssertionInfo> ais = aim.getAssertionInfo(SP12Constants.USERNAME_TOKEN);
            for (AssertionInfo ai : ais) {
                if (ai.isAsserted()) {
                    ai.setAsserted(false);
                }
            }
            return;
        }
        Element el = (Element)h.getObject();
        utBuilder.prepare(el.getOwnerDocument());
        el.appendChild(utBuilder.getUsernameTokenElement());
    }


    protected WSSecUsernameToken addUsernameToken(SoapMessage message, UsernameToken token) {
        String userName = (String)message.getContextualProperty(SecurityConstants.USERNAME);
        WSSConfig wssConfig = (WSSConfig)message.getContextualProperty(WSSConfig.class.getName());
        if (wssConfig == null) {
            wssConfig = WSSConfig.getNewInstance();
        }

        if (!StringUtils.isEmpty(userName)) {
            // If NoPassword property is set we don't need to set the password
            if (token.isNoPassword()) {
                WSSecUsernameToken utBuilder = new WSSecUsernameToken(wssConfig);
                utBuilder.setUserInfo(userName, null);
                utBuilder.setPasswordType(null);
                return utBuilder;
            }
            
            String password = (String)message.getContextualProperty(SecurityConstants.PASSWORD);
            if (StringUtils.isEmpty(password)) {
                password = getPassword(userName, token, WSPasswordCallback.USERNAME_TOKEN, message);
            }
            
            if (!StringUtils.isEmpty(password)) {
                //If the password is available then build the token
                WSSecUsernameToken utBuilder = new WSSecUsernameToken(wssConfig);
                if (token.isHashPassword()) {
                    utBuilder.setPasswordType(WSConstants.PASSWORD_DIGEST);  
                } else {
                    utBuilder.setPasswordType(WSConstants.PASSWORD_TEXT);
                }
                
                if (token.isRequireCreated()) {
                    utBuilder.addCreated();
                }
                if (token.isRequireNonce()) {
                    utBuilder.addNonce();
                }
                
                utBuilder.setUserInfo(userName, password);
                return utBuilder;
            } else {
                policyNotAsserted(token, "No username available", message);
            }
        } else {
            policyNotAsserted(token, "No username available", message);
        }
        return null;
    }

    
}
