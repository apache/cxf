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

import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSDerivedKeyTokenPrincipal;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.transform.STRTransform;

/**
 * Validate a WSSecurityEngineResult corresponding to the processing of a Signature, EncryptedKey or
 * EncryptedData structure against an AlgorithmSuite policy.
 */
public class AlgorithmSuitePolicyValidator {
    
    private List<WSSecurityEngineResult> results;

    public AlgorithmSuitePolicyValidator(
        List<WSSecurityEngineResult> results
    ) {
        this.results = results;
    }
    
    public boolean validatePolicy(
        AssertionInfo aiBinding, AlgorithmSuite algorithmPolicy
    ) {
        for (WSSecurityEngineResult result : results) {
            Integer actInt = (Integer)result.get(WSSecurityEngineResult.TAG_ACTION);
            if (WSConstants.SIGN == actInt 
                && !checkSignatureAlgorithms(result, algorithmPolicy, aiBinding)) {
                return false;
            } else if (WSConstants.ENCR == actInt 
                && !checkEncryptionAlgorithms(result, algorithmPolicy, aiBinding)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check the Signature Algorithms
     */
    private boolean checkSignatureAlgorithms(
        WSSecurityEngineResult result, 
        AlgorithmSuite algorithmPolicy,
        AssertionInfo ai
    ) {
        String signatureMethod = 
            (String)result.get(WSSecurityEngineResult.TAG_SIGNATURE_METHOD);
        if (!algorithmPolicy.getAsymmetricSignature().equals(signatureMethod)
            && !algorithmPolicy.getSymmetricSignature().equals(signatureMethod)) {
            ai.setNotAsserted(
                "The signature method does not match the requirement"
            );
            return false;
        }
        String c14nMethod = 
            (String)result.get(WSSecurityEngineResult.TAG_CANONICALIZATION_METHOD);
        if (!algorithmPolicy.getInclusiveC14n().equals(c14nMethod)) {
            ai.setNotAsserted(
                "The c14n method does not match the requirement"
            );
            return false;
        }

        List<WSDataRef> dataRefs = 
            CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
        if (!checkDataRefs(dataRefs, algorithmPolicy, ai)) {
            return false;
        }
        
        if (!checkKeyLengths(result, algorithmPolicy, ai, true)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check the individual signature references
     */
    private boolean checkDataRefs(
        List<WSDataRef> dataRefs,
        AlgorithmSuite algorithmPolicy,
        AssertionInfo ai
    ) {
        for (WSDataRef dataRef : dataRefs) {
            String digestMethod = dataRef.getDigestAlgorithm();
            if (!algorithmPolicy.getDigest().equals(digestMethod)) {
                ai.setNotAsserted(
                    "The digest method does not match the requirement"
                );
                return false;
            }
            
            List<String> transformAlgorithms = dataRef.getTransformAlgorithms();
            // Only a max of 2 transforms per reference is allowed
            if (transformAlgorithms == null || transformAlgorithms.size() > 2) {
                ai.setNotAsserted("The transform algorithms do not match the requirement");
                return false;
            }
            for (String transformAlgorithm : transformAlgorithms) {
                if (!(algorithmPolicy.getInclusiveC14n().equals(transformAlgorithm)
                    || STRTransform.TRANSFORM_URI.equals(transformAlgorithm))) {
                    ai.setNotAsserted("The transform algorithms do not match the requirement");
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Check the Encryption Algorithms
     */
    private boolean checkEncryptionAlgorithms(
        WSSecurityEngineResult result, 
        AlgorithmSuite algorithmPolicy,
        AssertionInfo ai
    ) {
        String transportMethod = 
            (String)result.get(WSSecurityEngineResult.TAG_ENCRYPTED_KEY_TRANSPORT_METHOD);
        if (transportMethod != null 
            && !algorithmPolicy.getSymmetricKeyWrap().equals(transportMethod)
            && !algorithmPolicy.getAsymmetricKeyWrap().equals(transportMethod)) {
            ai.setNotAsserted(
                "The Key transport method does not match the requirement"
            );
            return false;
        }
        
        List<WSDataRef> dataRefs = 
            CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
        if (dataRefs != null) {
            for (WSDataRef dataRef : dataRefs) {
                String encryptionAlgorithm = dataRef.getAlgorithm();
                if (!algorithmPolicy.getEncryption().equals(encryptionAlgorithm)) {
                    ai.setNotAsserted(
                        "The encryption algorithm does not match the requirement"
                    );
                    return false;
                }
            }
        }
        
        if (!checkKeyLengths(result, algorithmPolicy, ai, false)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check the key lengths of the secret and public keys.
     */
    private boolean checkKeyLengths(
        WSSecurityEngineResult result, 
        AlgorithmSuite algorithmPolicy,
        AssertionInfo ai,
        boolean signature
    ) {
        PublicKey publicKey = (PublicKey)result.get(WSSecurityEngineResult.TAG_PUBLIC_KEY);
        if (publicKey != null && !checkPublicKeyLength(publicKey, algorithmPolicy, ai)) {
            return false;
        }
        
        X509Certificate x509Cert = 
            (X509Certificate)result.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
        if (x509Cert != null && !checkPublicKeyLength(x509Cert.getPublicKey(), algorithmPolicy, ai)) {
            return false;
        }
        
        byte[] secret = (byte[])result.get(WSSecurityEngineResult.TAG_SECRET);
        if (signature) {
            Principal principal = (Principal)result.get(WSSecurityEngineResult.TAG_PRINCIPAL);
            if (principal instanceof WSDerivedKeyTokenPrincipal) {
                int requiredLength = algorithmPolicy.getSignatureDerivedKeyLength();
                if (secret == null || secret.length != (requiredLength / 8)) {
                    ai.setNotAsserted(
                        "The signature derived key length does not match the requirement"
                    );
                    return false;
                }
            } else if (secret != null 
                && (secret.length < (algorithmPolicy.getMinimumSymmetricKeyLength() / 8)
                    || secret.length > (algorithmPolicy.getMaximumSymmetricKeyLength() / 8))) {
                ai.setNotAsserted(
                    "The symmetric key length does not match the requirement"
                );
                return false;
            }
        } else if (secret != null 
            && (secret.length < (algorithmPolicy.getMinimumSymmetricKeyLength() / 8)
                || secret.length > (algorithmPolicy.getMaximumSymmetricKeyLength() / 8))) {
            ai.setNotAsserted(
                "The symmetric key length does not match the requirement"
            );
            return false;
        }
        
        return true;
    }
        
    /**
     * Check the public key lengths
     */
    private boolean checkPublicKeyLength(
        PublicKey publicKey, 
        AlgorithmSuite algorithmPolicy,
        AssertionInfo ai
    ) {
        if (publicKey instanceof RSAPublicKey) {
            int modulus = ((RSAPublicKey)publicKey).getModulus().bitLength();
            if (modulus < algorithmPolicy.getMinimumAsymmetricKeyLength()
                || modulus > algorithmPolicy.getMaximumAsymmetricKeyLength()) {
                ai.setNotAsserted(
                    "The asymmetric key length does not match the requirement"
                );
                return false;
            }
        } else if (publicKey instanceof DSAPublicKey) {
            int length = ((DSAPublicKey)publicKey).getParams().getP().bitLength();
            if (length < algorithmPolicy.getMinimumAsymmetricKeyLength()
                || length > algorithmPolicy.getMaximumAsymmetricKeyLength()) {
                ai.setNotAsserted(
                    "The asymmetric key length does not match the requirement"
                );
                return false;
            }
        } else {
            ai.setNotAsserted(
                "An unknown public key was provided"
            );
            return false;
        }
        
        return true;
    }
    
}
