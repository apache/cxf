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
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.neethi.Policy;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class PolicyVerificationInInterceptorTest extends Assert {
 
    private IMocksControl control;
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
        control = EasyMock.createNiceControl(); 
        bus = control.createMock(Bus.class);  
    }
    
    @Test
    public void testHandleMessageNoOp() throws NoSuchMethodException {
        
        PolicyVerificationInInterceptor interceptor = new PolicyVerificationInInterceptor();
        interceptor.setBus(bus);        
        
        setupMessage(false, false, false, false);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();
        
        control.reset();
        setupMessage(true, false, false, false);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();
        control.reset();
        setupMessage(true, true, false, false);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();
        
        control.reset();
        setupMessage(true, true, true, false);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();
        
        /*
        control.reset();
        setupMessage(true, true, true, true);
        EasyMock.expect(message.get(Message.PARTIAL_RESPONSE_MESSAGE)).andReturn(Boolean.TRUE);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();
        */
    }
    
    @Test
    public void testHandleMessage() throws NoSuchMethodException {
        
        control.reset();
        Method m = AbstractPolicyInterceptor.class.getDeclaredMethod("getTransportAssertions",
            new Class[] {Message.class});
        PolicyVerificationInInterceptor interceptor = 
            control.createMock(PolicyVerificationInInterceptor.class, new Method[] {m});
        interceptor.setBus(bus);
        setupMessage(true, true, true, true);
        EasyMock.expect(message.get(Message.PARTIAL_RESPONSE_MESSAGE)).andReturn(Boolean.FALSE);
        interceptor.getTransportAssertions(message);
        EasyMock.expectLastCall();
        EffectivePolicy effectivePolicy = control.createMock(EffectivePolicy.class); 
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.TRUE);
        EasyMock.expect(engine.getEffectiveClientResponsePolicy(ei, boi)).andReturn(effectivePolicy);
        Policy policy = control.createMock(Policy.class);
        EasyMock.expect(effectivePolicy.getPolicy()).andReturn(policy);
        aim.checkEffectivePolicy(policy);
        EasyMock.expectLastCall();
        control.replay();
        interceptor.handleMessage(message);
        control.verify();
        
        control.reset();
        setupMessage(true, true, true, true);
        EasyMock.expect(message.get(Message.PARTIAL_RESPONSE_MESSAGE)).andReturn(Boolean.FALSE);
        interceptor.getTransportAssertions(message);
        EasyMock.expectLastCall();
        effectivePolicy = control.createMock(EffectivePolicy.class); 
        EasyMock.expect(message.get(Message.REQUESTOR_ROLE)).andReturn(Boolean.FALSE);
        EasyMock.expect(engine.getEffectiveServerRequestPolicy(ei, boi)).andReturn(effectivePolicy);
        policy = control.createMock(Policy.class);
        EasyMock.expect(effectivePolicy.getPolicy()).andReturn(policy);
        aim.checkEffectivePolicy(policy);
        EasyMock.expectLastCall();
        control.replay();
        interceptor.handleMessage(message);
        control.verify();
    }
    
    void setupMessage(boolean setupBindingOperationInfo,
                      boolean setupEndpoint,
                      boolean setupPolicyEngine,
                      boolean setupAssertionInfoMap) {
        if (null == message) {
            message = control.createMock(Message.class); 
        }
        if (null == exchange) {
            exchange = control.createMock(Exchange.class);            
        }
        EasyMock.expect(message.getExchange()).andReturn(exchange);
        if (setupBindingOperationInfo && null == boi) {
            boi = control.createMock(BindingOperationInfo.class);
        }
        EasyMock.expect(exchange.get(BindingOperationInfo.class)).andReturn(boi);
        if (!setupBindingOperationInfo) {
            return;
        }
        if (setupEndpoint && null == endpoint) {
            endpoint = control.createMock(Endpoint.class);
        }
        EasyMock.expect(exchange.get(Endpoint.class)).andReturn(endpoint);
        if (!setupEndpoint) {
            return;
        }
        ei = control.createMock(EndpointInfo.class);
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(ei);
        
        if (setupPolicyEngine && null == engine) {
            engine = control.createMock(PolicyEngine.class);
        }
        EasyMock.expect(bus.getExtension(PolicyEngine.class)).andReturn(engine);
        if (!setupPolicyEngine) {
            return;           
        }
        if (setupAssertionInfoMap && null == aim) {
            aim = control.createMock(AssertionInfoMap.class);
        }
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(aim);
    }

}
