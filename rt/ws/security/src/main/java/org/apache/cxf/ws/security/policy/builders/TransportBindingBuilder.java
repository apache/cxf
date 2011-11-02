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

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.helpers.DOMUtils;
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
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Constants;
import org.apache.neethi.builders.AssertionBuilder;

public class TransportBindingBuilder implements AssertionBuilder<Element> {
    
    PolicyBuilder builder;
    Bus bus;
    public TransportBindingBuilder(PolicyBuilder b, Bus bus) {
        builder = b;
        this.bus = bus;
    }
    
    public Assertion build(Element element, AssertionBuilderFactory factory)
        throws IllegalArgumentException {
        
        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;

        TransportBinding transportBinding = new TransportBinding(consts, builder);
        processAlternative(element, transportBinding, consts, factory);

        return transportBinding;
    }

    public QName[] getKnownElements() {
        return new QName[]{SP11Constants.TRANSPORT_BINDING, SP12Constants.TRANSPORT_BINDING};
    }

    private void processAlternative(Element element, 
                                    TransportBinding parent,
                                    SPConstants consts,
                                    AssertionBuilderFactory factory) {
        Element polEl = DOMUtils.getFirstElement(element);
        while (polEl != null) {
            if (Constants.isPolicyElement(new QName(polEl.getNamespaceURI(),
                                                       polEl.getLocalName()))) {
                Element child = DOMUtils.getFirstElement(polEl);
                while (child != null) {
                    String name = child.getLocalName();
                    if (name.equals(SPConstants.ALGO_SUITE)) {
                        parent.setAlgorithmSuite((AlgorithmSuite)new AlgorithmSuiteBuilder(bus)
                            .build(child, factory));
                    } else if (name.equals(SPConstants.TRANSPORT_TOKEN)) {
                        parent.setTransportToken((TransportToken)new TransportTokenBuilder(builder)
                                                        .build(child, factory));
                    } else if (name.equals(SPConstants.INCLUDE_TIMESTAMP)) {
                        parent.setIncludeTimestamp(true);
                    } else if (name.equals(SPConstants.LAYOUT)) {
                        parent.setLayout((Layout)new LayoutBuilder().build(child, factory));
                    } else if (name.equals(SPConstants.SIGNED_SUPPORTING_TOKENS)
                        || name.equals(SPConstants.SIGNED_ENDORSING_SUPPORTING_TOKENS)) {
                        
                        if (consts.getVersion() == SPConstants.Version.SP_V11) {
                            parent.setSignedSupportingToken((SupportingToken)
                                                            new SupportingTokensBuilder(builder)
                                                            .build(child, factory));
                        } else {
                            parent.setSignedSupportingToken((SupportingToken)
                                                            new SupportingTokens12Builder(builder)
                                                                .build(child, factory));
                        }
                    }
                    child = DOMUtils.getNextElement(child);
                }
            }
            polEl = DOMUtils.getNextElement(polEl);
        }
        
    }

}
