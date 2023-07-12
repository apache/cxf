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

import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.Destination;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class PolicyVerificationOutInterceptorTest {
    @Test
    public void testHandleMessage() throws NoSuchMethodException {
        PolicyVerificationOutInterceptor interceptor = new PolicyVerificationOutInterceptor();

        Destination destination = mock(Destination.class);
        Exchange exchange = mock(Exchange.class);
        when(exchange.getDestination()).thenReturn(destination);
        Message message = mock(Message.class);
        when(message.get(Message.PARTIAL_RESPONSE_MESSAGE)).thenReturn(Boolean.TRUE);
        when(message.getExchange()).thenReturn(exchange);
        interceptor.handleMessage(message);

        when(message.get(Message.PARTIAL_RESPONSE_MESSAGE)).thenReturn(null);
        when(message.get(AssertionInfoMap.class)).thenReturn(null);
        interceptor.handleMessage(message);

        when(message.get(Message.PARTIAL_RESPONSE_MESSAGE)).thenReturn(null);
        AssertionInfoMap aim = mock(AssertionInfoMap.class);
        when(message.get(AssertionInfoMap.class)).thenReturn(aim);
        interceptor.getTransportAssertions(message);

        EffectivePolicy ep = mock(EffectivePolicy.class);
        when(message.get(EffectivePolicy.class)).thenReturn(ep);
        when(ep.getPolicy()).thenReturn(null);

        when(aim.checkEffectivePolicy(null)).thenReturn(null);

        interceptor.handleMessage(message);
    }
}
