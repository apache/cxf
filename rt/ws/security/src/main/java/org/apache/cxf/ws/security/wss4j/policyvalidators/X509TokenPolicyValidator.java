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

import org.w3c.dom.Element;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.X509Token;
import org.apache.cxf.ws.security.wss4j.WSS4JUtils;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Merlin;
import org.apache.ws.security.message.token.BinarySecurity;
import org.apache.ws.security.message.token.X509Security;
import org.apache.ws.security.str.STRParser;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * Validate an X509 Token policy.
 */
public class X509TokenPolicyValidator extends AbstractTokenPolicyValidator implements TokenPolicyValidator {
    
    private static final String X509_V3_VALUETYPE = WSConstants.X509TOKEN_NS + "#X509v3";
    private static final String PKI_VALUETYPE = WSConstants.X509TOKEN_NS + "#X509PKIPathv1";
    private static final Logger LOG = LogUtils.getL7dLogger(X509TokenPolicyValidator.class);
    
    public boolean validatePolicy(
        AssertionInfoMap aim,
        Message message,
        Element soapBody,
        List<WSSecurityEngineResult> results,
        List<WSSecurityEngineResult> signedResults
    ) {
        Collection<AssertionInfo> ais = aim.get(SP12Constants.X509_TOKEN);
        if (ais == null || ais.isEmpty()) {
            return true;
        }

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

            if (!checkTokenType(x509TokenPolicy.getTokenVersionAndType(), bstResults, signedResults)) {
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
        String requiredVersionAndType,
        List<WSSecurityEngineResult> bstResults,
        List<WSSecurityEngineResult> signedResults
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
                            X509Security token = new X509Security(keyIdentifier, false);
                            X509Certificate cert = token.getX509Certificate(new Merlin());
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
