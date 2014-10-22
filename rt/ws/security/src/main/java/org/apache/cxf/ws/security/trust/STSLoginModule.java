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
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.w3c.dom.Document;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.security.SimplePrincipal;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.saml.SAMLUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.Credential;

/**
 * A JAAS LoginModule for authenticating a Username/Password to the STS. The 
 * STSClient object itself must be configured separately and picked up either via 
 * the endpoint name, the "default" STSClient or as a JAX-WS/JAX-RS property with the
 * key "ws-security.sts.client".
 */
public class STSLoginModule implements LoginModule {
    /**
     * Whether we require roles or not from the STS. If this is not set then the 
     * WS-Trust validate binding is used. If it is set then the issue binding is 
     * used, where the Username + Password credentials are passed via "OnBehalfOf".
     */
    public static final String REQUIRE_ROLES = "require.roles";
    
    private static final Logger LOG = LogUtils.getL7dLogger(STSLoginModule.class);
    
    private Set<Principal> principals = new HashSet<Principal>();
    private Subject subject;
    private CallbackHandler callbackHandler;
    private boolean requireRoles;
    
    @Override
    public void initialize(Subject subj, CallbackHandler cbHandler, Map<String, ?> sharedState,
                           Map<String, ?> options) {
        subject = subj;
        callbackHandler = cbHandler;
        if (options.containsKey(REQUIRE_ROLES)) {
            requireRoles = Boolean.parseBoolean((String)options.get(REQUIRE_ROLES));
        }
    }

    @Override
    public boolean login() throws LoginException {
        // Get username and password
        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", false);

        try {
            callbackHandler.handle(callbacks);
        } catch (IOException ioException) {
            throw new LoginException(ioException.getMessage());
        } catch (UnsupportedCallbackException unsupportedCallbackException) {
            throw new LoginException(unsupportedCallbackException.getMessage() 
                                     + " not available to obtain information from user.");
        }

        String user = ((NameCallback) callbacks[0]).getName();

        char[] tmpPassword = ((PasswordCallback) callbacks[1]).getPassword();
        if (tmpPassword == null) {
            tmpPassword = new char[0];
        }
        String password = new String(tmpPassword);
        
        principals = new HashSet<Principal>();
        
        STSTokenValidator validator = new STSTokenValidator(true);
        validator.setUseIssueBinding(requireRoles);
        
        // Authenticate token
        try {
            UsernameToken token = convertToToken(user, password);
            Credential credential = new Credential();
            credential.setUsernametoken(token);
            
            RequestData data = new RequestData();
            Message message = PhaseInterceptorChain.getCurrentMessage();
            data.setMsgContext(message);
            credential = validator.validate(credential, data);

            // Add user principal
            principals.add(new SimplePrincipal(user));
            
            // Add roles if a SAML Assertion was returned from the STS
            principals.addAll(getRoles(message, credential));
        } catch (Exception e) {
            LOG.log(Level.INFO, "User " + user + "authentication failed", e);
            throw new LoginException("User " + user + " authentication failed: " + e.getMessage());
        }
        
        return true;
    }

    private UsernameToken convertToToken(String username, String password) 
        throws Exception {

        Document doc = DOMUtils.createDocument();
        UsernameToken token = new UsernameToken(false, doc, 
                                                WSConstants.PASSWORD_TEXT);
        token.setName(username);
        token.setPassword(password);
        return token;
    }
    
    private Set<Principal> getRoles(Message msg, Credential credential) {
        SamlAssertionWrapper samlAssertion = credential.getTransformedToken();
        if (samlAssertion == null) {
            samlAssertion = credential.getSamlAssertion();
        }
        if (samlAssertion != null) {
            String roleAttributeName = 
                (String)msg.getContextualProperty(SecurityConstants.SAML_ROLE_ATTRIBUTENAME);
            if (roleAttributeName == null || roleAttributeName.length() == 0) {
                roleAttributeName = WSS4JInInterceptor.SAML_ROLE_ATTRIBUTENAME_DEFAULT;
            }

            ClaimCollection claims = 
                SAMLUtils.getClaims((SamlAssertionWrapper)samlAssertion);
            return SAMLUtils.parseRolesFromClaims(claims, roleAttributeName, null);
        }
        
        return Collections.emptySet();
    }

    
    @Override
    public boolean commit() throws LoginException {
        if (principals.isEmpty()) {
            return false;
        }
        subject.getPrincipals().addAll(principals);
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        subject.getPrincipals().removeAll(principals);
        principals.clear();
        return true;
    }
    
    
}
