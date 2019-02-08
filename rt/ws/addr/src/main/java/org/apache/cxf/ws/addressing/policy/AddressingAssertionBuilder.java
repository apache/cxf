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

package org.apache.cxf.ws.addressing.policy;

import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.neethi.builders.PolicyContainingPrimitiveAssertion;
import org.apache.neethi.builders.xml.XMLPrimitiveAssertionBuilder;

/**
 *
 */
public class AddressingAssertionBuilder implements AssertionBuilder<Element> {
    private static final Logger LOG =
        LogUtils.getL7dLogger(AddressingAssertionBuilder.class);

    private static final QName[] KNOWN_ELEMENTS = {
        MetadataConstants.ADDRESSING_ASSERTION_QNAME,
        MetadataConstants.ANON_RESPONSES_ASSERTION_QNAME,
        MetadataConstants.NON_ANON_RESPONSES_ASSERTION_QNAME,
        MetadataConstants.ADDRESSING_ASSERTION_QNAME_0705,
        MetadataConstants.ANON_RESPONSES_ASSERTION_QNAME_0705,
        MetadataConstants.NON_ANON_RESPONSES_ASSERTION_QNAME_0705
    };

    public AddressingAssertionBuilder() {
    }


    public Assertion build(Element elem, AssertionBuilderFactory factory) {

        String localName = elem.getLocalName();
        QName qn = new QName(elem.getNamespaceURI(), localName);

        boolean optional = false;

        Attr attribute = PolicyConstants.findOptionalAttribute(elem);
        if (attribute != null) {
            optional = Boolean.valueOf(attribute.getValue());
        }
        if (MetadataConstants.ADDRESSING_ASSERTION_QNAME.equals(qn)
            || MetadataConstants.ADDRESSING_ASSERTION_QNAME_0705.equals(qn)) {
            Assertion nap = new XMLPrimitiveAssertionBuilder() {
                public Assertion newPrimitiveAssertion(Element element, Map<QName, String> mp) {
                    return new PrimitiveAssertion(MetadataConstants.ADDRESSING_ASSERTION_QNAME,
                                                  isOptional(element), isIgnorable(element), mp);
                }
                public Assertion newPolicyContainingAssertion(Element element,
                                                              Map<QName, String> mp,
                                                              Policy policy) {
                    return new PolicyContainingPrimitiveAssertion(
                                                  MetadataConstants.ADDRESSING_ASSERTION_QNAME,
                                                  isOptional(element), isIgnorable(element),
                                                  mp,
                                                  policy);
                }
            }.build(elem, factory);
            if (!(nap instanceof PolicyContainingPrimitiveAssertion
                    || nap instanceof org.apache.neethi.builders.PrimitiveAssertion)) {
                // this happens when neethi fails to recognize the specified addressing policy element
                LOG.warning("Unable to recognize the addressing policy");
            }
            return nap;
        } else if (MetadataConstants.ANON_RESPONSES_ASSERTION_QNAME.equals(qn)
            || MetadataConstants.ANON_RESPONSES_ASSERTION_QNAME_0705.equals(qn)) {
            return new PrimitiveAssertion(MetadataConstants.ANON_RESPONSES_ASSERTION_QNAME,
                                          optional);
        } else if (MetadataConstants.NON_ANON_RESPONSES_ASSERTION_QNAME.getLocalPart()
            .equals(localName)
            || MetadataConstants.NON_ANON_RESPONSES_ASSERTION_QNAME_0705.getLocalPart()
                .equals(localName)) {
            return new PrimitiveAssertion(MetadataConstants.NON_ANON_RESPONSES_ASSERTION_QNAME,
                                          optional);
        }
        return null;
    }

    public QName[] getKnownElements() {
        return KNOWN_ELEMENTS;
    }

}
