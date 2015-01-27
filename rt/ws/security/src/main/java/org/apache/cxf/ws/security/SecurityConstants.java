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

package org.apache.cxf.ws.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Configuration tags used to configure the WS-SecurityPolicy layer. Some of them are also 
 * used by the non WS-SecurityPolicy approach in the WSS4J(Out|In)Interceptors.
 */
public final class SecurityConstants {
    
    //
    // User properties
    //
    
    /**
     * The user's name. It is used differently by each of the WS-Security functions:
     * a) It is used as the name in the UsernameToken
     * b) It is used as the alias name in the keystore to get the user's cert and private key for signature
     *    if {@link SIGNATURE_USERNAME} is not set.
     * c) It is used as the alias name in the keystore to get the user's public key for encryption if 
     *    {@link ENCRYPT_USERNAME} is not set.
     */
    public static final String USERNAME = "ws-security.username";
    
    /**
     * The user's password when a {@link CALLBACK_HANDLER} is not defined. It is currently only used for 
     * the case of adding a password to a UsernameToken.
     */
    public static final String PASSWORD = "ws-security.password";
    
    /**
     * The user's name for signature. It is used as the alias name in the keystore to get the user's cert 
     * and private key for signature. If this is not defined, then {@link USERNAME} is used instead. If 
     * that is also not specified, it uses the the default alias set in the properties file referenced by
     * {@link SIGNATURE_PROPERTIES}. If that's also not set, and the keystore only contains a single key, 
     * that key will be used. 
     */
    public static final String SIGNATURE_USERNAME = "ws-security.signature.username";
    
    /**
     * The user's name for encryption. It is used as the alias name in the keystore to get the user's public 
     * key for encryption. If this is not defined, then {@link USERNAME} is used instead. If 
     * that is also not specified, it uses the the default alias set in the properties file referenced by
     * {@link ENCRYPT_PROPERTIES}. If that's also not set, and the keystore only contains a single key, 
     * that key will be used.
     * 
     * For the web service provider, the "useReqSigCert" keyword can be used to accept (encrypt to) any 
     * client whose public key is in the service's truststore (defined in {@link ENCRYPT_PROPERTIES}).
     */
    public static final String ENCRYPT_USERNAME = "ws-security.encryption.username";
    
    /**
     * The actor or role name of the wsse:Security header. If this parameter 
     * is omitted, the actor name is not set.
     */
    public static final String ACTOR = "ws-security.actor";
    
    //
    // Callback class and Crypto properties
    //
    
    /**
     * The CallbackHandler implementation class used to obtain passwords, for both outbound and inbound 
     * requests. The value of this tag must be either:
     * a) The class name of a {@link javax.security.auth.callback.CallbackHandler} instance, which must
     * be accessible via the classpath.
     * b) A {@link javax.security.auth.callback.CallbackHandler} instance.
     */
    public static final String CALLBACK_HANDLER = "ws-security.callback-handler";
    
    /**
     * The SAML CallbackHandler implementation class used to construct SAML Assertions. The value of this 
     * tag must be either:
     * a) The class name of a {@link javax.security.auth.callback.CallbackHandler} instance, which must
     * be accessible via the classpath.
     * b) A {@link javax.security.auth.callback.CallbackHandler} instance.
     */
    public static final String SAML_CALLBACK_HANDLER = "ws-security.saml-callback-handler";
    
    /**
     * The Crypto property configuration to use for signature, if {@link SIGNATURE_CRYPTO} is not set instead.
     * The value of this tag must be either:
     * a) A Java Properties object that contains the Crypto configuration.
     * b) The path of the Crypto property file that contains the Crypto configuration.
     * c) A URL that points to the Crypto property file that contains the Crypto configuration.
     */
    public static final String SIGNATURE_PROPERTIES = "ws-security.signature.properties";
    
    /**
     * The Crypto property configuration to use for encryption, if {@link ENCRYPT_CRYPTO} is not set instead.
     * The value of this tag must be either:
     * a) A Java Properties object that contains the Crypto configuration.
     * b) The path of the Crypto property file that contains the Crypto configuration.
     * c) A URL that points to the Crypto property file that contains the Crypto configuration.
     */
    public static final String ENCRYPT_PROPERTIES = "ws-security.encryption.properties";
    
    /**
     * A Crypto object to be used for signature. If this is not defined then the 
     * {@link SIGNATURE_PROPERTIES} is used instead.
     */
    public static final String SIGNATURE_CRYPTO = "ws-security.signature.crypto";
    
    /**
     * A Crypto object to be used for encryption. If this is not defined then the 
     * {@link ENCRYPT_PROPERTIES} is used instead.
     */
    public static final String ENCRYPT_CRYPTO = "ws-security.encryption.crypto";
    
    /**
     * A message property for prepared X509 certificate to be used for encryption. 
     * If this is not defined, then the certificate will be either loaded from the 
     * keystore {@link ENCRYPT_PROPERTIES} or extracted from request 
     * (if {@link ENCRYPT_USERNAME} has value "useReqSigCert").
     */
    public static final String ENCRYPT_CERT = "ws-security.encryption.certificate";
    
    //
    // Boolean WS-Security configuration tags, e.g. the value should be "true" or "false".
    //
    
    /**
     * Whether to validate the password of a received UsernameToken or not. The default is true.
     */
    public static final String VALIDATE_TOKEN = "ws-security.validate.token";
    
    /**
     * Whether to enable Certificate Revocation List (CRL) checking or not when verifying trust 
     * in a certificate. The default value is "false".
     */
    public static final String ENABLE_REVOCATION = "ws-security.enableRevocation";
    
    // WebLogic and WCF always encrypt UsernameTokens whenever possible
    //See:  http://e-docs.bea.com/wls/docs103/webserv_intro/interop.html
    //Be default, we will encrypt as well for interop reasons.  However, this
    //setting can be set to false to turn that off.
    /**
     * Whether to always encrypt UsernameTokens that are defined as a SupportingToken. The default 
     * is true. This should not be set to false in a production environment, as it exposes the 
     * password (or the digest of the password) on the wire.
     */
    public static final String ALWAYS_ENCRYPT_UT = "ws-security.username-token.always.encrypted";
    
    /**
     * Whether to ensure compliance with the Basic Security Profile (BSP) 1.1 or not. The
     * default value is "true".
     */
    public static final String IS_BSP_COMPLIANT = "ws-security.is-bsp-compliant";
    
    /**
     * Whether to allow unsigned saml assertions as SecurityContext Principals. The default is false.
     */
    public static final String ENABLE_UNSIGNED_SAML_ASSERTION_PRINCIPAL = 
            "ws-security.enable.unsigned-saml-assertion.principal";
    
    /**
     * Whether to cache UsernameToken nonces. The default value is "true" for message recipients, and 
     * "false" for message initiators. Set it to true to cache for both cases. Set this to "false" to
     * not cache UsernameToken nonces. Note that caching only applies when either a UsernameToken
     * WS-SecurityPolicy is in effect, or else that a UsernameToken action has been configured
     * for the non-security-policy case.
     */
    public static final String ENABLE_NONCE_CACHE = "ws-security.enable.nonce.cache";
    
    /**
     * Whether to cache Timestamp Created Strings (these are only cached in conjunction with a message 
     * Signature).The default value is "true" for message recipients, and "false" for message initiators.
     * Set it to true to cache for both cases. Set this to "false" to not cache Timestamp Created Strings.
     * Note that caching only applies when either a "IncludeTimestamp" policy is in effect, or
     * else that a Timestamp action has been configured for the non-security-policy case.
     */
    public static final String ENABLE_TIMESTAMP_CACHE = "ws-security.enable.timestamp.cache";
    
    /**
     * Whether to cache SAML2 Token Identifiers, if the token contains a "OneTimeUse" Condition.
     * The default value is "true" for message recipients, and "false" for message initiators.
     * Set it to true to cache for both cases. Set this to "false" to not cache SAML2 Token Identifiers.
     * Note that caching only applies when either a "SamlToken" policy is in effect, or
     * else that a SAML action has been configured for the non-security-policy case.
     */
    public static final String ENABLE_SAML_ONE_TIME_USE_CACHE = "ws-security.enable.saml.cache";
    
    /**
     * Whether to validate the SubjectConfirmation requirements of a received SAML Token
     * (sender-vouches or holder-of-key). The default is true.
     */
    public static final String VALIDATE_SAML_SUBJECT_CONFIRMATION = 
        "ws-security.validate.saml.subject.conf";
    
    /**
     * Whether to enable streaming WS-Security. If set to false (the default), the old DOM
     * implementation is used. If set to true, the new streaming (StAX) implementation is used.
     */
    public static final String ENABLE_STREAMING_SECURITY = 
        "ws-security.enable.streaming";
    
    /**
     * Whether to return the security error message to the client, and not the default error message.
     * The "real" security errors should not be returned to the client in a deployment scenario,
     * as they may leak information about the deployment, or otherwise provide a "oracle" for attacks.
     * The default is false.
     */
    public static final String RETURN_SECURITY_ERROR = "ws-security.return.security.error";
    
    /**
     * Set this to "false" in order to remove the SOAP mustUnderstand header from security headers generated based on
     * a WS-SecurityPolicy.
     *
     * The default value is "true" which included the SOAP mustUnderstand header.
     */
    public static final String MUST_UNDERSTAND = "ws-security.must-understand";

    /**
     * Set this to "false" if security context must not be created from JAAS Subject.
     *
     * The default value is "true".
     */
    public static final String SC_FROM_JAAS_SUBJECT = "ws-security.sc.jaas-subject";
    
    /**
     * Enable SAML AudienceRestriction validation. If this is set to "true", then IF the
     * SAML Token contains Audience Restriction URIs, one of them must match either the
     * request URL or the Service QName. The default is "true".
     */
    public static final String AUDIENCE_RESTRICTION_VALIDATION = "ws-security.validate.audience-restriction";
    
    //
    // Non-boolean WS-Security Configuration parameters
    //
    
    /**
     * The time in seconds to append to the Creation value of an incoming Timestamp to determine
     * whether to accept the Timestamp as valid or not. The default value is 300 seconds (5 minutes).
     */
    public static final String TIMESTAMP_TTL = "ws-security.timestamp.timeToLive";
    
    /**
     * The time in seconds in the future within which the Created time of an incoming 
     * Timestamp is valid. The default value is "60", to avoid problems where clocks are 
     * slightly askew. To reject all future-created Timestamps, set this value to "0". 
     */
    public static final String TIMESTAMP_FUTURE_TTL = "ws-security.timestamp.futureTimeToLive";
    
    /**
     * The time in seconds to append to the Creation value of an incoming UsernameToken to determine
     * whether to accept the UsernameToken as valid or not. The default value is 300 seconds (5 minutes).
     */
    public static final String USERNAMETOKEN_TTL = "ws-security.usernametoken.timeToLive";
    
    /**
     * The time in seconds in the future within which the Created time of an incoming 
     * UsernameToken is valid. The default value is "60", to avoid problems where clocks are 
     * slightly askew. To reject all future-created UsernameTokens, set this value to "0". 
     */
    public static final String USERNAMETOKEN_FUTURE_TTL = "ws-security.usernametoken.futureTimeToLive";
    
    /**
     * The attribute URI of the SAML AttributeStatement where the role information is stored.
     * The default is "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role".
     */
    public static final String SAML_ROLE_ATTRIBUTENAME = "ws-security.saml-role-attributename";
    
    /**
     * The SpnegoClientAction implementation to use for SPNEGO. This allows the user to plug in
     * a different implementation to obtain a service ticket.
     */
    public static final String SPNEGO_CLIENT_ACTION = "ws-security.spnego.client.action";
    
    /**
     * This holds a reference to a ReplayCache instance used to cache UsernameToken nonces. The
     * default instance that is used is the EHCacheReplayCache.
     */
    public static final String NONCE_CACHE_INSTANCE = 
        "ws-security.nonce.cache.instance";
    
    /**
     * This holds a reference to a ReplayCache instance used to cache Timestamp Created Strings. The
     * default instance that is used is the EHCacheReplayCache.
     */
    public static final String TIMESTAMP_CACHE_INSTANCE = 
        "ws-security.timestamp.cache.instance";
    
    /**
     * This holds a reference to a ReplayCache instance used to cache SAML2 Token Identifiers, when
     * the token has a "OneTimeUse" Condition. The default instance that is used is the EHCacheReplayCache.
     */
    public static final String SAML_ONE_TIME_USE_CACHE_INSTANCE = 
        "ws-security.saml.cache.instance";
    
    /**
     * Set this property to point to a configuration file for the underlying caching implementation.
     * The default configuration file that is used is cxf-ehcache.xml in this module.
     */
    public static final String CACHE_CONFIG_FILE = 
        "ws-security.cache.config.file";
    
    /**
     * The TokenStore instance to use to cache security tokens. By default this uses the
     * EHCacheTokenStore if EhCache is available. Otherwise it uses the MemoryTokenStore.
     */
    public static final String TOKEN_STORE_CACHE_INSTANCE = 
        "org.apache.cxf.ws.security.tokenstore.TokenStore";
    
    /**
     * The Cache Identifier to use with the TokenStore. CXF uses the following key to retrieve a
     * token store: "org.apache.cxf.ws.security.tokenstore.TokenStore-<identifier>". This key can be 
     * used to configure service-specific cache configuration. If the identifier does not match, then it 
     * falls back to a cache configuration with key "org.apache.cxf.ws.security.tokenstore.TokenStore".
     * 
     * The default "<identifier>" is the QName of the service in question. However to pick up a 
     * custom cache configuration (for example, if you want to specify a TokenStore per-client proxy),
     * it can be configured with this identifier instead.
     */
    public static final String CACHE_IDENTIFIER = "ws-security.cache.identifier";

    /**
     * A comma separated String of regular expressions which will be applied to the subject DN of 
     * the certificate used for signature validation, after trust verification of the certificate 
     * chain associated with the  certificate.
     */
    public static final String SUBJECT_CERT_CONSTRAINTS = "ws-security.subject.cert.constraints";
    
    /**
     * The Subject Role Classifier to use. If one of the WSS4J Validators returns a JAAS Subject
     * from Validation, then the WSS4JInInterceptor will attempt to create a SecurityContext
     * based on this Subject. If this value is not specified, then it tries to get roles using
     * the DefaultSecurityContext in cxf-rt-core. Otherwise it uses this value in combination
     * with the SUBJECT_ROLE_CLASSIFIER_TYPE to get the roles from the Subject.
     */
    public static final String SUBJECT_ROLE_CLASSIFIER = "ws-security.role.classifier";
    
    /**
     * The Subject Role Classifier Type to use. If one of the WSS4J Validators returns a JAAS Subject
     * from Validation, then the WSS4JInInterceptor will attempt to create a SecurityContext
     * based on this Subject. Currently accepted values are "prefix" or "classname". Must be
     * used in conjunction with the SUBJECT_ROLE_CLASSIFIER. The default value is "prefix".
     */
    public static final String SUBJECT_ROLE_CLASSIFIER_TYPE = "ws-security.role.classifier.type";
    
    /**
     * This configuration tag allows the user to override the default Asymmetric Signature 
     * algorithm (RSA-SHA1) for use in WS-SecurityPolicy, as the WS-SecurityPolicy specification
     * does not allow the use of other algorithms at present.
     */
    public static final String ASYMMETRIC_SIGNATURE_ALGORITHM = 
        "ws-security.asymmetric.signature.algorithm";
    
    /**
     * This holds a reference to a PasswordEncryptor instance, which is used to encrypt or 
     * decrypt passwords in the Merlin Crypto implementation (or any custom Crypto implementations).
     * 
     * By default, WSS4J uses the JasyptPasswordEncryptor, which must be instantiated with a 
     * master password to use to decrypt keystore passwords in the Merlin Crypto properties file.
     * This master password is obtained via the CallbackHandler defined via PW_CALLBACK_CLASS
     * or PW_CALLBACK_REF.
     * 
     * The encrypted passwords must be stored in the format "ENC(encoded encrypted password)".
     */
    public static final String PASSWORD_ENCRYPTOR_INSTANCE = 
        "ws-security.password.encryptor.instance";
    
    /**
     * A delegated credential to use for WS-Security. Currently only a Kerberos GSSCredential
     * Object is supported. This is used to retrieve a service ticket instead of using the
     * client credentials.
     */
    public static final String DELEGATED_CREDENTIAL = "ws-security.delegated.credential";
    
    //
    // Validator implementations for validating received security tokens
    //
    
    /**
     * The WSS4J Validator instance to use to validate UsernameTokens. The default value is the
     * UsernameTokenValidator.
     */
    public static final String USERNAME_TOKEN_VALIDATOR = "ws-security.ut.validator";
    
    /**
     * The WSS4J Validator instance to use to validate SAML 1.1 Tokens. The default value is the
     * SamlAssertionValidator.
     */
    public static final String SAML1_TOKEN_VALIDATOR = "ws-security.saml1.validator";
    
    /**
     * The WSS4J Validator instance to use to validate SAML 2.0 Tokens. The default value is the
     * SamlAssertionValidator.
     */
    public static final String SAML2_TOKEN_VALIDATOR = "ws-security.saml2.validator";
    
    /**
     * The WSS4J Validator instance to use to validate Timestamps. The default value is the
     * TimestampValidator.
     */
    public static final String TIMESTAMP_TOKEN_VALIDATOR = "ws-security.timestamp.validator";
    
    /**
     * The WSS4J Validator instance to use to validate trust in credentials used in
     * Signature verification. The default value is the SignatureTrustValidator.
     */
    public static final String SIGNATURE_TOKEN_VALIDATOR = "ws-security.signature.validator";
    
    /**
     * The WSS4J Validator instance to use to validate BinarySecurityTokens. The default value 
     * is the NoOpValidator.
     */
    public static final String BST_TOKEN_VALIDATOR = "ws-security.bst.validator";
    
    /**
     * The WSS4J Validator instance to use to validate SecurityContextTokens. The default value is 
     * the NoOpValidator.
     */
    public static final String SCT_TOKEN_VALIDATOR = "ws-security.sct.validator";
    
    //
    // STS Client Configuration tags
    //
    
    /**
     * A reference to the STSClient class used to communicate with the STS.
     */
    public static final String STS_CLIENT = "ws-security.sts.client";
    
    /**
     * The "AppliesTo" address to send to the STS. The default is the endpoint address of the 
     * service provider.
     */
    public static final String STS_APPLIES_TO = "ws-security.sts.applies-to";
    
    /**
     * Whether to write out an X509Certificate structure in UseKey/KeyInfo, or whether to write
     * out a KeyValue structure. The default value is "false".
     */
    public static final String STS_TOKEN_USE_CERT_FOR_KEYINFO = "ws-security.sts.token.usecert";
    
    /**
     * Whether to cancel a token when using SecureConversation after successful invocation. The
     * default is "false".
     */
    public static final String STS_TOKEN_DO_CANCEL = "ws-security.sts.token.do.cancel";
    
    /**
     * Whether to fall back to calling "issue" after failing to renew an expired token. Some
     * STSs do not support the renew binding, and so we should just issue a new token after expiry.
     * The default is true.
     */
    public static final String STS_ISSUE_AFTER_FAILED_RENEW = "ws-security.issue.after.failed.renew";
    
    /**
     * Set this to "false" to not cache a SecurityToken per proxy object in the 
     * IssuedTokenInterceptorProvider. This should be done if a token is being retrieved
     * from an STS in an intermediary. The default value is "true".
     */
    public static final String CACHE_ISSUED_TOKEN_IN_ENDPOINT = 
        "ws-security.cache.issued.token.in.endpoint";
    
    /**
     * Whether to avoid STS client trying send WS-MetadataExchange call using
     * STS EPR WSA address when the endpoint contract contains no WS-MetadataExchange info.
     * The default value is "false".
     */
    public static final String DISABLE_STS_CLIENT_WSMEX_CALL_USING_EPR_ADDRESS =
        "ws-security.sts.disable-wsmex-call-using-epr-address";
    
    /**
     * Whether to prefer to use WS-MEX over a STSClient's location/wsdlLocation properties
     * when making an STS RequestSecurityToken call. This can be set to true for the scenario
     * of making a WS-MEX call to an initial STS, and using the returned token to make another
     * call to an STS (which is configured using the STSClient configuration). Default is 
     * "false".
     */
    public static final String PREFER_WSMEX_OVER_STS_CLIENT_CONFIG = 
        "ws-security.sts.prefer-wsmex";
    
    /**
     * Switch STS client to send Soap 1.2 messages
     */
    public static final String STS_CLIENT_SOAP12_BINDING =
        "ws-security.sts.client-soap12-binding";

    /**
     * 
     * A Crypto object to be used for the STS. If this is not defined then the 
     * {@link STS_TOKEN_PROPERTIES} is used instead.
     * 
     * WCF's trust server sometimes will encrypt the token in the response IN ADDITION TO
     * the full security on the message. These properties control the way the STS client
     * will decrypt the EncryptedData elements in the response.
     * 
     * These are also used by the STSClient to send/process any RSA/DSAKeyValue tokens 
     * used if the KeyType is "PublicKey" 
     */
    public static final String STS_TOKEN_CRYPTO = "ws-security.sts.token.crypto";
    
    /**
     * The Crypto property configuration to use for the STS, if {@link STS_TOKEN_CRYPTO} is not
     * set instead.
     * The value of this tag must be either:
     * a) A Java Properties object that contains the Crypto configuration.
     * b) The path of the Crypto property file that contains the Crypto configuration.
     * c) A URL that points to the Crypto property file that contains the Crypto configuration.
     */
    public static final String STS_TOKEN_PROPERTIES = "ws-security.sts.token.properties";
    
    /**
     * The alias name in the keystore to get the user's public key to send to the STS for the
     * PublicKey KeyType case.
     */
    public static final String STS_TOKEN_USERNAME = "ws-security.sts.token.username";
    
    /**
     * The token to be sent to the STS in an "ActAs" field. It can be either:
     * a) A String (which must be an XML statement like "<wst:OnBehalfOf xmlns:wst=...>...</wst:OnBehalfOf>")
     * b) A DOM Element
     * c) A CallbackHandler object to use to obtain the token
     * 
     * In the case of a CallbackHandler, it must be able to handle a 
     * org.apache.cxf.ws.security.trust.delegation.DelegationCallback Object, which contains a 
     * reference to the current Message. The CallbackHandler implementation is required to set 
     * the token Element to be sent in the request on the Callback.
     * 
     * Some examples that can be reused are:
     * org.apache.cxf.ws.security.trust.delegation.ReceivedTokenCallbackHandler
     * org.apache.cxf.ws.security.trust.delegation.WSSUsernameCallbackHandler
     */
    public static final String STS_TOKEN_ACT_AS = "ws-security.sts.token.act-as";
    
    /**
     * The token to be sent to the STS in an "OnBehalfOf" field. It can be either:
     * a) A String (which must be an XML statement like "<wst:OnBehalfOf xmlns:wst=...>...</wst:OnBehalfOf>")
     * b) A DOM Element
     * c) A CallbackHandler object to use to obtain the token
     * 
     * In the case of a CallbackHandler, it must be able to handle a 
     * org.apache.cxf.ws.security.trust.delegation.DelegationCallback Object, which contains a 
     * reference to the current Message. The CallbackHandler implementation is required to set 
     * the token Element to be sent in the request on the Callback.
     * 
     * Some examples that can be reused are:
     * org.apache.cxf.ws.security.trust.delegation.ReceivedTokenCallbackHandler
     * org.apache.cxf.ws.security.trust.delegation.WSSUsernameCallbackHandler
     */
    public static final String STS_TOKEN_ON_BEHALF_OF = "ws-security.sts.token.on-behalf-of";

    /**
     * This is the value in seconds within which a token is considered to be expired by the
     * client. When a cached token (from a STS) is retrieved by the client, it is considered
     * to be expired if it will expire in a time less than the value specified by this tag.
     * This prevents token expiry when the message is en route / being processed by the
     * service. When the token is found to be expired then it will be renewed via the STS.
     * 
     * The default value is 10 (seconds). Specify 0 to avoid this check.
     */
    public static final String STS_TOKEN_IMMINENT_EXPIRY_VALUE =
        "ws-security.sts.token.imminent-expiry-value";
    
    //
    // Kerberos Configuration tags
    //
    
    /**
     * Whether to request credential delegation or not in the KerberosClient. If this is set to "true",
     * then it tries to get a kerberos service ticket that can be used for delegation. The default
     * is "false".
     */
    public static final String KERBEROS_REQUEST_CREDENTIAL_DELEGATION = 
        "ws-security.kerberos.request.credential.delegation";
    
    /**
     * Whether to use credential delegation or not in the KerberosClient. If this is set to "true",
     * then it tries to get a GSSCredential Object from the Message Context using the 
     * DELEGATED_CREDENTIAL configuration tag below, and then use this to obtain a service ticket.
     * The default is "false".
     */
    public static final String KERBEROS_USE_CREDENTIAL_DELEGATION = 
        "ws-security.kerberos.use.credential.delegation";
    
    /**
     * Whether the Kerberos username is in servicename form or not. The default is "false".
     */
    public static final String KERBEROS_IS_USERNAME_IN_SERVICENAME_FORM = 
        "ws-security.kerberos.is.username.in.servicename.form";
    
    /**
     * The JAAS Context name to use for Kerberos.
     */
    public static final String KERBEROS_JAAS_CONTEXT_NAME = "ws-security.kerberos.jaas.context";
    
    /**
     * The Kerberos Service Provider Name (spn) to use.
     */
    public static final String KERBEROS_SPN = "ws-security.kerberos.spn";
    
    /**
     * A reference to the KerberosClient class used to obtain a service ticket. 
     */
    public static final String KERBEROS_CLIENT = "ws-security.kerberos.client";

    //
    // Internal tags
    //
    
    public static final String TOKEN = "ws-security.token";
    public static final String TOKEN_ID = "ws-security.token.id";
    
    public static final Set<String> ALL_PROPERTIES;
    
    static {
        Set<String> s = new HashSet<String>(Arrays.asList(new String[] {
            USERNAME, PASSWORD, SIGNATURE_USERNAME, ENCRYPT_USERNAME, ACTOR,
            CALLBACK_HANDLER, SAML_CALLBACK_HANDLER, SIGNATURE_PROPERTIES, 
            SIGNATURE_CRYPTO, ENCRYPT_PROPERTIES, ENCRYPT_CRYPTO,
            VALIDATE_TOKEN, ENABLE_REVOCATION, ALWAYS_ENCRYPT_UT, IS_BSP_COMPLIANT, 
            ENABLE_NONCE_CACHE, ENABLE_TIMESTAMP_CACHE,
            TIMESTAMP_TTL, TIMESTAMP_FUTURE_TTL, SAML_ROLE_ATTRIBUTENAME,
            KERBEROS_CLIENT, SPNEGO_CLIENT_ACTION, KERBEROS_JAAS_CONTEXT_NAME, KERBEROS_SPN, 
            NONCE_CACHE_INSTANCE, TIMESTAMP_CACHE_INSTANCE, CACHE_CONFIG_FILE, 
            TOKEN_STORE_CACHE_INSTANCE, SUBJECT_CERT_CONSTRAINTS,
            USERNAME_TOKEN_VALIDATOR, SAML1_TOKEN_VALIDATOR, SAML2_TOKEN_VALIDATOR, 
            TIMESTAMP_TOKEN_VALIDATOR, SIGNATURE_TOKEN_VALIDATOR, BST_TOKEN_VALIDATOR, 
            SCT_TOKEN_VALIDATOR, STS_CLIENT, STS_APPLIES_TO, STS_TOKEN_USE_CERT_FOR_KEYINFO,
            STS_TOKEN_DO_CANCEL, CACHE_ISSUED_TOKEN_IN_ENDPOINT,
            DISABLE_STS_CLIENT_WSMEX_CALL_USING_EPR_ADDRESS, STS_TOKEN_CRYPTO,
            STS_TOKEN_PROPERTIES, STS_TOKEN_USERNAME, STS_TOKEN_ACT_AS, STS_TOKEN_ON_BEHALF_OF,
            TOKEN, TOKEN_ID, SUBJECT_ROLE_CLASSIFIER, SUBJECT_ROLE_CLASSIFIER_TYPE, MUST_UNDERSTAND,
            ASYMMETRIC_SIGNATURE_ALGORITHM, PASSWORD_ENCRYPTOR_INSTANCE, ENABLE_SAML_ONE_TIME_USE_CACHE,
            SAML_ONE_TIME_USE_CACHE_INSTANCE, ENABLE_STREAMING_SECURITY, RETURN_SECURITY_ERROR,
            CACHE_IDENTIFIER, CACHE_ISSUED_TOKEN_IN_ENDPOINT, PREFER_WSMEX_OVER_STS_CLIENT_CONFIG,
            DELEGATED_CREDENTIAL, KERBEROS_USE_CREDENTIAL_DELEGATION, 
            KERBEROS_IS_USERNAME_IN_SERVICENAME_FORM, STS_TOKEN_IMMINENT_EXPIRY_VALUE,
            KERBEROS_REQUEST_CREDENTIAL_DELEGATION, ENABLE_UNSIGNED_SAML_ASSERTION_PRINCIPAL,
            AUDIENCE_RESTRICTION_VALIDATION
        }));
        ALL_PROPERTIES = Collections.unmodifiableSet(s);
    }
    
    private SecurityConstants() {
        //utility class
    }
}
