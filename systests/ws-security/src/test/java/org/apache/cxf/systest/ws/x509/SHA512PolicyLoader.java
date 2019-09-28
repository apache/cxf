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
package org.apache.cxf.systest.ws.x509;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.ws.policy.AssertionBuilderRegistry;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertionBuilder;
import org.apache.cxf.ws.security.policy.custom.AlgorithmSuiteLoader;
import org.apache.neethi.Assertion;
import org.apache.neethi.AssertionBuilderFactory;
import org.apache.neethi.Policy;
import org.apache.neethi.builders.xml.XMLPrimitiveAssertionBuilder;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractSecurityAssertion;
import org.apache.wss4j.policy.model.AlgorithmSuite;

/**
 * This class retrieves the default AlgorithmSuites plus a custom AlgorithmSuite with the RSA SHA-512
 * signature
 */
public class SHA512PolicyLoader implements AlgorithmSuiteLoader {

    public SHA512PolicyLoader(Bus bus) {
        bus.setExtension(this, AlgorithmSuiteLoader.class);
    }

    public AlgorithmSuite getAlgorithmSuite(Bus bus, SPConstants.SPVersion version, Policy nestedPolicy) {
        AssertionBuilderRegistry reg = bus.getExtension(AssertionBuilderRegistry.class);
        if (reg != null) {
            String ns = "http://cxf.apache.org/custom/security-policy";
            final Map<QName, Assertion> assertions = new HashMap<>();
            QName qName = new QName(ns, "Basic128RsaSha512");
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
        return new SHA512AlgorithmSuite(version, nestedPolicy);
    }

    public static class SHA512AlgorithmSuite extends AlgorithmSuite {

        static {
            ALGORITHM_SUITE_TYPES.put(
                "Basic128RsaSha512",
                new AlgorithmSuiteType(
                    "Basic128RsaSha512",
                    "http://www.w3.org/2001/04/xmlenc#sha512",
                    WSS4JConstants.AES_128,
                    SPConstants.KW_AES128,
                    SPConstants.KW_RSA_OAEP,
                    SPConstants.P_SHA1_L128,
                    SPConstants.P_SHA1_L128,
                    128, 128, 128, 512, 1024, 4096
                )
            );
        }

        SHA512AlgorithmSuite(SPConstants.SPVersion version, Policy nestedPolicy) {
            super(version, nestedPolicy);
            getAlgorithmSuiteType().setAsymmetricSignature("http://www.w3.org/2001/04/xmldsig-more#rsa-sha512");
        }

        @Override
        protected AbstractSecurityAssertion cloneAssertion(Policy nestedPolicy) {
            return new SHA512AlgorithmSuite(getVersion(), nestedPolicy);
        }

        @Override
        protected void parseCustomAssertion(Assertion assertion) {
            String assertionName = assertion.getName().getLocalPart();
            String assertionNamespace = assertion.getName().getNamespaceURI();
            if (!"http://cxf.apache.org/custom/security-policy".equals(assertionNamespace)) {
                return;
            }

            if ("Basic128RsaSha512".equals(assertionName)) {
                setAlgorithmSuiteType(ALGORITHM_SUITE_TYPES.get("Basic128RsaSha512"));
                getAlgorithmSuiteType().setNamespace(assertionNamespace);
            }
        }
    }


}
