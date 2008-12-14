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
import org.apache.cxf.ws.security.policy.SPConstants.SupportTokenType;
import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedElements;
import org.apache.cxf.ws.security.policy.model.SignedEncryptedParts;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.cxf.ws.security.policy.model.Token;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;


public class SupportingTokensBuilder implements AssertionBuilder {
    private static final List<QName> KNOWN_ELEMENTS 
        = Arrays.asList(SP11Constants.SUPPORTING_TOKENS, 
                        SP11Constants.SIGNED_SUPPORTING_TOKENS,
                        SP11Constants.ENDORSING_SUPPORTING_TOKENS,
                        SP11Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS);

    
    PolicyBuilder builder;
    public SupportingTokensBuilder(PolicyBuilder b) {
        builder = b;
    }
    public List<QName> getKnownElements() {
        return KNOWN_ELEMENTS;
    }

    
    
    public PolicyAssertion build(Element element)
        throws IllegalArgumentException {
        QName name = DOMUtils.getElementQName(element);
        SupportingToken supportingToken = null;

        if (SP11Constants.SUPPORTING_TOKENS.equals(name)) {
            supportingToken = new SupportingToken(SupportTokenType.SUPPORTING_TOKEN_SUPPORTING,
                                                  SP11Constants.INSTANCE);
        } else if (SP11Constants.SIGNED_SUPPORTING_TOKENS.equals(name)) {
            supportingToken = new SupportingToken(SupportTokenType.SUPPORTING_TOKEN_SIGNED, 
                                                  SP11Constants.INSTANCE);
        } else if (SP11Constants.ENDORSING_SUPPORTING_TOKENS.equals(name)) {
            supportingToken = new SupportingToken(SupportTokenType.SUPPORTING_TOKEN_ENDORSING, 
                                                  SP11Constants.INSTANCE);
        } else if (SP11Constants.SIGNED_ENDORSING_SUPPORTING_TOKENS.equals(name)) {
            supportingToken = new SupportingToken(SupportTokenType.SUPPORTING_TOKEN_SIGNED_ENDORSING,
                                                  SP11Constants.INSTANCE);
        }

        Policy policy = builder.getPolicy(DOMUtils.getFirstElement(element));
        policy = (Policy)policy.normalize(false);

        for (Iterator iterator = policy.getAlternatives(); iterator.hasNext();) {
            processAlternative((List)iterator.next(), supportingToken);
            /*
             * for the moment we will say there should be only one alternative
             */
            break;
        }

        return supportingToken;
    }


    private void processAlternative(List assertions, SupportingToken supportingToken) {

        for (Iterator iterator = assertions.iterator(); iterator.hasNext();) {

            Assertion primitive = (Assertion)iterator.next();
            QName qname = primitive.getName();

            if (SP12Constants.ALGORITHM_SUITE.equals(qname)) {
                supportingToken.setAlgorithmSuite((AlgorithmSuite)primitive);

            } else if (SP12Constants.SIGNED_PARTS.equals(qname)) {
                supportingToken.setSignedParts((SignedEncryptedParts)primitive);

            } else if (SP12Constants.SIGNED_ELEMENTS.equals(qname)) {
                supportingToken.setSignedElements((SignedEncryptedElements)primitive);

            } else if (SP12Constants.ENCRYPTED_PARTS.equals(qname)) {
                supportingToken.setEncryptedParts((SignedEncryptedParts)primitive);

            } else if (SP12Constants.ENCRYPTED_ELEMENTS.equals(qname)) {
                supportingToken.setEncryptedElements((SignedEncryptedElements)primitive);

            } else if (primitive instanceof Token) {
                supportingToken.addToken((Token)primitive);
            }
        }
    }



    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        // TODO Auto-generated method stub
        return null;
    }
}
