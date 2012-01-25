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
package org.apache.cxf.systest.ws.gcm;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.custom.AlgorithmSuiteLoader;
import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;

/**
 * This class retrieves a custom AlgorithmSuite for use with security policies that require GCM
 * algorithms.
 */
public class GCMAlgorithmSuiteLoader implements AlgorithmSuiteLoader {
    
    public GCMAlgorithmSuiteLoader(Bus bus) {
        bus.setExtension(this, AlgorithmSuiteLoader.class);
    }

    public AlgorithmSuite getAlgorithmSuite(Element policyElement, SPConstants consts) {
        if (policyElement != null) {
            GCMAlgorithmSuite algorithmSuite = new GCMAlgorithmSuite(consts);
            String algorithmSuiteName = DOMUtils.getFirstElement(policyElement).getLocalName();
            algorithmSuite.setAlgorithmSuite(algorithmSuiteName);
            return algorithmSuite;
        }
        return null;
    }

}
