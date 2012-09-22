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
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertion;
import org.apache.cxf.ws.rm.RM10Constants;
import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rmp.v200502.RMAssertion;
import org.apache.cxf.ws.rmp.v200502.RMAssertion.AcknowledgementInterval;
import org.apache.cxf.ws.rmp.v200502.RMAssertion.BaseRetransmissionInterval;
import org.apache.cxf.ws.rmp.v200502.RMAssertion.InactivityTimeout;

/**
 * 
 */
public final class RM10PolicyUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(RM10PolicyUtils.class); 
    
    /**
     * Prevents instantiation.
     *
     */
    private RM10PolicyUtils() {        
    }

    /**
     * Collect RMAssertions from map. This checks both namespaces defined for WS-RM policy assertions.
     * 
     * @param aim map, may be <code>null</code>
     * @return merged collection, never <code>null</code>
     */
    public static Collection<AssertionInfo> collectRMAssertions(AssertionInfoMap aim) {
        Collection<AssertionInfo> mergedAsserts = new ArrayList<AssertionInfo>();
        if (aim != null) {
            Collection<AssertionInfo> ais = aim.get(RM10Constants.WSRMP_RMASSERTION_QNAME);
            if (ais != null) {
                mergedAsserts.addAll(ais);
            }

            //REVISIT this class shouldn't be picking RMP200702 assertions (see below)
            ais = aim.get(RM11Constants.WSRMP_RMASSERTION_QNAME);
            if (ais != null) {
                mergedAsserts.addAll(ais);
            }
        }
        return mergedAsserts;
    }

    /**
     * Returns an RMAssertion that is compatible with the default value
     * and all RMAssertions pertaining to the message (can never be null).
     * 
     * @param rma the default value
     * @param message the message
     * @return the compatible RMAssertion
     */
    public static RMAssertion getRMAssertion(RMAssertion defaultValue, Message message) {        
        RMAssertion compatible = defaultValue;
        Collection<AssertionInfo> ais = collectRMAssertions(message.get(AssertionInfoMap.class));
        for (AssertionInfo ai : ais) {
            //REVISIT this class can't handle RMP200702 assertions, so guard against CCE 
            if (ai.getAssertion() instanceof JaxbAssertion 
                && ((JaxbAssertion<?>)ai.getAssertion()).getData() instanceof RMAssertion) {
                JaxbAssertion<RMAssertion> ja = getAssertion(ai);
                RMAssertion rma = (RMAssertion)ja.getData(); 
                compatible = null == defaultValue ? rma : intersect(compatible, rma);
            } else {
                LOG.warning("Ignoring unexpected assertion class: " + ai.getAssertion());
            }
        }
        return compatible;
    }
    
    public static RMAssertion intersect(RMAssertion a, RMAssertion b) {
        if (equals(a, b)) {
            return a;
        }
        RMAssertion compatible = new RMAssertion();
        
        // use maximum of inactivity timeout
        
        Long aval = null;
        if (null != a.getInactivityTimeout()) {
            aval = a.getInactivityTimeout().getMilliseconds();
        }
        Long bval = null;
        if (null != b.getInactivityTimeout()) {
            bval = b.getInactivityTimeout().getMilliseconds();            
        }
        if (null != aval || null != bval) {
            InactivityTimeout ia = new RMAssertion.InactivityTimeout();
            if (null != aval && null != bval) {
                ia.setMilliseconds(bval);
            } else {
                ia.setMilliseconds(aval != null ? aval : bval);
            }
            compatible.setInactivityTimeout(ia);
        }
        
        // use minimum of base retransmission interval
        
        aval = null;
        if (null != a.getBaseRetransmissionInterval()) {
            aval = a.getBaseRetransmissionInterval().getMilliseconds();
        }
        bval = null;
        if (null != b.getBaseRetransmissionInterval()) {
            bval = b.getBaseRetransmissionInterval().getMilliseconds();            
        }
        if (null != aval || null != bval) {
            BaseRetransmissionInterval bri = new RMAssertion.BaseRetransmissionInterval();
            if (null != aval && null != bval) {
                bri.setMilliseconds(bval);
            } else {
                bri.setMilliseconds(aval != null ? aval : bval);
            }
            compatible.setBaseRetransmissionInterval(bri);
        }
        
        // use minimum of acknowledgement interval
        
        aval = null;
        if (null != a.getAcknowledgementInterval()) {
            aval = a.getAcknowledgementInterval().getMilliseconds();
        }
        bval = null;
        if (null != b.getAcknowledgementInterval()) {
            bval = b.getAcknowledgementInterval().getMilliseconds(); 
        }
        if (null != aval || null != bval) {
            AcknowledgementInterval ai = new RMAssertion.AcknowledgementInterval();
            if (null != aval && null != bval) {
                ai.setMilliseconds(bval);
            } else {
                ai.setMilliseconds(aval != null ? aval : bval);
            }
            compatible.setAcknowledgementInterval(ai);
        }
    
        // backoff parameter
        if (null != a.getExponentialBackoff() || null != b.getExponentialBackoff()) {
            compatible.setExponentialBackoff(new RMAssertion.ExponentialBackoff());
        }
        return compatible;
    }
    
    public static boolean equals(RMAssertion a, RMAssertion b) {
        if (a == b) {
            return true;
        }
        
        Long aval = null;
        if (null != a.getInactivityTimeout()) {
            aval = a.getInactivityTimeout().getMilliseconds();
        }
        Long bval = null;
        if (null != b.getInactivityTimeout()) {
            bval = b.getInactivityTimeout().getMilliseconds();            
        }
        if (!equals(aval, bval)) {
            return false;
        }
            
        aval = null;
        if (null != a.getBaseRetransmissionInterval()) {
            aval = a.getBaseRetransmissionInterval().getMilliseconds();
        }
        bval = null;
        if (null != b.getBaseRetransmissionInterval()) {
            bval = b.getBaseRetransmissionInterval().getMilliseconds();            
        }
        if (!equals(aval, bval)) {
            return false;
        }
        
        aval = null;
        if (null != a.getAcknowledgementInterval()) {
            aval = a.getAcknowledgementInterval().getMilliseconds();
        }
        bval = null;
        if (null != b.getAcknowledgementInterval()) {
            bval = b.getAcknowledgementInterval().getMilliseconds(); 
        }
        if (!equals(aval, bval)) {
            return false;
        }
        
        return null == a.getExponentialBackoff()
            ? null == b.getExponentialBackoff() 
            : null != b.getExponentialBackoff();         
    }
        
    private static boolean equals(Long aval, Long bval) {
        if (null != aval) {
            if (null != bval) {
                if (!aval.equals(bval)) {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            if (null != bval) {
                return false;
            }
            return true;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static JaxbAssertion<RMAssertion> getAssertion(AssertionInfo ai) {
        return (JaxbAssertion<RMAssertion>)ai.getAssertion();
    }
}
