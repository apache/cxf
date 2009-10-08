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
import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;
import org.apache.cxf.ws.security.policy.model.Layout;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.cxf.ws.security.policy.model.TransportBinding;
import org.apache.cxf.ws.security.policy.model.TransportToken;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;

public class TransportBindingBuilder implements AssertionBuilder {
    private static final List<QName> KNOWN_ELEMENTS 
        = Arrays.asList(SP11Constants.TRANSPORT_BINDING, SP12Constants.TRANSPORT_BINDING);

    
    PolicyBuilder builder;
    public TransportBindingBuilder(PolicyBuilder b) {
        builder = b;
    }
    
    public PolicyAssertion build(Element element)
        throws IllegalArgumentException {
        
        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;

        TransportBinding transportBinding = new TransportBinding(consts);

        Policy policy = builder.getPolicy(DOMUtils.getFirstElement(element));
        policy = (Policy)policy.normalize(false);

        for (Iterator iterator = policy.getAlternatives(); iterator.hasNext();) {
            processAlternative((List)iterator.next(), transportBinding, consts);

            /*
             * since there should be only one alternative
             */
            break;
        }

        return transportBinding;
    }

    public List<QName> getKnownElements() {
        return KNOWN_ELEMENTS;
    }

    private void processAlternative(List assertionList, 
                                    TransportBinding parent,
                                    SPConstants consts) {

        for (Iterator iterator = assertionList.iterator(); iterator.hasNext();) {

            Assertion primitive = (Assertion)iterator.next();
            QName name = primitive.getName();

            if (!consts.getNamespace().equals(name.getNamespaceURI())) {
                continue;
            }
            
            if (name.getLocalPart().equals(SPConstants.ALGO_SUITE)) {
                parent.setAlgorithmSuite((AlgorithmSuite)primitive);
            } else if (name.getLocalPart().equals(SPConstants.TRANSPORT_TOKEN)) {
                parent.setTransportToken((TransportToken)primitive);
            } else if (name.getLocalPart().equals(SPConstants.INCLUDE_TIMESTAMP)) {
                parent.setIncludeTimestamp(true);
            } else if (name.getLocalPart().equals(SPConstants.LAYOUT)) {
                parent.setLayout((Layout)primitive);
            } else if (name.getLocalPart().equals(SPConstants.SIGNED_SUPPORTING_TOKENS)) {
                parent.setSignedSupportingToken((SupportingToken)primitive);
            } else if (name.getLocalPart().equals(SPConstants.SIGNED_ENDORSING_SUPPORTING_TOKENS)) {
                parent.setSignedEndorsingSupportingTokens((SupportingToken)primitive);
            }
        }
    }

    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        // TODO Auto-generated method stub
        return null;
    }
}
