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

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

import org.w3c.dom.Element;

import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.SignatureProperties;
import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.common.PasswordCallbackHandler;
import org.apache.cxf.sts.request.BinarySecret;
import org.apache.cxf.sts.request.Entropy;
import org.apache.cxf.sts.request.KeyRequirements;
import org.apache.cxf.sts.request.ReceivedCredential;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.service.EncryptionProperties;
import org.apache.cxf.test.TestUtilities;
import org.apache.cxf.ws.security.sts.provider.STSException;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.common.util.DOM2Writer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Some unit tests for creating SAML Tokens with various KeyType parameters via the SAMLTokenProvider.
 */
public class SAMLProviderKeyTypeTest {

    /**
     * Create a default Saml1 Bearer Assertion.
     */
    @org.junit.Test
    public void testDefaultSaml1BearerAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertFalse(tokenString.contains("AuthenticationStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML1Constants.CONF_BEARER));
        assertFalse(tokenString.contains(SAML1Constants.CONF_HOLDER_KEY));
    }

    /**
     * Create a default Saml2 Bearer Assertion.
     */
    @org.junit.Test
    public void testDefaultSaml2BearerAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertFalse(tokenString.contains("AuthenticationStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
        assertFalse(tokenString.contains(SAML2Constants.CONF_HOLDER_KEY));
    }


    /**
     * Create a default Saml1 PublicKey Assertion.
     */
    @org.junit.Test
    public void testDefaultSaml1PublicKeyAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.SAML_NS, STSConstants.PUBLIC_KEY_KEYTYPE);
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.SAML_NS));

        try {
            samlTokenProvider.createToken(providerParameters);
            fail("Failure expected on no certificate");
        } catch (STSException ex) {
            // expected as no certificate is provided
        }

        // Now get a certificate and set it on the key requirements of the provider parameter
        Crypto crypto = providerParameters.getStsProperties().getEncryptionCrypto();
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias("myclientkey");
        X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
        ReceivedCredential receivedCredential = new ReceivedCredential();
        receivedCredential.setX509Cert(certs[0]);
        providerParameters.getKeyRequirements().setReceivedCredential(receivedCredential);

        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertFalse(tokenString.contains("AuthenticationStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML1Constants.CONF_HOLDER_KEY));
        assertFalse(tokenString.contains(SAML1Constants.CONF_BEARER));
    }

    /**
     * Create a default Saml2 PublicKey Assertion.
     */
    @org.junit.Test
    public void testDefaultSaml2PublicKeyAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.SAML2_NS, STSConstants.PUBLIC_KEY_KEYTYPE);
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.SAML2_NS));

        try {
            samlTokenProvider.createToken(providerParameters);
            fail("Failure expected on no certificate");
        } catch (STSException ex) {
            // expected as no certificate is provided
        }

        // Now get a certificate and set it on the key requirements of the provider parameter
        Crypto crypto = providerParameters.getStsProperties().getEncryptionCrypto();
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias("myclientkey");
        X509Certificate[] certs = crypto.getX509Certificates(cryptoType);
        ReceivedCredential receivedCredential = new ReceivedCredential();
        receivedCredential.setX509Cert(certs[0]);
        providerParameters.getKeyRequirements().setReceivedCredential(receivedCredential);

        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertFalse(tokenString.contains("AuthenticationStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_HOLDER_KEY));
        assertFalse(tokenString.contains(SAML2Constants.CONF_BEARER));
    }

    /**
     * Create a default Saml1 SymmetricKey Assertion.
     */
    @org.junit.Test
    public void testDefaultSaml1SymmetricKeyAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML_TOKEN_TYPE, STSConstants.SYMMETRIC_KEY_KEYTYPE);
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE));

        Entropy entropy = new Entropy();
        BinarySecret binarySecret = new BinarySecret();
        Random random = new Random();
        byte[] secret = new byte[256 / 8];
        random.nextBytes(secret);
        binarySecret.setBinarySecretValue(secret);
        entropy.setBinarySecret(binarySecret);
        providerParameters.getKeyRequirements().setEntropy(entropy);

        binarySecret.setBinarySecretType("bad-type");
        try {
            samlTokenProvider.createToken(providerParameters);
            fail("Failure expected on a bad type");
        } catch (STSException ex) {
            // expected as no type is provided
        }

        binarySecret.setBinarySecretType(STSConstants.NONCE_TYPE);
        try {
            samlTokenProvider.createToken(providerParameters);
            fail("Failure expected on no computed key algorithm");
        } catch (STSException ex) {
            // expected as no computed key algorithm is provided
        }

        providerParameters.getKeyRequirements().setComputedKeyAlgorithm(STSConstants.COMPUTED_KEY_PSHA1);

        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertFalse(tokenString.contains("AuthenticationStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML1Constants.CONF_HOLDER_KEY));
        assertFalse(tokenString.contains(SAML1Constants.CONF_BEARER));

        // Test custom keySize
        SignatureProperties signatureProperties =
            providerParameters.getStsProperties().getSignatureProperties();
        signatureProperties.setMinimumKeySize(-8);
        providerParameters.getKeyRequirements().setKeySize(-8);
        try {
            samlTokenProvider.createToken(providerParameters);
            fail("Failure expected on a bad KeySize");
        } catch (STSException ex) {
            // expected on a bad KeySize
        }

        signatureProperties.setMinimumKeySize(128);
        providerParameters.getKeyRequirements().setKeySize(192);
        samlTokenProvider.createToken(providerParameters);
    }

    /**
     * Create a default Saml1 SymmetricKey Assertion. Rather than using a Nonce as the Entropy,
     * a secret key is supplied by the client instead.
     */
    @org.junit.Test
    public void testDefaultSaml1SymmetricKeyAssertionSecretKey() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML_TOKEN_TYPE, STSConstants.SYMMETRIC_KEY_KEYTYPE);
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE));

        Entropy entropy = new Entropy();
        BinarySecret binarySecret = new BinarySecret();
        Random random = new Random();
        byte[] secret = new byte[256 / 8];
        random.nextBytes(secret);
        binarySecret.setBinarySecretValue(secret);
        entropy.setBinarySecret(binarySecret);
        providerParameters.getKeyRequirements().setEntropy(entropy);

        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertFalse(tokenString.contains("AuthenticationStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML1Constants.CONF_HOLDER_KEY));
        assertFalse(tokenString.contains(SAML1Constants.CONF_BEARER));

        assertFalse(providerResponse.isComputedKey());
        assertNull(providerResponse.getEntropy());
    }


    /**
     * Create a default Saml2 SymmetricKey Assertion.
     */
    @org.junit.Test
    public void testDefaultSaml2SymmetricKeyAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.SYMMETRIC_KEY_KEYTYPE);
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));

        Entropy entropy = new Entropy();
        BinarySecret binarySecret = new BinarySecret();
        Random random = new Random();
        byte[] secret = new byte[256 / 8];
        random.nextBytes(secret);
        binarySecret.setBinarySecretValue(secret);
        entropy.setBinarySecret(binarySecret);
        providerParameters.getKeyRequirements().setEntropy(entropy);

        binarySecret.setBinarySecretType("bad-type");
        try {
            samlTokenProvider.createToken(providerParameters);
            fail("Failure expected on a bad type");
        } catch (STSException ex) {
            // expected as no type is provided
        }

        binarySecret.setBinarySecretType(STSConstants.NONCE_TYPE);
        try {
            samlTokenProvider.createToken(providerParameters);
            fail("Failure expected on no computed key algorithm");
        } catch (STSException ex) {
            // expected as no computed key algorithm is provided
        }

        providerParameters.getKeyRequirements().setComputedKeyAlgorithm(STSConstants.COMPUTED_KEY_PSHA1);

        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertFalse(tokenString.contains("AuthenticationStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_HOLDER_KEY));
        assertFalse(tokenString.contains(SAML2Constants.CONF_BEARER));

        // Test custom keySize
        SignatureProperties signatureProperties =
            providerParameters.getStsProperties().getSignatureProperties();
        signatureProperties.setMinimumKeySize(-8);
        providerParameters.getKeyRequirements().setKeySize(-8);
        try {
            samlTokenProvider.createToken(providerParameters);
            fail("Failure expected on a bad KeySize");
        } catch (STSException ex) {
            // expected on a bad KeySize
        }

        signatureProperties.setMinimumKeySize(128);
        providerParameters.getKeyRequirements().setKeySize(192);
        samlTokenProvider.createToken(providerParameters);
    }

    /**
     * Create a default Assertion with a bad keytype
     */
    @org.junit.Test
    public void testDefaultBadKeytypeAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML_TOKEN_TYPE, "bad-keytype");
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE));
        try {
            samlTokenProvider.createToken(providerParameters);
            fail("Failure expected on a bad KeyType");
        } catch (STSException ex) {
            // expected
        }

        samlTokenProvider = new SAMLTokenProvider();
        providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, "bad-keytype");
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));
        try {
            samlTokenProvider.createToken(providerParameters);
            fail("Failure expected on a bad KeyType");
        } catch (STSException ex) {
            // expected
        }
    }

    /**
     * Create a default Saml1 Bearer Assertion that uses a KeyValue to sign the Assertion.
     */
    @org.junit.Test
    public void testDefaultSaml1BearerKeyValueAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);

        providerParameters.getStsProperties().getSignatureProperties().setUseKeyValue(true);
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertFalse(tokenString.contains("AuthenticationStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML1Constants.CONF_BEARER));
        assertFalse(tokenString.contains(SAML1Constants.CONF_HOLDER_KEY));
        assertTrue(tokenString.contains("KeyValue"));
    }

    /**
     * Create a default Saml2 Unsigned Bearer Assertion.
     */
    @org.junit.Test
    public void testDefaultSaml2BearerUnsignedAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML2_TOKEN_TYPE));

        providerParameters.getStsProperties().setSignatureCrypto(null);
        ((SAMLTokenProvider)samlTokenProvider).setSignToken(false);

        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertFalse(tokenString.contains("AuthenticationStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML2Constants.CONF_BEARER));
        assertFalse(tokenString.contains(SAML2Constants.CONF_HOLDER_KEY));
        assertFalse(tokenString.contains("Signature"));
    }

    /**
     * Create a default Saml1 Bearer Assertion signed by a PKCS12 keystore
     */
    @org.junit.Test
    public void testDefaultSaml1BearerAssertionPKCS12() throws Exception {
        if (!TestUtilities.checkUnrestrictedPoliciesInstalled()) {
            return;
        }
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParametersPKCS12(
                WSS4JConstants.WSS_SAML_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE
            );
        assertTrue(samlTokenProvider.canHandleToken(WSS4JConstants.WSS_SAML_TOKEN_TYPE));
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(providerResponse.getTokenId()));
        assertTrue(tokenString.contains("AttributeStatement"));
        assertFalse(tokenString.contains("AuthenticationStatement"));
        assertTrue(tokenString.contains("alice"));
        assertTrue(tokenString.contains(SAML1Constants.CONF_BEARER));
        assertFalse(tokenString.contains(SAML1Constants.CONF_HOLDER_KEY));
    }

    /**
     * Create a default Saml2 Bearer Assertion using a specified C14n Algorithm
     */
    @org.junit.Test
    public void testDefaultSaml2BearerDifferentC14nAssertion() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);
        KeyRequirements keyRequirements = providerParameters.getKeyRequirements();

        keyRequirements.setC14nAlgorithm(WSS4JConstants.C14N_EXCL_WITH_COMMENTS);

        // This will fail as the requested c14n algorithm is rejected
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertFalse(tokenString.contains(WSS4JConstants.C14N_EXCL_WITH_COMMENTS));
        assertTrue(tokenString.contains(WSS4JConstants.C14N_EXCL_OMIT_COMMENTS));

        STSPropertiesMBean stsProperties = providerParameters.getStsProperties();
        SignatureProperties sigProperties = new SignatureProperties();
        sigProperties.setAcceptedC14nAlgorithms(Arrays.asList(
            WSS4JConstants.C14N_EXCL_OMIT_COMMENTS,
            WSS4JConstants.C14N_EXCL_WITH_COMMENTS));
        stsProperties.setSignatureProperties(sigProperties);

        // This will succeed as the requested c14n algorithm is accepted
        providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        token = (Element)providerResponse.getToken();
        tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(WSS4JConstants.C14N_EXCL_WITH_COMMENTS));
    }

    /**
     * Create a default Saml2 Bearer Assertion using a different Signature algorithm
     */
    @org.junit.Test
    public void testDefaultSaml2BearerDifferentSignatureAlgorithm() throws Exception {
        if (!TestUtilities.checkUnrestrictedPoliciesInstalled()) {
            return;
        }

        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);
        KeyRequirements keyRequirements = providerParameters.getKeyRequirements();

        // Default
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"));

        // Try with unsupported alternative
        String signatureAlgorithm = WSS4JConstants.DSA;
        keyRequirements.setSignatureAlgorithm(signatureAlgorithm);

        // This will fail as the requested signature algorithm is rejected
        providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        token = (Element)providerResponse.getToken();
        tokenString = DOM2Writer.nodeToString(token);
        assertFalse(tokenString.contains(signatureAlgorithm));
        assertTrue(tokenString.contains("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256"));

        // Supported alternative
        signatureAlgorithm = WSS4JConstants.RSA_SHA1;
        keyRequirements.setSignatureAlgorithm(signatureAlgorithm);

        // This will succeed as the requested signature algorithm is accepted
        providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        token = (Element)providerResponse.getToken();
        tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(signatureAlgorithm));
    }

    /**
     * Create a default Saml2 Bearer Assertion using a different Signature Digest algorithm
     */
    @org.junit.Test
    public void testDefaultSaml2BearerDifferentSignatureDigestAlgorithm() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.BEARER_KEY_KEYTYPE);

        // Default
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        Element token = (Element)providerResponse.getToken();
        String tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(WSS4JConstants.SHA256));

        // Supported alternative
        SignatureProperties signatureProperties =
                providerParameters.getStsProperties().getSignatureProperties();
        signatureProperties.setDigestAlgorithm(WSS4JConstants.SHA1);

        providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        token = (Element)providerResponse.getToken();
        tokenString = DOM2Writer.nodeToString(token);
        assertTrue(tokenString.contains(WSS4JConstants.SHA1));
    }

    /**
     * Create a default Saml2 Symmetric Key Assertion using EncryptWith Algorithms.
     */
    @org.junit.Test
    public void testDefaultSaml2EncryptWith() throws Exception {
        TokenProvider samlTokenProvider = new SAMLTokenProvider();
        TokenProviderParameters providerParameters =
            createProviderParameters(WSS4JConstants.WSS_SAML2_TOKEN_TYPE, STSConstants.SYMMETRIC_KEY_KEYTYPE);
        KeyRequirements keyRequirements = providerParameters.getKeyRequirements();

        keyRequirements.setEncryptWith(WSS4JConstants.AES_128);
        keyRequirements.setKeySize(92);
        TokenProviderResponse providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);

        keyRequirements.setKeySize(128);
        keyRequirements.setEncryptWith(WSS4JConstants.AES_256);
        providerResponse = samlTokenProvider.createToken(providerParameters);
        assertNotNull(providerResponse);
        assertTrue(providerResponse.getToken() != null && providerResponse.getTokenId() != null);
    }

    private TokenProviderParameters createProviderParameters(
        String tokenType, String keyType
    ) throws WSSecurityException {
        TokenProviderParameters parameters = new TokenProviderParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(tokenType);
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
        keyRequirements.setKeyType(keyType);
        parameters.setKeyRequirements(keyRequirements);

        parameters.setPrincipal(new CustomTokenPrincipal("alice"));
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        parameters.setMessageContext(msgCtx);

        parameters.setAppliesToAddress("http://dummy-service.com/dummy");

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionProperties());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        stsProperties.setSignatureUsername("mystskey");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        parameters.setStsProperties(stsProperties);

        parameters.setEncryptionProperties(new EncryptionProperties());

        return parameters;
    }

    private TokenProviderParameters createProviderParametersPKCS12(
        String tokenType, String keyType
    ) throws WSSecurityException {
        TokenProviderParameters parameters = new TokenProviderParameters();

        TokenRequirements tokenRequirements = new TokenRequirements();
        tokenRequirements.setTokenType(tokenType);
        parameters.setTokenRequirements(tokenRequirements);

        KeyRequirements keyRequirements = new KeyRequirements();
        keyRequirements.setKeyType(keyType);
        parameters.setKeyRequirements(keyRequirements);

        parameters.setPrincipal(new CustomTokenPrincipal("alice"));
        // Mock up message context
        MessageImpl msg = new MessageImpl();
        WrappedMessageContext msgCtx = new WrappedMessageContext(msg);
        parameters.setMessageContext(msgCtx);

        parameters.setAppliesToAddress("http://dummy-service.com/dummy");

        // Add STSProperties object
        StaticSTSProperties stsProperties = new StaticSTSProperties();
        Crypto crypto = CryptoFactory.getInstance(getEncryptionPropertiesPKCS12());
        stsProperties.setEncryptionCrypto(crypto);
        stsProperties.setSignatureCrypto(crypto);
        stsProperties.setEncryptionUsername("myservicekey");
        // stsProperties.setSignatureUsername("16c73ab6-b892-458f-abf5-2f875f74882e");
        stsProperties.setCallbackHandler(new PasswordCallbackHandler());
        stsProperties.setIssuer("STS");
        parameters.setStsProperties(stsProperties);

        parameters.setEncryptionProperties(new EncryptionProperties());

        return parameters;
    }

    private Properties getEncryptionProperties() {
        Properties properties = new Properties();
        properties.put(
            "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", "stsspass");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "keys/stsstore.jks");

        return properties;
    }

    private Properties getEncryptionPropertiesPKCS12() {
        Properties properties = new Properties();
        properties.put(
            "org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin"
        );
        properties.put("org.apache.wss4j.crypto.merlin.keystore.password", "security");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.file", "x509.p12");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.type", "pkcs12");
        properties.put("org.apache.wss4j.crypto.merlin.keystore.private.password", "security");

        return properties;
    }



}
