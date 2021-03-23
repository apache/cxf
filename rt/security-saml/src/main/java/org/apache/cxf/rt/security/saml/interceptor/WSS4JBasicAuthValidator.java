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
package org.apache.cxf.rt.security.saml.interceptor;

import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Document;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.saml.claims.SAMLSecurityContext;
import org.apache.cxf.rt.security.saml.utils.SAMLUtils;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.WSUsernameTokenPrincipalImpl;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.UsernameTokenValidator;
import org.apache.wss4j.dom.validate.Validator;

/**
 * An abstract class containing some functionality to validate a username + password received
 * via HTTP Basic Authentication via a WSS4J Validator (and hence JAAS, the STS, etc.). It can
 * be subclassed and used as a CXF interceptor or else via a JAX-RS ContainerRequestFilter.
 */
public abstract class WSS4JBasicAuthValidator {

    private static final Logger LOG = LogUtils.getL7dLogger(WSS4JBasicAuthValidator.class);
    private static final String SAML_ROLE_ATTRIBUTENAME_DEFAULT =
        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

    private Validator validator;
    private CallbackHandler callbackHandler;

    protected void validate(Message message) throws WSSecurityException {

        AuthorizationPolicy policy = message.get(AuthorizationPolicy.class);
        if (policy == null || policy.getUserName() == null || policy.getPassword() == null) {
            String name = null;
            if (policy != null) {
                name = policy.getUserName();
            }
            String errorMsg = "No user name and/or password is available, name: " + name;
            LOG.warning(errorMsg);
            throw new SecurityException(errorMsg);
        }

        UsernameToken token = convertPolicyToToken(policy);
        Credential credential = new Credential();
        credential.setUsernametoken(token);

        RequestData data = new RequestData();
        data.setMsgContext(message);
        data.setCallbackHandler(callbackHandler);
        credential = getValidator().validate(credential, data);

        // Create a Principal/SecurityContext
        final SecurityContext sc;
        if (credential != null && credential.getPrincipal() != null) {
            sc = createSecurityContext(message, credential);
        } else {
            Principal p = new WSUsernameTokenPrincipalImpl(policy.getUserName(), false);
            ((WSUsernameTokenPrincipalImpl)p).setPassword(policy.getPassword());
            sc = createSecurityContext(p);
        }

        message.put(SecurityContext.class, sc);
    }

    protected UsernameToken convertPolicyToToken(AuthorizationPolicy policy) {

        Document doc = DOMUtils.getEmptyDocument();
        UsernameToken token = new UsernameToken(false, doc,
                                                WSS4JConstants.PASSWORD_TEXT);
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
                roleAttributeName = SAML_ROLE_ATTRIBUTENAME_DEFAULT;
            }

            ClaimCollection claims =
                SAMLUtils.getClaims(samlAssertion);
            Set<Principal> roles =
                SAMLUtils.parseRolesFromClaims(claims, roleAttributeName, null);

            SAMLSecurityContext context =
                new SAMLSecurityContext(credential.getPrincipal(), roles, claims);
            context.setIssuer(SAMLUtils.getIssuer(samlAssertion));
            context.setAssertionElement(SAMLUtils.getAssertionElement(samlAssertion));
            return context;
        }
        return createSecurityContext(credential.getPrincipal());
    }

    public Validator getValidator() {
        if (validator != null) {
            return validator;
        }
        return new UsernameTokenValidator();
    }

    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

}
