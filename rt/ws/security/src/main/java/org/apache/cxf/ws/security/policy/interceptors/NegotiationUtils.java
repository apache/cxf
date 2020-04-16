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
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.addressing.policy.MetadataConstants;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.EndpointPolicy;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.tokenstore.TokenStoreUtils;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.wss4j.common.derivedKey.ConversationConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.token.SecurityContextToken;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractBinding;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.Trust10;
import org.apache.wss4j.policy.model.Trust13;
import org.apache.wss4j.stax.securityEvent.WSSecurityEventConstants;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;

/**
 * This is a collection of utility methods for use in negotiation exchanges such as WS-SecureConversation
 * and WS-Trust for SPNEGO.
 */
final class NegotiationUtils {

    private NegotiationUtils() {
        // complete
    }

    static Trust10 getTrust10(AssertionInfoMap aim) {
        AssertionInfo ai = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.TRUST_10);
        if (ai == null) {
            return null;
        }
        return (Trust10)ai.getAssertion();
    }

    static Trust13 getTrust13(AssertionInfoMap aim) {
        AssertionInfo ai = PolicyUtils.getFirstAssertionByLocalname(aim, SPConstants.TRUST_13);
        if (ai == null) {
            return null;
        }
        return (Trust13)ai.getAssertion();
    }

    static Assertion getAddressingPolicy(AssertionInfoMap aim, boolean optional) {
        Collection<AssertionInfo> lst = aim.get(MetadataConstants.USING_ADDRESSING_2004_QNAME);
        Assertion assertion = null;
        if (null != lst && !lst.isEmpty()) {
            assertion = lst.iterator().next().getAssertion();
        }
        if (assertion == null) {
            lst = aim.get(MetadataConstants.USING_ADDRESSING_2005_QNAME);
            if (null != lst && !lst.isEmpty()) {
                assertion = lst.iterator().next().getAssertion();
            }
        }
        if (assertion == null) {
            lst = aim.get(MetadataConstants.USING_ADDRESSING_2006_QNAME);
            if (null != lst && !lst.isEmpty()) {
                assertion = lst.iterator().next().getAssertion();
            }
        }
        if (assertion == null) {
            return new PrimitiveAssertion(MetadataConstants.USING_ADDRESSING_2006_QNAME,
                                          optional);
        } else if (optional) {
            return new PrimitiveAssertion(assertion.getName(),
                                          optional);
        }
        return assertion;
    }

    static AlgorithmSuite getAlgorithmSuite(AssertionInfoMap aim) {
        AbstractBinding binding = PolicyUtils.getSecurityBinding(aim);
        if (binding != null) {
            return binding.getAlgorithmSuite();
        }
        return null;
    }

    static int getWSCVersion(String tokenTypeValue) throws WSSecurityException {
        if (tokenTypeValue == null) {
            return ConversationConstants.DEFAULT_VERSION;
        }

        if (tokenTypeValue.startsWith(ConversationConstants.WSC_NS_05_02)) {
            return ConversationConstants.getWSTVersion(ConversationConstants.WSC_NS_05_02);
        } else if (tokenTypeValue.startsWith(ConversationConstants.WSC_NS_05_12)) {
            return ConversationConstants.getWSTVersion(ConversationConstants.WSC_NS_05_12);
        } else {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                          "unsupportedSecConvVersion");
        }
    }

    static void recalcEffectivePolicy(
        SoapMessage message,
        String namespace,
        Policy policy,
        Invoker invoker,
        boolean secConv
    ) {
        Exchange ex = message.getExchange();
        Bus bus = ex.getBus();
        PolicyEngine pe = bus.getExtension(PolicyEngine.class);
        if (null == pe) {
            return;
        }
        Destination destination = ex.getDestination();
        try {
            Endpoint endpoint = message.getExchange().getEndpoint();

            TokenStore store = TokenStoreUtils.getTokenStore(message);
            if (secConv) {
                endpoint = STSUtils.createSCEndpoint(bus,
                                                     namespace,
                                                     endpoint.getEndpointInfo().getTransportId(),
                                                     destination.getAddress().getAddress().getValue(),
                                                     message.getVersion().getBindingId(),
                                                     policy);
            } else {
                endpoint = STSUtils.createSTSEndpoint(bus,
                                                      namespace,
                                                      endpoint.getEndpointInfo().getTransportId(),
                                                      destination.getAddress().getAddress().getValue(),
                                                      message.getVersion().getBindingId(),
                                                      policy,
                                                      null);
            }
            endpoint.getEndpointInfo().setProperty(TokenStore.class.getName(), store);
            message.getExchange().put(TokenStore.class.getName(), store);

            EndpointPolicy ep = pe.getServerEndpointPolicy(endpoint.getEndpointInfo(), destination, message);
            List<Interceptor<? extends Message>> interceptors = ep.getInterceptors(message);
            message.getInterceptorChain().add(interceptors);

            Collection<Assertion> assertions = ep.getVocabulary(message);
            if (null != assertions) {
                message.put(AssertionInfoMap.class, new AssertionInfoMap(assertions));
            }
            endpoint.getService().setInvoker(invoker);
            ex.put(Endpoint.class, endpoint);
            ex.put(Service.class, endpoint.getService());
            ex.put(org.apache.cxf.binding.Binding.class, endpoint.getBinding());
            ex.remove(BindingOperationInfo.class);
            message.put(MAPAggregator.ACTION_VERIFIED, Boolean.TRUE);
        } catch (Exception exc) {
            throw new Fault(exc);
        }
    }

    /**
     * Return true on successfully parsing a SecurityContextToken result
     */
    static boolean parseSCTResult(SoapMessage message) throws TokenStoreException {
        List<WSHandlerResult> results =
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));
        if (results == null) {
            // Try Streaming results
            @SuppressWarnings("unchecked")
            final List<SecurityEvent> incomingEventList =
                (List<SecurityEvent>) message.getExchange().get(SecurityEvent.class.getName() + ".in");
            if (incomingEventList != null) {
                for (SecurityEvent incomingEvent : incomingEventList) {
                    if (WSSecurityEventConstants.SECURITY_CONTEXT_TOKEN
                        == incomingEvent.getSecurityEventType()) {
                        return true;
                    }
                }
            }
            return false;
        }

        for (WSHandlerResult rResult : results) {

            List<WSSecurityEngineResult> sctResults =
                rResult.getActionResults().get(WSConstants.SCT);
            if (sctResults != null) {
                for (WSSecurityEngineResult wser : sctResults) {
                    SecurityContextToken tok =
                        (SecurityContextToken)wser.get(WSSecurityEngineResult.TAG_SECURITY_CONTEXT_TOKEN);
                    message.getExchange().put(SecurityConstants.TOKEN_ID, tok.getIdentifier());

                    SecurityToken token = TokenStoreUtils.getTokenStore(message).getToken(tok.getIdentifier());
                    if (token == null || token.isExpired()) {
                        byte[] secret = (byte[])wser.get(WSSecurityEngineResult.TAG_SECRET);
                        if (secret != null) {
                            token = new SecurityToken(tok.getIdentifier());
                            token.setToken(tok.getElement());
                            token.setSecret(secret);
                            token.setTokenType(tok.getTokenType());
                            TokenStoreUtils.getTokenStore(message).add(token);
                        }
                    }
                    if (token != null) {
                        final SecurityContext sc = token.getSecurityContext();
                        if (sc != null) {
                            message.put(SecurityContext.class, sc);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
