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
import org.apache.cxf.sts.request.BinarySecret;
import org.apache.cxf.sts.request.Entropy;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.derivedKey.P_SHA1;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;

/**
 * Some common functionality relating to parsing and generating Symmetric Keys.
 */
public class SymmetricKeyHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(SymmetricKeyHandler.class);

    private int keySize;
    private Entropy clientEntropy;
    private byte[] entropyBytes;
    private byte[] secret;
    private boolean computedKey;

    public SymmetricKeyHandler(TokenProviderParameters tokenParameters) {
        KeyRequirements keyRequirements = tokenParameters.getKeyRequirements();

        keySize = (int)keyRequirements.getKeySize();
        STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
        SignatureProperties signatureProperties = stsProperties.getSignatureProperties();

        // Test EncryptWith
        String encryptWith = keyRequirements.getEncryptWith();
        if (encryptWith != null) {
            if ((WSS4JConstants.AES_128.equals(encryptWith) || WSS4JConstants.AES_128_GCM.equals(encryptWith))
                && keySize < 128) {
                keySize = 128;
            } else if ((WSS4JConstants.AES_192.equals(encryptWith)
                || WSS4JConstants.AES_192_GCM.equals(encryptWith))
                && keySize < 192) {
                keySize = 192;
            } else if ((WSS4JConstants.AES_256.equals(encryptWith)
                || WSS4JConstants.AES_256_GCM.equals(encryptWith))
                && keySize < 256) {
                keySize = 256;
            } else if (WSS4JConstants.TRIPLE_DES.equals(encryptWith) && keySize < 192) {
                keySize = 192;
            }
        }

        // Test KeySize
        if (keySize < signatureProperties.getMinimumKeySize()
            || keySize > signatureProperties.getMaximumKeySize()) {
            keySize = (int)signatureProperties.getKeySize();
            LOG.log(
                Level.WARNING, "Received KeySize of " + keyRequirements.getKeySize()
                + " not accepted so defaulting to " + signatureProperties.getKeySize()
            );
        }

        // Test Entropy
        clientEntropy = keyRequirements.getEntropy();
        if (clientEntropy == null) {
            LOG.log(Level.WARNING, "A SymmetricKey KeyType is requested, but no client entropy is provided");
        } else if (clientEntropy.getBinarySecret() != null) {
            BinarySecret binarySecret = clientEntropy.getBinarySecret();
            if (STSConstants.NONCE_TYPE.equals(binarySecret.getBinarySecretType())) {
                byte[] nonce = binarySecret.getBinarySecretValue();
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
            } else if (STSConstants.SYMMETRIC_KEY_TYPE.equals(binarySecret.getBinarySecretType())
                || binarySecret.getBinarySecretType() == null) {
                byte[] secretValue = binarySecret.getBinarySecretValue();
                if ((secretValue.length * 8L) < signatureProperties.getMinimumKeySize()
                    || (secretValue.length * 8L) > signatureProperties.getMaximumKeySize()) {
                    LOG.log(
                        Level.WARNING, "Received secret of length " + secretValue.length
                        + " bits is not accepted"
                    );
                    LOG.log(Level.WARNING, "User Entropy rejected");
                    clientEntropy = null;
                }
            } else {
                LOG.log(
                    Level.WARNING, "The type " + binarySecret.getBinarySecretType() + " is not supported"
                );
                throw new STSException(
                    "No user supplied entropy for SymmetricKey case", STSException.INVALID_REQUEST
                );
            }
        } else if (clientEntropy.getDecryptedKey() != null) {
            byte[] secretValue = clientEntropy.getDecryptedKey();
            if ((secretValue.length * 8L) < signatureProperties.getMinimumKeySize()
                || (secretValue.length * 8L) > signatureProperties.getMaximumKeySize()) {
                LOG.log(
                    Level.WARNING, "Received secret of length " + secretValue.length
                    + " bits is not accepted"
                );
                LOG.log(Level.WARNING, "User Entropy rejected");
                clientEntropy = null;
            }
        } else {
            LOG.log(Level.WARNING, "The user supplied Entropy structure is invalid");
            throw new STSException(
                "The user supplied Entropy structure is invalid", STSException.INVALID_REQUEST
            );
        }
    }

    /**
     * Create the Symmetric Key
     */
    public void createSymmetricKey() {
        computedKey = false;
        boolean generateEntropy = true;

        if (clientEntropy != null) {
            BinarySecret binarySecret = clientEntropy.getBinarySecret();
            if (binarySecret != null
                && (STSConstants.SYMMETRIC_KEY_TYPE.equals(binarySecret.getBinarySecretType())
                    || binarySecret.getBinarySecretType() == null)) {
                secret = binarySecret.getBinarySecretValue();
                generateEntropy = false;
            } else if (clientEntropy.getDecryptedKey() != null) {
                secret = clientEntropy.getDecryptedKey();
                generateEntropy = false;
            }
        }

        if (generateEntropy) {
            try {
                entropyBytes = XMLSecurityConstants.generateBytes(keySize / 8);
                secret = entropyBytes;
            } catch (XMLSecurityException ex) {
                LOG.log(Level.WARNING, "", ex);
                throw new STSException("Error in creating symmetric key", ex, STSException.INVALID_REQUEST);
            }
            if (clientEntropy != null && clientEntropy.getBinarySecret() != null) {
                byte[] nonce = clientEntropy.getBinarySecret().getBinarySecretValue();
                try {
                    P_SHA1 psha1 = new P_SHA1();
                    secret = psha1.createKey(nonce, entropyBytes, 0, keySize / 8);
                    computedKey = true;
                } catch (WSSecurityException ex) {
                    LOG.log(Level.WARNING, "", ex);
                    throw new STSException("Error in creating symmetric key", STSException.INVALID_REQUEST);
                }
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
