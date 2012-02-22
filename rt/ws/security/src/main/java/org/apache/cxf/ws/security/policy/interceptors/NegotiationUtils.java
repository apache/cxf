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

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.addressing.policy.MetadataConstants;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.EndpointPolicy;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;
import org.apache.cxf.ws.security.policy.model.Binding;
import org.apache.cxf.ws.security.policy.model.Trust10;
import org.apache.cxf.ws.security.policy.model.Trust13;
import org.apache.cxf.ws.security.tokenstore.MemoryTokenStore;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.conversation.ConversationConstants;
import org.apache.ws.security.conversation.ConversationException;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.message.token.SecurityContextToken;

/**
 * This is a collection of utility methods for use in negotiation exchanges such as WS-SecureConversation 
 * and WS-Trust for SPNEGO.
 */
final class NegotiationUtils {
    
    private NegotiationUtils() {
        // complete
    }

    static Trust10 getTrust10(AssertionInfoMap aim) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.TRUST_10);
        if (ais == null || ais.isEmpty()) {
            ais = aim.get(SP11Constants.TRUST_10);
        }
        if (ais == null || ais.isEmpty()) {
            return null;
        }
        return (Trust10)ais.iterator().next().getAssertion();
    }
    
    static Trust13 getTrust13(AssertionInfoMap aim) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.TRUST_13);
        if (ais == null || ais.isEmpty()) {
            return null;
        }
        return (Trust13)ais.iterator().next().getAssertion();
    }
    
    static TokenStore getTokenStore(Message message) {
        EndpointInfo info = message.getExchange().get(Endpoint.class).getEndpointInfo();
        synchronized (info) {
            TokenStore tokenStore = (TokenStore)message.getContextualProperty(TokenStore.class.getName());
            if (tokenStore == null) {
                tokenStore = (TokenStore)info.getProperty(TokenStore.class.getName());
            }
            if (tokenStore == null) {
                tokenStore = new MemoryTokenStore();
                info.setProperty(TokenStore.class.getName(), tokenStore);
            }
            return tokenStore;
        }
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
        Binding transport = null;
        Collection<AssertionInfo> ais = aim.get(SP12Constants.TRANSPORT_BINDING);
        if (ais != null) {
            for (AssertionInfo ai : ais) {
                transport = (Binding)ai.getAssertion();
            }                    
        } else {
            ais = aim.get(SP12Constants.ASYMMETRIC_BINDING);
            if (ais != null) {
                for (AssertionInfo ai : ais) {
                    transport = (Binding)ai.getAssertion();
                }                    
            } else {
                ais = aim.get(SP12Constants.SYMMETRIC_BINDING);
                if (ais != null) {
                    for (AssertionInfo ai : ais) {
                        transport = (Binding)ai.getAssertion();
                    }                    
                }
            }
        }
        if (transport != null) {
            return transport.getAlgorithmSuite();
        }
        return null;
    }
    
    static int getWSCVersion(String tokenTypeValue) throws ConversationException {
        if (tokenTypeValue == null) {
            return ConversationConstants.DEFAULT_VERSION;
        }

        if (tokenTypeValue.startsWith(ConversationConstants.WSC_NS_05_02)) {
            return ConversationConstants.getWSTVersion(ConversationConstants.WSC_NS_05_02);
        } else if (tokenTypeValue.startsWith(ConversationConstants.WSC_NS_05_12)) {
            return ConversationConstants.getWSTVersion(ConversationConstants.WSC_NS_05_12);
        } else {
            throw new ConversationException("unsupportedSecConvVersion");
        }
    }
    
    static void recalcEffectivePolicy(
        SoapMessage message, 
        String namespace, 
        Policy policy,
        Invoker invoker
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

            TokenStore store = (TokenStore)message.getContextualProperty(TokenStore.class.getName());
            if (store == null) {
                store = new MemoryTokenStore();
                endpoint.getEndpointInfo().setProperty(TokenStore.class.getName(), store);
            }
            endpoint = STSUtils.createSTSEndpoint(bus, 
                    namespace,
                    endpoint.getEndpointInfo().getTransportId(),
                    destination.getAddress().getAddress().getValue(),
                    message.getVersion().getBindingId(), 
                    policy,
                    null);
            endpoint.getEndpointInfo().setProperty(TokenStore.class.getName(), store);
            message.getExchange().put(TokenStore.class.getName(), store);

            EndpointPolicy ep = pe.getServerEndpointPolicy(endpoint.getEndpointInfo(), destination);
            List<Interceptor<? extends Message>> interceptors = ep.getInterceptors();
            for (Interceptor<? extends Message> i : interceptors) {
                message.getInterceptorChain().add(i);
            }

            Collection<Assertion> assertions = ep.getVocabulary();
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
    static boolean parseSCTResult(SoapMessage message) {
        List<WSHandlerResult> results = 
            CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));
        if (results == null) {
            return false;
        }
        
        for (WSHandlerResult rResult : results) {
            List<WSSecurityEngineResult> wsSecEngineResults = rResult.getResults();

            for (WSSecurityEngineResult wser : wsSecEngineResults) {
                Integer actInt = (Integer)wser.get(WSSecurityEngineResult.TAG_ACTION);
                if (actInt.intValue() == WSConstants.SCT) {
                    SecurityContextToken tok = 
                        (SecurityContextToken)wser.get(WSSecurityEngineResult.TAG_SECURITY_CONTEXT_TOKEN);
                    message.getExchange().put(SecurityConstants.TOKEN_ID, tok.getIdentifier());
                    
                    byte[] secret = (byte[])wser.get(WSSecurityEngineResult.TAG_SECRET);
                    if (secret != null) {
                        SecurityToken token = new SecurityToken(tok.getIdentifier());
                        token.setToken(tok.getElement());
                        token.setSecret(secret);
                        token.setTokenType(tok.getTokenType());
                        message.getExchange().put(SecurityConstants.TOKEN, token);
                    }
                    return true;
                }
            }
        }
        return false;
    }
    
    static CallbackHandler getCallbackHandler(Object o, Class<?> clazz) {
        CallbackHandler handler = null;
        if (o instanceof CallbackHandler) {
            handler = (CallbackHandler)o;
        } else if (o instanceof String) {
            try {
                handler = 
                    (CallbackHandler)ClassLoaderUtils.loadClass((String)o, clazz).newInstance();
            } catch (Exception e) {
                handler = null;
            }
        }
        return handler;
    }
    
}
