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
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class PolicyUtilsTest extends Assert {

    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }
    
    @Test
    public void testRMAssertionEquals() {
        RMAssertion a = new RMAssertion();
        assertTrue(RMPolicyUtilities.equals(a, a));
        
        RMAssertion b = new RMAssertion();
        assertTrue(RMPolicyUtilities.equals(a, b));
        
        InactivityTimeout iat = new RMAssertion.InactivityTimeout();
        iat.setMilliseconds(new Long(10));
        a.setInactivityTimeout(iat);
        assertTrue(!RMPolicyUtilities.equals(a, b));
        b.setInactivityTimeout(iat);
        assertTrue(RMPolicyUtilities.equals(a, b));
        
        ExponentialBackoff eb = new RMAssertion.ExponentialBackoff();
        a.setExponentialBackoff(eb);
        assertTrue(!RMPolicyUtilities.equals(a, b));
        b.setExponentialBackoff(eb);
        assertTrue(RMPolicyUtilities.equals(a, b));    
    }
    
    @Test
    public void testIntersect() {
        RMAssertion rma = new RMAssertion();
        RMConfiguration cfg0 = new RMConfiguration();
        assertTrue(RMPolicyUtilities.equals(cfg0, RMPolicyUtilities.intersect(rma, cfg0)));
        
        InactivityTimeout aiat = new RMAssertion.InactivityTimeout();
        aiat.setMilliseconds(new Long(7200000));
        rma.setInactivityTimeout(aiat);
        cfg0.setInactivityTimeout(new Long(3600000));
        
        RMConfiguration cfg1 = RMPolicyUtilities.intersect(rma, cfg0);
        assertEquals(7200000L, cfg1.getInactivityTimeout().longValue());
        assertNull(cfg1.getBaseRetransmissionInterval());
        assertNull(cfg1.getAcknowledgementInterval());
        assertFalse(cfg1.isExponentialBackoff());
        
        BaseRetransmissionInterval abri = new RMAssertion.BaseRetransmissionInterval();
        abri.setMilliseconds(new Long(20000));
        rma.setBaseRetransmissionInterval(abri);
        cfg0.setBaseRetransmissionInterval(new Long(10000));
        
        cfg1 = RMPolicyUtilities.intersect(rma, cfg0);
        assertEquals(7200000L, cfg1.getInactivityTimeout().longValue());
        assertEquals(20000L, cfg1.getBaseRetransmissionInterval().longValue());
        assertNull(cfg1.getAcknowledgementInterval());
        assertFalse(cfg1.isExponentialBackoff());
       
        AcknowledgementInterval aai = new RMAssertion.AcknowledgementInterval();
        aai.setMilliseconds(new Long(2000));
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
        cfg.setBaseRetransmissionInterval(new Long(3000));
        cfg.setExponentialBackoff(true);
        
        Message message = control.createMock(Message.class);
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(null);
        control.replay();
        assertSame(cfg, RMPolicyUtilities.getRMConfiguration(cfg, message));
        control.verify();
        
        control.reset();
        AssertionInfoMap aim = control.createMock(AssertionInfoMap.class);
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(aim);
        Collection<AssertionInfo> ais = new ArrayList<AssertionInfo>();
        EasyMock.expect(aim.get(RM10Constants.RMASSERTION_QNAME)).andReturn(ais);
        control.replay();
        assertSame(cfg, RMPolicyUtilities.getRMConfiguration(cfg, message));
        control.verify();
        
        control.reset();
        RMAssertion b = new RMAssertion();
        BaseRetransmissionInterval bbri = new RMAssertion.BaseRetransmissionInterval();
        bbri.setMilliseconds(new Long(2000));
        b.setBaseRetransmissionInterval(bbri);
        JaxbAssertion<RMAssertion> assertion = new JaxbAssertion<RMAssertion>();
        assertion.setName(RM10Constants.RMASSERTION_QNAME);
        assertion.setData(b);
        AssertionInfo ai = new AssertionInfo(assertion);
        ais.add(ai);
        EasyMock.expect(message.get(AssertionInfoMap.class)).andReturn(aim);
        EasyMock.expect(aim.get(RM10Constants.RMASSERTION_QNAME)).andReturn(ais);
        control.replay();
        RMConfiguration cfg1 = RMPolicyUtilities.getRMConfiguration(cfg, message);
        assertNull(cfg1.getAcknowledgementInterval());
        assertNull(cfg1.getInactivityTimeout());
        assertEquals(2000L, cfg1.getBaseRetransmissionInterval().longValue());
        assertTrue(cfg1.isExponentialBackoff());   
        control.verify();
    }
    
}
