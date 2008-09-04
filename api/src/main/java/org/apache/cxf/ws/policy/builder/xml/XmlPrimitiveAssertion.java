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

package org.apache.cxf.ws.policy.builder.xml;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.neethi.PolicyComponent;

/**
 * XmlPrimitiveAssertion is a primitive implementation of an AssertionBuilder
 * that simply wraps the underlying xml element.
 * 
 */
public class XmlPrimitiveAssertion extends PrimitiveAssertion {

    private Element element;

    /**
     * Constructs a XmlPrimitiveAssertion from an xml element.
     * 
     * @param e the xml element
     */
    public XmlPrimitiveAssertion(Element e) {
        super(e);
        element = e;
    }

    /**
     * Returns the wrapped element.
     * 
     * @return the wrapped element
     */
    public Element getValue() {
        return element;
    }


    /**
     * Throws an UnsupportedOperationException since an assertion of an unknown
     * element can't be fully normalized due to it's unknown composite.
     */
    public PolicyComponent normalize(boolean isDeep) {
        throw new UnsupportedOperationException();
    }
    
    protected PolicyAssertion cloneMandatory() {
        Element e = (Element)element.cloneNode(true);
        if (isOptional()) {
            Attr att = PolicyConstants.findOptionalAttribute(e);
            if (att != null) {
                e.removeAttributeNode(att);
            }
        }
        return new XmlPrimitiveAssertion(e);        
    }
}
