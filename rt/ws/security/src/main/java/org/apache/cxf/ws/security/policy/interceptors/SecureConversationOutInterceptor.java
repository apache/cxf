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

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.SecureConversationToken;
import org.apache.cxf.ws.security.policy.model.Trust10;
import org.apache.cxf.ws.security.policy.model.Trust13;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.ws.security.WSConstants;

class SecureConversationOutInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
    public SecureConversationOutInterceptor() {
        super(Phase.PREPARE_SEND);
    }
    public void handleMessage(SoapMessage message) throws Fault {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        // extract Assertion information
        if (aim != null) {
            Collection<AssertionInfo> ais = aim.get(SP12Constants.SECURE_CONVERSATION_TOKEN);
            if (ais == null || ais.isEmpty()) {
                return;
            }
            if (isRequestor(message)) {
                SecureConversationToken itok = (SecureConversationToken)ais.iterator()
                    .next().getAssertion();
                
                SecurityToken tok = (SecurityToken)message.getContextualProperty(SecurityConstants.TOKEN);
                if (tok == null) {
                    String tokId = (String)message.getContextualProperty(SecurityConstants.TOKEN_ID);
                    if (tokId != null) {
                        tok = SecureConversationTokenInterceptorProvider
                            .getTokenStore(message).getToken(tokId);
                    }
                }
                if (tok == null) {
                    tok = issueToken(message, aim, itok);
                } else {
                    renewToken(message, aim, tok, itok);
                }
                if (tok != null) {
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }
                    message.getExchange().get(Endpoint.class).put(SecurityConstants.TOKEN_ID, 
                                                                  tok.getId());
                    message.getExchange().put(SecurityConstants.TOKEN_ID, 
                                              tok.getId());
                    SecureConversationTokenInterceptorProvider.getTokenStore(message).add(tok);
                    
                }
            } else {
                //server side should be checked on the way in
                for (AssertionInfo ai : ais) {
                    ai.setAsserted(true);
                }                    
            }
        }
    }
    
    
    private void renewToken(SoapMessage message,
                            AssertionInfoMap aim, 
                            SecurityToken tok,
                            SecureConversationToken itok) {
        if (tok.getState() != SecurityToken.State.EXPIRED) {
            return;
        }
        
        STSClient client = SecureConversationTokenInterceptorProvider.getClient(message);
        AddressingProperties maps =
            (AddressingProperties)message
                .get("javax.xml.ws.addressing.context.outbound");
        if (maps == null) {
            maps = (AddressingProperties)message
                .get("javax.xml.ws.addressing.context");
        } else if (maps.getAction().getValue().endsWith("Renew")) {
            return;
        }
        synchronized (client) {
            try {
                SecureConversationTokenInterceptorProvider.setupClient(client, message, aim, itok, true);

                String s = message
                    .getContextualProperty(Message.ENDPOINT_ADDRESS).toString();
                client.setLocation(s);
                
                Map<String, Object> ctx = client.getRequestContext();
                ctx.put(SecurityConstants.TOKEN, tok);
                if (maps != null) {
                    client.setAddressingNamespace(maps.getNamespaceURI());
                }
                client.renewSecurityToken(tok);
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
    private SecurityToken issueToken(SoapMessage message,
                                     AssertionInfoMap aim,
                                     SecureConversationToken itok) {
        STSClient client = SecureConversationTokenInterceptorProvider.getClient(message);
        AddressingProperties maps =
            (AddressingProperties)message
                .get("javax.xml.ws.addressing.context.outbound");
        if (maps == null) {
            maps = (AddressingProperties)message
                .get("javax.xml.ws.addressing.context");
        }
        synchronized (client) {
            try {
                String s = SecureConversationTokenInterceptorProvider
                    .setupClient(client, message, aim, itok, false);

                SecurityToken tok = null;
                if (maps != null) {
                    client.setAddressingNamespace(maps.getNamespaceURI());
                }
                tok = client.requestSecurityToken(s);
                tok.setTokenType(WSConstants.WSC_SCT);
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