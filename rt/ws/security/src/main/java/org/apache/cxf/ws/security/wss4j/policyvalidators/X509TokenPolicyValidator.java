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
import java.util.List;

import org.w3c.dom.Element;

import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
<<<<<<< HEAD
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.X509Token;
import org.apache.cxf.ws.security.wss4j.WSS4JUtils;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.message.token.BinarySecurity;
=======
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.apache.wss4j.dom.bsp.BSPEnforcer;
import org.apache.wss4j.dom.message.token.BinarySecurity;
import org.apache.wss4j.dom.message.token.X509Security;
import org.apache.wss4j.dom.str.STRParser;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.X509Token;
import org.apache.wss4j.policy.model.X509Token.TokenType;
>>>>>>> 6d6ce13... Adding a Action -> Policy test for a KeyIdentifier fix

/**
 * Validate an X509 Token policy.
 */
public class X509TokenPolicyValidator extends AbstractTokenPolicyValidator implements TokenPolicyValidator {
    
    private static final String X509_V3_VALUETYPE = WSConstants.X509TOKEN_NS + "#X509v3";
    private static final String PKI_VALUETYPE = WSConstants.X509TOKEN_NS + "#X509PKIPathv1";
    
    public boolean validatePolicy(
        AssertionInfoMap aim,
        Message message,
        Element soapBody,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults
    ) {
<<<<<<< HEAD
        Collection<AssertionInfo> ais = aim.get(SP12Constants.X509_TOKEN);
        if (ais == null || ais.isEmpty()) {
            return true;
        }
        
=======
        Collection<AssertionInfo> ais = getAllAssertionsByLocalname(aim, SPConstants.X509_TOKEN);
        if (!ais.isEmpty()) {
            parsePolicies(ais, message, signedResults, results);
            
            assertPolicy(aim, SPConstants.WSS_X509_PKI_PATH_V1_TOKEN10);
            assertPolicy(aim, SPConstants.WSS_X509_PKI_PATH_V1_TOKEN11);
            assertPolicy(aim, SPConstants.WSS_X509_V1_TOKEN10);
            assertPolicy(aim, SPConstants.WSS_X509_V1_TOKEN11);
            assertPolicy(aim, SPConstants.WSS_X509_V3_TOKEN10);
            assertPolicy(aim, SPConstants.WSS_X509_V3_TOKEN11);
            
            assertPolicy(aim, SPConstants.REQUIRE_ISSUER_SERIAL_REFERENCE);
            assertPolicy(aim, SPConstants.REQUIRE_THUMBPRINT_REFERENCE);
            assertPolicy(aim, SPConstants.REQUIRE_KEY_IDENTIFIER_REFERENCE);
            assertPolicy(aim, SPConstants.REQUIRE_EMBEDDED_TOKEN_REFERENCE);
        }
        
        return true;
    }
    
    private void parsePolicies(
        Collection<AssertionInfo> ais, 
        Message message,
        List<WSSecurityEngineResult> signedResults,
        List<WSSecurityEngineResult> results
    ) {
>>>>>>> 6d6ce13... Adding a Action -> Policy test for a KeyIdentifier fix
        List<WSSecurityEngineResult> bstResults = 
            WSS4JUtils.fetchAllActionResults(results, WSConstants.BST);
        
        for (AssertionInfo ai : ais) {
            X509Token x509TokenPolicy = (X509Token)ai.getAssertion();
            ai.setAsserted(true);

            if (!isTokenRequired(x509TokenPolicy, message)) {
                continue;
            }

            if (bstResults.isEmpty() && signedResults.isEmpty()) {
                ai.setNotAsserted(
                    "The received token does not match the token inclusion requirement"
                );
                continue;
            }

<<<<<<< HEAD
            if (!checkTokenType(x509TokenPolicy.getTokenVersionAndType(), bstResults)) {
=======
            if (!checkTokenType(x509TokenPolicy.getTokenType(), bstResults, signedResults)) {
>>>>>>> 6d6ce13... Adding a Action -> Policy test for a KeyIdentifier fix
                ai.setNotAsserted("An incorrect X.509 Token Type is detected");
                continue;
            }
        }
        return true;
    }
    
    /**
     * Check that at least one received token matches the token type.
     */
    private boolean checkTokenType(
<<<<<<< HEAD
        String requiredVersionAndType,
        List<WSSecurityEngineResult> bstResults
=======
        TokenType tokenType,
        List<WSSecurityEngineResult> bstResults,
        List<WSSecurityEngineResult> signedResults
>>>>>>> 6d6ce13... Adding a Action -> Policy test for a KeyIdentifier fix
    ) {
        if (bstResults.isEmpty() && signedResults.isEmpty()) {
            return false;
        }

        String requiredType = X509_V3_VALUETYPE;
        if (SPConstants.WSS_X509_PKI_PATH_V1_TOKEN10.equals(requiredVersionAndType)
            || SPConstants.WSS_X509_PKI_PATH_V1_TOKEN11.equals(requiredVersionAndType)) {
            requiredType = PKI_VALUETYPE;
        }

        for (WSSecurityEngineResult result : bstResults) {
            BinarySecurity binarySecurityToken = 
                (BinarySecurity)result.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
            if (binarySecurityToken != null) {
                String type = binarySecurityToken.getValueType();
                if (requiredType.equals(type)) {
                    return true;
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
                WSSecurityUtil.getDirectChildElement(
                    signatureElement, "KeyInfo", WSConstants.SIG_NS
                );
            if (keyInfoElement != null) {
                Element strElement = 
                    WSSecurityUtil.getDirectChildElement(
                        keyInfoElement, "SecurityTokenReference", WSConstants.WSSE_NS
                    );
                if (strElement != null) {
                    Element kiElement = 
                        WSSecurityUtil.getDirectChildElement(
                            strElement, "KeyIdentifier", WSConstants.WSSE_NS
                        );
                    return kiElement;
                }
            }
        }
        return null;
    }
}
