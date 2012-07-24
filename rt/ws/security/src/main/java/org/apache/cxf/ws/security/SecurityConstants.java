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
 * Configuration tags used to configure the WS-SecurityPolicy layer.
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
    
    //
    // Boolean WS-Security configuration tags, e.g. the value should be "true" or "false".
    //
    
    public static final String VALIDATE_TOKEN = "ws-security.validate.token";
    
    public static final String ENABLE_REVOCATION = "ws-security.enableRevocation";
    
    //WebLogic and WCF always encrypt UsernameTokens whenever possible
    //See:  http://e-docs.bea.com/wls/docs103/webserv_intro/interop.html
    //Be default, we will encrypt as well for interop reasons.  However, this
    //setting can be set to false to turn that off. 
    public static final String ALWAYS_ENCRYPT_UT = "ws-security.username-token.always.encrypted";
    
    /**
     * Whether to ensure compliance with the Basic Security Profile (BSP) 1.1 or not. The
     * default value is "true".
     */
    public static final String IS_BSP_COMPLIANT = "ws-security.is-bsp-compliant";
    
    /**
     * This configuration tag specifies whether to self-sign a SAML Assertion or not. If this
     * is set to true, then an enveloped signature will be generated when the SAML Assertion is
     * constructed. The default is false.
     */
    public static final String SELF_SIGN_SAML_ASSERTION = "ws-security.self-sign-saml-assertion";
    
    /**
     * Set this to "false" to not cache UsernameToken nonces. The default value is "true" for
     * message recipients, and "false" for message initiators. Set it to true to cache for
     * both cases.
     */
    public static final String ENABLE_NONCE_CACHE = 
        "ws-security.enable.nonce.cache";
    
    /**
     * Set this to "false" to not cache Timestamp Created Strings (these are only cached in 
     * conjunction with a message Signature). The default value is "true" for message recipients, 
     * and "false" for message initiators. Set it to true to cache for both cases.
     */
    public static final String ENABLE_TIMESTAMP_CACHE = 
        "ws-security.enable.timestamp.cache";
    
    //
    // (Non-boolean) Configuration parameters
    //
    
    /**
     * This configuration tag specifies the time in seconds after Creation that an incoming 
     * Timestamp is valid for. The default value is 300 seconds (5 minutes).
     */
    public static final String TIMESTAMP_TTL = "ws-security.timestamp.timeToLive";
    
    /**
     * This configuration tag specifies the time in seconds in the future within which
     * the Created time of an incoming Timestamp is valid. WSS4J rejects by default any
     * timestamp which is "Created" in the future, and so there could potentially be
     * problems in a scenario where a client's clock is slightly askew. The default
     * value for this parameter is "0", meaning that no future-created Timestamps are
     * allowed.
     */
    public static final String TIMESTAMP_FUTURE_TTL = "ws-security.timestamp.futureTimeToLive";
    
    /**
     * This configuration tag specifies the attribute URI of the SAML attributestatement
     * where the role information is stored.
     * The default is "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role".
     */
    public static final String SAML_ROLE_ATTRIBUTENAME = "ws-security.saml-role-attributename";
    
    public static final String KERBEROS_CLIENT = "ws-security.kerberos.client";
    
    /**
     * The JAAS Context name to use for Kerberos. This is currently only supported for SPNEGO.
     */
    public static final String KERBEROS_JAAS_CONTEXT_NAME = "ws-security.kerberos.jaas.context";
    
    /**
     * The Kerberos Service Provider Name (spn) to use. This is currently only supported for SPNEGO.
     */
    public static final String KERBEROS_SPN = "ws-security.kerberos.spn";
    
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
     * This configuration tag is a comma separated String of regular expressions which
     * will be applied to the subject DN of the certificate used for signature
     * validation, after trust verification of the certificate chain associated with the 
     * certificate. These constraints are not used when the certificate is contained in
     * the keystore (direct trust).
     */
    public static final String SUBJECT_CERT_CONSTRAINTS = "ws-security.subject.cert.constraints";
    
    //
    // Validator implementations for validating received security tokens
    //
    
    public static final String USERNAME_TOKEN_VALIDATOR = "ws-security.ut.validator";
    public static final String SAML1_TOKEN_VALIDATOR = "ws-security.saml1.validator";
    public static final String SAML2_TOKEN_VALIDATOR = "ws-security.saml2.validator";
    public static final String TIMESTAMP_TOKEN_VALIDATOR = "ws-security.timestamp.validator";
    public static final String SIGNATURE_TOKEN_VALIDATOR = "ws-security.signature.validator";
    public static final String BST_TOKEN_VALIDATOR = "ws-security.bst.validator";
    public static final String SCT_TOKEN_VALIDATOR = "ws-security.sct.validator";
    
    //
    // STS Client Configuration tags
    //
    
    public static final String STS_CLIENT = "ws-security.sts.client";
    public static final String STS_APPLIES_TO = "ws-security.sts.applies-to";
    
    public static final String STS_TOKEN_USE_CERT_FOR_KEYINFO = 
            "ws-security.sts.token.usecert";
    
    public static final String STS_TOKEN_DO_CANCEL = "ws-security.sts.token.do.cancel";
    
    /**
     * Set this to "false" to not cache a SecurityToken per proxy object in the 
     * IssuedTokenInterceptorProvider. This should be done if a token is being retrieved
     * from an STS in an intermediary. The default value is "true".
     */
    public static final String CACHE_ISSUED_TOKEN_IN_ENDPOINT = 
        "ws-security.cache.issued.token.in.endpoint";
    
    /**
>>>>>>> 35b13a4... Merged revisions 1365097 via  git cherry-pick from
     * Set this property to avoid STS client trying send WS-MetadataExchange call using
     * STS EPR WSA address when the endpoint contract contains no WS-MetadataExchange info.
     */
    public static final String DISABLE_STS_CLIENT_WSMEX_CALL_USING_EPR_ADDRESS =
        "ws-security.sts.disable-wsmex-call-using-epr-address";
    
    /**
     * WCF's trust server sometimes will encrypt the token in the response IN ADDITION TO
     * the full security on the message. These properties control the way the STS client
     * will decrypt the EncryptedData elements in the response
     * 
     * These are also used by the STSClient to send/process any RSA/DSAKeyValue tokens 
     * used if the KeyType is "PublicKey" 
     */
    public static final String STS_TOKEN_CRYPTO = "ws-security.sts.token.crypto";
    public static final String STS_TOKEN_PROPERTIES = "ws-security.sts.token.properties";
    public static final String STS_TOKEN_USERNAME = "ws-security.sts.token.username";
    
    public static final String STS_TOKEN_ACT_AS = "ws-security.sts.token.act-as";
    
    public static final String STS_TOKEN_ON_BEHALF_OF = "ws-security.sts.token.on-behalf-of";
    
    //
    // Internal tags
    //
    
    public static final String TOKEN = "ws-security.token";
    public static final String TOKEN_ID = "ws-security.token.id";
    
    public static final Set<String> ALL_PROPERTIES;
    
    static {
        Set<String> s = new HashSet<String>(Arrays.asList(new String[] {
            USERNAME, PASSWORD, CALLBACK_HANDLER, 
            SIGNATURE_USERNAME, SIGNATURE_PROPERTIES, SIGNATURE_CRYPTO,
            ENCRYPT_USERNAME, ENCRYPT_PROPERTIES, ENCRYPT_CRYPTO,
            TOKEN, TOKEN_ID, STS_CLIENT, STS_TOKEN_PROPERTIES, STS_TOKEN_CRYPTO,
            STS_TOKEN_DO_CANCEL, TIMESTAMP_TTL, ALWAYS_ENCRYPT_UT,
            STS_TOKEN_ACT_AS, STS_TOKEN_USERNAME, STS_TOKEN_USE_CERT_FOR_KEYINFO,
            SAML1_TOKEN_VALIDATOR, SAML2_TOKEN_VALIDATOR, TIMESTAMP_TOKEN_VALIDATOR,
            SIGNATURE_TOKEN_VALIDATOR, IS_BSP_COMPLIANT, TIMESTAMP_FUTURE_TTL,
            BST_TOKEN_VALIDATOR, SAML_CALLBACK_HANDLER, STS_TOKEN_ON_BEHALF_OF,
            KERBEROS_CLIENT, SCT_TOKEN_VALIDATOR, CACHE_ISSUED_TOKEN_IN_ENDPOINT,
            KERBEROS_JAAS_CONTEXT_NAME, KERBEROS_SPN, SPNEGO_CLIENT_ACTION,
            ENABLE_NONCE_CACHE, NONCE_CACHE_INSTANCE, ENABLE_TIMESTAMP_CACHE,
            TIMESTAMP_CACHE_INSTANCE, CACHE_CONFIG_FILE,
            SAML_ROLE_ATTRIBUTENAME, DISABLE_STS_CLIENT_WSMEX_CALL_USING_EPR_ADDRESS,
            SUBJECT_CERT_CONSTRAINTS
        }));
        ALL_PROPERTIES = Collections.unmodifiableSet(s);
    }
    
    private SecurityConstants() {
        //utility class
    }
}
