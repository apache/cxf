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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.security.callback.CallbackHandlerProvider;
import org.apache.cxf.interceptor.security.callback.CallbackHandlerProviderAuthPol;
import org.apache.cxf.interceptor.security.callback.CallbackHandlerProviderUsernameToken;
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
    private List<CallbackHandlerProvider> callbackHandlerProviders;
    private boolean allowAnonymous = true;
    private boolean allowNamedPrincipals;

    public JAASLoginInterceptor() {
        this(Phase.UNMARSHAL);
    }

    public JAASLoginInterceptor(String phase) {
        super(phase);
        this.callbackHandlerProviders = new ArrayList<>();
        this.callbackHandlerProviders.add(new CallbackHandlerProviderAuthPol());
        this.callbackHandlerProviders.add(new CallbackHandlerProviderUsernameToken());
    }

    public void setContextName(String name) {
        contextName = name;
    }

    public String getContextName() {
        return contextName;
    }

    /**
     * @deprecated replaced by {@link #setRoleClassifier(String)}
     * @param name
     */
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

    private CallbackHandler getFirstCallbackHandler(Message message) {
        for (CallbackHandlerProvider cbp : callbackHandlerProviders) {
            CallbackHandler cbh = cbp.create(message);
            if (cbh != null) {
                return cbh;
            }
        }
        return null;
    }

    public void handleMessage(final Message message) {
        if (allowNamedPrincipals) {
            SecurityContext sc = message.get(SecurityContext.class);
            if (sc != null && sc.getUserPrincipal() != null
                && sc.getUserPrincipal().getName() != null) {
                return;
            }
        }

        CallbackHandler handler = getFirstCallbackHandler(message);

        if (handler == null && !allowAnonymous) {
            throw new AuthenticationException("Authentication required but no authentication information was supplied");
        }

        try {
            LoginContext ctx = new LoginContext(getContextName(), null, handler, loginConfig);
            ctx.login();
            Subject subject = ctx.getSubject();
            String name = getUsername(handler);
            message.put(SecurityContext.class, createSecurityContext(name, subject));

            // Run the further chain in the context of this subject.
            // This allows other code to retrieve the subject using pure JAAS
            if (useDoAs) {
                Subject.doAs(subject, new PrivilegedAction<Void>() {

                    @Override
                    public Void run() {
                        InterceptorChain chain = message.getInterceptorChain();
                        if (chain != null) {
                            message.put("suspend.chain.on.current.interceptor", Boolean.TRUE);
                            chain.doIntercept(message);
                        }
                        return null;
                    }
                });
            }

        } catch (LoginException ex) {
            String errorMessage = "Authentication failed: " + ex.getMessage();
            LOG.log(Level.FINE, errorMessage, ex);
            if (reportFault) {
                AuthenticationException aex = new AuthenticationException(errorMessage);
                aex.initCause(ex);
                throw aex;

            }
            throw new AuthenticationException("Authentication failed (details can be found in server log)");
        }
    }

    private String getUsername(CallbackHandler handler) {
        if (handler == null) {
            return null;
        }
        try {
            NameCallback usernameCallBack = new NameCallback("user");
            handler.handle(new Callback[]{usernameCallBack });
            return usernameCallBack.getName();
        } catch (Exception e) {
            return null;
        }
    }

    protected CallbackHandler getCallbackHandler(String name, String password) {
        return new NamePasswordCallbackHandler(name, password);
    }

    protected SecurityContext createSecurityContext(String name, Subject subject) {
        if (getRoleClassifier() != null) {
            return new RolePrefixSecurityContextImpl(subject, getRoleClassifier(),
                                                     getRoleClassifierType());
        }
        return new DefaultSecurityContext(name, subject);
    }

    public Configuration getLoginConfig() {
        return loginConfig;
    }

    public void setLoginConfig(Configuration loginConfig) {
        this.loginConfig = loginConfig;
    }

    public List<CallbackHandlerProvider> getCallbackHandlerProviders() {
        return callbackHandlerProviders;
    }

    public void setCallbackHandlerProviders(List<CallbackHandlerProvider> callbackHandlerProviders) {
        this.callbackHandlerProviders.clear();
        this.callbackHandlerProviders.addAll(callbackHandlerProviders);
    }

    public void addCallbackHandlerProviders(List<CallbackHandlerProvider> callbackHandlerProviders2) {
        this.callbackHandlerProviders.addAll(callbackHandlerProviders2);
    }

    public void setAllowAnonymous(boolean allowAnonymous) {
        this.allowAnonymous = allowAnonymous;
    }

    public void setAllowNamedPrincipals(boolean allowNamedPrincipals) {
        this.allowNamedPrincipals = allowNamedPrincipals;
    }

}
