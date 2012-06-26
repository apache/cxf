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
 * 
 */
public final class SecurityConstants {
    public static final String USERNAME = "ws-security.username";
    public static final String PASSWORD = "ws-security.password";
    public static final String VALIDATE_TOKEN = "ws-security.validate.token";
    public static final String USERNAME_TOKEN_VALIDATOR = "ws-security.ut.validator";
    public static final String SAML1_TOKEN_VALIDATOR = "ws-security.saml1.validator";
    public static final String SAML2_TOKEN_VALIDATOR = "ws-security.saml2.validator";
    public static final String TIMESTAMP_TOKEN_VALIDATOR = "ws-security.timestamp.validator";
    public static final String SIGNATURE_TOKEN_VALIDATOR = "ws-security.signature.validator";
    public static final String BST_TOKEN_VALIDATOR = "ws-security.bst.validator";
    public static final String SCT_TOKEN_VALIDATOR = "ws-security.sct.validator";
    
    public static final String CALLBACK_HANDLER = "ws-security.callback-handler";
    public static final String SAML_CALLBACK_HANDLER = "ws-security.saml-callback-handler";
    
    public static final String SIGNATURE_USERNAME = "ws-security.signature.username";
    public static final String SIGNATURE_PROPERTIES = "ws-security.signature.properties";
    
    public static final String ENCRYPT_USERNAME = "ws-security.encryption.username";
    public static final String ENCRYPT_PROPERTIES = "ws-security.encryption.properties";
    
    public static final String SIGNATURE_CRYPTO = "ws-security.signature.crypto";
    public static final String ENCRYPT_CRYPTO = "ws-security.encryption.crypto";
    

    public static final String TOKEN = "ws-security.token";
    public static final String TOKEN_ID = "ws-security.token.id";

    public static final String STS_CLIENT = "ws-security.sts.client";
    public static final String STS_APPLIES_TO = "ws-security.sts.applies-to";
    
    public static final String TIMESTAMP_TTL = "ws-security.timestamp.timeToLive";
    
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
     * This configuration tag specifies the time in seconds in the future within which
     * the Created time of an incoming Timestamp is valid. WSS4J rejects by default any
     * timestamp which is "Created" in the future, and so there could potentially be
     * problems in a scenario where a client's clock is slightly askew. The default
     * value for this parameter is "0", meaning that no future-created Timestamps are
     * allowed.
     */
    public static final String TIMESTAMP_FUTURE_TTL = "ws-security.timestamp.futureTimeToLive";
    
    /**
     * This configuration tag specifies whether to self-sign a SAML Assertion or not. If this
     * is set to true, then an enveloped signature will be generated when the SAML Assertion is
     * constructed. The default is false.
     */
    public static final String SELF_SIGN_SAML_ASSERTION = "ws-security.self-sign-saml-assertion";
    
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
    public static final String STS_TOKEN_USE_CERT_FOR_KEYINFO = 
        "ws-security.sts.token.usecert";
    
    public static final String STS_TOKEN_DO_CANCEL = "ws-security.sts.token.do.cancel";
    
    public static final String STS_TOKEN_ACT_AS = "ws-security.sts.token.act-as";
    
    public static final String STS_TOKEN_ON_BEHALF_OF = "ws-security.sts.token.on-behalf-of";
    
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
     * Set this to "false" to not cache a SecurityToken per proxy object in the 
     * IssuedTokenInterceptorProvider. This should be done if a token is being retrieved
     * from an STS in an intermediary. The default value is "true".
     */
    public static final String CACHE_ISSUED_TOKEN_IN_ENDPOINT = 
        "ws-security.cache.issued.token.in.endpoint";
    
    /**
     * Set this to "true" to cache UsernameToken nonces. The default value is "false".
     */
    public static final String ENABLE_NONCE_CACHE = 
        "ws-security.enable.nonce.cache";
    
    /**
     * This holds a reference to a ReplayCache instance used to cache UsernameToken nonces. The
     * default instance that is used is the EHCacheReplayCache.
     */
    public static final String NONCE_CACHE_INSTANCE = 
        "ws-security.nonce.cache.instance";
    
    /**
     * Set this to "true" to cache Timestamp Created Strings (these are only cached in 
     * conjunction with a message Signature). The default value is "false".
     */
    public static final String ENABLE_TIMESTAMP_CACHE = 
        "ws-security.enable.timestamp.cache";
    
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
     * This configuration tag is a comma separated String of regular expressions which
     * will be applied to the subject DN of the certificate used for signature
     * validation, after trust verification of the certificate chain associated with the 
     * certificate. These constraints are not used when the certificate is contained in
     * the keystore (direct trust).
     */
    public static final String SUBJECT_CERT_CONSTRAINTS = "ws-security.subject.cert.constraints";
    
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
            TIMESTAMP_CACHE_INSTANCE, CACHE_CONFIG_FILE, SUBJECT_CERT_CONSTRAINTS
        }));
        ALL_PROPERTIES = Collections.unmodifiableSet(s);
    }
    
    private SecurityConstants() {
        //utility class
    }
}
