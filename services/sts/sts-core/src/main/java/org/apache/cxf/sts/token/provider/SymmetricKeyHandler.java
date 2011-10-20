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

package org.apache.cxf.sts.token.provider;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.SignatureProperties;
import org.apache.cxf.sts.request.Entropy;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.ws.security.sts.provider.STSException;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.conversation.ConversationException;
import org.apache.ws.security.conversation.dkalgo.P_SHA1;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * Some common functionality relating to parsing and generating Symmetric Keys.
 */
public class SymmetricKeyHandler {
    
    private static final Logger LOG = LogUtils.getL7dLogger(SymmetricKeyHandler.class);
    
    private int keySize = 256;
    private Entropy clientEntropy;
    private byte[] entropyBytes;
    private byte[] secret;
    private boolean computedKey;
    
    public SymmetricKeyHandler(TokenProviderParameters tokenParameters) {
        KeyRequirements keyRequirements = tokenParameters.getKeyRequirements();
        
        // Test KeySize
        keySize = Long.valueOf(keyRequirements.getKeySize()).intValue();
        STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
        SignatureProperties signatureProperties = stsProperties.getSignatureProperties();
        if (keySize < signatureProperties.getMinimumKeySize()
            || keySize > signatureProperties.getMaximumKeySize()) {
            keySize = Long.valueOf(signatureProperties.getKeySize()).intValue();
            LOG.log(
                Level.WARNING, "Received KeySize of " + keyRequirements.getKeySize() 
                + " not accepted so defaulting to " + signatureProperties.getKeySize()
            );
        }

        // Test Entropy
        clientEntropy = keyRequirements.getEntropy();
        if (clientEntropy == null) {
            LOG.log(Level.WARNING, "A SymmetricKey KeyType is requested, but no client entropy is provided");
        } else {
            String binarySecurityType = clientEntropy.getBinarySecretType();
            byte[] nonce = clientEntropy.getBinarySecretValue();
            if (!STSConstants.NONCE_TYPE.equals(binarySecurityType)) {
                LOG.log(Level.WARNING, "The type " + binarySecurityType + " is not supported");
                throw new STSException(
                    "No user supplied entropy for SymmetricKey case", STSException.INVALID_REQUEST
                );
            }
            if (nonce == null || (nonce.length < (keySize / 8))) {
                LOG.log(Level.WARNING, "User Entropy rejected");
                clientEntropy = null;
            }
            String computedKeyAlgorithm = keyRequirements.getComputedKeyAlgorithm();
            if (!STSConstants.COMPUTED_KEY_PSHA1.equals(computedKeyAlgorithm)) {
                LOG.log(
                    Level.WARNING, 
                    "The computed key algorithm of " + computedKeyAlgorithm + " is not supported"
                );
                throw new STSException(
                    "Computed Key Algorithm not supported", STSException.INVALID_REQUEST
                );
            }
        }
    }

    /**
     * Create the Symmetric Key
     */
    public void createSymmetricKey() {
        try {
            entropyBytes = WSSecurityUtil.generateNonce(keySize / 8);
            secret = entropyBytes;
            computedKey = false;
        } catch (WSSecurityException ex) {
            LOG.log(Level.WARNING, "", ex);
            throw new STSException("Error in creating symmetric key", ex, STSException.INVALID_REQUEST);
        } 
        if (clientEntropy != null) {
            byte[] nonce = clientEntropy.getBinarySecretValue();
            try {
                P_SHA1 psha1 = new P_SHA1();
                secret = psha1.createKey(nonce, entropyBytes, 0, keySize / 8);
                computedKey = true;
            } catch (ConversationException ex) {
                LOG.log(Level.WARNING, "", ex);
                throw new STSException("Error in creating symmetric key", STSException.INVALID_REQUEST);
            }
        }
    }
    
    /**
     * Get the KeySize.
     */
    public long getKeySize() {
        return keySize;
    }
    
    /**
     * Get the Entropy bytes
     */
    public byte[] getEntropyBytes() {
        return entropyBytes;
    }

    /**
     * Get the secret
     */
    public byte[] getSecret() {
        return secret;
    }
    
    /**
     * Get whether this is a computed key or not
     */
    public boolean isComputedKey() {
        return computedKey;
    }
    
}
