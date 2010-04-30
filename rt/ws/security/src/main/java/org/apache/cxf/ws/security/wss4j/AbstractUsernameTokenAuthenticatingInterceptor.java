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
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.security.SecurityContext;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.WSUsernameTokenPrincipal;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.message.token.UsernameToken;
import org.apache.ws.security.processor.Processor;


/**
 * Base class providing an extensibility point for populating 
 * javax.security.auth.Subject from a current UsernameToken.
 * 
 * WSS4J requires a password for validating digests which may not be available
 * when external security systems provide for the authentication. This class
 * implements WSS4J Processor interface so that it can delegate a UsernameToken
 * validation to an external system.
 * 
 * In order to handle digests, this class currently creates a new WSS4J Security Engine for
 * every request. If clear text passwords are expected then a supportDigestPasswords boolean
 * property with a false value can be used to disable creating security engines.
 * 
 * Note that if a UsernameToken containing a clear text password has been encrypted then
 * an application is expected to provide a password callback handler for decrypting the token only.     
 *
 */
public abstract class AbstractUsernameTokenAuthenticatingInterceptor extends WSS4JInInterceptor 
    implements Processor {
    
    private static final Logger LOG = 
        LogUtils.getL7dLogger(AbstractUsernameTokenAuthenticatingInterceptor.class);
    
    private boolean supportDigestPasswords;
    
    public AbstractUsernameTokenAuthenticatingInterceptor() {
        super();
    }
    
    public AbstractUsernameTokenAuthenticatingInterceptor(Map<String, Object> properties) {
        super(properties);
    }
    
    public void setSupportDigestPasswords(boolean support) {
        supportDigestPasswords = support;
    }
    
    
    
    @Override
    protected SecurityContext createSecurityContext(final Principal p) {
        Message msg = PhaseInterceptorChain.getCurrentMessage();
        if (msg == null) {
            throw new IllegalStateException("Current message is not available");
        }
        return doCreateSecurityContext(p, msg.get(Subject.class));
    }
    
    /**
     * Creates default SecurityContext which implements isUserInRole using the
     * following approach : skip the first Subject principal, and then check optional
     * Groups the principal is a member of. Subclasses can override this method and implement
     * a custom strategy instead
     *   
     * @param p principal
     * @param subject subject 
     * @return security context
     */
    protected SecurityContext doCreateSecurityContext(final Principal p, final Subject subject) {
        return new DefaultSecurityContext(p, subject);
    }

        
    protected void setSubject(String name, 
                              String password, 
                              boolean isDigest,
                              String nonce,
                              String created) throws WSSecurityException {
        Message msg = PhaseInterceptorChain.getCurrentMessage();
        if (msg == null) {
            throw new IllegalStateException("Current message is not available");
        }
        Subject subject = null;
        try {
            subject = createSubject(name, password, isDigest, nonce, created);
        } catch (Exception ex) {
            throw new WSSecurityException("Failed Authentication : Subject has not been created", ex);
        }
        if (subject == null || subject.getPrincipals().size() == 0
            || !subject.getPrincipals().iterator().next().getName().equals(name)) {
            throw new WSSecurityException("Failed Authentication : Invalid Subject");
        }
        msg.put(Subject.class, subject);
    }
    
    /**
     * Create a Subject representing a current user and its roles. 
     * This Subject is expected to contain at least one Principal representing a user
     * and optionally followed by one or more principal Groups this user is a member of.
     * It will also be available in doCreateSecurityContext.   
     * @param name username
     * @param password password
     * @param isDigest true if a password digest is used
     * @param nonce optional nonce
     * @param created optional timestamp
     * @return subject
     * @throws SecurityException
     */
    protected abstract Subject createSubject(String name, 
                                    String password, 
                                    boolean isDigest,
                                    String nonce,
                                    String created) throws SecurityException;
    
    
    /**
     * {@inheritDoc}
     * 
     */
    @Override
    protected CallbackHandler getCallback(RequestData reqData, int doAction) 
        throws WSSecurityException {
        
        if ((doAction & WSConstants.UT) != 0 && !supportDigestPasswords) {    
            CallbackHandler pwdCallback = null;
            try {
                pwdCallback = super.getCallback(reqData, doAction);
            } catch (Exception ex) {
                // ignore
            }
            return new DelegatingCallbackHandler(pwdCallback);
        }
        
        return super.getCallback(reqData, doAction);
    }
    
    @Override 
    protected WSSecurityEngine getSecurityEngine() {
        if (!supportDigestPasswords) {
            return super.getSecurityEngine();
        }
        Map<QName, Object> profiles = new HashMap<QName, Object>(3);
        profiles.put(new QName(WSConstants.USERNAMETOKEN_NS, WSConstants.USERNAME_TOKEN_LN), this);
        profiles.put(new QName(WSConstants.WSSE_NS, WSConstants.USERNAME_TOKEN_LN), this);
        profiles.put(new QName(WSConstants.WSSE11_NS, WSConstants.USERNAME_TOKEN_LN), this);
        return createSecurityEngine(profiles);
    }
    
    public void handleToken(Element elem, 
                            Crypto crypto, 
                            Crypto decCrypto, 
                            CallbackHandler cb, 
                            WSDocInfo wsDocInfo, 
                            Vector returnResults, 
                            WSSConfig config) throws WSSecurityException {
        new CustomUsernameTokenProcessor().handleToken(elem, crypto, decCrypto, cb, wsDocInfo, 
                                                       returnResults, config);
    }
    
    
    private class DelegatingCallbackHandler implements CallbackHandler {

        private CallbackHandler pwdHandler;
        
        public DelegatingCallbackHandler(CallbackHandler pwdHandler) {
            this.pwdHandler = pwdHandler;
        }
        
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback c : callbacks) {
                if (c instanceof WSPasswordCallback) {
                    WSPasswordCallback pc = (WSPasswordCallback)c;
                    if (WSConstants.PASSWORD_TEXT.equals(pc.getPasswordType()) 
                        && pc.getUsage() == WSPasswordCallback.USERNAME_TOKEN_UNKNOWN) {
                        AbstractUsernameTokenAuthenticatingInterceptor.this.setSubject(
                            pc.getIdentifier(), pc.getPassword(), false, null, null);
                    } else if (pwdHandler != null) {
                        pwdHandler.handle(callbacks);
                    }
                }
            }
            
        }
        
    }
    
    /**
     * Custom UsernameTokenProcessor
     * Unfortunately, WSS4J UsernameTokenProcessor makes it impossible to
     * override its handleUsernameToken only. 
     *
     */
    private class CustomUsernameTokenProcessor implements Processor {
        
        private String utId;
        private UsernameToken ut;
        
        @SuppressWarnings("unchecked")
        public void handleToken(Element elem, Crypto crypto, Crypto decCrypto, CallbackHandler cb, 
            WSDocInfo wsDocInfo, Vector returnResults, WSSConfig wsc) throws WSSecurityException {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Found UsernameToken list element");
            }
            
            Principal principal = handleUsernameToken((Element) elem, cb);
            returnResults.add(
                0, 
                new WSSecurityEngineResult(WSConstants.UT, principal, null, null, null)
            );
            utId = ut.getID();
        }
        
        private WSUsernameTokenPrincipal handleUsernameToken(
            Element token, CallbackHandler cb) throws WSSecurityException {
            //
            // Parse the UsernameToken element
            //
            ut = new UsernameToken(token, false);
            String user = ut.getName();
            String password = ut.getPassword();
            String nonce = ut.getNonce();
            String createdTime = ut.getCreated();
            String pwType = ut.getPasswordType();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("UsernameToken user " + user);
                LOG.fine("UsernameToken password " + password);
            }
            
            AbstractUsernameTokenAuthenticatingInterceptor.this.setSubject(
                user, password, ut.isHashed(), nonce, createdTime);    
            
            WSUsernameTokenPrincipal principal = new WSUsernameTokenPrincipal(user, ut.isHashed());
            principal.setNonce(nonce);
            principal.setPassword(password);
            principal.setCreatedTime(createdTime);
            principal.setPasswordType(pwType);

            return principal;
        }

        public String getId() {
            return utId;
        }
    }
    
}
