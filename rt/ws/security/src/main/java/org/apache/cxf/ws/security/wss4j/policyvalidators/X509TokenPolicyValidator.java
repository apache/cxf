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

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.bsp.BSPEnforcer;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.common.token.X509Security;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.str.STRParser;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.X509Token;
import org.apache.wss4j.policy.model.X509Token.TokenType;

/**
 * Validate an X509 Token policy.
 */
public class X509TokenPolicyValidator extends AbstractSecurityPolicyValidator {

    private static final Logger LOG = LogUtils.getL7dLogger(X509TokenPolicyValidator.class);

    private static final String X509_V3_VALUETYPE = WSConstants.X509TOKEN_NS + "#X509v3";
    private static final String PKI_VALUETYPE = WSConstants.X509TOKEN_NS + "#X509PKIPathv1";

    /**
     * Return true if this SecurityPolicyValidator implementation is capable of validating a
     * policy defined by the AssertionInfo parameter
     */
    public boolean canValidatePolicy(AssertionInfo assertionInfo) {
        return assertionInfo.getAssertion() != null
            && (SP12Constants.X509_TOKEN.equals(assertionInfo.getAssertion().getName())
                || SP11Constants.X509_TOKEN.equals(assertionInfo.getAssertion().getName()));
    }

    /**
     * Validate policies.
     */
    public void validatePolicies(PolicyValidatorParameters parameters, Collection<AssertionInfo> ais) {
        List<WSSecurityEngineResult> bstResults =
            parameters.getResults().getActionResults().get(WSConstants.BST);

        for (AssertionInfo ai : ais) {
            X509Token x509TokenPolicy = (X509Token)ai.getAssertion();
            ai.setAsserted(true);
            assertToken(x509TokenPolicy, parameters.getAssertionInfoMap());

            if (!isTokenRequired(x509TokenPolicy, parameters.getMessage())) {
                continue;
            }

            if ((bstResults == null || bstResults.isEmpty()) && parameters.getSignedResults().isEmpty()) {
                ai.setNotAsserted(
                    "The received token does not match the token inclusion requirement"
                );
                continue;
            }

            if (!checkTokenType(x509TokenPolicy.getTokenType(), bstResults, parameters.getSignedResults())) {
                ai.setNotAsserted("An incorrect X.509 Token Type is detected");
                continue;
            }
        }
    }

    private void assertToken(X509Token token, AssertionInfoMap aim) {
        String namespace = token.getName().getNamespaceURI();

        // Assert references
        if (token.isRequireIssuerSerialReference()) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.REQUIRE_ISSUER_SERIAL_REFERENCE));
        }
        if (token.isRequireThumbprintReference()) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.REQUIRE_THUMBPRINT_REFERENCE));
        }
        if (token.isRequireEmbeddedTokenReference()) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.REQUIRE_EMBEDDED_TOKEN_REFERENCE));
        }
        if (token.isRequireKeyIdentifierReference()) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.REQUIRE_KEY_IDENTIFIER_REFERENCE));
        }

        // Assert TokenType
        TokenType tokenType = token.getTokenType();
        if (tokenType != null) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, tokenType.name()));
        }
    }

    /**
     * Check that at least one received token matches the token type.
     */
    private boolean checkTokenType(
        TokenType tokenType,
        List<WSSecurityEngineResult> bstResults,
        List<WSSecurityEngineResult> signedResults
    ) {
        if ((bstResults == null || bstResults.isEmpty()) && signedResults.isEmpty()) {
            return false;
        }

        String requiredType = X509_V3_VALUETYPE;
        boolean v3certRequired = false;
        if (tokenType == TokenType.WssX509PkiPathV1Token10
            || tokenType == TokenType.WssX509PkiPathV1Token11) {
            requiredType = PKI_VALUETYPE;
        } else if (tokenType == TokenType.WssX509V3Token10
            || tokenType == TokenType.WssX509V3Token11) {
            v3certRequired = true;
        }

        if (bstResults != null) {
            for (WSSecurityEngineResult result : bstResults) {
                BinarySecurity binarySecurityToken =
                    (BinarySecurity)result.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                if (binarySecurityToken != null && requiredType.equals(binarySecurityToken.getValueType())) {
                    if (v3certRequired && binarySecurityToken instanceof X509Security) {
                        try {
                            X509Certificate cert =
                                ((X509Security)binarySecurityToken).getX509Certificate(null);
                            if (cert != null && cert.getVersion() == 3) {
                                return true;
                            }
                        } catch (WSSecurityException e) {
                            LOG.log(Level.FINE, e.getMessage());
                        }
                    } else {
                        return true;
                    }
                }
            }
        }

        // Maybe the X.509 token was included as a KeyIdentifier
        if (X509_V3_VALUETYPE.equals(requiredType)) {
            for (WSSecurityEngineResult result : signedResults) {
                STRParser.REFERENCE_TYPE referenceType =
                    (STRParser.REFERENCE_TYPE)result.get(WSSecurityEngineResult.TAG_X509_REFERENCE_TYPE);
                if (STRParser.REFERENCE_TYPE.KEY_IDENTIFIER == referenceType) {
                    Element signatureElement =
                        (Element)result.get(WSSecurityEngineResult.TAG_TOKEN_ELEMENT);
                    Element keyIdentifier = getKeyIdentifier(signatureElement);
                    if (keyIdentifier != null
                        && X509_V3_VALUETYPE.equals(keyIdentifier.getAttributeNS(null, "ValueType"))) {
                        try {
                            X509Security token =
                                new X509Security(keyIdentifier,
                                                 new BSPEnforcer(true));
                            X509Certificate cert = token.getX509Certificate(null);
                            if (cert != null && cert.getVersion() == 3) {
                                return true;
                            }
                        } catch (WSSecurityException e) {
                            LOG.log(Level.FINE, e.getMessage());
                        }
                    }
                }
            }
        }
        return false;
    }

    private Element getKeyIdentifier(Element signatureElement) {
        if (signatureElement != null) {
            Element keyInfoElement =
                XMLUtils.getDirectChildElement(
                    signatureElement, "KeyInfo", WSS4JConstants.SIG_NS
                );
            if (keyInfoElement != null) {
                Element strElement =
                    XMLUtils.getDirectChildElement(
                        keyInfoElement, "SecurityTokenReference", WSS4JConstants.WSSE_NS
                    );
                if (strElement != null) {
                    return XMLUtils.getDirectChildElement(
                            strElement, "KeyIdentifier", WSS4JConstants.WSSE_NS
                        );
                }
            }
        }
        return null;
    }
}
