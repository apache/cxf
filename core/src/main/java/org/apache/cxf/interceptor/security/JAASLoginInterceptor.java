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
package org.apache.cxf.interceptor.security;

import java.security.PrivilegedAction;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.security.SecurityToken;
import org.apache.cxf.common.security.TokenType;
import org.apache.cxf.common.security.UsernameToken;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.security.SecurityContext;

public class JAASLoginInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final String ROLE_CLASSIFIER_PREFIX = "prefix";
    public static final String ROLE_CLASSIFIER_CLASS_NAME = "classname";

    private static final Logger LOG = LogUtils.getL7dLogger(JAASLoginInterceptor.class);
    
    private String contextName = "";
    private Configuration loginConfig;
    private String roleClassifier;
    private String roleClassifierType = ROLE_CLASSIFIER_PREFIX;
    private boolean reportFault;
    private boolean useDoAs = true;
    private boolean allowNamedPrincipals = true;
    
    public JAASLoginInterceptor() {
        super(Phase.UNMARSHAL);
    }
    
    public JAASLoginInterceptor(String phase) {
        super(phase);
    }
    
    public void setContextName(String name) {
        contextName = name;
    }
    
    public String getContextName() {
        return contextName;
    }
    
    @Deprecated
    public void setRolePrefix(String name) {
        setRoleClassifier(name);
    }
    
    public void setRoleClassifier(String value) {
        roleClassifier = value;
    }
    
    public String getRoleClassifier() {
        return roleClassifier;
    }
    
    public void setRoleClassifierType(String value) {
        if (!ROLE_CLASSIFIER_PREFIX.equals(value)
            && !ROLE_CLASSIFIER_CLASS_NAME.equals(value)) {
            throw new IllegalArgumentException("Unsupported role classifier");
        }
        roleClassifierType = value;
    }
    
    public String getRoleClassifierType() {
        return roleClassifierType;
    }
    
    public void setReportFault(boolean reportFault) {
        this.reportFault = reportFault;
    }
    
    public void setUseDoAs(boolean useDoAs) {
        this.useDoAs = useDoAs;
    }

    public void handleMessage(final Message message) throws Fault {
        if (allowNamedPrincipals) {
            SecurityContext sc = message.get(SecurityContext.class);
            if (sc != null && sc.getUserPrincipal() != null 
                && sc.getUserPrincipal().getName() != null) {
                return;
            }
        }
        
        String name = null;
        String password = null;
        
        AuthorizationPolicy policy = message.get(AuthorizationPolicy.class);
        if (policy != null) {
            name = policy.getUserName();
            password = policy.getPassword();
        } else {
            // try the UsernameToken
            SecurityToken token = message.get(SecurityToken.class);
            if (token != null && token.getTokenType() == TokenType.UsernameToken) {
                UsernameToken ut = (UsernameToken)token;
                name = ut.getName();
                password = ut.getPassword();
            }
        }

        if (name == null || password == null) {
            throw new AuthenticationException("Authentication required but no user or password was supplied");
        }

        try {
            CallbackHandler handler = getCallbackHandler(name, password);  
            LoginContext ctx = new LoginContext(getContextName(), null, handler, loginConfig);  
            
            ctx.login();
            
            Subject subject = ctx.getSubject();
            message.put(SecurityContext.class, createSecurityContext(name, subject));
            
            // Run the further chain in the context of this subject.
            // This allows other code to retrieve the subject using pure JAAS
            if (useDoAs) {
                Subject.doAs(subject, new PrivilegedAction<Void>() {

                    @Override
                    public Void run() {
                        InterceptorChain chain = message.getInterceptorChain();
                        if (chain != null) {
                            chain.doIntercept(message);
                        }
                        return null;
                    }
                });
            }

        } catch (LoginException ex) {
            String errorMessage = "Authentication failed for user " + name + " : " + ex.getMessage();
            LOG.fine(errorMessage);
            if (reportFault) {
                throw new AuthenticationException(errorMessage);
            } else {
                throw new AuthenticationException("Authentication failed (details can be found in server log)");
            }
        }
    }

    protected CallbackHandler getCallbackHandler(String name, String password) {
        return new NamePasswordCallbackHandler(name, password);
    }
    
    protected SecurityContext createSecurityContext(String name, Subject subject) {
        if (getRoleClassifier() != null) {
            return new RolePrefixSecurityContextImpl(subject, getRoleClassifier(),
                                                     getRoleClassifierType());
        } else {
            return new DefaultSecurityContext(name, subject);
        }
    }

    public Configuration getLoginConfig() {
        return loginConfig;
    }

    public void setLoginConfig(Configuration loginConfig) {
        this.loginConfig = loginConfig;
    }
    
    public void setAllowNamedPrincipals(boolean allowNamedPrincipals) {
        this.allowNamedPrincipals = allowNamedPrincipals;
    }

}
