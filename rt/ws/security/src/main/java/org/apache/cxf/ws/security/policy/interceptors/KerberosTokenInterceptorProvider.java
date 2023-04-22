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

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.crypto.SecretKey;
import javax.xml.namespace.QName;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.kerberos.KerberosClient;
import org.apache.cxf.ws.security.kerberos.KerberosUtils;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.tokenstore.TokenStoreUtils;
import org.apache.cxf.ws.security.wss4j.KerberosTokenInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JStaxInInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JStaxOutInterceptor;
import org.apache.cxf.ws.security.wss4j.StaxSecurityContextInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.policyvalidators.PolicyValidatorParameters;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SecurityPolicyValidator;
import org.apache.cxf.ws.security.wss4j.policyvalidators.ValidatorUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.stax.securityEvent.KerberosTokenSecurityEvent;
import org.apache.wss4j.stax.securityEvent.WSSecurityEventConstants;
import org.apache.wss4j.stax.securityToken.KerberosServiceSecurityToken;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.utils.XMLUtils;

/**
 *
 */
public class KerberosTokenInterceptorProvider extends AbstractPolicyInterceptorProvider {

    private static final long serialVersionUID = 5922028830873137490L;

    public KerberosTokenInterceptorProvider() {
        super(Arrays.asList(SP11Constants.KERBEROS_TOKEN, SP12Constants.KERBEROS_TOKEN));

        KerberosTokenOutInterceptor outInterceptor = new KerberosTokenOutInterceptor();
        this.getOutInterceptors().add(outInterceptor);
        this.getOutFaultInterceptors().add(outInterceptor);

        KerberosTokenDOMInInterceptor domInInterceptor = new KerberosTokenDOMInInterceptor();
        this.getInInterceptors().add(domInInterceptor);
        this.getInFaultInterceptors().add(domInInterceptor);

        KerberosTokenStaxInInterceptor staxInInterceptor = new KerberosTokenStaxInInterceptor();
        this.getInInterceptors().add(staxInInterceptor);
        this.getInFaultInterceptors().add(staxInInterceptor);

        this.getOutInterceptors().add(new KerberosTokenInterceptor());
        this.getInInterceptors().add(new KerberosTokenInterceptor());

        PolicyBasedWSS4JStaxOutInterceptor so = new PolicyBasedWSS4JStaxOutInterceptor();
        PolicyBasedWSS4JStaxInInterceptor si = new PolicyBasedWSS4JStaxInInterceptor();
        this.getOutInterceptors().add(so);
        this.getOutFaultInterceptors().add(so);
        this.getInInterceptors().add(si);
        this.getInFaultInterceptors().add(si);
    }

    static class KerberosTokenOutInterceptor extends AbstractPhaseInterceptor<Message> {
        KerberosTokenOutInterceptor() {
            super(Phase.PREPARE_SEND);
        }
        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais =
                    PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.KERBEROS_TOKEN);
                if (ais.isEmpty()) {
                    return;
                }
                if (isRequestor(message)) {
                    final SecurityToken tok;
                    try {
                        KerberosClient client = KerberosUtils.getClient(message, "kerberos");
                        synchronized (client) {
                            tok = client.requestSecurityToken();
                        }
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new Fault(e);
                    }
                    if (tok != null) {
                        for (AssertionInfo ai : ais) {
                            ai.setAsserted(true);
                        }
                        message.getExchange().getEndpoint().put(SecurityConstants.TOKEN_ID,
                                                                      tok.getId());
                        message.getExchange().put(SecurityConstants.TOKEN_ID,
                                                  tok.getId());
                        try {
                            TokenStoreUtils.getTokenStore(message).add(tok);

                            // Create another cache entry with the SHA1 Identifier as the key for easy retrieval
                            if (tok.getSHA1() != null) {
                                TokenStoreUtils.getTokenStore(message).add(tok.getSHA1(), tok);
                            }
                        } catch (TokenStoreException ex) {
                            throw new Fault(ex);
                        }
                    }
                } else {
                    //server side should be checked on the way in
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }
                }

                PolicyUtils.assertPolicy(aim, "WssKerberosV5ApReqToken11");
                PolicyUtils.assertPolicy(aim, "WssGssKerberosV5ApReqToken11");
            }
        }

    }

    static class KerberosTokenDOMInInterceptor extends AbstractPhaseInterceptor<Message> {
        KerberosTokenDOMInInterceptor() {
            super(Phase.PRE_PROTOCOL);
            addAfter(WSS4JInInterceptor.class.getName());
            addAfter(PolicyBasedWSS4JInInterceptor.class.getName());
        }

        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information

            boolean enableStax =
                MessageUtils.getContextualBoolean(message, SecurityConstants.ENABLE_STREAMING_SECURITY);
            if (aim != null && !enableStax) {
                Collection<AssertionInfo> ais =
                    PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.KERBEROS_TOKEN);
                if (ais.isEmpty()) {
                    return;
                }
                if (!isRequestor(message)) {
                    List<WSHandlerResult> results =
                        CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));
                    if (results != null && !results.isEmpty()) {
                        parseHandlerResults(results.get(0), message, ais);
                    }
                } else {
                    //client side should be checked on the way out
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }
                }

                PolicyUtils.assertPolicy(aim, "WssKerberosV5ApReqToken11");
                PolicyUtils.assertPolicy(aim, "WssGssKerberosV5ApReqToken11");
            }
        }

        private void parseHandlerResults(
            WSHandlerResult rResult,
            Message message,
            Collection<AssertionInfo> ais
        ) {

            PolicyValidatorParameters parameters = new PolicyValidatorParameters();
            parameters.setAssertionInfoMap(message.get(AssertionInfoMap.class));
            parameters.setMessage(message);
            parameters.setResults(rResult);

            QName qName = ais.iterator().next().getAssertion().getName();
            Map<QName, SecurityPolicyValidator> validators =
                ValidatorUtils.getSecurityPolicyValidators(message);
            if (validators.containsKey(qName)) {
                validators.get(qName).validatePolicies(parameters, ais);
            }
        }

    }

    static class KerberosTokenStaxInInterceptor extends AbstractPhaseInterceptor<Message> {

        private static final Logger LOG =
            LogUtils.getL7dLogger(KerberosTokenStaxInInterceptor.class);

        KerberosTokenStaxInInterceptor() {
            super(Phase.PRE_PROTOCOL);
            getBefore().add(StaxSecurityContextInInterceptor.class.getName());
        }

        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information

            boolean enableStax =
                MessageUtils.getContextualBoolean(message, SecurityConstants.ENABLE_STREAMING_SECURITY);
            if (aim != null && enableStax) {
                Collection<AssertionInfo> ais =
                    PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.KERBEROS_TOKEN);
                if (ais.isEmpty()) {
                    return;
                }
                if (!isRequestor(message)) {
                    SecurityEvent event = findKerberosEvent(message);
                    if (event != null) {
                        for (AssertionInfo ai : ais) {
                            ai.setAsserted(true);
                        }
                        KerberosServiceSecurityToken kerberosToken =
                            ((KerberosTokenSecurityEvent)event).getSecurityToken();
                        if (kerberosToken != null) {
                            try {
                                storeKerberosToken(message, kerberosToken);
                            } catch (TokenStoreException ex) {
                                throw new Fault(ex);
                            }
                        }
                    }
                } else {
                    //client side should be checked on the way out
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }
                }

                PolicyUtils.assertPolicy(aim, "WssKerberosV5ApReqToken11");
                PolicyUtils.assertPolicy(aim, "WssGssKerberosV5ApReqToken11");
            }
        }

        private void storeKerberosToken(Message message, KerberosServiceSecurityToken kerberosToken)
                throws TokenStoreException {
            SecurityToken token = new SecurityToken(kerberosToken.getId());
            token.setTokenType(kerberosToken.getKerberosTokenValueType());

            SecretKey secretKey = getSecretKeyFromToken(kerberosToken);
            token.setKey(secretKey);
            if (secretKey != null) {
                token.setSecret(secretKey.getEncoded());
            }

            byte[] ticket = kerberosToken.getBinaryContent();
            try {
                token.setSHA1(XMLUtils.encodeToString(KeyUtils.generateDigest(ticket)));
            } catch (WSSecurityException e) {
                // Just consume this for now as it isn't critical...
            }

            TokenStoreUtils.getTokenStore(message).add(token);
            message.getExchange().put(SecurityConstants.TOKEN_ID, token.getId());
        }

        private SecurityEvent findKerberosEvent(Message message) {
            @SuppressWarnings("unchecked")
            final List<SecurityEvent> incomingEventList =
                (List<SecurityEvent>)message.get(SecurityEvent.class.getName() + ".in");
            if (incomingEventList != null) {
                for (SecurityEvent incomingEvent : incomingEventList) {
                    if (WSSecurityEventConstants.KERBEROS_TOKEN
                        == incomingEvent.getSecurityEventType()) {
                        return incomingEvent;
                    }
                }
            }
            return null;
        }

        private SecretKey getSecretKeyFromToken(KerberosServiceSecurityToken kerberosToken) {
            try {
                Map<String, Key> secretKeys = kerberosToken.getSecretKey();
                if (secretKeys != null) {
                    SecretKey foundKey = null;
                    for (Entry<String, Key> entry : kerberosToken.getSecretKey().entrySet()) {
                        if (entry.getValue() instanceof SecretKey) {
                            SecretKey secretKey = (SecretKey)entry.getValue();
                            if (foundKey == null
                                || secretKey.getEncoded().length > foundKey.getEncoded().length) {
                                foundKey = secretKey;
                            }
                        }
                    }
                    return foundKey;
                }
            } catch (XMLSecurityException e) {
                LOG.fine(e.getMessage());
            }
            return null;
        }
    }

}
