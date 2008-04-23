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

import org.apache.cxf.message.Message;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class PolicyVerificationOutInterceptorTest extends Assert {
 
    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();   
    } 
    
    @Test
    public void testHandleMessage() throws NoSuchMethodException {
        Method m = AbstractPolicyInterceptor.class.getDeclaredMethod("getTransportAssertions",
            new Class[] {Message.class});
        PolicyVerificationOutInterceptor interceptor = 
            control.createMock(PolicyVerificationOutInterceptor.class, new Method[] {m});
        
        Message message = control.createMock(Message.class);
        EasyMock.expect(message.get(Message.PARTIAL_RESPONSE_MESSAGE)).andReturn(Boolean.TRUE);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();
        
        control.reset();
        EasyMock.expect(message.get(Message.PARTIAL_RESPONSE_MESSAGE)).andReturn(null);
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(null);
        control.replay();
        interceptor.handleMessage(message);
        control.verify();
        
        control.reset();   
        EasyMock.expect(message.get(Message.PARTIAL_RESPONSE_MESSAGE)).andReturn(null);
        AssertionInfoMap aim = control.createMock(AssertionInfoMap.class);
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(aim);
        interceptor.getTransportAssertions(message);
        EasyMock.expectLastCall();
        EffectivePolicy ep = control.createMock(EffectivePolicy.class);
        EasyMock.expect(message.get(EffectivePolicy.class)).andReturn(ep);
        EasyMock.expect(ep.getPolicy()).andReturn(null);

        aim.checkEffectivePolicy(null);
        
        EasyMock.expectLastCall();
        
        control.replay();        
        interceptor.handleMessage(message);       
        control.verify();
    }
}
