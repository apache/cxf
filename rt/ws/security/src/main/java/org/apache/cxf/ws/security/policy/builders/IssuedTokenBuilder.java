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

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.VersionTransformer;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.security.policy.SP11Constants;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.IssuedToken;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.builders.AssertionBuilder;


public class IssuedTokenBuilder implements AssertionBuilder<Element> {
    
    PolicyBuilder builder;
    public IssuedTokenBuilder(PolicyBuilder b) {
        builder = b;
    }
    public QName[] getKnownElements() {
        return new QName[]{SP11Constants.ISSUED_TOKEN, SP12Constants.ISSUED_TOKEN};
    }

    public Assertion build(Element element, AssertionBuilderFactory factory)
        throws IllegalArgumentException {
        
        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;
    

        IssuedToken issuedToken = new IssuedToken(consts);
        issuedToken.setOptional(PolicyConstants.isOptional(element));
        issuedToken.setIgnorable(PolicyConstants.isIgnorable(element));

        String includeAttr = DOMUtils.getAttribute(element, consts.getIncludeToken());
        if (includeAttr != null) {
            issuedToken.setInclusion(consts.getInclusionFromAttributeValue(includeAttr));
        }
        
        Element child = DOMUtils.getFirstElement(element);
        while (child != null) {
            String ln = child.getLocalName();
            if (SPConstants.ISSUER.equals(ln)) {
                try {
                    EndpointReferenceType epr = VersionTransformer.parseEndpointReference(child);
                    issuedToken.setIssuerEpr(epr);
                } catch (JAXBException e) {
                    throw new IllegalArgumentException(e);
                }
            } else if (SPConstants.REQUEST_SECURITY_TOKEN_TEMPLATE.equals(ln)) {
                issuedToken.setRstTemplate(child);
            } else if (org.apache.neethi.Constants.ELEM_POLICY.equals(ln)) {
                Policy policy = builder.getPolicy(child);
                policy = (Policy)policy.normalize(builder.getPolicyRegistry(), false);

                for (Iterator<List<Assertion>> iterator = policy.getAlternatives(); iterator.hasNext();) {
                    processAlternative(iterator.next(), issuedToken);
                    break; // since there should be only one alternative ..
                }                
            } else if (SPConstants.ISSUER_NAME.equals(ln)) {
                String issuerName = child.getNodeValue();
                issuedToken.setIssuerName(issuerName);
            }
            
            child = DOMUtils.getNextElement(child);
        }
        return issuedToken;
    }


    private void processAlternative(List<Assertion> assertions, IssuedToken parent) {
        QName name;

        for (Assertion assertion : assertions) {
            name = assertion.getName();

            if (SPConstants.REQUIRE_DERIVED_KEYS.equals(name.getLocalPart())) {
                parent.setDerivedKeys(true);
            } else if (SPConstants.REQUIRE_EXTERNAL_REFERENCE.equals(name.getLocalPart())) {
                parent.setRequireExternalReference(true);
            } else if (SPConstants.REQUIRE_INTERNAL_REFERENCE.equals(name.getLocalPart())) {
                parent.setRequireInternalReference(true);
            }
        }

    }

}
