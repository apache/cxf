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
package org.apache.cxf.transport.http.auth;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AccountException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class WSDLGetAuthenticatorInterceptor extends AbstractPhaseInterceptor<Message> {
        
    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";

    private static final String AUTHENTICATION_SCHEME_BASIC = "Basic";
       
    private static final Logger LOG = LogUtils.getL7dLogger(WSDLGetAuthenticatorInterceptor.class);
    
    private String contextName; 
    
    public WSDLGetAuthenticatorInterceptor() {
        super(Phase.READ);
        getBefore().add("org.apache.cxf.frontend.WSDLGetInterceptor");
    }

    public void handleMessage(Message message) throws Fault {
        
        String method = (String)message.get(Message.HTTP_REQUEST_METHOD);
        String query = (String)message.get(Message.QUERY_STRING);
        if (!"GET".equals(method) || StringUtils.isEmpty(query)) {
            return;
        }
        Endpoint endpoint = message.getExchange().getEndpoint();
        synchronized (endpoint) {
            if (!StringUtils.isEmpty(contextName)) {
                AuthorizationPolicy policy = message.get(AuthorizationPolicy.class);
                if (policy == null) {
                    handle401response(message, endpoint);
                    return;
                } else {
                    Subject subject = (Subject)authenticate(policy.getUserName(), policy.getPassword());
                    if (subject == null) {
                        handle401response(message, endpoint);
                        return;
                    }
                }
                
            }
        
        }
    }
        
    private void handle401response(Message message, Endpoint e) {
        HttpServletResponse response = (HttpServletResponse)message.get("HTTP.RESPONSE");
        response.setHeader(HEADER_WWW_AUTHENTICATE, AUTHENTICATION_SCHEME_BASIC + " realm=\"" + contextName + "\"");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentLength(0);
        message.getInterceptorChain().pause();
    }

    public Object authenticate(final String username, final String password) {
        return doAuthenticate(username, password);
    }

    public Subject doAuthenticate(final String username, final String password) {
        try {
            Subject subject = new Subject();
            LoginContext loginContext = new LoginContext(getContextName(), subject, new CallbackHandler() {
                public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                    for (int i = 0; i < callbacks.length; i++) {
                        if (callbacks[i] instanceof NameCallback) {
                            ((NameCallback)callbacks[i]).setName(username);
                        } else if (callbacks[i] instanceof PasswordCallback) {
                            ((PasswordCallback)callbacks[i]).setPassword(password.toCharArray());
                        } else {
                            throw new UnsupportedCallbackException(callbacks[i]);
                        }
                    }
                }
            });
            loginContext.login();
            return subject;
        } catch (FailedLoginException e) {
            LOG.log(Level.FINE, "Login failed ", e);
            return null;
        } catch (AccountException e) {
            LOG.log(Level.WARNING, "Account failure ",  e);
            return null;
        } catch (GeneralSecurityException e) {
            LOG.log(Level.SEVERE, "General Security Exception ", e);
            return null;
        }
    }

    public String getContextName() {
        return contextName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

}
