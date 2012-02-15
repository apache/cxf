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
import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;
import org.apache.cxf.ws.security.policy.model.Layout;
import org.apache.cxf.ws.security.policy.model.ProtectionToken;
import org.apache.cxf.ws.security.policy.model.SymmetricBinding;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.builders.AssertionBuilder;


public class SymmetricBindingBuilder implements AssertionBuilder<Element> {
    
    PolicyBuilder builder;
    public SymmetricBindingBuilder(PolicyBuilder b) {
        builder = b;
    }
    public QName[] getKnownElements() {
        return new QName[]{SP11Constants.SYMMETRIC_BINDING, SP12Constants.SYMMETRIC_BINDING};
    }
    
    public Assertion build(Element element, AssertionBuilderFactory factory)
        throws IllegalArgumentException {
        
        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;

        SymmetricBinding symmetricBinding = new SymmetricBinding(consts, builder);

        Policy policy = builder.getPolicy(DOMUtils.getFirstElement(element));
        policy = (Policy)policy.normalize(builder.getPolicyRegistry(), false);

        Iterator<List<Assertion>> iterator = policy.getAlternatives();
        if (!iterator.hasNext()) {
            throw new IllegalArgumentException(
                "sp:SymmetricBinding must specify at least one alternative"
            );
        }
        processAlternatives(iterator.next(), symmetricBinding, consts);

        return symmetricBinding;
    }


    private void processAlternatives(
        List<Assertion> assertions,
        SymmetricBinding symmetricBinding,
        SPConstants consts
    ) {
        QName name;
        boolean foundAlgorithmSuite = false;
        for (Assertion assertion : assertions) {
            name = assertion.getName();

            if (!consts.getNamespace().equals(name.getNamespaceURI())
                && !SP12Constants.INSTANCE.getNamespace().equals(name.getNamespaceURI())) {
                continue;
            }

            if (SPConstants.ALGO_SUITE.equals(name.getLocalPart())) {
                foundAlgorithmSuite = true;
                symmetricBinding.setAlgorithmSuite((AlgorithmSuite)assertion);

            } else if (SPConstants.LAYOUT.equals(name.getLocalPart())) {
                symmetricBinding.setLayout((Layout)assertion);

            } else if (SPConstants.INCLUDE_TIMESTAMP.equals(name.getLocalPart())) {
                symmetricBinding.setIncludeTimestamp(true);

            } else if (SPConstants.PROTECTION_TOKEN.equals(name.getLocalPart())) {
                symmetricBinding.setProtectionToken((ProtectionToken)assertion);

            } else if (SPConstants.ENCRYPT_BEFORE_SIGNING.equals(name.getLocalPart())) {
                symmetricBinding.setProtectionOrder(SPConstants.ProtectionOrder.EncryptBeforeSigning);

            } else if (SPConstants.SIGN_BEFORE_ENCRYPTING.equals(name.getLocalPart())) {
                symmetricBinding.setProtectionOrder(SPConstants.ProtectionOrder.SignBeforeEncrypting);

            } else if (SPConstants.ONLY_SIGN_ENTIRE_HEADERS_AND_BODY.equals(name.getLocalPart())) {
                symmetricBinding.setEntireHeadersAndBodySignatures(true);
            } else if (SPConstants.ENCRYPT_SIGNATURE.equals(name.getLocalPart())) {
                symmetricBinding.setSignatureProtection(true);
            } else if (SPConstants.PROTECT_TOKENS.equals(name.getLocalPart())) {
                symmetricBinding.setTokenProtection(true);
            } 
        }
        
        if (!foundAlgorithmSuite && consts != SP11Constants.INSTANCE) {
            throw new IllegalArgumentException(
                "sp:SymmetricBinding/wsp:Policy/sp:AlgorithmSuite must have a value"
            );
        }
    }
}
