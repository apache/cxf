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
package org.apache.cxf.ws.security.policy.custom;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;

/**
 * This class retrieves the default AlgorithmSuites.
 */
public class DefaultAlgorithmSuiteLoader implements AlgorithmSuiteLoader {
    
    private static final String CXF_CUSTOM_POLICY_NS = 
         "http://cxf.apache.org/custom/security-policy";

    public AlgorithmSuite getAlgorithmSuite(Element policyElement, SPConstants consts) {
        if (policyElement != null) {
            Element algorithm = DOMUtils.getFirstElement(policyElement);
            if (algorithm != null) {
                AlgorithmSuite algorithmSuite = null;
                if (CXF_CUSTOM_POLICY_NS.equals(algorithm.getNamespaceURI())) {
                    algorithmSuite = new GCMAlgorithmSuite(consts);
                } else {
                    algorithmSuite = new AlgorithmSuite(consts);
                }
                algorithmSuite.setAlgorithmSuite(algorithm.getLocalName());
                return algorithmSuite;
            }
        }
        return null;
    }

}
