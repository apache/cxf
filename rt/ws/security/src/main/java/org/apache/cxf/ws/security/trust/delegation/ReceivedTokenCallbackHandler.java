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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.token.PKIPathSecurity;
import org.apache.wss4j.common.token.X509Security;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;

/**
 * This CallbackHandler implementation obtains the previously received message from a
 * DelegationCallback object, and obtains a received token
 * (SAML/UsernameToken/BinarySecurityToken) from it to be used as the delegation token.
 */
public class ReceivedTokenCallbackHandler implements CallbackHandler {

    private static final List<Integer> DEFAULT_SECURITY_PRIORITIES = new ArrayList<>();
    static {
        DEFAULT_SECURITY_PRIORITIES.add(WSConstants.ST_SIGNED);
        DEFAULT_SECURITY_PRIORITIES.add(WSConstants.ST_UNSIGNED);
        DEFAULT_SECURITY_PRIORITIES.add(WSConstants.UT);
        DEFAULT_SECURITY_PRIORITIES.add(WSConstants.BST);
        DEFAULT_SECURITY_PRIORITIES.add(WSConstants.UT_NOPASSWORD);
    }

    private List<Integer> securityPriorities = new ArrayList<>(DEFAULT_SECURITY_PRIORITIES);

    private boolean useTransformedToken = true;

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
                for (WSHandlerResult handlerResult : results) {
                    Element token = getTokenFromResults(handlerResult);
                    if (token != null) {
                        return token;
                    }
                }
            }
        }
        return null;
    }

    private Element getTokenFromResults(WSHandlerResult handlerResult) {
        // Now go through the results in a certain order. Highest priority is first.
        Map<Integer, List<WSSecurityEngineResult>> actionResults = handlerResult.getActionResults();
        for (Integer resultPriority : securityPriorities) {
            List<WSSecurityEngineResult> foundResults = actionResults.get(resultPriority);
            if (foundResults != null && !foundResults.isEmpty()) {
                for (WSSecurityEngineResult result : foundResults) {

                    if (!skipResult(resultPriority, result)) {
                        // First check for a transformed token
                        Object transformedToken = result.get(WSSecurityEngineResult.TAG_TRANSFORMED_TOKEN);
                        if (useTransformedToken && transformedToken instanceof SamlAssertionWrapper) {
                            return ((SamlAssertionWrapper)transformedToken).getElement();
                        }

                        if (result.get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT) != null) {
                            return (Element)result.get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT);
                        }
                    }
                }
            }
        }

        return null;
    }

    protected boolean skipResult(Integer resultPriority, WSSecurityEngineResult result) {
        Object binarySecurity = result.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);

        return resultPriority == WSConstants.BST
            && (binarySecurity instanceof X509Security || binarySecurity instanceof PKIPathSecurity);
    }

    public boolean isUseTransformedToken() {
        return useTransformedToken;
    }

    /**
     * Set whether to use the transformed token if it is available from a previous security result.
     * It false, it uses the original "received" token instead. The default is "true".
     * @param useTransformedToken whether to use the transformed token if it is available
     */
    public void setUseTransformedToken(boolean useTransformedToken) {
        this.useTransformedToken = useTransformedToken;
    }

    public List<Integer> getSecurityPriorities() {
        return securityPriorities;
    }

    public void setSecurityPriorities(List<Integer> securityPriorities) {
        this.securityPriorities = securityPriorities;
    }

}
