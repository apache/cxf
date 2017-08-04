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

import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rm.RMConstants;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.neethi.builders.PolicyContainingPrimitiveAssertion;
import org.apache.neethi.builders.xml.XMLPrimitiveAssertionBuilder;

/**
 * Builds a WS-RMP 1.2 assertion and nested assertions from the raw XML.
 */
public class RM12AssertionBuilder implements AssertionBuilder<Element> {

    public static final String SEQUENCESTR_NAME = "SequenceSTR";
    public static final String SEQUENCETRANSEC_NAME = "SequenceTransportSecurity";
    public static final String DELIVERYASSURANCE_NAME = "DeliveryAssurance";
    public static final String EXACTLYONCE_NAME = "ExactlyOnce";
    public static final String ATLEASTONCE_NAME = "AtLeastOnce";
    public static final String ATMOSTONCE_NAME = "AtMostOnce";
    public static final String INORDER_NAME = "InOrder";

    public static final QName SEQSTR_QNAME = new QName(RM11Constants.WSRMP_NAMESPACE_URI, SEQUENCESTR_NAME);
    public static final QName SEQTRANSSEC_QNAME =
        new QName(RM11Constants.WSRMP_NAMESPACE_URI, SEQUENCETRANSEC_NAME);
    public static final QName DELIVERYASSURANCE_QNAME =
        new QName(RM11Constants.WSRMP_NAMESPACE_URI, DELIVERYASSURANCE_NAME);
    public static final QName EXACTLYONCE_QNAME = new QName(RM11Constants.WSRMP_NAMESPACE_URI, EXACTLYONCE_NAME);
    public static final QName ATLEASTONCE_QNAME = new QName(RM11Constants.WSRMP_NAMESPACE_URI, ATLEASTONCE_NAME);
    public static final QName ATMOSTONCE_QNAME = new QName(RM11Constants.WSRMP_NAMESPACE_URI, ATMOSTONCE_NAME);
    public static final QName INORDER_QNAME = new QName(RM11Constants.WSRMP_NAMESPACE_URI, INORDER_NAME);

    public static final QName[] KNOWN_ELEMENTS = {
        RM11Constants.WSRMP_RMASSERTION_QNAME,
        SEQSTR_QNAME,
        SEQTRANSSEC_QNAME,
        DELIVERYASSURANCE_QNAME,
        EXACTLYONCE_QNAME,
        ATLEASTONCE_QNAME,
        ATMOSTONCE_QNAME,
        INORDER_QNAME
    };

    /**
     * @see org.apache.neethi.builders.AssertionBuilder#getKnownElements()
     */
    public QName[] getKnownElements() {
        return KNOWN_ELEMENTS;
    }

    /**
     * @see org.apache.neethi.builders.AssertionBuilder#build(org.w3c.dom.Element,
     *  org.apache.neethi.AssertionBuilderFactory)
     */
    public Assertion build(Element elem, AssertionBuilderFactory factory) throws IllegalArgumentException {
        Assertion assertion = null;
        if (RM11Constants.WSRMP_NAMESPACE_URI.equals(elem.getNamespaceURI())) {
            boolean optional = XMLPrimitiveAssertionBuilder.isOptional(elem);
            String lname = elem.getLocalName();
            if (RMConstants.RMASSERTION_NAME.equals(lname)) {

                // top-level RMAssertion, with nested policy
                XMLPrimitiveAssertionBuilder nesting = new XMLPrimitiveAssertionBuilder() {
                    public Assertion newPrimitiveAssertion(Element element, Map<QName, String> mp) {
                        return new PrimitiveAssertion(RM11Constants.WSRMP_RMASSERTION_QNAME, isOptional(element),
                            isIgnorable(element), mp);
                    }
                    public Assertion newPolicyContainingAssertion(Element element, Map<QName, String> mp,
                        Policy policy) {
                        return new PolicyContainingPrimitiveAssertion(RM11Constants.WSRMP_RMASSERTION_QNAME,
                            isOptional(element), isIgnorable(element), mp, policy);
                    }
                };
                assertion = nesting.build(elem, factory);

            } else if (SEQUENCESTR_NAME.equals(lname)) {
                assertion = new PrimitiveAssertion(SEQSTR_QNAME,  optional);
            } else if (SEQUENCETRANSEC_NAME.equals(lname)) {
                assertion = new PrimitiveAssertion(SEQTRANSSEC_QNAME,  optional);
            } else if (DELIVERYASSURANCE_NAME.equals(lname)) {

                // DeliveryAssurance, with nested policy
                XMLPrimitiveAssertionBuilder nesting = new XMLPrimitiveAssertionBuilder() {
                    public Assertion newPrimitiveAssertion(Element element, Map<QName, String> mp) {
                        return new PrimitiveAssertion(DELIVERYASSURANCE_QNAME, isOptional(element),
                            isIgnorable(element), mp);
                    }
                    public Assertion newPolicyContainingAssertion(Element element, Map<QName, String> mp,
                        Policy policy) {
                        return new PolicyContainingPrimitiveAssertion(DELIVERYASSURANCE_QNAME,
                            isOptional(element), isIgnorable(element), mp, policy);
                    }
                };
                assertion = nesting.build(elem, factory);

            } else if (EXACTLYONCE_NAME.equals(lname)) {
                assertion = new PrimitiveAssertion(EXACTLYONCE_QNAME,  optional);
            } else if (ATLEASTONCE_NAME.equals(lname)) {
                assertion = new PrimitiveAssertion(ATLEASTONCE_QNAME,  optional);
            } else if (ATMOSTONCE_NAME.equals(lname)) {
                assertion = new PrimitiveAssertion(ATMOSTONCE_QNAME,  optional);
            } else if (INORDER_NAME.equals(lname)) {
                assertion = new PrimitiveAssertion(INORDER_QNAME,  optional);
            }
        }
        return assertion;
    }
}