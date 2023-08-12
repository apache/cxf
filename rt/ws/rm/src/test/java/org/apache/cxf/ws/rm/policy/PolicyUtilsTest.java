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

package org.apache.cxf.ws.rm.policy;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertion;
import org.apache.cxf.ws.rm.RM10Constants;
import org.apache.cxf.ws.rm.RMConfiguration;
import org.apache.cxf.ws.rmp.v200502.RMAssertion;
import org.apache.cxf.ws.rmp.v200502.RMAssertion.AcknowledgementInterval;
import org.apache.cxf.ws.rmp.v200502.RMAssertion.BaseRetransmissionInterval;
import org.apache.cxf.ws.rmp.v200502.RMAssertion.ExponentialBackoff;
import org.apache.cxf.ws.rmp.v200502.RMAssertion.InactivityTimeout;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class PolicyUtilsTest {
    @Test
    public void testRMAssertionEquals() {
        RMAssertion a = new RMAssertion();
        assertTrue(RMPolicyUtilities.equals(a, a));

        RMAssertion b = new RMAssertion();
        assertTrue(RMPolicyUtilities.equals(a, b));

        InactivityTimeout iat = new RMAssertion.InactivityTimeout();
        iat.setMilliseconds(Long.valueOf(10));
        a.setInactivityTimeout(iat);
        assertFalse(RMPolicyUtilities.equals(a, b));
        b.setInactivityTimeout(iat);
        assertTrue(RMPolicyUtilities.equals(a, b));

        ExponentialBackoff eb = new RMAssertion.ExponentialBackoff();
        a.setExponentialBackoff(eb);
        assertFalse(RMPolicyUtilities.equals(a, b));
        b.setExponentialBackoff(eb);
        assertTrue(RMPolicyUtilities.equals(a, b));
    }

    @Test
    public void testIntersect() {
        RMAssertion rma = new RMAssertion();
        RMConfiguration cfg0 = new RMConfiguration();
        assertTrue(RMPolicyUtilities.equals(cfg0, RMPolicyUtilities.intersect(rma, cfg0)));

        InactivityTimeout aiat = new RMAssertion.InactivityTimeout();
        aiat.setMilliseconds(Long.valueOf(7200000));
        rma.setInactivityTimeout(aiat);
        cfg0.setInactivityTimeout(Long.valueOf(3600000));

        RMConfiguration cfg1 = RMPolicyUtilities.intersect(rma, cfg0);
        assertEquals(7200000L, cfg1.getInactivityTimeout().longValue());
        assertNull(cfg1.getBaseRetransmissionInterval());
        assertNull(cfg1.getAcknowledgementInterval());
        assertFalse(cfg1.isExponentialBackoff());

        BaseRetransmissionInterval abri = new RMAssertion.BaseRetransmissionInterval();
        abri.setMilliseconds(Long.valueOf(20000));
        rma.setBaseRetransmissionInterval(abri);
        cfg0.setBaseRetransmissionInterval(Long.valueOf(10000));

        cfg1 = RMPolicyUtilities.intersect(rma, cfg0);
        assertEquals(7200000L, cfg1.getInactivityTimeout().longValue());
        assertEquals(20000L, cfg1.getBaseRetransmissionInterval().longValue());
        assertNull(cfg1.getAcknowledgementInterval());
        assertFalse(cfg1.isExponentialBackoff());

        AcknowledgementInterval aai = new RMAssertion.AcknowledgementInterval();
        aai.setMilliseconds(Long.valueOf(2000));
        rma.setAcknowledgementInterval(aai);

        cfg1 = RMPolicyUtilities.intersect(rma, cfg0);
        assertEquals(7200000L, cfg1.getInactivityTimeout().longValue());
        assertEquals(20000L, cfg1.getBaseRetransmissionInterval().longValue());
        assertEquals(2000L, cfg1.getAcknowledgementInterval().longValue());
        assertFalse(cfg1.isExponentialBackoff());

        cfg0.setExponentialBackoff(true);
        cfg1 = RMPolicyUtilities.intersect(rma, cfg0);
        assertEquals(7200000L, cfg1.getInactivityTimeout().longValue());
        assertEquals(20000L, cfg1.getBaseRetransmissionInterval().longValue());
        assertEquals(2000L, cfg1.getAcknowledgementInterval().longValue());
        assertTrue(cfg1.isExponentialBackoff());
    }

    @Test
    public void testGetRMConfiguration() {
        RMConfiguration cfg = new RMConfiguration();
        cfg.setBaseRetransmissionInterval(Long.valueOf(3000));
        cfg.setExponentialBackoff(true);

        Message message = mock(Message.class);
        when(message.get(AssertionInfoMap.class)).thenReturn(null);
        assertSame(cfg, RMPolicyUtilities.getRMConfiguration(cfg, message));

        AssertionInfoMap aim = mock(AssertionInfoMap.class);
        when(message.get(AssertionInfoMap.class)).thenReturn(aim);
        Collection<AssertionInfo> ais = new ArrayList<>();
        when(aim.get(RM10Constants.RMASSERTION_QNAME)).thenReturn(ais);
        assertSame(cfg, RMPolicyUtilities.getRMConfiguration(cfg, message));

        RMAssertion b = new RMAssertion();
        BaseRetransmissionInterval bbri = new RMAssertion.BaseRetransmissionInterval();
        bbri.setMilliseconds(Long.valueOf(2000));
        b.setBaseRetransmissionInterval(bbri);
        JaxbAssertion<RMAssertion> assertion = new JaxbAssertion<>();
        assertion.setName(RM10Constants.RMASSERTION_QNAME);
        assertion.setData(b);
        AssertionInfo ai = new AssertionInfo(assertion);
        ais.add(ai);
        when(message.get(AssertionInfoMap.class)).thenReturn(aim);
        when(aim.get(RM10Constants.RMASSERTION_QNAME)).thenReturn(ais);

        RMConfiguration cfg1 = RMPolicyUtilities.getRMConfiguration(cfg, message);
        assertNull(cfg1.getAcknowledgementInterval());
        assertNull(cfg1.getInactivityTimeout());
        assertEquals(2000L, cfg1.getBaseRetransmissionInterval().longValue());
        assertTrue(cfg1.isExponentialBackoff());
    }

}