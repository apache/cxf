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

package org.apache.cxf.ws.policy.builder.primitive;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.neethi.Constants;

public class NestedPrimitiveAssertionBuilder extends PrimitiveAssertionBuilder {
    
    private PolicyBuilder builder;    
    
    public NestedPrimitiveAssertionBuilder() {
    }
    public NestedPrimitiveAssertionBuilder(PolicyBuilder b) {
        builder = b;
    }
    
    public void setPolicyBuilder(PolicyBuilder b) {
        builder = b;
    }
        

    
    @Override
    public PolicyAssertion build(Element element) {
        Node nd = element.getFirstChild();
        int count = 0;
        int policyCount = 0;
        while (nd != null) {
            if (nd instanceof Element) {
                count++;
                Element el = (Element)nd;
                if (Constants.isPolicyElement(el.getNamespaceURI(), el.getLocalName())) {
                    policyCount++;
                }
            }
            nd = nd.getNextSibling();
        }
        if (count == 0) {
            return new PrimitiveAssertion(element);
        } else if (policyCount == 1 && count == 1) {
            return new NestedPrimitiveAssertion(element, builder); 
        }
        return new PrimitiveAssertion(element);
    }

    
}
