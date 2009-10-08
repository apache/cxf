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
package org.apache.cxf.ws.security.policy.builders;


import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.AssertionBuilder;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.Layout;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;


public class LayoutBuilder implements AssertionBuilder {
    private static final List<QName> KNOWN_ELEMENTS 
        = Arrays.asList(SP11Constants.LAYOUT, SP12Constants.LAYOUT);
    
    PolicyBuilder builder;
    public LayoutBuilder(PolicyBuilder b) {
        builder = b;
    }
    public List<QName> getKnownElements() {
        return KNOWN_ELEMENTS;
    }
    
    
    public PolicyAssertion build(Element element)
        throws IllegalArgumentException {
        
        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;

        
        Layout layout = new Layout(consts);

        Policy policy = builder.getPolicy(DOMUtils.getFirstElement(element));
        policy = (Policy)policy.normalize(false);

        for (Iterator iterator = policy.getAlternatives(); iterator.hasNext();) {
            processAlternative((List)iterator.next(), layout, consts);
            break; // there should be only one alternative
        }

        return layout;
    }

    public void processAlternative(List assertions, Layout parent, SPConstants consts) {

        for (Iterator iterator = assertions.iterator(); iterator.hasNext();) {
            Assertion assertion = (Assertion)iterator.next();
            QName qname = assertion.getName();

            if (!consts.getNamespace().equals(qname.getNamespaceURI())) {
                continue;
            }

            
            if (SPConstants.LAYOUT_STRICT.equals(qname.getLocalPart())) {
                parent.setValue(SPConstants.LAYOUT_STRICT);
            } else if (SPConstants.LAYOUT_LAX.equals(qname.getLocalPart())) {
                parent.setValue(SPConstants.LAYOUT_LAX);
            } else if (SPConstants.LAYOUT_LAX_TIMESTAMP_FIRST.equals(qname.getLocalPart())) {
                parent.setValue(SPConstants.LAYOUT_LAX_TIMESTAMP_FIRST);
            } else if (SPConstants.LAYOUT_LAX_TIMESTAMP_LAST.equals(qname.getLocalPart())) {
                parent.setValue(SPConstants.LAYOUT_LAX_TIMESTAMP_LAST);
            }

        }
    }
    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        // TODO Auto-generated method stub
        return null;
    }
}
