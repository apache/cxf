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

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.builders.AssertionBuilder;
import org.apache.neethi.builders.xml.XMLPrimitiveAssertionBuilder;

/**
 * Builds a WS-I RSP Conformant assertion.
 */
public class RSPAssertionBuilder implements AssertionBuilder<Element> {

    public static final String CONFORMANT_NAME = "Conformant";
    public static final String RSP_NAMESPACE = "http://ws-i.org/profiles/rsp/1.0/";
    public static final QName CONFORMANT_QNAME = new QName(RSP_NAMESPACE, CONFORMANT_NAME);

    public static final QName[] KNOWN_ELEMENTS = {
        CONFORMANT_QNAME
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
        if (RSP_NAMESPACE.equals(elem.getNamespaceURI()) && CONFORMANT_NAME.equals(elem.getLocalName())) {
            boolean optional = XMLPrimitiveAssertionBuilder.isOptional(elem);
            assertion = new PrimitiveAssertion(CONFORMANT_QNAME,  optional);
        }
        return assertion;
    }
}