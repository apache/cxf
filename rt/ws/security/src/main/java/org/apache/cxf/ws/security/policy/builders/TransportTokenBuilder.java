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

import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.Token;
import org.apache.cxf.ws.security.policy.model.TransportToken;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.builders.AssertionBuilder;


public class TransportTokenBuilder implements AssertionBuilder<Element> {
    
    PolicyBuilder builder;
    public TransportTokenBuilder(PolicyBuilder b) {
        builder = b;
    }
    
    public Assertion build(Element element, AssertionBuilderFactory factory)
        throws IllegalArgumentException {
        
        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;

        
        TransportToken transportToken = new TransportToken(consts, builder);

        Policy policy = builder.getPolicy(DOMUtils.getFirstElement(element));
        policy = (Policy)policy.normalize(builder.getPolicyRegistry(), false);

        for (Iterator<List<Assertion>> iterator = policy.getAlternatives(); iterator.hasNext();) {
            transportToken.setToken((Token)((iterator.next()).get(0)));
            break; // since there should be only one alternative
        }

        return transportToken;
    }

    public QName[] getKnownElements() {
        return new QName[]{SP11Constants.TRANSPORT_TOKEN, SP12Constants.TRANSPORT_TOKEN};
    }
}
