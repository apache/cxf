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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyException;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.UsernameToken;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.WSUsernameTokenPrincipal;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.WSSecUsernameToken;
import org.apache.ws.security.processor.UsernameTokenProcessor;

/**
 * 
 */
public class UsernameTokenInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(UsernameTokenInterceptor.class);
    private static final Set<QName> HEADERS = new HashSet<QName>();
    static {
        HEADERS.add(new QName(WSConstants.WSSE_NS, "Security"));
        HEADERS.add(new QName(WSConstants.WSSE11_NS, "Security"));
    }


    /**
     * @param p
     */
    public UsernameTokenInterceptor() {
        super(Phase.PRE_PROTOCOL);
        addAfter(PolicyBasedWSS4JInInterceptor.class.getName());
        addAfter(PolicyBasedWSS4JOutInterceptor.class.getName());
    }
    public Set<QName> getUnderstoodHeaders() {
        return HEADERS;
    }


    public void handleMessage(SoapMessage message) throws Fault {

        boolean isReq = MessageUtils.isRequestor(message);
        boolean isOut = MessageUtils.isOutbound(message);
        if (isReq != isOut) {
            //outbound on server side and inbound on client side doesn't need
            //any username token stuff, assert policies and return
            assertUsernameTokens(message, null);
            return;
        }
        if (isReq) {
            if (message.containsKey(PolicyBasedWSS4JOutInterceptor.SECURITY_PROCESSED)) {
                //The full policy interceptors handled this
                return;
            }
            addUsernameToken(message);
        } else {
            if (message.containsKey(WSS4JInInterceptor.SECURITY_PROCESSED)) {
                //The full policy interceptors handled this
                return;
            }
            processUsernameToken(message);
        }
    }


    private void processUsernameToken(SoapMessage message) {
        Header h = findSecurityHeader(message, false);
        if (h == null) {
            return;
        }
        Element el = (Element)h.getObject();
        Element child = DOMUtils.getFirstElement(el);
        while (child != null) {
            if (SPConstants.USERNAME_TOKEN.equals(child.getLocalName())) {
                UsernameTokenProcessor p = new UsernameTokenProcessor();
                
                try  {
                    final WSUsernameTokenPrincipal princ = p.handleUsernameToken(child, 
                                                                           getCallback(message));
                    if (princ != null) {
                        Vector<WSSecurityEngineResult>v = new Vector<WSSecurityEngineResult>();
                        v.add(0, new WSSecurityEngineResult(WSConstants.UT, princ, null, null, null));
                        List<Object> results = CastUtils.cast((List)message
                                                                  .get(WSHandlerConstants.RECV_RESULTS));
                        if (results == null) {
                            results = new Vector<Object>();
                            message.put(WSHandlerConstants.RECV_RESULTS, results);
                        }
                        WSHandlerResult rResult = new WSHandlerResult(null, v);
                        results.add(0, rResult);

                        assertUsernameTokens(message, princ);
                        message.put(WSS4JInInterceptor.PRINCIPAL_RESULT, princ);                   
                        SecurityContext sc = message.get(SecurityContext.class);
                        if (sc == null || sc.getUserPrincipal() == null) {
                            SecurityContext c = new SecurityContext() {
                                public Principal getUserPrincipal() {
                                    return princ;
                                }
                                public boolean isUserInRole(String role) {
                                    return false;
                                }
                            };
                            message.put(SecurityContext.class, c);
                        }

                    }
                } catch (WSSecurityException ex) {
                    throw new Fault(ex);
                }
            }
            child = DOMUtils.getNextElement(child);
        }
    }

    private UsernameToken assertUsernameTokens(SoapMessage message, WSUsernameTokenPrincipal princ) {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais = aim.getAssertionInfo(SP12Constants.USERNAME_TOKEN);
        UsernameToken tok = null;
        for (AssertionInfo ai : ais) {
            tok = (UsernameToken)ai.getAssertion();
            if (princ != null && tok.isHashPassword() != princ.isPasswordDigest()) {
                ai.setNotAsserted("Password hashing policy not enforced");
            } else {
                ai.setAsserted(true);                
            }
        }
        ais = aim.getAssertionInfo(SP12Constants.SUPPORTING_TOKENS);
        for (AssertionInfo ai : ais) {
            ai.setAsserted(true);
        }
        ais = aim.getAssertionInfo(SP12Constants.SIGNED_SUPPORTING_TOKENS);
        for (AssertionInfo ai : ais) {
            ai.setAsserted(true);
        }
        return tok;
    }

    private void addUsernameToken(SoapMessage message) {
        UsernameToken tok = assertUsernameTokens(message, null);

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


    private Header findSecurityHeader(SoapMessage message, boolean create) {
        for (Header h : message.getHeaders()) {
            QName n = h.getName();
            if (n.getLocalPart().equals("Security")
                && (n.getNamespaceURI().equals(WSConstants.WSSE_NS) 
                    || n.getNamespaceURI().equals(WSConstants.WSSE11_NS))) {
                return h;
            }
        }
        if (!create) {
            return null;
        }
        Document doc = DOMUtils.createDocument();
        Element el = doc.createElementNS(WSConstants.WSSE_NS, "wsse:Security");
        el.setAttributeNS(WSConstants.XMLNS_NS, "xmlns:wsse", WSConstants.WSSE_NS);
        SoapHeader sh = new SoapHeader(new QName(WSConstants.WSSE_NS, "Security"), el);
        sh.setMustUnderstand(true);
        message.getHeaders().add(sh);
        return sh;
    }
    protected WSSecUsernameToken addUsernameToken(SoapMessage message, UsernameToken token) {
        String userName = (String)message.getContextualProperty(SecurityConstants.USERNAME);
        
        if (!StringUtils.isEmpty(userName)) {
            // If NoPassword property is set we don't need to set the password
            if (token.isNoPassword()) {
                WSSecUsernameToken utBuilder = new WSSecUsernameToken();
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
                WSSecUsernameToken utBuilder = new WSSecUsernameToken();
                if (token.isHashPassword()) {
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
    private CallbackHandler getCallback(SoapMessage message) {
        //Then try to get the password from the given callback handler
        Object o = message.getContextualProperty(SecurityConstants.CALLBACK_HANDLER);
    
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
        return handler;
    }
    public String getPassword(String userName, UsernameToken info, int type, SoapMessage message) {
        //Then try to get the password from the given callback handler
    
        CallbackHandler handler = getCallback(message);
        if (handler == null) {
            policyNotAsserted(info, "No callback handler and no password available", message);
            return null;
        }
        
        WSPasswordCallback[] cb = {new WSPasswordCallback(userName,
                                                          type)};
        try {
            handler.handle(cb);
        } catch (Exception e) {
            policyNotAsserted(info, e, message);
        }
        
        //get the password
        return cb[0].getPassword();
    }
    protected void policyNotAsserted(UsernameToken assertion, String reason, SoapMessage message) {
        if (assertion == null) {
            return;
        }
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);

        Collection<AssertionInfo> ais;
        ais = aim.get(assertion.getName());
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == assertion) {
                    ai.setNotAsserted(reason);
                }
            }
        }
        if (!assertion.isOptional()) {
            throw new PolicyException(new Message(reason, LOG));
        }
    }
    protected void policyNotAsserted(UsernameToken assertion, Exception reason, SoapMessage message) {
        if (assertion == null) {
            return;
        }
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        Collection<AssertionInfo> ais;
        ais = aim.get(assertion.getName());
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                if (ai.getAssertion() == assertion) {
                    ai.setNotAsserted(reason.getMessage());
                }
            }
        }
        throw new PolicyException(reason);
    }
}
