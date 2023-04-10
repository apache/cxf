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

import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;

import org.apache.cxf.common.security.SecurityToken;
import org.apache.cxf.common.security.UsernameToken;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.interceptor.security.callback.CallbackHandlerProvider;
import org.apache.cxf.interceptor.security.callback.CallbackHandlerTlsCert;
import org.apache.cxf.interceptor.security.callback.CertKeyToUserNameMapper;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.security.transport.TLSSessionInfo;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JAASLoginInterceptorTest {

    private static final String TEST_SUBJECT_DN = "CN=" + TestUserPasswordLoginModule.TESTUSER
                                                  + ", o=Test Org";

    /**
     * Using default CallbackhandlerProviders and no authentication information
     */
    @Test(expected = AuthenticationException.class)
    public void testLoginWithDefaultHandler() {
        JAASLoginInterceptor jaasInt = createTestJaasLoginInterceptor();
        Message message = new MessageImpl();
        jaasInt.handleMessage(message);
    }

    /**
     * Using default CallbackhandlerProviders and AuthPolicy
     */
    @Test
    public void testLoginWithDefaultHandlerAndAuthPol() {
        JAASLoginInterceptor jaasInt = createTestJaasLoginInterceptor();
        Message message = new MessageImpl();
        addAuthPolicy(message, TestUserPasswordLoginModule.TESTUSER, TestUserPasswordLoginModule.TESTPASS);
        jaasInt.handleMessage(message);
    }

    @Test(expected = AuthenticationException.class)
    public void testLoginWithDefaultHandlerAndAuthPolWrongPass() {
        JAASLoginInterceptor jaasInt = createTestJaasLoginInterceptor();
        Message message = new MessageImpl();
        addAuthPolicy(message, TestUserPasswordLoginModule.TESTUSER, "wrong");
        jaasInt.handleMessage(message);
    }

    /**
     * Using default CallbackhandlerProviders and UserNameToken
     */
    @Test
    public void testLoginWithDefaultHandlerAndUsernameToken() {
        JAASLoginInterceptor jaasInt = createTestJaasLoginInterceptor();
        Message message = new MessageImpl();
        addUsernameToken(message, TestUserPasswordLoginModule.TESTUSER, TestUserPasswordLoginModule.TESTPASS);
        jaasInt.handleMessage(message);
    }

    @Test(expected = AuthenticationException.class)
    public void testLoginWithDefaultHandlerAndUsernameTokenWrongPass() {
        JAASLoginInterceptor jaasInt = createTestJaasLoginInterceptor();
        Message message = new MessageImpl();
        addUsernameToken(message, TestUserPasswordLoginModule.TESTUSER, "wrong");
        jaasInt.handleMessage(message);
    }

    @Test
    public void testLoginWithTlsHandler() {
        JAASLoginInterceptor jaasInt = createTestJaasLoginInterceptor();
        CallbackHandlerTlsCert tlsHandler = new CallbackHandlerTlsCert();
        tlsHandler.setFixedPassword(TestUserPasswordLoginModule.TESTPASS);
        CertKeyToUserNameMapper certMapper = new CertKeyToUserNameMapper();
        certMapper.setKey("CN");
        tlsHandler.setCertMapper(certMapper);
        jaasInt.setCallbackHandlerProviders(Collections.singletonList((CallbackHandlerProvider)tlsHandler));
        Message message = new MessageImpl();
        TLSSessionInfo sessionInfo = new TLSSessionInfo("", null, new Certificate[] {
            createTestCert(TEST_SUBJECT_DN)
        });
        message.put(TLSSessionInfo.class, sessionInfo);

        jaasInt.handleMessage(message);
    }

    private X509Certificate createTestCert(String subjectDn) {
        X509Certificate cert = mock(X509Certificate.class);
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(subjectDn);
        when(cert.getSubjectDN()).thenReturn(principal);
        return cert;
    }

    private void addAuthPolicy(Message message, String username, String password) {
        AuthorizationPolicy authPol = new AuthorizationPolicy();
        authPol.setUserName(username);
        authPol.setPassword(password);
        message.put(AuthorizationPolicy.class, authPol);
    }

    private void addUsernameToken(Message message, String username, String password) {
        UsernameToken token = new UsernameToken(username, password, "", false, null, "");
        message.put(SecurityToken.class, token);
    }

    private JAASLoginInterceptor createTestJaasLoginInterceptor() {
        JAASLoginInterceptor jaasInt = new JAASLoginInterceptor();
        jaasInt.setReportFault(true);
        Configuration config = new Configuration() {

            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                Map<String, String> options = new HashMap<>();
                AppConfigurationEntry configEntry = new AppConfigurationEntry(
                                                                              TestUserPasswordLoginModule.class
                                                                                  .getName(),
                                                                              LoginModuleControlFlag.REQUIRED,
                                                                              options);
                return Collections.singleton(configEntry).toArray(new AppConfigurationEntry[] {});
            }
        };
        jaasInt.setLoginConfig(config);
        return jaasInt;
    }

}
