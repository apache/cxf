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

import javax.security.auth.Subject;

import org.apache.cxf.common.security.SecurityToken;
import org.apache.cxf.common.security.TokenType;
import org.apache.cxf.common.security.UsernameToken;

public abstract class AbstractUsernameTokenInInterceptor extends AbstractSecurityContextInInterceptor {

    protected Subject createSubject(SecurityToken token) {
        if (token.getTokenType() != TokenType.UsernameToken) {
            reportSecurityException("Unsupported token type " + token.getTokenType().toString());
        }
        UsernameToken ut = (UsernameToken)token;
        return createSubject(ut);
    }

    protected abstract Subject createSubject(UsernameToken token);


}
