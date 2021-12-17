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

package org.apache.cxf.ws.security.policy.interceptors;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.policy.interceptors.IssuedTokenInterceptorProvider.IssuedTokenOutInterceptor;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.tokenstore.TokenStoreUtils;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.SecureConversationToken;
import org.apache.wss4j.policy.model.Trust10;
import org.apache.wss4j.policy.model.Trust13;

class SecureConversationOutInterceptor extends AbstractPhaseInterceptor<SoapMessage> {

    private static final Logger LOG = LogUtils.getL7dLogger(SecureConversationOutInterceptor.class);

    SecureConversationOutInterceptor() {
        super(Phase.PREPARE_SEND);
        addBefore(SpnegoContextTokenOutInterceptor.class.getName());
        addBefore(IssuedTokenOutInterceptor.class.getName());
    }

    public void handleMessage(SoapMessage message) throws Fault {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        // extract Assertion information
        if (aim != null) {
            Collection<AssertionInfo> ais =
                PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.SECURE_CONVERSATION_TOKEN);
            if (ais.isEmpty()) {
                return;
            }
            if (isRequestor(message)) {
                SecureConversationToken itok = (SecureConversationToken)ais.iterator()
                    .next().getAssertion();

                try {
                    SecurityToken tok = (SecurityToken) message.getContextualProperty(SecurityConstants.TOKEN);
                    if (tok == null) {
                        String tokId = (String) message.getContextualProperty(SecurityConstants.TOKEN_ID);
                        if (tokId != null) {
                            tok = TokenStoreUtils.getTokenStore(message).getToken(tokId);
                        }
                    }
                    if (tok == null) {
                        tok = issueToken(message, aim, itok);
                    } else {
                        tok = renewToken(message, aim, tok, itok);
                    }
                    if (tok != null) {
                        for (AssertionInfo ai : ais) {
                            ai.setAsserted(true);
                        }
                        message.getExchange().getEndpoint().put(SecurityConstants.TOKEN, tok);
                        message.getExchange().getEndpoint().put(SecurityConstants.TOKEN_ID, tok.getId());
                        message.getExchange().put(SecurityConstants.TOKEN_ID, tok.getId());
                        message.getExchange().put(SecurityConstants.TOKEN, tok);
                        TokenStoreUtils.getTokenStore(message).add(tok);
                    }
                    PolicyUtils.assertPolicy(aim, SPConstants.BOOTSTRAP_POLICY);
                } catch (TokenStoreException ex) {
                    throw new Fault(ex);
                }
            } else {
                //server side should be checked on the way in
                for (AssertionInfo ai : ais) {
                    ai.setAsserted(true);
                }
                PolicyUtils.assertPolicy(aim, SPConstants.BOOTSTRAP_POLICY);
            }
        }
    }


    private SecurityToken renewToken(SoapMessage message,
                            AssertionInfoMap aim,
                            SecurityToken tok,
                            SecureConversationToken itok) throws TokenStoreException {
        if (!tok.isExpired()) {
            return tok;
        }

        // Remove the old token
        message.getExchange().getEndpoint().remove(SecurityConstants.TOKEN);
        message.getExchange().getEndpoint().remove(SecurityConstants.TOKEN_ID);
        message.getExchange().remove(SecurityConstants.TOKEN_ID);
        message.getExchange().remove(SecurityConstants.TOKEN);
        TokenStoreUtils.getTokenStore(message).remove(tok.getId());

        STSClient client = STSUtils.getClient(message, "sct");
        AddressingProperties maps =
            (AddressingProperties)message
                .get("jakarta.xml.ws.addressing.context.outbound");
        if (maps == null) {
            maps = (AddressingProperties)message
                .get("jakarta.xml.ws.addressing.context");
        } else if (maps.getAction().getValue().endsWith("Renew")) {
            return tok;
        }
        synchronized (client) {
            try {
                SecureConversationTokenInterceptorProvider.setupClient(client, message, aim, itok, true);

                String s = message.getContextualProperty(Message.ENDPOINT_ADDRESS).toString();
                client.setLocation(s);

                Map<String, Object> ctx = client.getRequestContext();
                ctx.put(SecurityConstants.TOKEN_ID, tok.getId());
                if (maps != null) {
                    client.setAddressingNamespace(maps.getNamespaceURI());
                }
                return client.renewSecurityToken(tok);
            } catch (RuntimeException ex) {
                LOG.log(Level.WARNING, "Error renewing a token", ex);
                boolean issueAfterFailedRenew =
                    MessageUtils.getContextualBoolean(
                        message, SecurityConstants.STS_ISSUE_AFTER_FAILED_RENEW, true
                    );
                if (issueAfterFailedRenew) {
                    // Perhaps the STS does not support renewing, so try to issue a new token
                    return issueToken(message, aim, itok);
                }
                throw ex;
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Error renewing a token", ex);
                boolean issueAfterFailedRenew =
                    MessageUtils.getContextualBoolean(
                        message, SecurityConstants.STS_ISSUE_AFTER_FAILED_RENEW, true
                    );
                if (issueAfterFailedRenew) {
                    // Perhaps the STS does not support renewing, so try to issue a new token
                    return issueToken(message, aim, itok);
                }
                throw new Fault(ex);
            } finally {
                client.setTrust((Trust10)null);
                client.setTrust((Trust13)null);
                client.setTemplate(null);
                client.setLocation(null);
                client.setAddressingNamespace(null);
            }
        }
    }
    private SecurityToken issueToken(SoapMessage message,
                                     AssertionInfoMap aim,
                                     SecureConversationToken itok) {
        STSClient client = STSUtils.getClient(message, "sct");
        AddressingProperties maps =
            (AddressingProperties)message
                .get("jakarta.xml.ws.addressing.context.outbound");
        if (maps == null) {
            maps = (AddressingProperties)message
                .get("jakarta.xml.ws.addressing.context");
        }
        synchronized (client) {
            try {
                String s = SecureConversationTokenInterceptorProvider
                    .setupClient(client, message, aim, itok, false);

                if (maps != null) {
                    client.setAddressingNamespace(maps.getNamespaceURI());
                }
                SecurityToken tok = client.requestSecurityToken(s);
                String tokenType = tok.getTokenType();
                tok.setTokenType(tokenType);
                if (tokenType == null || "".equals(tokenType)) {
                    tok.setTokenType(WSS4JConstants.WSC_SCT);
                }
                return tok;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new Fault(e);
            } finally {
                client.setTrust((Trust10)null);
                client.setTrust((Trust13)null);
                client.setTemplate(null);
                client.setLocation(null);
                client.setAddressingNamespace(null);
            }
        }
    }

}
