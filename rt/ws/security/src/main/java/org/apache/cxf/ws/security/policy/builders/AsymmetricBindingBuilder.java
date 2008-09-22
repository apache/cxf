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
import org.apache.cxf.ws.security.policy.model.AsymmetricBinding;
import org.apache.cxf.ws.security.policy.model.InitiatorToken;
import org.apache.cxf.ws.security.policy.model.Layout;
import org.apache.cxf.ws.security.policy.model.RecipientToken;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;

public class AsymmetricBindingBuilder implements AssertionBuilder {
    private static final List<QName> KNOWN_ELEMENTS 
        = Arrays.asList(SP11Constants.ASYMMETRIC_BINDING, SP12Constants.ASYMMETRIC_BINDING);

    PolicyBuilder builder;
    public AsymmetricBindingBuilder(PolicyBuilder b) {
        builder = b;
    }    
    public List<QName> getKnownElements() {
        return KNOWN_ELEMENTS;
    }
    
    public PolicyAssertion build(Element element)
        throws IllegalArgumentException {

        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;

        
        AsymmetricBinding asymmetricBinding = new AsymmetricBinding(consts);

        Policy policy = builder.getPolicy(DOMUtils.getFirstElement(element));
        policy = (Policy)policy.normalize(false);

        for (Iterator iterator = policy.getAlternatives(); iterator.hasNext();) {
            processAlternative((List)iterator.next(), asymmetricBinding, consts);

            /*
             * since there should be only one alternative
             */
            break;
        }

        return asymmetricBinding;
    }

    private void processAlternative(List assertions, 
                                    AsymmetricBinding asymmetricBinding,
                                    SPConstants consts) {

        Assertion assertion;
        QName name;

        for (Iterator iterator = assertions.iterator(); iterator.hasNext();) {
            assertion = (Assertion)iterator.next();
            name = assertion.getName();

            if (!consts.getNamespace().equals(name.getNamespaceURI())
                && !SP12Constants.INSTANCE.getNamespace().equals(name.getNamespaceURI())) {
                continue;
            }

            
            if (SPConstants.INITIATOR_TOKEN.equals(name.getLocalPart())) {
                asymmetricBinding.setInitiatorToken((InitiatorToken)assertion);

            } else if (SPConstants.RECIPIENT_TOKEN.equals(name.getLocalPart())) {
                asymmetricBinding.setRecipientToken((RecipientToken)assertion);

            } else if (SPConstants.ALGO_SUITE.equals(name.getLocalPart())) {
                asymmetricBinding.setAlgorithmSuite((AlgorithmSuite)assertion);

            } else if (SPConstants.LAYOUT.equals(name.getLocalPart())) {
                asymmetricBinding.setLayout((Layout)assertion);

            } else if (SPConstants.INCLUDE_TIMESTAMP.equals(name.getLocalPart())) {
                asymmetricBinding.setIncludeTimestamp(true);

            } else if (SPConstants.ENCRYPT_BEFORE_SIGNING.equals(name.getLocalPart())) {
                asymmetricBinding.setProtectionOrder(SPConstants.ProtectionOrder.EncryptBeforeSigning);

            } else if (SPConstants.SIGN_BEFORE_ENCRYPTING.equals(name.getLocalPart())) {
                asymmetricBinding.setProtectionOrder(SPConstants.ProtectionOrder.SignBeforeEncrypting);

            } else if (SPConstants.ENCRYPT_SIGNATURE.equals(name.getLocalPart())) {
                asymmetricBinding.setSignatureProtection(true);

            } else if (SPConstants.PROTECT_TOKENS.equals(name.getLocalPart())) {
                asymmetricBinding.setTokenProtection(true);

            } else if (SPConstants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY.equals(name.getLocalPart())) {
                asymmetricBinding.setEntireHeadersAndBodySignatures(true);
            }
        }
    }


    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        // TODO Auto-generated method stub
        return null;
    }

}
