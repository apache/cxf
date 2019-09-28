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

import java.lang.reflect.Method;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.neethi.Policy;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class PolicyVerificationInFaultInterceptorTest {

    private IMocksControl control;
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
        control = EasyMock.createNiceControl();
        bus = control.createMock(Bus.class);
    }

    @Test
    public void testHandleMessage() throws NoSuchMethodException {
        Method m = AbstractPolicyInterceptor.class.getDeclaredMethod("getTransportAssertions",
            new Class[] {Message.class});

        PolicyVerificationInFaultInterceptor interceptor =
            EasyMock.createMockBuilder(PolicyVerificationInFaultInterceptor.class)
                .addMockedMethod(m).createMock(control);

        setupMessage(false, false, false, false, false, false);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();

        control.reset();
        setupMessage(true, false, false, false, false, false);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();

        control.reset();
        setupMessage(true, true, false, false, false, false);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();

        control.reset();
        setupMessage(true, true, true, false, false, false);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();

        control.reset();
        setupMessage(true, true, true, true, false, false);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();

        control.reset();
        setupMessage(true, true, true, true, true, false);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();

        control.reset();
        setupMessage(true, true, true, true, true, true);
        interceptor.getTransportAssertions(message);
        EasyMock.expectLastCall();
        EffectivePolicyImpl effectivePolicy = control.createMock(EffectivePolicyImpl.class);
        EasyMock.expect(engine.getEffectiveClientFaultPolicy(ei, boi, bfi, message)).andReturn(effectivePolicy);
        Policy policy = control.createMock(Policy.class);
        EasyMock.expect(effectivePolicy.getPolicy()).andReturn(policy);
        aim.checkEffectivePolicy(policy);
        EasyMock.expectLastCall().andReturn(null);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();
    }

    void setupMessage(boolean requestor,
                      boolean setupOperationInfo,
                      boolean setupEndpoint,
                      boolean setupPolicyEngine,
                      boolean setupAssertionInfoMap,
                      boolean setupBindingFaultInfo) {

        if (null == message) {
            message = control.createMock(Message.class);
        }
        if (setupAssertionInfoMap && null == aim) {
            aim = control.createMock(AssertionInfoMap.class);
        }
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(aim);
        if (!setupAssertionInfoMap) {
            return;
        }

        if (null == exchange) {
            exchange = control.createMock(Exchange.class);
        }

        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(
            requestor ? Boolean.TRUE : Boolean.FALSE);
        if (!requestor) {
            return;
        }

        EasyMock.expect(message.getExchange()).andReturn(exchange);
        if (setupOperationInfo && null == boi) {
            boi = control.createMock(BindingOperationInfo.class);
        }
        EasyMock.expect(exchange.getBindingOperationInfo()).andReturn(boi);
        if (!setupOperationInfo) {
            return;
        }
        if (setupEndpoint && null == endpoint) {
            endpoint = control.createMock(Endpoint.class);
        }
        EasyMock.expect(exchange.getEndpoint()).andReturn(endpoint);
        if (!setupEndpoint) {
            return;
        }
        if (null == ei) {
            ei = control.createMock(EndpointInfo.class);
        }
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(ei);

        EasyMock.expect(exchange.getBus()).andReturn(bus);
        if (setupPolicyEngine && null == engine) {
            engine = control.createMock(PolicyEngine.class);
        }
        EasyMock.expect(bus.getExtension(PolicyEngine.class)).andReturn(engine);
        if (!setupPolicyEngine) {
            return;
        }

        if (null == ex) {
            ex = control.createMock(Exception.class);
        }
        EasyMock.expect(message.getContent(Exception.class)).andReturn(null);
        EasyMock.expect(exchange.get(Exception.class)).andReturn(ex);

        if (setupBindingFaultInfo && null == bfi) {
            bfi = control.createMock(BindingFaultInfo.class);
        }
        EasyMock.expect(message.get(BindingFaultInfo.class)).andReturn(bfi);
    }

}
