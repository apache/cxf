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
import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rm.RMConfiguration;
import org.apache.cxf.ws.rm.RMUtils;
import org.apache.cxf.ws.rm.manager.DeliveryAssuranceType;
import org.apache.cxf.ws.rm.policy.RM12Assertion.Order;
import org.apache.cxf.ws.rmp.v200502.RMAssertion;

/**
 * Utilities for working with policies and configurations.
 */
public final class RMPolicyUtilities {
    
    private RMPolicyUtilities() {
    }
    
    /**
     * Returns an RMAssertion that is compatible with the default value and all RMAssertions pertaining to the
     * message (can never be null).
     * 
     * @param rma the default value (non-<code>null</code>)
     * @param message the message
     * @return the compatible RMAssertion
     */
    public static RMConfiguration getRMConfiguration(RMConfiguration defaultValue, Message message) {
        RMConfiguration compatible = defaultValue;
        Collection<AssertionInfo> ais = collectRMAssertions(message.get(AssertionInfoMap.class));
        for (AssertionInfo ai : ais) {
            if (ai.getAssertion() instanceof JaxbAssertion<?>) {
                RMAssertion rma = (RMAssertion)((JaxbAssertion<?>)ai.getAssertion()).getData();
                compatible = intersect(rma, compatible);
            } else if (ai.getAssertion() instanceof RM12Assertion) {
                RM12Assertion rma = (RM12Assertion) ai.getAssertion();
                compatible = intersect(rma, compatible);
            }
        }
        return compatible;
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
            ais = aim.get(RM11Constants.WSRMP_RMASSERTION_QNAME);
            if (ais != null) {
                mergedAsserts.addAll(ais);
            }
        }
        return mergedAsserts;
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
        if (!RMUtils.equalLongs(aval, bval)) {
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
        if (!RMUtils.equalLongs(aval, bval)) {
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
        if (!RMUtils.equalLongs(aval, bval)) {
            return false;
        }
        
        return null == a.getExponentialBackoff()
            ? null == b.getExponentialBackoff() 
            : null != b.getExponentialBackoff();         
    }
    
    public static boolean equals(RMConfiguration a, RMConfiguration b) {
        if (a == b) {
            return true;
        }
        if (a == null) {
            return b == null;
        } else if (b == null) {
            return false;
        }
        if (a.getDeliveryAssurance() == null) {
            if (b.getDeliveryAssurance() != null) {
                return false;
            }
        } else if (b.getDeliveryAssurance() == null) {
            return false;
        } else if (a.getDeliveryAssurance().isSetAtLeastOnce() != b.getDeliveryAssurance().isSetAtLeastOnce()
                || a.getDeliveryAssurance().isSetAtMostOnce() != b.getDeliveryAssurance().isSetAtMostOnce()
                || a.getDeliveryAssurance().isSetInOrder() != b.getDeliveryAssurance().isSetInOrder()) {
            return false;
        }
        if (a.getRM10AddressingNamespace() == null) {
            if (b.getRM10AddressingNamespace() != null) {
                return false;
            }
        } else if (b.getRM10AddressingNamespace() == null) {
            return false;
        } else if (!RMUtils.equalStrings(a.getRM10AddressingNamespace().getUri(),
                                         b.getRM10AddressingNamespace().getUri())) {
            return false;
        }
        if (!RMUtils.equalStrings(a.getRMNamespace(), b.getRMNamespace())) {
            return false;
        }
        return a.isExactlyOnce() == b.isExactlyOnce()
               && a.isExponentialBackoff() == b.isExponentialBackoff()
               && a.isSequenceSTRRequired() == b.isSequenceSTRRequired()
               && a.isSequenceTransportSecurityRequired() == b.isSequenceTransportSecurityRequired()
               && RMUtils.equalLongs(a.getAcknowledgementInterval(), b.getAcknowledgementInterval())
               && RMUtils.equalLongs(a.getBaseRetransmissionInterval(), b.getBaseRetransmissionInterval())
               && RMUtils.equalLongs(a.getInactivityTimeout(), b.getInactivityTimeout());
    }
    
    /**
     * Intersect a policy with a supplied configuration.
     * 
     * @param rma
     * @param cfg
     * @return result configuration
     */
    public static RMConfiguration intersect(RMAssertion rma, RMConfiguration cfg) {
        if (isCompatible(rma, cfg)) {
            return cfg;
        }
        
        RMConfiguration compatible = new RMConfiguration();
        
        // if supplied, policy value overrides default inactivity timeout
        Long aval = cfg.getInactivityTimeout();
        Long bval = null;
        if (null != rma.getInactivityTimeout()) {
            bval = rma.getInactivityTimeout().getMilliseconds();            
        }
        if (null != aval || null != bval) {
            Long use;
            if (bval != null) {
                use = bval;
            } else {
                use = aval;
            }
            compatible.setInactivityTimeout(use);
        }
        
        // if supplied, policy value overrides base retransmission interval
        aval = cfg.getBaseRetransmissionInterval();
        bval = null;
        if (null != rma.getBaseRetransmissionInterval()) {
            bval = rma.getBaseRetransmissionInterval().getMilliseconds();            
        }
        if (null != aval || null != bval) {
            Long use;
            if (bval != null) {
                use = bval;
            } else {
                use = aval;
            }
            compatible.setBaseRetransmissionInterval(use);
        }
        
        // if supplied, policy value overrides acknowledgement interval
        aval = cfg.getAcknowledgementInterval();
        bval = null;
        if (null != rma.getAcknowledgementInterval()) {
            bval = rma.getAcknowledgementInterval().getMilliseconds(); 
        }
        if (null != aval || null != bval) {
            Long use;
            if (bval != null) {
                use = bval;
            } else {
                use = aval;
            }
            compatible.setAcknowledgementInterval(use);
        }
    
        // backoff parameter
        if (cfg.isExponentialBackoff() || null != rma.getExponentialBackoff()) {
            compatible.setExponentialBackoff(true);
        }
        return compatible;
    }
    
    /**
     * Check if a policy is compatible with a supplied configuration.
     * 
     * @param asser
     * @param cfg
     * @return <code>true</code> if compatible, <code>false</code> if not
     */
    public static boolean isCompatible(RMAssertion asser, RMConfiguration cfg) {
        Long aval = null;
        if (null != asser.getInactivityTimeout()) {
            aval = asser.getInactivityTimeout().getMilliseconds();            
        }
        if (!RMUtils.equalLongs(cfg.getInactivityTimeout(), aval)) {
            return false;
        }
            
        aval = null;
        if (null != asser.getBaseRetransmissionInterval()) {
            aval = asser.getBaseRetransmissionInterval().getMilliseconds();            
        }
        if (!RMUtils.equalLongs(cfg.getBaseRetransmissionInterval(), aval)) {
            return false;
        }
        
        aval = null;
        if (null != asser.getAcknowledgementInterval()) {
            aval = asser.getAcknowledgementInterval().getMilliseconds(); 
        }
        if (!RMUtils.equalLongs(cfg.getAcknowledgementInterval(), aval)) {
            return false;
        }
        
        return cfg.isExponentialBackoff()
            ? null == asser.getExponentialBackoff() 
            : null != asser.getExponentialBackoff();         
    }
    
    /**
     * Intersect a policy with a supplied configuration.
     * 
     * @param rma
     * @param cfg
     * @return result configuration
     */
    public static RMConfiguration intersect(RM12Assertion rma, RMConfiguration cfg) {
        if (isCompatible(rma, cfg)) {
            return cfg;
        }
        RMConfiguration compatible = new RMConfiguration(cfg);
        
        // policy values override supplied settings
        if (rma.isSequenceSTR()) {
            compatible.setSequenceSTRRequired(true);
        }
        if (rma.isSequenceTransportSecurity()) {
            compatible.setSequenceTransportSecurityRequired(true);
        }
        if (rma.isAssuranceSet()) {
            DeliveryAssuranceType assurance = compatible.getDeliveryAssurance();
            if (assurance == null) {
                assurance = new DeliveryAssuranceType();
            }
            if (rma.isInOrder()) {
                assurance.setInOrder(new DeliveryAssuranceType.InOrder());
            }
            Order order = rma.getOrder();
            if (order != null) {
                switch (order) {
                case AtLeastOnce:
                    assurance.setAtLeastOnce(new DeliveryAssuranceType.AtLeastOnce());
                    break;
                case AtMostOnce:
                    assurance.setAtMostOnce(new DeliveryAssuranceType.AtMostOnce());
                    break;
                case ExactlyOnce:
                    assurance.setExactlyOnce(new DeliveryAssuranceType.ExactlyOnce());
                    break;
                default:
                    // unreachable code, required by checkstyle
                    break;
                }
            }
        }
        return compatible;
    }
    
    /**
     * Check if a policy is compatible with a supplied configuration.
     * 
     * @param rma
     * @param cfg
     * @return <code>true</code> if compatible, <code>false</code> if not
     */
    public static boolean isCompatible(RM12Assertion rma, RMConfiguration cfg) {
        if ((rma.isSequenceSTR() && !cfg.isSequenceSTRRequired())
            || (rma.isSequenceTransportSecurity() && !cfg.isSequenceTransportSecurityRequired())) {
            return false;
        }
        DeliveryAssuranceType assurance = cfg.getDeliveryAssurance();
        if (rma.isInOrder() && (assurance == null || assurance.getInOrder() == null)) {
            return false;
        }
        Order order = rma.getOrder();
        if (order != null) {
            switch (order) {
            case AtLeastOnce:
                return assurance.isSetAtLeastOnce();
            case AtMostOnce:
                return assurance.isSetAtMostOnce();
            case ExactlyOnce:
                return assurance.isSetExactlyOnce();
            default:
                // unreachable code, required by checkstyle
                break;
            }
        }
        return true;
    }
}