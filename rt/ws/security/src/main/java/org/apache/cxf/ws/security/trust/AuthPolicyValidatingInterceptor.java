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

import java.security.Principal;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.saml.claims.SAMLSecurityContext;
import org.apache.cxf.rt.security.saml.utils.SAMLUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.wss4j.common.principal.WSUsernameTokenPrincipalImpl;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.Validator;

public class AuthPolicyValidatingInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(AuthPolicyValidatingInterceptor.class);
    private static final Logger LOG = LogUtils.getL7dLogger(AuthPolicyValidatingInterceptor.class);
    
    private Validator validator;
    
    public AuthPolicyValidatingInterceptor() {
        this(Phase.UNMARSHAL);
    }
    
    public AuthPolicyValidatingInterceptor(String phase) {
        super(phase);
    }
    
    public void handleMessage(Message message) throws Fault {

        AuthorizationPolicy policy = message.get(AuthorizationPolicy.class);
        if (policy == null || policy.getUserName() == null || policy.getPassword() == null) {
            String name = null;
            if (policy != null) {
                name = policy.getUserName();
            }
            org.apache.cxf.common.i18n.Message errorMsg = 
                new org.apache.cxf.common.i18n.Message("NO_USER_PASSWORD", 
                                                       BUNDLE, 
                                                       name);
            LOG.warning(errorMsg.toString());
            throw new SecurityException(errorMsg.toString());
        }
        
        try {
            UsernameToken token = convertPolicyToToken(policy);
            Credential credential = new Credential();
            credential.setUsernametoken(token);
            
            RequestData data = new RequestData();
            data.setMsgContext(message);
            credential = validator.validate(credential, data);
            
            // Create a Principal/SecurityContext
            SecurityContext sc = null;
            if (credential != null && credential.getPrincipal() != null) {
                sc = createSecurityContext(message, credential);
            } else {
                Principal p = new WSUsernameTokenPrincipalImpl(policy.getUserName(), false);
                ((WSUsernameTokenPrincipalImpl)p).setPassword(policy.getPassword());
                sc = createSecurityContext(p);
            }
            
            message.put(SecurityContext.class, sc);
        } catch (Exception ex) {
            throw new Fault(ex);
        }
    }

    protected UsernameToken convertPolicyToToken(AuthorizationPolicy policy) 
        throws Exception {

        Document doc = DOMUtils.createDocument();
        UsernameToken token = new UsernameToken(false, doc, 
                                                WSConstants.PASSWORD_TEXT);
        token.setName(policy.getUserName());
        token.setPassword(policy.getPassword());
        return token;
    }
    
    protected SecurityContext createSecurityContext(final Principal p) {
        return new SecurityContext() {

            public Principal getUserPrincipal() {
                return p;
            }

            public boolean isUserInRole(String arg0) {
                return false;
            }
        };
    }
    
    protected SecurityContext createSecurityContext(Message msg, Credential credential) {
        SamlAssertionWrapper samlAssertion = credential.getTransformedToken();
        if (samlAssertion == null) {
            samlAssertion = credential.getSamlAssertion();
        }
        if (samlAssertion != null) {
            String roleAttributeName = 
                (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SAML_ROLE_ATTRIBUTENAME, msg);
            if (roleAttributeName == null || roleAttributeName.length() == 0) {
                roleAttributeName = WSS4JInInterceptor.SAML_ROLE_ATTRIBUTENAME_DEFAULT;
            }

            ClaimCollection claims = 
                SAMLUtils.getClaims((SamlAssertionWrapper)samlAssertion);
            Set<Principal> roles = 
                SAMLUtils.parseRolesFromClaims(claims, roleAttributeName, null);

            SAMLSecurityContext context = 
                new SAMLSecurityContext(credential.getPrincipal(), roles, claims);
            context.setIssuer(SAMLUtils.getIssuer(samlAssertion));
            context.setAssertionElement(SAMLUtils.getAssertionElement(samlAssertion));
            return context;
        } else {
            return createSecurityContext(credential.getPrincipal());
        }
    }

    public void setValidator(Validator validator) {
        this.validator = validator;
    }
    
}
