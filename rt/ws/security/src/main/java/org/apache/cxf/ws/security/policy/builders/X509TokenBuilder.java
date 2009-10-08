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
import org.apache.cxf.ws.security.policy.model.X509Token;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;

public class X509TokenBuilder implements AssertionBuilder {
    private static final List<QName> KNOWN_ELEMENTS 
        = Arrays.asList(SP11Constants.X509_TOKEN, SP12Constants.X509_TOKEN);
    
    
    PolicyBuilder builder;
    public X509TokenBuilder(PolicyBuilder b) {
        builder = b;
    }

    public PolicyAssertion build(Element element)
        throws IllegalArgumentException {
        
        SPConstants consts = SP11Constants.SP_NS.equals(element.getNamespaceURI())
            ? SP11Constants.INSTANCE : SP12Constants.INSTANCE;
        X509Token x509Token = new X509Token(consts);

        Element policyElement = DOMUtils.getFirstElement(element);

        // Process token inclusion
        String includeAttr = DOMUtils.getAttribute(element, consts.getIncludeToken());

        if (includeAttr != null) {
            SPConstants.IncludeTokenType inclusion 
                = consts.getInclusionFromAttributeValue(includeAttr);
            x509Token.setInclusion(inclusion);
        }

        if (policyElement != null) {

            if (DOMUtils.getFirstChildWithName(policyElement, consts.getRequiredDerivedKeys()) != null) {
                x509Token.setDerivedKeys(true);
            } else if (DOMUtils.getFirstChildWithName(policyElement, 
                                                      SP12Constants.REQUIRE_IMPLIED_DERIVED_KEYS) != null) {
                x509Token.setImpliedDerivedKeys(true);
            } else if (DOMUtils.getFirstChildWithName(policyElement, 
                                                      SP12Constants.REQUIRE_EXPLICIT_DERIVED_KEYS) != null) {
                x509Token.setExplicitDerivedKeys(true);
            }


            Policy policy = builder.getPolicy(DOMUtils.getFirstElement(element));
            policy = (Policy)policy.normalize(false);

            for (Iterator iterator = policy.getAlternatives(); iterator.hasNext();) {
                processAlternative((List)iterator.next(), x509Token, consts);

                /*
                 * since there should be only one alternative
                 */
                break;
            }
        }
        return x509Token;
    }

    private void processAlternative(List assertions, X509Token parent, SPConstants consts) {
        Assertion assertion;
        QName name;

        for (Iterator iterator = assertions.iterator(); iterator.hasNext();) {
            assertion = (Assertion)iterator.next();
            name = assertion.getName();
            
            if (!consts.getNamespace().equals(name.getNamespaceURI())) {
                continue;
            }

            if (SPConstants.REQUIRE_KEY_IDENTIFIER_REFERENCE.equals(name.getLocalPart())) {
                parent.setRequireKeyIdentifierReference(true);

            } else if (SPConstants.REQUIRE_ISSUER_SERIAL_REFERENCE.equals(name.getLocalPart())) {
                parent.setRequireIssuerSerialReference(true);

            } else if (SPConstants.REQUIRE_EMBEDDED_TOKEN_REFERENCE.equals(name.getLocalPart())) {
                parent.setRequireEmbeddedTokenReference(true);

            } else if (SPConstants.REQUIRE_THUMBPRINT_REFERENCE.equals(name.getLocalPart())) {
                parent.setRequireThumbprintReference(true);

            } else if (SPConstants.WSS_X509_V1_TOKEN10.equals(name.getLocalPart())) {
                parent.setTokenVersionAndType(SPConstants.WSS_X509_V1_TOKEN10);

            } else if (SPConstants.WSS_X509_V1_TOKEN11.equals(name.getLocalPart())) {
                parent.setTokenVersionAndType(SPConstants.WSS_X509_V1_TOKEN11);

            } else if (SPConstants.WSS_X509_V3_TOKEN10.equals(name.getLocalPart())) {
                parent.setTokenVersionAndType(SPConstants.WSS_X509_V3_TOKEN10);

            } else if (SPConstants.WSS_X509_V3_TOKEN11.equals(name.getLocalPart())) {
                parent.setTokenVersionAndType(SPConstants.WSS_X509_V3_TOKEN11);

            } else if (SPConstants.WSS_X509_PKCS7_TOKEN10.equals(name.getLocalPart())) {
                parent.setTokenVersionAndType(SPConstants.WSS_X509_PKCS7_TOKEN10);

            } else if (SPConstants.WSS_X509_PKCS7_TOKEN11.equals(name.getLocalPart())) {
                parent.setTokenVersionAndType(SPConstants.WSS_X509_PKCS7_TOKEN11);

            } else if (SPConstants.WSS_X509_PKI_PATH_V1_TOKEN10.equals(name.getLocalPart())) {
                parent.setTokenVersionAndType(SPConstants.WSS_X509_PKI_PATH_V1_TOKEN10);

            } else if (SPConstants.WSS_X509_PKI_PATH_V1_TOKEN11.equals(name.getLocalPart())) {
                parent.setTokenVersionAndType(SPConstants.WSS_X509_PKI_PATH_V1_TOKEN11);
            }
        }
    }

    public List<QName> getKnownElements() {
        return KNOWN_ELEMENTS;
    }

    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        // TODO Auto-generated method stub
        return null;
    }
}
