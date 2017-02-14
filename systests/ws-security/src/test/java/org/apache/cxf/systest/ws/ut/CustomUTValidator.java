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
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.WSUsernameTokenPrincipalImpl;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.UsernameTokenValidator;

/**
 * A custom UsernameToken Validator that wraps the default Validator in WSS4J and set a Subject
 * on the context as well. It adds a role for "Alice" of "manager", and a role for everyone of
 * "worker".
 */
public class CustomUTValidator extends UsernameTokenValidator {

    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        Credential cred = super.validate(credential, data);

        UsernameToken ut = credential.getUsernametoken();
        WSUsernameTokenPrincipalImpl principal =
            new WSUsernameTokenPrincipalImpl(ut.getName(), ut.isHashed());
        principal.setCreatedTime(ut.getCreated());
        principal.setNonce(principal.getNonce());
        principal.setPassword(ut.getPassword());
        principal.setPasswordType(ut.getPasswordType());

        Subject subject = new Subject();
        subject.getPrincipals().add(principal);
        if ("Alice".equals(ut.getName())) {
            subject.getPrincipals().add(new SimpleGroup("manager", ut.getName()));
        }
        subject.getPrincipals().add(new SimpleGroup("worker", ut.getName()));
        cred.setSubject(subject);

        return cred;
    }
}
