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
package org.apache.cxf.interceptor.security.callback;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.common.security.SecurityToken;
import org.apache.cxf.common.security.UsernameToken;
import org.apache.cxf.interceptor.security.NameDigestPasswordCallbackHandler;
import org.apache.cxf.interceptor.security.NamePasswordCallbackHandler;
import org.apache.cxf.message.Message;

public class CallbackHandlerProviderUsernameToken implements CallbackHandlerProvider {

    @Override
    public CallbackHandler create(Message message) {
        SecurityToken token = message.get(SecurityToken.class);
        if (!(token instanceof UsernameToken)) {
            return null;
        }
        UsernameToken ut = (UsernameToken)token;
        if (ut.getPasswordType().endsWith("PasswordDigest")) {
            return new NameDigestPasswordCallbackHandler(ut.getName(),
                                                         ut.getPassword(),
                                                         ut.getNonce(),
                                                         ut.getCreatedTime());
        }
        return new NamePasswordCallbackHandler(ut.getName(), ut.getPassword());
    }

}
