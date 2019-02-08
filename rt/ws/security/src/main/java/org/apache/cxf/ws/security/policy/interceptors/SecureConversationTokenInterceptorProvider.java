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

import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.neethi.All;
import org.apache.neethi.ExactlyOne;
import org.apache.neethi.Policy;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AlgorithmSuite;
import org.apache.wss4j.policy.model.SecureConversationToken;
import org.apache.wss4j.policy.model.SupportingTokens;

/**
 *
 */
public class SecureConversationTokenInterceptorProvider extends AbstractPolicyInterceptorProvider {

    private static final long serialVersionUID = 8739057200687855383L;
    private static final Logger LOG =
        LogUtils.getL7dLogger(SecureConversationTokenInterceptorProvider.class);


    public SecureConversationTokenInterceptorProvider() {
        super(Arrays.asList(SP11Constants.SECURE_CONVERSATION_TOKEN,
                            SP12Constants.SECURE_CONVERSATION_TOKEN,
                            SP12Constants.BOOTSTRAP_POLICY,
                            SP11Constants.BOOTSTRAP_POLICY));
        SecureConversationOutInterceptor outInterceptor = new SecureConversationOutInterceptor();
        this.getOutInterceptors().add(outInterceptor);
        this.getOutFaultInterceptors().add(outInterceptor);

        SecureConversationInInterceptor inInterceptor = new SecureConversationInInterceptor();
        this.getInInterceptors().add(inInterceptor);
        this.getInFaultInterceptors().add(inInterceptor);
    }

    static String setupClient(STSClient client,
                            SoapMessage message,
                            AssertionInfoMap aim,
                            SecureConversationToken itok,
                            boolean endorse) {
        if (itok.getBootstrapPolicy() == null || itok.getBootstrapPolicy().getPolicy() == null) {
            throw new Fault("The SecureConversationToken does not define a BootstrapPolicy", LOG);
        }

        client.setTrust(NegotiationUtils.getTrust10(aim));
        client.setTrust(NegotiationUtils.getTrust13(aim));
        Policy pol = itok.getBootstrapPolicy().getPolicy();
        Policy p = new Policy();
        ExactlyOne ea = new ExactlyOne();
        p.addPolicyComponent(ea);
        All all = new All();
        all.addPolicyComponent(NegotiationUtils.getAddressingPolicy(aim, false));
        ea.addPolicyComponent(all);

        if (endorse) {
            SupportingTokens st =
                new SupportingTokens(SPConstants.SPVersion.SP12,
                                     SP12Constants.ENDORSING_SUPPORTING_TOKENS,
                                     new Policy());
            st.addToken(itok);
            all.addPolicyComponent(st);
        }
        pol = p.merge(pol);

        client.setPolicy(pol);
        client.setSoap11(message.getVersion() == Soap11.getInstance());
        client.setSecureConv(true);
        String s = message
            .getContextualProperty(Message.ENDPOINT_ADDRESS).toString();
        client.setLocation(s);
        AlgorithmSuite suite = NegotiationUtils.getAlgorithmSuite(aim);
        if (suite != null) {
            client.setAlgorithmSuite(suite);
            int x = suite.getAlgorithmSuiteType().getMaximumSymmetricKeyLength();
            if (x < 256) {
                client.setKeySize(x);
            }
        }
        Map<String, Object> ctx = client.getRequestContext();
        mapSecurityProps(message, ctx);
        return s;
    }

    private static void mapSecurityProps(Message message, Map<String, Object> ctx) {
        for (String s : SecurityConstants.ALL_PROPERTIES) {
            Object v = message.getContextualProperty(s + ".sct");
            if (v == null) {
                v = message.getContextualProperty(s);
            }
            if (v != null && !s.equals(SecurityConstants.TOKEN) && !s.equals(SecurityConstants.TOKEN_ID)) {
                ctx.put(s, v);
            }
        }
    }

}
