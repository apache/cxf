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

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.rm.RM11Constants;
import org.apache.cxf.ws.rm.policy.RM12Assertion.Order;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Constants;
import org.apache.neethi.builders.AssertionBuilder;

/**
 * Builds a WS-RMP 1.2 assertion from the raw XML. Unlike WS-RMP 1.0, in WS-RMP 1.2 the actual assertions are
 * nested within layers of <wsp:Policy> operators so need to be handled directly (not by JAXB).
 */
public class RM12AssertionBuilder implements AssertionBuilder<Element> {
    
    /**
     * @see org.apache.neethi.builders.AssertionBuilder#getKnownElements()
     */
    public QName[] getKnownElements() {
        return new QName[] {RM11Constants.WSRMP_RMASSERTION_QNAME};
    }
    
    /**
     * @see org.apache.neethi.builders.AssertionBuilder#build(java.lang.Object,
     *  org.apache.neethi.AssertionBuilderFactory)
     */
    public Assertion build(Element element, AssertionBuilderFactory factory) throws IllegalArgumentException {
        
        RM12Assertion assertion = new RM12Assertion();
        assertion.setOptional(PolicyConstants.isOptional(element));
        assertion.setIgnorable(PolicyConstants.isIgnorable(element));
        
        // dig into the nested structure to set property values
        Element elem = DOMUtils.getFirstElement(element);
        while (elem != null) {
            if (DOMUtils.getFirstChildWithName(elem, 
                RM11Constants.WSRMP_NAMESPACE_URI, RM12Assertion.DELIVERYASSURANCE_NAME) != null) {
                
                // find nested policy and definitions within (note this won't handle nested policy operators)
                Element childEl = DOMUtils.getFirstElement(elem);
                while (childEl != null) {
                    if (Constants.isPolicyElement(childEl.getNamespaceURI(), childEl.getLocalName())) {
                        handlePolicy(childEl, assertion);
                    }
                }

            } else if (DOMUtils.getFirstChildWithName(elem, 
                RM11Constants.WSRMP_NAMESPACE_URI, RM12Assertion.SEQUENCESTR_NAME) != null) {
                assertion.setSequenceSTR(true);
            } else if (DOMUtils.getFirstChildWithName(elem, 
                RM11Constants.WSRMP_NAMESPACE_URI, RM12Assertion.SEQUENCETRANSEC_NAME) != null) {
                assertion.setSequenceTransportSecurity(true);              
            }
            elem = DOMUtils.getNextElement(elem);
        }

        return assertion;
    }

    /**
     * @param childEl
     * @param assertion
     */
    private void handlePolicy(Element childEl, RM12Assertion assertion) {
        
        // don't check for conflicts or repeats, just use the last values supplied
        Element innerEl = DOMUtils.getFirstElement(childEl);
        if (RM11Constants.WSRMP_NAMESPACE_URI.equals(innerEl.getNamespaceURI())) {
            String lname = innerEl.getLocalName();
            if (RM12Assertion.INORDER_NAME.equals(lname)) {
                assertion.setInOrder(true);
            } else {
                Order order = RM12Assertion.Order.valueOf(lname);
                if (order != null) {
                    assertion.setOrder(order);
                }
            }
        }
    }
}