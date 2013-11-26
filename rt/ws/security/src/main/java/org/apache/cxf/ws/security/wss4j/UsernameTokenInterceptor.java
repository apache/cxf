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
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.UsernameTokenPrincipal;
import org.apache.wss4j.common.principal.WSUsernameTokenPrincipalImpl;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.WSSConfig;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.apache.wss4j.dom.bsp.BSPEnforcer;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.WSSecUsernameToken;
import org.apache.wss4j.dom.processor.UsernameTokenProcessor;
import org.apache.wss4j.dom.validate.Validator;
import org.apache.wss4j.policy.SP13Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractSecurityAssertion;
import org.apache.wss4j.policy.model.SupportingTokens;
import org.apache.wss4j.policy.model.UsernameToken;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.utils.Base64;

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
        Element el = (Element)h.getObject();
        Element child = DOMUtils.getFirstElement(el);
        while (child != null) {
            if (SPConstants.USERNAME_TOKEN.equals(child.getLocalName())
                && WSConstants.WSSE_NS.equals(child.getNamespaceURI())) {
                try  {
                    final UsernameTokenPrincipal princ = getPrincipal(child, message);
                    if (princ != null) {
                        List<WSSecurityEngineResult>v = new ArrayList<WSSecurityEngineResult>();
                        int action = WSConstants.UT;
                        if (princ.getPassword() == null) {
                            action = WSConstants.UT_NOPASSWORD;
                        }
                        v.add(0, new WSSecurityEngineResult(action, princ, null, null, null));
                        List<WSHandlerResult> results = CastUtils.cast((List<?>)message
                                                                  .get(WSHandlerConstants.RECV_RESULTS));
                        if (results == null) {
                            results = new ArrayList<WSHandlerResult>();
                            message.put(WSHandlerConstants.RECV_RESULTS, results);
                        }
                        WSHandlerResult rResult = new WSHandlerResult(null, v);
                        results.add(0, rResult);

                        assertTokens(message, princ, false);
                        message.put(WSS4JInInterceptor.PRINCIPAL_RESULT, princ);                   
                        
                        SecurityContext sc = message.get(SecurityContext.class);
                        if (sc == null || sc.getUserPrincipal() == null) {
                            String nonce = null;
                            if (princ.getNonce() != null) {
                                nonce = Base64.encode(princ.getNonce());
                            }
                            Subject subject = createSubject(princ.getName(), princ.getPassword(),
                                princ.isPasswordDigest(), nonce, princ.getCreatedTime());
                            message.put(SecurityContext.class, 
                                        createSecurityContext(princ, subject));
                        }

                    }
                } catch (WSSecurityException ex) {
                    throw new Fault(ex);
                } catch (Base64DecodingException ex) {
                    throw new Fault(ex);
                }
            }
            child = DOMUtils.getNextElement(child);
        }
    }

    protected UsernameTokenPrincipal getPrincipal(Element tokenElement, final SoapMessage message)
        throws WSSecurityException, Base64DecodingException {
        
        boolean bspCompliant = isWsiBSPCompliant(message);
        boolean utWithCallbacks = 
            MessageUtils.getContextualBoolean(message, SecurityConstants.VALIDATE_TOKEN, true);
        boolean allowNoPassword = isAllowNoPassword(message.get(AssertionInfoMap.class));
        if (utWithCallbacks) {
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
            config.setAllowUsernameTokenNoPassword(allowNoPassword);
            data.setWssConfig(config);
            if (!bspCompliant) {
                data.setDisableBSPEnforcement(true);
            }
            List<WSSecurityEngineResult> results = 
                p.handleToken(tokenElement, data, wsDocInfo);
            return (UsernameTokenPrincipal)results.get(0).get(WSSecurityEngineResult.TAG_PRINCIPAL);
        } else {
            UsernameTokenPrincipal principal = parseTokenAndCreatePrincipal(tokenElement, bspCompliant);
            WSS4JTokenConverter.convertToken(message, principal);
            return principal;
        }
    }
    
    protected UsernameTokenPrincipal parseTokenAndCreatePrincipal(Element tokenElement, boolean bspCompliant) 
        throws WSSecurityException, Base64DecodingException {
        BSPEnforcer bspEnforcer = new BSPEnforcer(!bspCompliant);
        org.apache.wss4j.dom.message.token.UsernameToken ut = 
            new org.apache.wss4j.dom.message.token.UsernameToken(tokenElement, false, bspEnforcer);
        
        WSUsernameTokenPrincipalImpl principal = new WSUsernameTokenPrincipalImpl(ut.getName(), ut.isHashed());
        if (ut.getNonce() != null) {
            principal.setNonce(Base64.decode(ut.getNonce()));
        }
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
        Collection<AssertionInfo> ais = getAllAssertionsByLocalname(aim, SPConstants.USERNAME_TOKEN);

        if (!ais.isEmpty()) {
            for (AssertionInfo ai : ais) {
                UsernameToken policy = (UsernameToken)ai.getAssertion();
                if (policy.getPasswordType() == UsernameToken.PasswordType.NoPassword) {
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
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        assertPolicy(aim, SPConstants.USERNAME_TOKEN10);
        assertPolicy(aim, SPConstants.USERNAME_TOKEN11);
        assertPolicy(aim, SPConstants.HASH_PASSWORD);
        assertPolicy(aim, SPConstants.NO_PASSWORD);
        assertPolicy(aim, SP13Constants.NONCE);
        assertPolicy(aim, SP13Constants.CREATED);

        return (UsernameToken)assertTokens(message, SPConstants.USERNAME_TOKEN, true);
    }
    
    private UsernameToken assertTokens(
        SoapMessage message, 
        UsernameTokenPrincipal princ,
        boolean signed
    ) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = getAllAssertionsByLocalname(aim, SPConstants.USERNAME_TOKEN);
        UsernameToken tok = null;
        for (AssertionInfo ai : ais) {
            tok = (UsernameToken)ai.getAssertion();
            ai.setAsserted(true);
            if ((tok.getPasswordType() == UsernameToken.PasswordType.HashPassword)
                && (princ == null || !princ.isPasswordDigest())) {
                ai.setNotAsserted("Password hashing policy not enforced");
            } else {
                assertPolicy(aim, SPConstants.HASH_PASSWORD);
            }
            
            if ((tok.getPasswordType() != UsernameToken.PasswordType.NoPassword)
                && isNonEndorsingSupportingToken(tok)
                && (princ == null || princ.getPassword() == null)) {
                ai.setNotAsserted("Username Token No Password supplied");
            } else {
                assertPolicy(aim, SPConstants.NO_PASSWORD);
            }
            
            if (tok.isCreated() && princ.getCreatedTime() == null) {
                ai.setNotAsserted("No Created Time");
            } else {
                assertPolicy(aim, SP13Constants.CREATED);
            }
            
            if (tok.isNonce() && princ.getNonce() == null) {
                ai.setNotAsserted("No Nonce");
            } else {
                assertPolicy(aim, SP13Constants.NONCE);
            }
        }
        
        assertPolicy(aim, SPConstants.USERNAME_TOKEN10);
        assertPolicy(aim, SPConstants.USERNAME_TOKEN11);
        assertPolicy(aim, SPConstants.SUPPORTING_TOKENS);

        if (signed || isTLSInUse(message)) {
            assertPolicy(aim, SPConstants.SIGNED_SUPPORTING_TOKENS);
        }
        return tok;
    }
    
    /**
     * Return true if this UsernameToken policy is a (non-endorsing)SupportingToken. If this is
     * true then the corresponding UsernameToken must have a password element.
     */
    private boolean isNonEndorsingSupportingToken(
        org.apache.wss4j.policy.model.UsernameToken usernameTokenPolicy
    ) {
        AbstractSecurityAssertion supportingToken = usernameTokenPolicy.getParentAssertion();
        if (supportingToken instanceof SupportingTokens
            && ((SupportingTokens)supportingToken).isEndorsing()) {
            return false;
        }
        return true;
    }

    protected void addToken(SoapMessage message) {
        UsernameToken tok = assertTokens(message);

        Header h = findSecurityHeader(message, true);
        WSSecUsernameToken utBuilder = 
            addUsernameToken(message, tok);
        if (utBuilder == null) {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            Collection<AssertionInfo> ais = 
                getAllAssertionsByLocalname(aim, SPConstants.USERNAME_TOKEN);
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
            if (token.getPasswordType() == UsernameToken.PasswordType.NoPassword) {
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
                if (token.getPasswordType() == UsernameToken.PasswordType.HashPassword) {
                    utBuilder.setPasswordType(WSConstants.PASSWORD_DIGEST);  
                } else {
                    utBuilder.setPasswordType(WSConstants.PASSWORD_TEXT);
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
