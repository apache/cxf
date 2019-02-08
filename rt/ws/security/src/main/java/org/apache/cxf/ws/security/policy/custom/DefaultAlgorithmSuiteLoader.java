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

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.ws.policy.AssertionBuilderRegistry;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertionBuilder;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.builders.xml.XMLPrimitiveAssertionBuilder;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractSecurityAssertion;
import org.apache.wss4j.policy.model.AlgorithmSuite;

/**
 * This class retrieves the default AlgorithmSuites plus the CXF specific GCM AlgorithmSuites.
 */
public class DefaultAlgorithmSuiteLoader implements AlgorithmSuiteLoader {

    public AlgorithmSuite getAlgorithmSuite(Bus bus, SPConstants.SPVersion version, Policy nestedPolicy) {
        AssertionBuilderRegistry reg = bus.getExtension(AssertionBuilderRegistry.class);
        if (reg != null) {
            String ns = "http://cxf.apache.org/custom/security-policy";
            final Map<QName, Assertion> assertions = new HashMap<>();
            QName qName = new QName(ns, "Basic128GCM");
            assertions.put(qName, new PrimitiveAssertion(qName));
            qName = new QName(ns, "Basic192GCM");
            assertions.put(qName, new PrimitiveAssertion(qName));
            qName = new QName(ns, "Basic256GCM");
            assertions.put(qName, new PrimitiveAssertion(qName));

            reg.registerBuilder(new PrimitiveAssertionBuilder(assertions.keySet()) {
                public Assertion build(Element element, AssertionBuilderFactory fact) {
                    if (XMLPrimitiveAssertionBuilder.isOptional(element)
                        || XMLPrimitiveAssertionBuilder.isIgnorable(element)) {
                        return super.build(element, fact);
                    }
                    QName q = new QName(element.getNamespaceURI(), element.getLocalName());
                    return assertions.get(q);
                }
            });
        }
        return new GCMAlgorithmSuite(version, nestedPolicy);
    }

    public static class GCMAlgorithmSuite extends AlgorithmSuite {

        static {
            ALGORITHM_SUITE_TYPES.put(
                "Basic128GCM",
                new AlgorithmSuiteType(
                    "Basic128GCM",
                    SPConstants.SHA1,
                    "http://www.w3.org/2009/xmlenc11#aes128-gcm",
                    SPConstants.KW_AES128,
                    SPConstants.KW_RSA_OAEP,
                    SPConstants.P_SHA1_L128,
                    SPConstants.P_SHA1_L128,
                    128, 128, 128, 256, 1024, 4096
                )
            );

            ALGORITHM_SUITE_TYPES.put(
                "Basic192GCM",
                new AlgorithmSuiteType(
                    "Basic192GCM",
                    SPConstants.SHA1,
                    "http://www.w3.org/2009/xmlenc11#aes192-gcm",
                    SPConstants.KW_AES192,
                    SPConstants.KW_RSA_OAEP,
                    SPConstants.P_SHA1_L192,
                    SPConstants.P_SHA1_L192,
                    192, 192, 192, 256, 1024, 4096
                )
            );

            ALGORITHM_SUITE_TYPES.put(
                "Basic256GCM",
                new AlgorithmSuiteType(
                    "Basic256GCM",
                    SPConstants.SHA1,
                    "http://www.w3.org/2009/xmlenc11#aes256-gcm",
                    SPConstants.KW_AES256,
                    SPConstants.KW_RSA_OAEP,
                    SPConstants.P_SHA1_L256,
                    SPConstants.P_SHA1_L192,
                    256, 192, 256, 256, 1024, 4096
                )
            );
        }

        GCMAlgorithmSuite(SPConstants.SPVersion version, Policy nestedPolicy) {
            super(version, nestedPolicy);
        }

        @Override
        protected AbstractSecurityAssertion cloneAssertion(Policy nestedPolicy) {
            return new GCMAlgorithmSuite(getVersion(), nestedPolicy);
        }

        @Override
        protected void parseCustomAssertion(Assertion assertion) {
            String assertionName = assertion.getName().getLocalPart();
            String assertionNamespace = assertion.getName().getNamespaceURI();
            if (!"http://cxf.apache.org/custom/security-policy".equals(assertionNamespace)) {
                return;
            }

            if ("Basic128GCM".equals(assertionName)) {
                setAlgorithmSuiteType(ALGORITHM_SUITE_TYPES.get("Basic128GCM"));
                getAlgorithmSuiteType().setNamespace(assertionNamespace);
            } else if ("Basic192GCM".equals(assertionName)) {
                setAlgorithmSuiteType(ALGORITHM_SUITE_TYPES.get("Basic192GCM"));
                getAlgorithmSuiteType().setNamespace(assertionNamespace);
            } else if ("Basic256GCM".equals(assertionName)) {
                setAlgorithmSuiteType(ALGORITHM_SUITE_TYPES.get("Basic256GCM"));
                getAlgorithmSuiteType().setNamespace(assertionNamespace);
            }
        }
    }


}
