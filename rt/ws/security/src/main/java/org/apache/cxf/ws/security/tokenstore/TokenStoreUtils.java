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
package org.apache.cxf.ws.security.tokenstore;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.w3c.dom.Element;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.token.SecurityContextToken;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.xml.security.Init;
import org.apache.xml.security.c14n.Canonicalizer;

/**
 * Some common functionality
 */
public final class TokenStoreUtils {

    private TokenStoreUtils() {
        // complete
    }

    public static TokenStore getTokenStore(Message message) throws TokenStoreException {
        EndpointInfo info = message.getExchange().getEndpoint().getEndpointInfo();
        synchronized (info) {
            TokenStore tokenStore =
                (TokenStore)message.getContextualProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            if (tokenStore == null) {
                tokenStore = (TokenStore)info.getProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            }
            if (tokenStore == null) {
                TokenStoreFactory tokenStoreFactory = TokenStoreFactory.newInstance();
                StringBuilder cacheKey = new StringBuilder(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
                String cacheIdentifier =
                    (String)message.getContextualProperty(SecurityConstants.CACHE_IDENTIFIER);
                if (cacheIdentifier != null) {
                    cacheKey.append('-').append(cacheIdentifier);
                }
                if (info.getName() != null) {
                    int hashcode = info.getName().toString().hashCode();
                    if (hashcode >= 0) {
                        cacheKey.append('-');
                    }
                    cacheKey.append(hashcode);
                }

                tokenStore = tokenStoreFactory.newTokenStore(cacheKey.toString(), message);
                info.setProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, tokenStore);
            }
            return tokenStore;
        }
    }

    /**
     * Get a cache key for a signed SAML Assertion, that can be used to store and retrieve the (validated)
     * Assertion in/from a TokenStore. The key is a SHA-256 digest over the canonicalized SignedInfo
     * (which binds the signed content of the Assertion) and the SignatureValue. It returns null if the
     * Assertion is not signed, or if the signature does not conform to the SAML signature profile (in which
     * case the signature might not cover the Assertion itself, and so the Assertion must not be cached).
     *
     * This method must be used to look up a received Assertion in a TokenStore.
     */
    public static String getCacheKey(SamlAssertionWrapper assertion) throws WSSecurityException {
        return getCacheKey(assertion, true);
    }

    /**
     * Get a cache key for a signed SAML Assertion - see getCacheKey(SamlAssertionWrapper).
     * @param validateSignatureProfile whether to check that the signature conforms to the SAML signature
     *        profile first, returning null if it doesn't. This must be true when looking up a received
     *        Assertion. It can be false when storing an Assertion that was just signed by the STS, as
     *        its DOM Element might not be attached to a Document, which the profile validation requires.
     */
    public static String getCacheKey(SamlAssertionWrapper assertion, boolean validateSignatureProfile)
        throws WSSecurityException {
        byte[] signatureValue = assertion.getSignatureValue();
        if (signatureValue == null || signatureValue.length == 0) {
            return null;
        }

        if (validateSignatureProfile) {
            try {
                assertion.validateSignatureAgainstProfile();
            } catch (WSSecurityException ex) {
                return null;
            }
        }

        Element signedInfo = null;
        Element assertionElement = assertion.getElement();
        if (assertionElement == null && assertion.getSamlObject() != null) {
            assertionElement = assertion.getSamlObject().getDOM();
        }
        if (assertionElement != null) {
            Element signature =
                DOMUtils.getFirstChildWithName(assertionElement, WSConstants.SIG_NS, WSConstants.SIG_LN);
            if (signature != null) {
                signedInfo = DOMUtils.getFirstChildWithName(signature, WSConstants.SIG_NS, "SignedInfo");
            }
        }
        if (signedInfo == null) {
            return null;
        }

        try {
            if (!Init.isInitialized()) {
                Init.init();
            }
            ByteArrayOutputStream signedInfoBytes = new ByteArrayOutputStream();
            Canonicalizer.getInstance(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS)
                .canonicalizeSubtree(signedInfo, signedInfoBytes);

            return computeCacheKey("SAML", signedInfoBytes.toByteArray(), signatureValue);
        } catch (Exception ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
        }
    }

    /**
     * Get a cache key for a UsernameToken, that can be used to store and retrieve the (validated)
     * UsernameToken in/from a TokenStore. The key is a SHA-256 digest over the UsernameToken values.
     */
    public static String getCacheKey(UsernameToken usernameToken) throws WSSecurityException {
        byte[] salt = usernameToken.getSalt();
        return computeCacheKey("UsernameToken",
                               toBytes(usernameToken.getName()),
                               toBytes(usernameToken.getPassword()),
                               toBytes(usernameToken.getPasswordType()),
                               toBytes(usernameToken.getNonce()),
                               toBytes(usernameToken.getCreated()),
                               salt,
                               toBytes(Integer.toString(usernameToken.getIteration())));
    }

    /**
     * Get a cache key for a BinarySecurityToken, that can be used to store and retrieve the (validated)
     * BinarySecurityToken in/from a TokenStore. The key is a SHA-256 digest over the token values.
     */
    public static String getCacheKey(BinarySecurity binarySecurity) {
        return computeCacheKey("BinarySecurityToken",
                               toBytes(binarySecurity.getValueType()),
                               toBytes(binarySecurity.getEncodingType()),
                               binarySecurity.getToken());
    }

    /**
     * Get a cache key for a SecurityContextToken, that can be used to store and retrieve the (validated)
     * SecurityContextToken in/from a TokenStore. The key is a SHA-256 digest over the token identifier.
     */
    public static String getCacheKey(SecurityContextToken securityContextToken) {
        String identifier = securityContextToken.getIdentifier();
        if (identifier == null) {
            return null;
        }
        return computeCacheKey("SecurityContextToken", toBytes(identifier));
    }

    private static byte[] toBytes(String value) {
        return value == null ? null : value.getBytes(StandardCharsets.UTF_8);
    }

    private static String computeCacheKey(String tokenType, byte[]... values) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF(tokenType);
            for (byte[] value : values) {
                // Length-prefix each value so that different combinations can't produce the same input
                if (value == null) {
                    out.writeInt(-1);
                } else {
                    out.writeInt(value.length);
                    out.write(value);
                }
            }
            out.flush();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return StringUtils.toHexString(digest.digest(bytes.toByteArray()));
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
