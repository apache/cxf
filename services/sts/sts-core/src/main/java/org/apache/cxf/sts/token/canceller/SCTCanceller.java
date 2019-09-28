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

package org.apache.cxf.sts.token.canceller;

import java.security.Key;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.SecretKey;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.token.SecurityContextToken;
import org.apache.wss4j.stax.securityEvent.WSSecurityEventConstants;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.securityEvent.AbstractSecuredElementSecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;

/**
 * This class cancels a SecurityContextToken.
 */
public class SCTCanceller implements TokenCanceller {

    private static final Logger LOG = LogUtils.getL7dLogger(SCTCanceller.class);

    // boolean to enable/disable the check of proof of possession
    private boolean verifyProofOfPossession = true;

    /**
     * Return true if this TokenCanceller implementation is capable of cancelling the
     * ReceivedToken argument.
     */
    public boolean canHandleToken(ReceivedToken targetToken) {
        Object token = targetToken.getToken();
        if (token instanceof Element) {
            Element tokenElement = (Element)token;
            String namespace = tokenElement.getNamespaceURI();
            String localname = tokenElement.getLocalName();
            if ((STSUtils.SCT_NS_05_02.equals(namespace)
                || STSUtils.SCT_NS_05_12.equals(namespace))
                && "SecurityContextToken".equals(localname)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cancel a Token using the given TokenCancellerParameters.
     */
    public TokenCancellerResponse cancelToken(TokenCancellerParameters tokenParameters) {
        LOG.fine("Trying to cancel a SecurityContextToken");
        TokenCancellerResponse response = new TokenCancellerResponse();
        ReceivedToken cancelTarget = tokenParameters.getToken();

        if (tokenParameters.getTokenStore() == null) {
            LOG.log(Level.FINE, "A cache must be configured to use the SCTCanceller");
            return response;
        }
        if (cancelTarget == null) {
            LOG.log(Level.FINE, "Cancel Target is null");
            return response;
        }

        cancelTarget.setState(STATE.NONE);
        response.setToken(cancelTarget);

        if (cancelTarget.isDOMElement()) {
            try {
                Element cancelTargetElement = (Element)cancelTarget.getToken();
                SecurityContextToken sct = new SecurityContextToken(cancelTargetElement);
                String identifier = sct.getIdentifier();
                SecurityToken token = tokenParameters.getTokenStore().getToken(identifier);
                if (token == null) {
                    LOG.fine("Identifier: " + identifier + " is not found in the cache");
                    return response;
                }
                if (verifyProofOfPossession && !matchKey(tokenParameters, token.getSecret())) {
                    throw new STSException(
                        "Failed to verify the proof of possession of the key associated with the "
                        + "security context. No matching key found in the request.",
                        STSException.INVALID_REQUEST
                    );
                }
                tokenParameters.getTokenStore().remove(token.getId());
                cancelTarget.setState(STATE.CANCELLED);
                LOG.fine("SecurityContextToken successfully cancelled");
            } catch (WSSecurityException ex) {
                LOG.log(Level.WARNING, "", ex);
            }
        }
        return response;
    }

    private boolean matchKey(TokenCancellerParameters tokenParameters, byte[] secretKey) {
        Map<String, Object> messageContext = tokenParameters.getMessageContext();

        if (matchDOMSignatureSecret(messageContext, secretKey)) {
            return true;
        }

        try {
            if (matchStreamingSignatureSecret(messageContext, secretKey)) {
                return true;
            }
        } catch (XMLSecurityException ex) {
            LOG.log(Level.FINE, ex.getMessage(), ex);
            return false;
        }

        return false;
    }

    /**
     * Set whether proof of possession is required or not to cancel a token
     */
    public void setVerifyProofOfPossession(boolean verifyProofOfPossession) {
        this.verifyProofOfPossession = verifyProofOfPossession;
    }

    private boolean matchDOMSignatureSecret(
        Map<String, Object> messageContext, byte[] secretToMatch
    ) {
        final List<WSHandlerResult> handlerResults =
            CastUtils.cast((List<?>) messageContext.get(WSHandlerConstants.RECV_RESULTS));

        if (handlerResults != null && !handlerResults.isEmpty()) {
            WSHandlerResult handlerResult = handlerResults.get(0);
            List<WSSecurityEngineResult> signedResults =
                handlerResult.getActionResults().get(WSConstants.SIGN);

            if (signedResults != null) {
                for (WSSecurityEngineResult engineResult : signedResults) {
                    byte[] receivedKey = (byte[])engineResult.get(WSSecurityEngineResult.TAG_SECRET);
                    if (MessageDigest.isEqual(secretToMatch, receivedKey)) {
                        LOG.log(
                                Level.FINE,
                                "Verification of the proof of possession of the key associated with "
                                + "the security context successful."
                        );
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean matchStreamingSignatureSecret(
        Map<String, Object> messageContext, byte[] secretToMatch
    ) throws XMLSecurityException {
        @SuppressWarnings("unchecked")
        final List<SecurityEvent> incomingEventList =
            (List<SecurityEvent>) messageContext.get(SecurityEvent.class.getName() + ".in");
        if (incomingEventList != null) {
            for (SecurityEvent incomingEvent : incomingEventList) {
                if (WSSecurityEventConstants.SIGNED_PART == incomingEvent.getSecurityEventType()
                    || WSSecurityEventConstants.SignedElement
                        == incomingEvent.getSecurityEventType()) {
                    org.apache.xml.security.stax.securityToken.SecurityToken token =
                        ((AbstractSecuredElementSecurityEvent)incomingEvent).getSecurityToken();
                    if (token != null && token.getSecretKey() != null) {
                        for (String key : token.getSecretKey().keySet()) {
                            Key keyObject = token.getSecretKey().get(key);
                            if (keyObject instanceof SecretKey
                                && MessageDigest.isEqual(secretToMatch, ((SecretKey)keyObject).getEncoded())) {
                                LOG.log(
                                    Level.FINE,
                                    "Verification of the proof of possession of the key associated with "
                                    + "the security context successful."
                                );
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }
}
