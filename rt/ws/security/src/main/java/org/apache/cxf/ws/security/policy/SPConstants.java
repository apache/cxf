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
package org.apache.cxf.ws.security.policy;

import javax.xml.namespace.QName;

public abstract class SPConstants {
    
    public static final String P_NS = "http://www.w3.org/ns/ws-policy";

    public static final String P_PREFIX = "wsp";

    public static final QName POLICY = new QName(P_NS, "Policy", P_PREFIX);
    
    
    public enum Version {
        SP_V11,
        SP_V12
    };

    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    
    public static final String LAYOUT = "Layout";

    public enum Layout {
        Lax,
        Strict,
        LaxTimestampFirst,
        LaxTimestampLast
    };

    
    ////////////////////////////////////////////////////////////////////////////////////////////////

    
    public enum ProtectionOrder {
        EncryptBeforeSigning,
        SignBeforeEncrypting
    };
    /**
     * Protection Order : EncryptBeforeSigning
     */
    public static final String ENCRYPT_BEFORE_SIGNING = "EncryptBeforeSigning";
    /**
     * Protection Order : SignBeforeEncrypting
     */
    public static final String SIGN_BEFORE_ENCRYPTING = "SignBeforeEncrypting";
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static final String ENCRYPT_SIGNATURE = "EncryptSignature";
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static final String PROTECT_TOKENS = "ProtectTokens";
    
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String ONLY_SIGN_ENTIRE_HEADERS_AND_BODY = "OnlySignEntireHeadersAndBody";
     
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static final String INCLUDE_TIMESTAMP = "IncludeTimestamp";
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static final String SIGNED_PARTS = "SignedParts";
    
    public static final String ENCRYPTED_PARTS = "EncryptedParts";
    
    public static final String SIGNED_ELEMENTS = "SignedElements";
    
    public static final String ENCRYPTED_ELEMENTS = "EncryptedElements";
    
    public static final String REQUIRED_ELEMENTS = "RequiredElements";
    
    public static final String CONTENT_ENCRYPTED_ELEMENTS = "ContentEncryptedElements";
    
    public static final String REQUIRED_PARTS = "RequiredParts";
    
    public static final String XPATH_VERSION = "XPathVersion";
    
    public static final String XPATH_EXPR = "XPath";
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    // X509 Token types 
    
    public static final String X509_TOKEN = "X509Token";

    public static final String WSS_X509_V1_TOKEN10 = "WssX509V1Token10";

    public static final String WSS_X509_V3_TOKEN10 = "WssX509V3Token10";

    public static final String WSS_X509_PKCS7_TOKEN10 = "WssX509Pkcs7Token10";

    public static final String WSS_X509_PKI_PATH_V1_TOKEN10 = "WssX509PkiPathV1Token10";

    public static final String WSS_X509_V1_TOKEN11 = "WssX509V1Token11";

    public static final String WSS_X509_V3_TOKEN11 = "WssX509V3Token11";

    public static final String WSS_X509_PKCS7_TOKEN11 = "WssX509Pkcs7Token11";

    public static final String WSS_X509_PKI_PATH_V1_TOKEN11 = "WssX509PkiPathV1Token11";
    
    
    public static final String USERNAME_TOKEN = "UsernameToken";
    public static final String KEYVALUE_TOKEN = "KeyValueToken";
    
    public static final String USERNAME_TOKEN10 = "WssUsernameToken10";
    
    public static final String USERNAME_TOKEN11 = "WssUsernameToken11";

    
    public static final String TRANSPORT_TOKEN = "TransportToken";
    
    public static final String HTTPS_TOKEN = "HttpsToken";
    
    public static final QName REQUIRE_CLIENT_CERTIFICATE = new QName("RequireClientCertificate");
    
    public static final QName HTTP_BASIC_AUTHENTICATION = new QName("HttpBasicAuthentication");
    
    public static final QName HTTP_DIGEST_AUTHENTICATION = new QName("HttpDigestAuthentication");
    
    public static final String SECURITY_CONTEXT_TOKEN = "SecurityContextToken";
    
    public static final String SECURE_CONVERSATION_TOKEN = "SecureConversationToken";
    
    public static final String ISSUED_TOKEN = "IssuedToken";
    

    public static final String SIGNATURE_TOKEN = "SignatureToken";
    
    public static final String ENCRYPTION_TOKEN = "EncryptionToken";
    
    public static final String PROTECTION_TOKEN = "ProtectionToken";
    
    public static final String INITIATOR_TOKEN = "InitiatorToken";
    
    public static final String RECIPIENT_TOKEN = "RecipientToken";
    
    
    
    public static final String SUPPORTING_TOKENS = "SupportingTokens";
    
    public static final String SIGNED_SUPPORTING_TOKENS = "SignedSupportingTokens";
    
    public static final String ENDORSING_SUPPORTING_TOKENS = "EndorsingSupportingTokens";
    
    public static final String SIGNED_ENDORSING_SUPPORTING_TOKENS = "SignedEndorsingSupportingTokens";
    
    public static final String ENCRYPTED_SUPPORTING_TOKENS = "EncryptedSupportingTokens";
    
    public static final String SIGNED_ENCRYPTED_SUPPORTING_TOKENS = "SignedEncryptedSupportingTokens";
    
    public static final String ENDORSING_ENCRYPTED_SUPPORTING_TOKENS = "EndorsingEncryptedSupportingTokens";
    
    public static final String SIGNED_ENDORSING_ENCRYPTED_SUPPORTING_TOKENS 
        = "SignedEndorsingEncryptedSupportingTokens";   
    
    
    public enum SupportTokenType {
        SUPPORTING_TOKEN_SUPPORTING,
        SUPPORTING_TOKEN_ENDORSING,
        SUPPORTING_TOKEN_SIGNED,
        SUPPORTING_TOKEN_SIGNED_ENDORSING,
        SUPPORTING_TOKEN_SIGNED_ENCRYPTED,
        SUPPORTING_TOKEN_ENCRYPTED,
        SUPPORTING_TOKEN_ENDORSING_ENCRYPTED,
        SUPPORTING_TOKEN_SIGNED_ENDORSING_ENCRYPTED
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////

    
    public static final String ALGO_SUITE = "AlgorithmSuite";
    
    
    // /
    // /Algorithm Suites
    // /
    public static final String ALGO_SUITE_BASIC256 = "Basic256";

    public static final String ALGO_SUITE_BASIC192 = "Basic192";

    public static final String ALGO_SUITE_BASIC128 = "Basic128";

    public static final String ALGO_SUITE_TRIPLE_DES = "TripleDes";

    public static final String ALGO_SUITE_BASIC256_RSA15 = "Basic256Rsa15";

    public static final String ALGO_SUITE_BASIC192_RSA15 = "Basic192Rsa15";

    public static final String ALGO_SUITE_BASIC128_RSA15 = "Basic128Rsa15";

    public static final String ALGO_SUITE_TRIPLE_DES_RSA15 = "TripleDesRsa15";

    public static final String ALGO_SUITE_BASIC256_SHA256 = "Basic256Sha256";

    public static final String ALGO_SUITE_BASIC192_SHA256 = "Basic192Sha256";

    public static final String ALGO_SUITE_BASIC128_SHA256 = "Basic128Sha256";

    public static final String ALGO_SUITE_TRIPLE_DES_SHA256 = "TripleDesSha256";

    public static final String ALGO_SUITE_BASIC256_SHA256_RSA15 = "Basic256Sha256Rsa15";

    public static final String ALGO_SUITE_BASIC192_SHA256_RSA15 = "Basic192Sha256Rsa15";

    public static final String ALGO_SUITE_BASIC128_SHA256_RSA15 = "Basic128Sha256Rsa15";

    public static final String ALGO_SUITE_TRIPLE_DES_SHA256_RSA15 = "TripleDesSha256Rsa15";

    // /
    // /Algorithms
    // /
    public static final String HMAC_SHA1 = "http://www.w3.org/2000/09/xmldsig#hmac-sha1";

    public static final String RSA_SHA1 = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";

    public static final String SHA1 = "http://www.w3.org/2000/09/xmldsig#sha1";

    public static final String SHA256 = "http://www.w3.org/2001/04/xmlenc#sha256";

    public static final String SHA512 = "http://www.w3.org/2001/04/xmlenc#sha512";

    public static final String AES128 = "http://www.w3.org/2001/04/xmlenc#aes128-cbc";

    public static final String AES192 = "http://www.w3.org/2001/04/xmlenc#aes192-cbc";

    public static final String AES256 = "http://www.w3.org/2001/04/xmlenc#aes256-cbc";

    public static final String TRIPLE_DES = "http://www.w3.org/2001/04/xmlenc#tripledes-cbc";

    public static final String KW_AES128 = "http://www.w3.org/2001/04/xmlenc#kw-aes128";

    public static final String KW_AES192 = "http://www.w3.org/2001/04/xmlenc#kw-aes192";

    public static final String KW_AES256 = "http://www.w3.org/2001/04/xmlenc#kw-aes256";

    public static final String KW_TRIPLE_DES = "http://www.w3.org/2001/04/xmlenc#kw-tripledes";

    public static final String KW_RSA_OAEP = "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p";

    public static final String KW_RSA15 = "http://www.w3.org/2001/04/xmlenc#rsa-1_5";

    public static final String P_SHA1 = "http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1";

    public static final String P_SHA1_L128 = "http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1";

    public static final String P_SHA1_L192 = "http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1";

    public static final String P_SHA1_L256 = "http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1";

    public static final String XPATH = "http://www.w3.org/TR/1999/REC-xpath-19991116";

    public static final String XPATH20 = "http://www.w3.org/2002/06/xmldsig-filter2";

    public static final String C14N = "http://www.w3.org/2001/10/xml-c14n#";

    public static final String EX_C14N = "http://www.w3.org/2001/10/xml-exc-c14n#";

    public static final String SNT = "http://www.w3.org/TR/soap12-n11n";

    public static final String STRT10 
        = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#STR-Transform";

    // /////////////////////////////////////////////////////////////////////////////////////////////

    public static final String INCLUSIVE_C14N = "InclusiveC14N";

    public static final String SOAP_NORMALIZATION_10 = "SoapNormalization10";

    public static final String STR_TRANSFORM_10 = "STRTransform10";

    public static final String XPATH10 = "XPath10";

    public static final String XPATH_FILTER20 = "XPathFilter20";
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static final String ATTR_INCLUDE_TOKEN = "IncludeToken";
    public static final String INCLUDE_TOKEN_NEVER_SUFFIX = "/IncludeToken/Never";
    public static final String INCLUDE_TOKEN_ONCE_SUFFIX = "/IncludeToken/Once";
    public static final String INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT_SUFFIX = "/IncludeToken/AlwaysToRecipient";
    public static final String INCLUDE_TOKEN_ALWAYS_TO_INITIATOR_SUFFIX = "/IncludeToken/AlwaysToInitiator";
    public static final String INCLUDE_TOKEN_ALWAYS_SUFFIX = "/IncludeToken/Always";
    
    
    public enum IncludeTokenType {
        INCLUDE_TOKEN_NEVER,
        INCLUDE_TOKEN_ONCE,
        INCLUDE_TOKEN_ALWAYS_TO_RECIPIENT,
        INCLUDE_TOKEN_ALWAYS_TO_INITIATOR,
        INCLUDE_TOKEN_ALWAYS
    };
    
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static final String TRANSPORT_BINDING = "TransportBinding";
    public static final String ASYMMETRIC_BINDING = "AsymmetricBinding";
    public static final String SYMMETRIC_BINDING = "SymmetricBinding";
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static final String REQUIRE_KEY_IDENTIFIER_REFERENCE = "RequireKeyIdentifierReference";
    public static final String REQUIRE_ISSUER_SERIAL_REFERENCE = "RequireIssuerSerialReference";
    public static final String REQUIRE_EMBEDDED_TOKEN_REFERENCE = "RequireEmbeddedTokenReference";
    public static final String REQUIRE_THUMBPRINT_REFERENCE = "RequireThumbprintReference";
    public static final String REQUIRE_SIGNATURE_CONFIRMATION = "RequireSignatureConfirmation";
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static final String MUST_SUPPORT_REF_KEY_IDENTIFIER = "MustSupportRefKeyIdentifier";
    
    public static final String MUST_SUPPORT_REF_ISSUER_SERIAL = "MustSupportRefIssuerSerial";
    
    public static final String MUST_SUPPORT_REF_EXTERNAL_URI = "MustSupportRefExternalURI";
    
    public static final String MUST_SUPPORT_REF_EMBEDDED_TOKEN = "MustSupportRefEmbeddedToken";
    
    public static final String MUST_SUPPORT_REF_THUMBPRINT = "MustSupportRefThumbprint";
    
    public static final String MUST_SUPPORT_REF_ENCRYPTED_KEY = "MustSupportRefEncryptedKey";
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static final String WSS10 = "Wss10";
    
    public static final String WSS11 = "Wss11";
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static final String TRUST_10 = "Trust10";
    
    public static final String TRUST_13 = "Trust13";
    
    public static final String MUST_SUPPORT_CLIENT_CHALLENGE = "MustSupportClientChanllenge";
    
    public static final String MUST_SUPPORT_SERVER_CHALLENGE = "MustSupportServerChanllenge";
    
    public static final String REQUIRE_CLIENT_ENTROPY = "RequireClientEntropy";
    
    public static final String REQUIRE_SERVER_ENTROPY = "RequireServerEntropy";
    
    public static final String MUST_SUPPORT_ISSUED_TOKENS = "MustSupportIssuedTokens";
    
    public static final String REQUIRE_REQUEST_SECURITY_TOKEN_COLLECTION 
        = "RequireRequestSecurityTokenCollection";
    
    public static final String REQUIRE_APPLIES_TO = "RequireAppliesTo";
    
    public static final String ISSUER = "Issuer";
    
    public static final String REQUIRE_DERIVED_KEYS = "RequireDerivedKeys";
    
    public static final String REQUIRE_IMPLIED_DERIVED_KEYS = "RequireImpliedDerivedKeys";
    
    public static final String REQUIRE_EXPLICIT_DERIVED_KEYS = "RequireExplicitDerivedKeys";
    
    public static final String REQUIRE_EXTERNAL_URI_REFERENCE = "RequireExternalUriReference";
    
    public static final String REQUIRE_EXTERNAL_REFERENCE = "RequireExternalReference";
    
    public static final String REQUIRE_INTERNAL_REFERENCE = "RequireInternalReference";
    
    public static final String REQUEST_SECURITY_TOKEN_TEMPLATE = "RequestSecurityTokenTemplate";
    
    public static final String SC10_SECURITY_CONTEXT_TOKEN = "SC10SecurityContextToken";
    
    public static final String BOOTSTRAP_POLICY = "BootstrapPolicy";
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static final String HEADER = "Header";
    
    public static final String BODY = "Body";
    
    public static final String ATTACHMENTS = "Attachments";
    
    public static final String NAME = "Name";
    
    public static final String NAMESPACE = "Namespace";
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    
    public static final String NO_PASSWORD = "NoPassword";
    
    public static final String HASH_PASSWORD = "HashPassword";
    
    
    
    public abstract Version getVersion();
    
    public abstract String getNamespace();
    public abstract QName getWSS10();
    public abstract QName getWSS11();
    public abstract QName getAlgorithmSuite();
    public abstract QName getAsymmetricBinding();
    public abstract QName getEncryptionToken();
    public abstract QName getHttpsToken();
    public abstract QName getInitiatorToken();
    public abstract QName getIssuedToken();
    public abstract QName getIncludeToken();
    public abstract QName getLayout();
    public abstract QName getProtectionToken();
    public abstract QName getRecipientToken();
    public abstract QName getRequiredElements();
    public abstract QName getSecureConversationToken();
    public abstract QName getSecurityContextToken();
    public abstract QName getSignatureToken();
    public abstract QName getSignedElements();
    public abstract QName getEncryptedElements();
    public abstract QName getSignedParts();
    public abstract QName getEncryptedParts();
    public abstract QName getIncludeTimestamp();
    
    public abstract QName getSymmetricBinding();
    public abstract QName getTransportBinding();
    public abstract QName getTransportToken();
    public abstract QName getUserNameToken();
    public abstract QName getX509Token();
    
    public abstract QName getSupportingTokens();
    public abstract QName getSignedSupportingTokens();
    public abstract QName getEndorsingSupportingTokens();
    public abstract QName getSignedEndorsingSupportingTokens();


    //For X509 tokens
    public abstract QName getRequiredDerivedKeys();
    
    
    public abstract IncludeTokenType getInclusionFromAttributeValue(String value);
    public abstract String getAttributeValueFromInclusion(IncludeTokenType value);

}
