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
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.AssertionBuilder;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;
import org.apache.cxf.ws.security.policy.model.Layout;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.cxf.ws.security.policy.model.TransportBinding;
import org.apache.cxf.ws.security.policy.model.TransportToken;

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
        processAlternative(element, transportBinding, consts);

        return transportBinding;
    }

    public List<QName> getKnownElements() {
        return KNOWN_ELEMENTS;
    }

    private void processAlternative(Element element, 
                                    TransportBinding parent,
                                    SPConstants consts) {
        Element polEl = DOMUtils.getFirstElement(element);
        while (polEl != null) {
            if (PolicyConstants.isPolicyElem(new QName(polEl.getNamespaceURI(),
                                                       polEl.getLocalName()))) {
                Element child = DOMUtils.getFirstElement(polEl);
                while (child != null) {
                    String name = child.getLocalName();
                    if (name.equals(SPConstants.ALGO_SUITE)) {
                        parent.setAlgorithmSuite((AlgorithmSuite)new AlgorithmSuiteBuilder().build(child));
                    } else if (name.equals(SPConstants.TRANSPORT_TOKEN)) {
                        parent.setTransportToken((TransportToken)new TransportTokenBuilder(builder)
                                                        .build(child));
                    } else if (name.equals(SPConstants.INCLUDE_TIMESTAMP)) {
                        parent.setIncludeTimestamp(true);
                    } else if (name.equals(SPConstants.LAYOUT)) {
                        parent.setLayout((Layout)new LayoutBuilder().build(child));
                    } else if (name.equals(SPConstants.SIGNED_SUPPORTING_TOKENS)
                        || name.equals(SPConstants.SIGNED_ENDORSING_SUPPORTING_TOKENS)) {
                        
                        if (consts.getVersion() == SPConstants.Version.SP_V11) {
                            parent.setSignedSupportingToken((SupportingToken)
                                                            new SupportingTokensBuilder(builder)
                                                            .build(child));
                        } else {
                            parent.setSignedSupportingToken((SupportingToken)
                                                            new SupportingTokens12Builder(builder)
                                                                .build(child));                        
                        }
                    }
                    child = DOMUtils.getNextElement(child);
                }
            }
            polEl = DOMUtils.getNextElement(polEl);
        }
        
    }

    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        // TODO Auto-generated method stub
        return null;
    }
}
