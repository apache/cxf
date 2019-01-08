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

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.Credential;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AuthPolicyValidatingInterceptorTest {

    @Test
    public void testValidateAuthorizationPolicy() throws Exception {
        AuthPolicyValidatingInterceptor in = new AuthPolicyValidatingInterceptor();
        TestSTSTokenValidator validator = new TestSTSTokenValidator();
        in.setValidator(validator);

        AuthorizationPolicy policy = new AuthorizationPolicy();
        policy.setUserName("bob");
        policy.setPassword("pswd");
        Message message = new MessageImpl();
        message.put(AuthorizationPolicy.class, policy);

        in.handleMessage(message);

        assertTrue(validator.isValidated());
    }

    @Test
    public void testInvalidUsernamePassword() throws Exception {
        AuthPolicyValidatingInterceptor in = new AuthPolicyValidatingInterceptor();
        TestSTSTokenValidator validator = new TestSTSTokenValidator();
        in.setValidator(validator);

        AuthorizationPolicy policy = new AuthorizationPolicy();
        policy.setUserName("bob");
        policy.setPassword("pswd2");
        Message message = new MessageImpl();
        message.put(AuthorizationPolicy.class, policy);

        in.handleMessage(message);

        assertFalse(validator.isValidated());
    }

    @Test
    public void testNoUsername() throws Exception {
        AuthPolicyValidatingInterceptor in = new AuthPolicyValidatingInterceptor();
        TestSTSTokenValidator validator = new TestSTSTokenValidator();
        in.setValidator(validator);

        AuthorizationPolicy policy = new AuthorizationPolicy();
        policy.setPassword("pswd");
        Message message = new MessageImpl();
        message.put(AuthorizationPolicy.class, policy);

        try {
            in.handleMessage(message);
            fail("Failure expected with no username");
        } catch (SecurityException ex) {
            // expected
        }
    }

    private static class TestSTSTokenValidator extends STSTokenValidator {

        private boolean validated;

        TestSTSTokenValidator() {
            super(true);
        }

        @Override
        public Credential validateWithSTS(Credential credential, Message message)
            throws WSSecurityException {
            UsernameToken token = credential.getUsernametoken();
            if ("bob".equals(token.getName()) && "pswd".equals(token.getPassword())) {
                // TODO: mock STS
                validated = true;
            }
            return credential;
        }

        public boolean isValidated() {
            return validated;
        }
    }
}