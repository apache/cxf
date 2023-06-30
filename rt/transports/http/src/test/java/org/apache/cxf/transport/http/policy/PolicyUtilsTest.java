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

package org.apache.cxf.transport.http.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.policy.PolicyDataEngine;
import org.apache.cxf.transport.http.policy.impl.ClientPolicyCalculator;
import org.apache.cxf.transport.http.policy.impl.ServerPolicyCalculator;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.HTTPServerPolicy;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyDataEngineImpl;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertion;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class PolicyUtilsTest {
    @Test
    public void testAssertClientPolicyNoop() {
        testAssertPolicyNoop(true);
    }

    @Test
    public void testAssertServerPolicyNoop() {
        testAssertPolicyNoop(false);
    }

    void testAssertPolicyNoop(boolean isRequestor) {
        PolicyDataEngine pde = new PolicyDataEngineImpl(null);
        Message message = mock(Message.class);
        when(message.get(AssertionInfoMap.class)).thenReturn(null);

        pde.assertMessage(message, null, new ClientPolicyCalculator());

        Collection<PolicyAssertion> as = new ArrayList<>();
        AssertionInfoMap aim = new AssertionInfoMap(as);
        when(message.get(AssertionInfoMap.class)).thenReturn(aim);

        if (isRequestor) {
            pde.assertMessage(message, null, new ClientPolicyCalculator());
        } else {
            pde.assertMessage(message, null, new ServerPolicyCalculator());
        }
    }


    @Test
    public void testAssertClientPolicyOutbound() {
        testAssertClientPolicy(true);
    }

    @Test
    public void testAssertClientPolicyInbound() {
        testAssertClientPolicy(false);
    }

    public AssertionInfo getClientPolicyAssertionInfo(HTTPClientPolicy policy) {
        JaxbAssertion<HTTPClientPolicy> assertion =
            new JaxbAssertion<>(new ClientPolicyCalculator().getDataClassName(), false);
        assertion.setData(policy);
        return new AssertionInfo(assertion);
    }

    void testAssertClientPolicy(boolean outbound) {
        Message message = mock(Message.class);
        HTTPClientPolicy ep = new HTTPClientPolicy();
        HTTPClientPolicy cmp = new HTTPClientPolicy();

        cmp.setConnectionTimeout(60000L);
        HTTPClientPolicy icmp = new HTTPClientPolicy();
        icmp.setAllowChunking(false);

        AssertionInfo eai = getClientPolicyAssertionInfo(ep);
        AssertionInfo cmai = getClientPolicyAssertionInfo(cmp);
        AssertionInfo icmai = getClientPolicyAssertionInfo(icmp);

        AssertionInfoMap aim = new AssertionInfoMap(CastUtils.cast(Collections.EMPTY_LIST,
                                                                   PolicyAssertion.class));
        Collection<AssertionInfo> ais = new ArrayList<>();
        ais.add(eai);
        ais.add(cmai);
        ais.add(icmai);
        aim.put(new ClientPolicyCalculator().getDataClassName(), ais);
        when(message.get(AssertionInfoMap.class)).thenReturn(aim);
        Exchange ex = mock(Exchange.class);
        when(message.getExchange()).thenReturn(ex);
        when(ex.getOutMessage()).thenReturn(outbound ? message : null);
        if (!outbound) {
            when(ex.getOutFaultMessage()).thenReturn(null);
        }

        PolicyDataEngine pde = new PolicyDataEngineImpl(null);
        pde.assertMessage(message, ep, new ClientPolicyCalculator());
        assertTrue(eai.isAsserted());
        assertTrue(cmai.isAsserted());
        assertTrue(icmai.isAsserted());
    }

    @Test
    public void testAssertServerPolicyOutbound() {
        testAssertServerPolicy(true);
    }

    @Test
    public void testAssertServerPolicyInbound() {
        testAssertServerPolicy(false);
    }

    public AssertionInfo getServerPolicyAssertionInfo(HTTPServerPolicy policy) {
        JaxbAssertion<HTTPServerPolicy> assertion =
            new JaxbAssertion<>(new ServerPolicyCalculator().getDataClassName(), false);
        assertion.setData(policy);
        return new AssertionInfo(assertion);
    }

    void testAssertServerPolicy(boolean outbound) {
        Message message = mock(Message.class);
        HTTPServerPolicy ep = new HTTPServerPolicy();
        HTTPServerPolicy mp = new HTTPServerPolicy();
        HTTPServerPolicy cmp = new HTTPServerPolicy();
        cmp.setReceiveTimeout(60000L);
        HTTPServerPolicy icmp = new HTTPServerPolicy();
        icmp.setSuppressClientSendErrors(true);

        AssertionInfo eai = getServerPolicyAssertionInfo(ep);
        AssertionInfo mai = getServerPolicyAssertionInfo(mp);
        AssertionInfo cmai = getServerPolicyAssertionInfo(cmp);
        AssertionInfo icmai = getServerPolicyAssertionInfo(icmp);

        Collection<AssertionInfo> ais = new ArrayList<>();
        ais.add(eai);
        ais.add(mai);
        ais.add(cmai);
        ais.add(icmai);
        AssertionInfoMap aim = new AssertionInfoMap(CastUtils.cast(Collections.EMPTY_LIST,
                                                                   PolicyAssertion.class));
        aim.put(new ServerPolicyCalculator().getDataClassName(), ais);
        when(message.get(AssertionInfoMap.class)).thenReturn(aim);
        Exchange ex = mock(Exchange.class);
        when(message.getExchange()).thenReturn(ex);
        when(ex.getOutMessage()).thenReturn(outbound ? message : null);
        if (!outbound) {
            when(ex.getOutFaultMessage()).thenReturn(null);
        }

        new PolicyDataEngineImpl(null).assertMessage(message, ep,
                                                     new ServerPolicyCalculator());
        assertTrue(eai.isAsserted());
        assertTrue(mai.isAsserted());
        assertTrue(outbound ? cmai.isAsserted() : !cmai.isAsserted());
        assertTrue(outbound ? icmai.isAsserted() : !icmai.isAsserted());
    }
}