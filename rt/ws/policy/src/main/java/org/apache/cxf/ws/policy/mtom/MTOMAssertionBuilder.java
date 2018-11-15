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

package org.apache.cxf.ws.policy.mtom;


import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.builders.AssertionBuilder;

public class MTOMAssertionBuilder implements AssertionBuilder<Element> {
    private static final QName[] KNOWN_ELEMENTS
        = {MetadataConstants.MTOM_ASSERTION_QNAME};

    public Assertion build(Element elem, AssertionBuilderFactory f) {
        String localName = elem.getLocalName();
        QName qn = new QName(elem.getNamespaceURI(), localName);

        boolean optional = false;
        Attr attribute = PolicyConstants.findOptionalAttribute(elem);
        if (attribute != null) {
            optional = Boolean.valueOf(attribute.getValue());
        }

        if (MetadataConstants.MTOM_ASSERTION_QNAME.equals(qn)) {
            return new PrimitiveAssertion(MetadataConstants.MTOM_ASSERTION_QNAME, optional);
        }

        return null;
    }

    public QName[] getKnownElements() {
        return KNOWN_ELEMENTS;
    }

}
