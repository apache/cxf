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

package org.apache.cxf.ws.security.wss4j.policyvalidators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.MapNamespaceContext;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageScope;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.model.RequiredElements;

/**
 * Validate either a SignedElements, EncryptedElements or ContentEncryptedElements policy
 */
public class SecuredElementsPolicyValidator implements SecurityPolicyValidator {

    private CoverageType coverageType = CoverageType.ENCRYPTED;
    private CoverageScope coverageScope = CoverageScope.ELEMENT;

    /**
     * Return true if this SecurityPolicyValidator implementation is capable of validating a
     * policy defined by the AssertionInfo parameter
     */
    public boolean canValidatePolicy(AssertionInfo assertionInfo) {
        if (coverageType == CoverageType.SIGNED) {
            return assertionInfo.getAssertion() != null
                && (SP12Constants.SIGNED_ELEMENTS.equals(assertionInfo.getAssertion().getName())
                    || SP11Constants.SIGNED_ELEMENTS.equals(assertionInfo.getAssertion().getName()));
        } else if (coverageScope == CoverageScope.CONTENT) {
            return assertionInfo.getAssertion() != null
                && (SP12Constants.CONTENT_ENCRYPTED_ELEMENTS.equals(assertionInfo.getAssertion().getName())
                    || SP11Constants.CONTENT_ENCRYPTED_ELEMENTS.equals(assertionInfo.getAssertion().getName()));
        } else {
            return assertionInfo.getAssertion() != null
                && (SP12Constants.ENCRYPTED_ELEMENTS.equals(assertionInfo.getAssertion().getName())
                    || SP11Constants.ENCRYPTED_ELEMENTS.equals(assertionInfo.getAssertion().getName()));
        }
    }

    /**
     * Validate policies.
     */
    public void validatePolicies(PolicyValidatorParameters parameters, Collection<AssertionInfo> ais) {

        // XPathFactory and XPath are not thread-safe so we must recreate them
        // each request.
        final XPathFactory factory = XPathFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        } catch (javax.xml.xpath.XPathFactoryConfigurationException ex) {
            // ignore
        }
        final XPath xpath = factory.newXPath();

        Element soapEnvelope = parameters.getSoapHeader() != null
            ? parameters.getSoapHeader().getOwnerDocument().getDocumentElement() : null;
        Collection<WSDataRef> dataRefs = parameters.getEncrypted();
        if (coverageType == CoverageType.SIGNED) {
            dataRefs = parameters.getSigned();
        }

        for (AssertionInfo ai : ais) {
            RequiredElements elements = (RequiredElements)ai.getAssertion();
            ai.setAsserted(true);

            if (elements != null && elements.getXPaths() != null && !elements.getXPaths().isEmpty()) {
                List<String> expressions = new ArrayList<>();
                MapNamespaceContext namespaceContext = new MapNamespaceContext();

                for (org.apache.wss4j.policy.model.XPath xPath : elements.getXPaths()) {
                    expressions.add(xPath.getXPath());
                    Map<String, String> namespaceMap = xPath.getPrefixNamespaceMap();
                    if (namespaceMap != null) {
                        namespaceContext.addNamespaces(namespaceMap);
                    }
                }

                if (parameters.getSoapHeader() == null) {
                    ai.setNotAsserted("No " + coverageType
                                      + " element found matching one of the XPaths "
                                      + Arrays.toString(expressions.toArray()));
                } else {
                    xpath.setNamespaceContext(namespaceContext);
                    try {
                        CryptoCoverageUtil.checkCoverage(soapEnvelope, dataRefs,
                                                         xpath, expressions, coverageType, coverageScope);
                    } catch (WSSecurityException e) {
                        ai.setNotAsserted("No " + coverageType
                                          + " element found matching one of the XPaths "
                                          + Arrays.toString(expressions.toArray()));
                    }
                }
            }
        }
    }

    public CoverageType getCoverageType() {
        return coverageType;
    }

    public void setCoverageType(CoverageType coverageType) {
        this.coverageType = coverageType;
    }

    public CoverageScope getCoverageScope() {
        return coverageScope;
    }

    public void setCoverageScope(CoverageScope coverageScope) {
        this.coverageScope = coverageScope;
    }

}
