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

package org.apache.cxf.ws.policy;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.neethi.Policy;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class PolicyVerificationInInterceptorTest {

    private Bus bus;
    private Message message;
    private Exchange exchange;
    private BindingOperationInfo boi;
    private Endpoint endpoint;
    private EndpointInfo ei;
    private PolicyEngine engine;
    private AssertionInfoMap aim;

    @Before
    public void setUp() {
        bus = mock(Bus.class);
    }

    @Test
    public void testHandleMessageNoOp() throws NoSuchMethodException {

        PolicyVerificationInInterceptor interceptor = new PolicyVerificationInInterceptor();

        setupMessage(false, false, false, false);
        interceptor.handleMessage(message);

        setupMessage(true, false, false, false);
        interceptor.handleMessage(message);

        setupMessage(true, true, false, false);
        interceptor.handleMessage(message);

        setupMessage(true, true, true, false);
        interceptor.handleMessage(message);
    }

    @Test
    public void testHandleMessage() throws NoSuchMethodException {
        PolicyVerificationInInterceptor interceptor = new PolicyVerificationInInterceptor();
        setupMessage(true, true, true, true);
        when(message.get(Message.PARTIAL_RESPONSE_MESSAGE)).thenReturn(Boolean.FALSE);
        interceptor.getTransportAssertions(message);

        EffectivePolicy effectivePolicy = mock(EffectivePolicy.class);
        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(Boolean.TRUE);
        when(engine.getEffectiveClientResponsePolicy(ei, boi, message)).thenReturn(effectivePolicy);
        Policy policy = mock(Policy.class);
        when(effectivePolicy.getPolicy()).thenReturn(policy);
        when(aim.checkEffectivePolicy(policy)).thenReturn(null);
        interceptor.handleMessage(message);

        setupMessage(true, true, true, true);
        when(message.get(Message.PARTIAL_RESPONSE_MESSAGE)).thenReturn(Boolean.FALSE);
        interceptor.getTransportAssertions(message);

        effectivePolicy = mock(EffectivePolicy.class);
        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(Boolean.FALSE);
        when(engine.getEffectiveServerRequestPolicy(ei, boi, message)).thenReturn(effectivePolicy);
        policy = mock(Policy.class);
        when(effectivePolicy.getPolicy()).thenReturn(policy);
        when(aim.checkEffectivePolicy(policy)).thenReturn(null);
        interceptor.handleMessage(message);
    }

    void setupMessage(boolean setupBindingOperationInfo,
                      boolean setupEndpoint,
                      boolean setupPolicyEngine,
                      boolean setupAssertionInfoMap) {
        if (null == message) {
            message = mock(Message.class);
        }
        if (null == exchange) {
            exchange = mock(Exchange.class);
        }
        if (setupAssertionInfoMap && null == aim) {
            aim = mock(AssertionInfoMap.class);
        }
        when(message.get(AssertionInfoMap.class)).thenReturn(aim);
        if (aim == null) {
            return;
        }

        when(exchange.getBus()).thenReturn(bus);
        when(message.getExchange()).thenReturn(exchange);
        if (setupBindingOperationInfo && null == boi) {
            boi = mock(BindingOperationInfo.class);
        }
        when(exchange.getBindingOperationInfo()).thenReturn(boi);
        if (!setupBindingOperationInfo) {
            return;
        }
        if (setupEndpoint && null == endpoint) {
            endpoint = mock(Endpoint.class);
        }
        when(exchange.getEndpoint()).thenReturn(endpoint);
        if (!setupEndpoint) {
            return;
        }
        ei = mock(EndpointInfo.class);
        when(endpoint.getEndpointInfo()).thenReturn(ei);

        if (setupPolicyEngine && null == engine) {
            engine = mock(PolicyEngine.class);
        }
        when(bus.getExtension(PolicyEngine.class)).thenReturn(engine);
        if (!setupPolicyEngine) {
            return;
        }
    }

}
