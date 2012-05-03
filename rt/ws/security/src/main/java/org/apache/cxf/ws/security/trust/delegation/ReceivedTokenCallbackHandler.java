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

package org.apache.cxf.ws.security.trust.delegation;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.message.token.UsernameToken;
import org.apache.ws.security.saml.ext.AssertionWrapper;

/**
 * This CallbackHandler implementation obtains the previously received message from a 
 * DelegationCallback object, and obtains a received token 
 * (SAML/UsernameToken/BinarySecurityToken) from it to be used as the delegation token.
 */
public class ReceivedTokenCallbackHandler implements CallbackHandler {
    
    @SuppressWarnings("unchecked")
    public void handle(Callback[] callbacks)
        throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof DelegationCallback) {
                DelegationCallback callback = (DelegationCallback) callbacks[i];
                Message message = callback.getCurrentMessage();
                
                if (message != null 
                    && message.get(PhaseInterceptorChain.PREVIOUS_MESSAGE) != null) {
                    WeakReference<SoapMessage> wr = 
                        (WeakReference<SoapMessage>)
                            message.get(PhaseInterceptorChain.PREVIOUS_MESSAGE);
                    SoapMessage previousSoapMessage = wr.get();
                    Element token = getTokenFromMessage(previousSoapMessage);
                    if (token != null) {
                        callback.setToken(token);
                    }
                }
                
            } else {
                throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
            }
        }
    }
    
    private Element getTokenFromMessage(SoapMessage soapMessage) {
        if (soapMessage != null) {
            List<WSHandlerResult> results = 
                CastUtils.cast((List<?>)soapMessage.get(WSHandlerConstants.RECV_RESULTS));
            if (results != null) {
                for (WSHandlerResult rResult : results) {
                    Element token = findToken(rResult.getResults());
                    if (token != null) {
                        return token;
                    }
                }
            }
        }
        return null;
    }
    
    private Element findToken(
        List<WSSecurityEngineResult> wsSecEngineResults
    ) {
        for (WSSecurityEngineResult wser : wsSecEngineResults) {
            Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
            if (actInt.intValue() == WSConstants.ST_SIGNED
                || actInt.intValue() == WSConstants.ST_UNSIGNED) {
                AssertionWrapper assertion = 
                    (AssertionWrapper)wser.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
                return assertion.getElement();
            } else if (actInt.intValue() == WSConstants.UT
                || actInt.intValue() == WSConstants.UT_NOPASSWORD) {
                UsernameToken token =
                    (UsernameToken)wser.get(WSSecurityEngineResult.TAG_USERNAME_TOKEN);
                return token.getElement();
            } else if (actInt.intValue() == WSConstants.BST) {
                BinarySecurity token = 
                    (BinarySecurity)wser.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                return token.getElement();
            }
        }
        return null;
    }
    
}
