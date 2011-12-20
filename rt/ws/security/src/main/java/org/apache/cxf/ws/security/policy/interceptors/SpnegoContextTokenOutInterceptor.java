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

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.Trust10;
import org.apache.cxf.ws.security.policy.model.Trust13;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.spnego.SpnegoTokenContext;
import org.apache.ws.security.util.Base64;

class SpnegoContextTokenOutInterceptor extends AbstractPhaseInterceptor<SoapMessage> {
    public SpnegoContextTokenOutInterceptor() {
        super(Phase.PREPARE_SEND);
    }
    public void handleMessage(SoapMessage message) throws Fault {
        AssertionInfoMap aim = message.get(AssertionInfoMap.class);
        // extract Assertion information
        if (aim != null) {
            Collection<AssertionInfo> ais = aim.get(SP12Constants.SPNEGO_CONTEXT_TOKEN);
            if (ais == null || ais.isEmpty()) {
                return;
            }
            if (isRequestor(message)) {
                SecurityToken tok = (SecurityToken)message.getContextualProperty(SecurityConstants.TOKEN);
                if (tok == null) {
                    String tokId = (String)message.getContextualProperty(SecurityConstants.TOKEN_ID);
                    if (tokId != null) {
                        tok = NegotiationUtils.getTokenStore(message).getToken(tokId);
                    }
                }
                if (tok == null) {
                    tok = issueToken(message, aim);
                }
                if (tok != null) {
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }
                    message.getExchange().get(Endpoint.class).put(SecurityConstants.TOKEN_ID, tok.getId());
                    message.getExchange().put(SecurityConstants.TOKEN_ID, tok.getId());
                    NegotiationUtils.getTokenStore(message).add(tok);
                }
            } else {
                // server side should be checked on the way in
                for (AssertionInfo ai : ais) {
                    ai.setAsserted(true);
                }                   
            }
        }
    }
    
    
    private SecurityToken issueToken(SoapMessage message, AssertionInfoMap aim) {
        //
        // Get a SPNEGO token
        //
        String jaasContext = 
            (String)message.getContextualProperty(SecurityConstants.KERBEROS_JAAS_CONTEXT_NAME);
        String kerberosSpn = 
            (String)message.getContextualProperty(SecurityConstants.KERBEROS_SPN);
        CallbackHandler callbackHandler = 
            NegotiationUtils.getCallbackHandler(
                message.getContextualProperty(SecurityConstants.CALLBACK_HANDLER), this.getClass()
            );
        
        SpnegoTokenContext spnegoToken = new SpnegoTokenContext();
        try {
            spnegoToken.retrieveServiceTicket(jaasContext, callbackHandler, kerberosSpn);
        } catch (WSSecurityException e) {
            throw new Fault(e);
        }
        
        //
        // Now initiate WS-Trust exchange
        //
        STSClient client = STSUtils.getClient(message, "spnego");
        AddressingProperties maps =
            (AddressingProperties)message.get("javax.xml.ws.addressing.context.outbound");
        if (maps == null) {
            maps = (AddressingProperties)message.get("javax.xml.ws.addressing.context");
        }
        synchronized (client) {
            try {
                String s = SpnegoTokenInterceptorProvider.setupClient(client, message, aim);
                if (maps != null) {
                    client.setAddressingNamespace(maps.getNamespaceURI());
                }
                SecurityToken tok = client.requestSecurityToken(s, Base64.encode(spnegoToken.getToken()));
                
                byte[] wrappedTok = spnegoToken.unwrapKey(tok.getSecret());
                tok.setSecret(wrappedTok);
                spnegoToken.clear();
                
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