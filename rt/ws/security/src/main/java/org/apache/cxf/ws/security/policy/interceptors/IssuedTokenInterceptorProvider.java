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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.policy.AbstractPolicyInterceptorProvider;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JOutInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JStaxInInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JStaxOutInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.policyvalidators.PolicyValidatorParameters;
import org.apache.cxf.ws.security.wss4j.policyvalidators.SecurityPolicyValidator;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.IssuedToken;

/**
 * 
 */
public class IssuedTokenInterceptorProvider extends AbstractPolicyInterceptorProvider {
    
    private static final long serialVersionUID = -6936475570762840527L;

    public IssuedTokenInterceptorProvider() {
        super(Arrays.asList(SP11Constants.ISSUED_TOKEN, SP12Constants.ISSUED_TOKEN));
        
        //issued tokens can be attached as a supporting token without
        //any type of binding.  Make sure we can support that.
        this.getOutInterceptors().add(PolicyBasedWSS4JOutInterceptor.INSTANCE);
        this.getOutFaultInterceptors().add(PolicyBasedWSS4JOutInterceptor.INSTANCE);
        this.getInInterceptors().add(PolicyBasedWSS4JInInterceptor.INSTANCE);
        this.getInFaultInterceptors().add(PolicyBasedWSS4JInInterceptor.INSTANCE);
        
        this.getOutInterceptors().add(new IssuedTokenOutInterceptor());
        this.getOutFaultInterceptors().add(new IssuedTokenOutInterceptor());
        this.getInInterceptors().add(new IssuedTokenInInterceptor());
        this.getInFaultInterceptors().add(new IssuedTokenInInterceptor());
        
        this.getOutInterceptors().add(PolicyBasedWSS4JStaxOutInterceptor.INSTANCE);
        this.getOutFaultInterceptors().add(PolicyBasedWSS4JStaxOutInterceptor.INSTANCE);
        this.getInInterceptors().add(PolicyBasedWSS4JStaxInInterceptor.INSTANCE);
        this.getInFaultInterceptors().add(PolicyBasedWSS4JStaxInInterceptor.INSTANCE);
    }
    
    protected static void assertIssuedToken(IssuedToken issuedToken, AssertionInfoMap aim) {
        if (issuedToken == null) {
            return;
        }
        // Assert some policies
        if (issuedToken.isRequireExternalReference()) {
            PolicyUtils.assertPolicy(aim, new QName(issuedToken.getName().getNamespaceURI(), 
                                                    SPConstants.REQUIRE_EXTERNAL_REFERENCE));
        }
        if (issuedToken.isRequireInternalReference()) {
            PolicyUtils.assertPolicy(aim, new QName(issuedToken.getName().getNamespaceURI(), 
                                                    SPConstants.REQUIRE_INTERNAL_REFERENCE));
        }
    }
    
    static class IssuedTokenOutInterceptor extends AbstractPhaseInterceptor<Message> {
        public IssuedTokenOutInterceptor() {
            super(Phase.PREPARE_SEND);
        }    
        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            
            if (aim != null) {
                Collection<AssertionInfo> ais = 
                    PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.ISSUED_TOKEN);
                if (ais.isEmpty()) {
                    return;
                }
                if (isRequestor(message)) {
                    IssuedToken itok = (IssuedToken)ais.iterator().next().getAssertion();
                    
                    SecurityToken tok = STSTokenHelper.getTokenByWSPolicy(message, itok, aim);
                    
                    if (tok != null) {
                        assertIssuedToken(itok, aim);
                        for (AssertionInfo ai : ais) {
                            ai.setAsserted(true);
                        }
                    }
                } else {
                    //server side should be checked on the way in
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }
                    IssuedToken itok = (IssuedToken)ais.iterator().next().getAssertion();
                    assertIssuedToken(itok, aim);
                }
            }
        }        
    }
    
    static class IssuedTokenInInterceptor extends AbstractPhaseInterceptor<Message> {
        public IssuedTokenInInterceptor() {
            super(Phase.PRE_PROTOCOL);
            addAfter(WSS4JInInterceptor.class.getName());
            addAfter(PolicyBasedWSS4JInInterceptor.class.getName());
        }

        public void handleMessage(Message message) throws Fault {
            AssertionInfoMap aim = message.get(AssertionInfoMap.class);
            // extract Assertion information
            if (aim != null) {
                Collection<AssertionInfo> ais = 
                    PolicyUtils.getAllAssertionsByLocalname(aim, SPConstants.ISSUED_TOKEN);
                if (ais.isEmpty()) {
                    return;
                }
                
                IssuedToken itok = (IssuedToken)ais.iterator().next().getAssertion();
                assertIssuedToken(itok, aim);
                
                if (!isRequestor(message)) {
                    message.getExchange().remove(SecurityConstants.TOKEN);
                    List<WSHandlerResult> results = 
                        CastUtils.cast((List<?>)message.get(WSHandlerConstants.RECV_RESULTS));
                    if (results != null && results.size() > 0) {
                        parseHandlerResults(results.get(0), message, ais);
                    }
                } else {
                    for (AssertionInfo ai : ais) {
                        ai.setAsserted(true);
                    }
                }
            }
        }
        
        private void parseHandlerResults(
            WSHandlerResult rResult,
            Message message,
            Collection<AssertionInfo> issuedAis
        ) {
            PolicyValidatorParameters parameters = new PolicyValidatorParameters();
            parameters.setAssertionInfoMap(message.get(AssertionInfoMap.class));
            parameters.setMessage(message);
            parameters.setResults(rResult);
            
            parameters.setSignedResults(rResult.getActionResults().get(WSConstants.SIGN));
            
            List<WSSecurityEngineResult> samlResults = new ArrayList<>();
            if (rResult.getActionResults().containsKey(WSConstants.ST_SIGNED)) {
                samlResults.addAll(rResult.getActionResults().get(WSConstants.ST_SIGNED));
            }
            if (rResult.getActionResults().containsKey(WSConstants.ST_UNSIGNED)) {
                samlResults.addAll(rResult.getActionResults().get(WSConstants.ST_UNSIGNED));
            }
            parameters.setSamlResults(samlResults);
            
            QName qName = issuedAis.iterator().next().getAssertion().getName();
            Map<QName, SecurityPolicyValidator> validators = 
                PolicyUtils.getSecurityPolicyValidators(message);
            if (validators.containsKey(qName)) {
                validators.get(qName).validatePolicies(parameters, issuedAis);
            }
        }
        
    }
        
}
