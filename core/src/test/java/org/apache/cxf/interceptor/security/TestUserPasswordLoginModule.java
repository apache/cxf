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

import java.io.IOException;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.cxf.common.security.SimpleGroup;
import org.apache.cxf.common.security.SimplePrincipal;

public class TestUserPasswordLoginModule implements LoginModule {

    public static final String TESTGROUP = "testgroup";
    public static final String TESTPASS = "testpass";
    public static final String TESTUSER = "testuser";
    private Subject subject;
    private CallbackHandler callbackHandler;

    @Override
    public void initialize(Subject subject2, CallbackHandler callbackHandler2, Map<String, ?> sharedState,
                           Map<String, ?> options) {
        this.subject = subject2;
        this.callbackHandler = callbackHandler2;
    }

    @Override
    public boolean login() throws LoginException {
        NameCallback nameCallback = new NameCallback("User");
        PasswordCallback passwordCallback = new PasswordCallback("Password", false);
        Callback[] callbacks = new Callback[] {nameCallback, passwordCallback};
        try {
            this.callbackHandler.handle(callbacks);
        } catch (IOException | UnsupportedCallbackException e) {
            throw new LoginException(e.getMessage());
        }
        String userName = nameCallback.getName();
        String password = new String(passwordCallback.getPassword());
        if (!TESTUSER.equals(userName)) {
            throw new LoginException("wrong username");
        }
        if (!TESTPASS.equals(password)) {
            throw new LoginException("wrong password");
        }
        subject.getPrincipals().add(new SimplePrincipal(userName));
        subject.getPrincipals().add(new SimpleGroup(TESTGROUP));
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        return false;
    }

    @Override
    public boolean logout() throws LoginException {
        return false;
    }

}
