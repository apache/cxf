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
package org.apache.cxf.systest.sts.restunit;

import java.io.IOException;
import java.security.Principal;

import javax.security.auth.callback.CallbackHandler;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

import org.w3c.dom.Document;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.security.SecurityContext;
import org.apache.wss4j.common.principal.WSUsernameTokenPrincipalImpl;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.UsernameTokenValidator;

/**
 * A simple filter to validate a Basic Auth username/password via a CallbackHandler
 */
public class BasicAuthFilter implements ContainerRequestFilter {

    private CallbackHandler callbackHandler;
    
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Message message = JAXRSUtils.getCurrentMessage();
        AuthorizationPolicy policy = message.get(AuthorizationPolicy.class);
        
        if (policy == null || policy.getUserName() == null || policy.getPassword() == null) {
            requestContext.abortWith(
                Response.status(401).header("WWW-Authenticate", "Basic realm=\"IdP\"").build());
        }

        try {
            UsernameToken token = convertPolicyToToken(policy);
            Credential credential = new Credential();
            credential.setUsernametoken(token);
            
            RequestData data = new RequestData();
            data.setMsgContext(message);
            data.setCallbackHandler(callbackHandler);
            UsernameTokenValidator validator = new UsernameTokenValidator();
            credential = validator.validate(credential, data);
            
            // Create a Principal/SecurityContext
            Principal p = null;
            if (credential != null && credential.getPrincipal() != null) {
                p = credential.getPrincipal();
            } else {
                p = new WSUsernameTokenPrincipalImpl(policy.getUserName(), false);
                ((WSUsernameTokenPrincipalImpl)p).setPassword(policy.getPassword());
            }
            message.put(SecurityContext.class, createSecurityContext(p));
        } catch (Exception ex) {
            throw ExceptionUtils.toInternalServerErrorException(ex, null);
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

    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

}