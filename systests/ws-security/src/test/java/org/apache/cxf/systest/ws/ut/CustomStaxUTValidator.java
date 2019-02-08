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

package org.apache.cxf.systest.ws.ut;

import javax.security.auth.Subject;

import org.apache.cxf.common.security.SimpleGroup;
import org.apache.wss4j.binding.wss10.UsernameTokenType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.stax.impl.securityToken.UsernameSecurityTokenImpl;
import org.apache.wss4j.stax.securityToken.UsernameSecurityToken;
import org.apache.wss4j.stax.validate.TokenContext;
import org.apache.wss4j.stax.validate.UsernameTokenValidatorImpl;
import org.apache.xml.security.stax.securityToken.InboundSecurityToken;

/**
 * A custom UsernameToken Validator that wraps the default Validator in WSS4J and set a Subject
 * on the context as well. It adds a role for "Alice" of "manager", and a role for everyone of
 * "worker".
 */
public class CustomStaxUTValidator extends UsernameTokenValidatorImpl {

    @SuppressWarnings("unchecked")
    @Override
    public <T extends UsernameSecurityToken & InboundSecurityToken> T validate(
            UsernameTokenType usernameTokenType, TokenContext tokenContext) throws WSSecurityException {
        UsernameSecurityTokenImpl token =
            super.</*fake @see above*/UsernameSecurityTokenImpl>validate(usernameTokenType, tokenContext);

        Subject subject = new Subject();
        subject.getPrincipals().add(token.getPrincipal());
        if ("Alice".equals(token.getUsername())) {
            subject.getPrincipals().add(new SimpleGroup("manager", token.getUsername()));
        }
        subject.getPrincipals().add(new SimpleGroup("worker", token.getUsername()));
        token.setSubject(subject);

        return (T)token;
    }
}
