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
package org.apache.cxf.systest.jaxrs.security.oauth2.grants;

import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Document;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.grants.owner.ResourceOwnerLoginHandler;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.UsernameTokenValidator;

/**
 * A simple ResourceOwnerLoginHandler implementation that delegates the username/password to a CallbackHandler
 */
public class CallbackHandlerLoginHandler implements ResourceOwnerLoginHandler {

    private CallbackHandler callbackHandler;

    static {
        WSSConfig.init();
    }

    @Override
    public UserSubject createSubject(Client client, String user, String pass) {
        Document doc = DOMUtils.getEmptyDocument();
        UsernameToken token = new UsernameToken(false, doc,
                                                WSS4JConstants.PASSWORD_TEXT);
        token.setName(user);
        token.setPassword(pass);

        Credential credential = new Credential();
        credential.setUsernametoken(token);

        RequestData data = new RequestData();
        data.setMsgContext(PhaseInterceptorChain.getCurrentMessage());
        data.setCallbackHandler(callbackHandler);
        UsernameTokenValidator validator = new UsernameTokenValidator();

        try {
            credential = validator.validate(credential, data);

            UserSubject subject = new UserSubject();
            subject.setLogin(user);
            return subject;
        } catch (Exception ex) {
            throw ExceptionUtils.toInternalServerErrorException(ex, null);
        }
    }

    public CallbackHandler getCallbackHandler() {
        return callbackHandler;
    }

    public void setCallbackHandler(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

}