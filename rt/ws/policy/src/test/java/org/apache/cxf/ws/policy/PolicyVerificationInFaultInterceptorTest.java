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
import org.apache.cxf.service.model.BindingFaultInfo;
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
public class PolicyVerificationInFaultInterceptorTest {

    private Bus bus;
    private Message message;
    private Exchange exchange;
    private BindingOperationInfo boi;
    private BindingFaultInfo bfi;
    private Endpoint endpoint;
    private EndpointInfo ei;
    private PolicyEngine engine;
    private AssertionInfoMap aim;
    private Exception ex;

    @Before
    public void setUp() {
        bus = mock(Bus.class);
    }

    @Test
    public void testHandleMessage() throws NoSuchMethodException {
        PolicyVerificationInFaultInterceptor interceptor = new PolicyVerificationInFaultInterceptor();

        setupMessage(false, false, false, false, false, false);
        interceptor.handleMessage(message);

        setupMessage(true, false, false, false, false, false);
        interceptor.handleMessage(message);

        setupMessage(true, true, false, false, false, false);
        interceptor.handleMessage(message);

        setupMessage(true, true, true, false, false, false);
        interceptor.handleMessage(message);

        setupMessage(true, true, true, true, false, false);
        interceptor.handleMessage(message);

        setupMessage(true, true, true, true, true, false);
        interceptor.handleMessage(message);

        setupMessage(true, true, true, true, true, true);
        interceptor.getTransportAssertions(message);

        EffectivePolicyImpl effectivePolicy = mock(EffectivePolicyImpl.class);
        when(engine.getEffectiveClientFaultPolicy(ei, boi, bfi, message)).thenReturn(effectivePolicy);
        Policy policy = mock(Policy.class);
        when(effectivePolicy.getPolicy()).thenReturn(policy);
        when(aim.checkEffectivePolicy(policy)).thenReturn(null);
        interceptor.handleMessage(message);
    }

    void setupMessage(boolean requestor,
                      boolean setupOperationInfo,
                      boolean setupEndpoint,
                      boolean setupPolicyEngine,
                      boolean setupAssertionInfoMap,
                      boolean setupBindingFaultInfo) {

        if (null == message) {
            message = mock(Message.class);
        }
        if (setupAssertionInfoMap && null == aim) {
            aim = mock(AssertionInfoMap.class);
        }
        when(message.get(AssertionInfoMap.class)).thenReturn(aim);
        if (!setupAssertionInfoMap) {
            return;
        }

        if (null == exchange) {
            exchange = mock(Exchange.class);
        }

        when(message.get(Message.REQUESTOR_ROLE)).thenReturn(
            requestor ? Boolean.TRUE : Boolean.FALSE);
        if (!requestor) {
            return;
        }

        when(message.getExchange()).thenReturn(exchange);
        if (setupOperationInfo && null == boi) {
            boi = mock(BindingOperationInfo.class);
        }
        when(exchange.getBindingOperationInfo()).thenReturn(boi);
        if (!setupOperationInfo) {
            return;
        }
        if (setupEndpoint && null == endpoint) {
            endpoint = mock(Endpoint.class);
        }
        when(exchange.getEndpoint()).thenReturn(endpoint);
        if (!setupEndpoint) {
            return;
        }
        if (null == ei) {
            ei = mock(EndpointInfo.class);
        }
        when(endpoint.getEndpointInfo()).thenReturn(ei);

        when(exchange.getBus()).thenReturn(bus);
        if (setupPolicyEngine && null == engine) {
            engine = mock(PolicyEngine.class);
        }
        when(bus.getExtension(PolicyEngine.class)).thenReturn(engine);
        if (!setupPolicyEngine) {
            return;
        }

        if (null == ex) {
            ex = mock(Exception.class);
        }
        when(message.getContent(Exception.class)).thenReturn(null);
        when(exchange.get(Exception.class)).thenReturn(ex);

        if (setupBindingFaultInfo && null == bfi) {
            bfi = mock(BindingFaultInfo.class);
        }
        when(message.get(BindingFaultInfo.class)).thenReturn(bfi);
    }

}
