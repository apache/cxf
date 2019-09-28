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

import java.util.Collection;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.MapNamespaceContext;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.model.RequiredElements;

/**
 * Validate a RequiredElements policy
 */
public class RequiredElementsPolicyValidator implements SecurityPolicyValidator {

    /**
     * Return true if this SecurityPolicyValidator implementation is capable of validating a
     * policy defined by the AssertionInfo parameter
     */
    public boolean canValidatePolicy(AssertionInfo assertionInfo) {
        return assertionInfo.getAssertion() != null
            && (SP12Constants.REQUIRED_ELEMENTS.equals(assertionInfo.getAssertion().getName())
                || SP11Constants.REQUIRED_ELEMENTS.equals(assertionInfo.getAssertion().getName()));
    }

    /**
     * Validate policies.
     */
    public void validatePolicies(PolicyValidatorParameters parameters, Collection<AssertionInfo> ais) {
        for (AssertionInfo ai : ais) {
            RequiredElements rp = (RequiredElements)ai.getAssertion();
            ai.setAsserted(true);

            if (rp != null && rp.getXPaths() != null && !rp.getXPaths().isEmpty()) {
                XPathFactory factory = XPathFactory.newInstance();
                try {
                    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
                } catch (javax.xml.xpath.XPathFactoryConfigurationException ex) {
                    // ignore
                }

                for (org.apache.wss4j.policy.model.XPath xPath : rp.getXPaths()) {
                    Map<String, String> namespaces = xPath.getPrefixNamespaceMap();
                    String expression = xPath.getXPath();

                    XPath xpath = factory.newXPath();
                    if (namespaces != null) {
                        xpath.setNamespaceContext(new MapNamespaceContext(namespaces));
                    }
                    NodeList list;
                    Element header = parameters.getSoapHeader();
                    header = (Element)DOMUtils.getDomElement(header);
                    if (header == null) {
                        ai.setNotAsserted("No header element matching XPath " + expression + " found.");
                    } else {
                        try {
                            list = (NodeList)xpath.evaluate(expression,
                                                                     header,
                                                                     XPathConstants.NODESET);
                            if (list.getLength() == 0) {
                                ai.setNotAsserted("No header element matching XPath " + expression + " found.");
                            }
                        } catch (XPathExpressionException e) {
                            ai.setNotAsserted("Invalid XPath expression " + expression + " " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

}
