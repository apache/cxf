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
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.InitiatorSignatureToken;
import org.apache.cxf.ws.security.policy.model.Token;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.builders.AssertionBuilder;

public class InitiatorSignatureTokenBuilder implements AssertionBuilder<Element> {
    
    PolicyBuilder builder;
    public InitiatorSignatureTokenBuilder(PolicyBuilder b) {
        builder = b;
    }
    public QName[] getKnownElements() {
        return new QName[]{SP11Constants.INITIATOR_SIGNATURE_TOKEN, SP12Constants.INITIATOR_SIGNATURE_TOKEN};
    }
    
    public Assertion build(Element element, AssertionBuilderFactory factory)
        throws IllegalArgumentException {
        
        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;

        InitiatorSignatureToken initiatorToken = new InitiatorSignatureToken(consts, builder);
        initiatorToken.setOptional(PolicyConstants.isOptional(element));
        initiatorToken.setIgnorable(PolicyConstants.isIgnorable(element));

        Policy policy = builder.getPolicy(DOMUtils.getFirstElement(element));
        policy = (Policy)policy.normalize(builder.getPolicyRegistry(), false);

        for (Iterator iterator = policy.getAlternatives(); iterator.hasNext();) {
            processAlternative((List)iterator.next(), initiatorToken);
            break; // TODO process all the token that must be set ..
        }

        return initiatorToken;
    }

    private void processAlternative(List assertions, InitiatorSignatureToken parent) {

        Object token;

        for (Iterator iterator = assertions.iterator(); iterator.hasNext();) {
            token = iterator.next();

            if (token instanceof Token) {
                parent.setInitiatorSignatureToken((Token)token);
            }
        }
    }

}
