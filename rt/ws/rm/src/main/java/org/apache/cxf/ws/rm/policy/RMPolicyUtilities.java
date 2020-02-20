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
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.policy.builder.jaxb.JaxbAssertion;
import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rm.RMConfiguration;
import org.apache.cxf.ws.rm.RMConfiguration.DeliveryAssurance;
import org.apache.cxf.ws.rm.RMConstants;
import org.apache.cxf.ws.rm.RMUtils;
import org.apache.cxf.ws.rmp.v200502.RMAssertion;
import org.apache.neethi.builders.PrimitiveAssertion;

/**
 * Utilities for working with policies and configurations.
 */
public final class RMPolicyUtilities {

    private static final List<QName> ASSERTION_NAMES;
    static {
        ASSERTION_NAMES = new ArrayList<>();
        ASSERTION_NAMES.addAll(RM10AssertionBuilder.KNOWN_ELEMENTS);
        for (QName qn : RM12AssertionBuilder.KNOWN_ELEMENTS) {
            ASSERTION_NAMES.add(qn);
        }
        ASSERTION_NAMES.add(RSPAssertionBuilder.CONFORMANT_QNAME);
    }

    private RMPolicyUtilities() {
    }

    /**
     * Returns an RMAssertion that is compatible with the default value and all RMAssertions pertaining to the
     * message (can never be null).
     *
     * @param defaultValue the default value (non-<code>null</code>)
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
            } else if (ai.getAssertion() instanceof PrimitiveAssertion) {
                PrimitiveAssertion assertion = (PrimitiveAssertion)ai.getAssertion();
                if (RM11Constants.WSRMP_NAMESPACE_URI.equals(assertion.getName().getNamespaceURI())) {
                    compatible = intersect(assertion, compatible);
                }
            }
        }
        return compatible;
    }

    /**
     * Collect RMAssertions from map. This checks both namespaces defined for WS-RM policy assertions, along with the
     * WS-I RSP namespace.
     *
     * @param aim map, may be <code>null</code>
     * @return merged collection, never <code>null</code>
     */
    public static Collection<AssertionInfo> collectRMAssertions(AssertionInfoMap aim) {
        Collection<AssertionInfo> mergedAsserts = new ArrayList<>();
        if (aim != null) {
            for (QName qn : ASSERTION_NAMES) {
                Collection<AssertionInfo> ais = aim.get(qn);
                if (ais != null) {
                    mergedAsserts.addAll(ais);
                }
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
        } else if (a.getDeliveryAssurance() != b.getDeliveryAssurance()) {
            return false;
        }
        if (a.getRM10AddressingNamespace() == null) {
            if (b.getRM10AddressingNamespace() != null) {
                return false;
            }
        } else if (b.getRM10AddressingNamespace() == null) {
            return false;
        } else if (!RMUtils.equalStrings(a.getRM10AddressingNamespace(), b.getRM10AddressingNamespace())) {
            return false;
        }
        if (!RMUtils.equalStrings(a.getRMNamespace(), b.getRMNamespace())) {
            return false;
        }
        return a.isInOrder() == b.isInOrder()
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

        RMConfiguration compatible = new RMConfiguration(cfg);

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
    public static RMConfiguration intersect(PrimitiveAssertion rma, RMConfiguration cfg) {
        if (isCompatible(rma, cfg)) {
            return cfg;
        }
        RMConfiguration compatible = new RMConfiguration(cfg);
        String lname = rma.getName().getLocalPart();
        if (RMConstants.RMASSERTION_NAME.equals(lname)) {
            compatible.setRMNamespace(RM11Constants.NAMESPACE_URI);
        } else if (RM12AssertionBuilder.SEQUENCESTR_NAME.equals(lname)) {
            compatible.setSequenceSTRRequired(true);
        } else if (RM12AssertionBuilder.SEQUENCETRANSEC_NAME.equals(lname)) {
            compatible.setSequenceTransportSecurityRequired(true);
        } else if (RM12AssertionBuilder.EXACTLYONCE_NAME.equals(lname)) {
            compatible.setDeliveryAssurance(DeliveryAssurance.EXACTLY_ONCE);
        } else if (RM12AssertionBuilder.ATLEASTONCE_NAME.equals(lname)) {
            compatible.setDeliveryAssurance(DeliveryAssurance.AT_LEAST_ONCE);
        } else if (RM12AssertionBuilder.ATMOSTONCE_NAME.equals(lname)) {
            compatible.setDeliveryAssurance(DeliveryAssurance.AT_MOST_ONCE);
        } else if (RM12AssertionBuilder.INORDER_NAME.equals(lname)) {
            compatible.setInOrder(true);
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
    public static boolean isCompatible(PrimitiveAssertion rma, RMConfiguration cfg) {
        String lname = rma.getName().getLocalPart();
        boolean compatible = true;
        if (RMConstants.RMASSERTION_NAME.equals(lname)) {
            compatible = RM11Constants.WSRMP_NAMESPACE_URI.equals(cfg.getRMNamespace());
        } else if (RM12AssertionBuilder.SEQUENCESTR_NAME.equals(lname)) {
            compatible = cfg.isSequenceSTRRequired();
        } else if (RM12AssertionBuilder.SEQUENCETRANSEC_NAME.equals(lname)) {
            compatible = cfg.isSequenceTransportSecurityRequired();
        } else if (RM12AssertionBuilder.EXACTLYONCE_NAME.equals(lname)) {
            compatible = cfg.getDeliveryAssurance() == DeliveryAssurance.EXACTLY_ONCE;
        } else if (RM12AssertionBuilder.ATLEASTONCE_NAME.equals(lname)) {
            compatible = cfg.getDeliveryAssurance() == DeliveryAssurance.AT_LEAST_ONCE;
        } else if (RM12AssertionBuilder.ATMOSTONCE_NAME.equals(lname)) {
            compatible = cfg.getDeliveryAssurance() == DeliveryAssurance.AT_MOST_ONCE;
        } else if (RM12AssertionBuilder.INORDER_NAME.equals(lname)) {
            compatible = cfg.isInOrder();
        }
        return compatible;
    }
}
