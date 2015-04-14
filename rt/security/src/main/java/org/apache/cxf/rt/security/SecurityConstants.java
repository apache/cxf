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

package org.apache.cxf.rt.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class contains some configuration tags that can be used to configure various security properties. These
 * tags are shared between the SOAP stack (WS-SecurityPolicy configuration), as well as the REST stack (JAX-RS
 * XML Security). 
 * 
 * The configuration tags largely relate to properties for signing, encryption as well as SAML tokens. Most of
 * the signing/encryption tags refer to Apache WSS4J "Crypto" objects, which are used by both stacks to control
 * how certificates/keys are retrieved, etc.
 * 
 * More specific configuration tags for WS-SecurityPolicy are configured in the SecurityConstants 
 * class in the cxf-rt-ws-security module, which extends this class.
 */
public class SecurityConstants {
    
    //
    // User properties
    //
    
    /**
     * The user's name. It is used as follows:
     * a) As the name in the UsernameToken for WS-Security.
     * b) As the alias name in the keystore to get the user's cert and private key for signature
     *    if {@link SIGNATURE_USERNAME} is not set.
     * c) As the alias name in the keystore to get the user's public key for encryption if 
     *    {@link ENCRYPT_USERNAME} is not set.
     */
    public static final String USERNAME = "security.username";
    
    /**
     * The user's password when a {@link CALLBACK_HANDLER} is not defined.
     */
    public static final String PASSWORD = "security.password";
    
    /**
     * The user's name for signature. It is used as the alias name in the keystore to get the user's cert 
     * and private key for signature. If this is not defined, then {@link USERNAME} is used instead. If 
     * that is also not specified, it uses the the default alias set in the properties file referenced by
     * {@link SIGNATURE_PROPERTIES}. If that's also not set, and the keystore only contains a single key, 
     * that key will be used. 
     */
    public static final String SIGNATURE_USERNAME = "security.signature.username";
    
    /**
     * The user's name for encryption. It is used as the alias name in the keystore to get the user's public 
     * key for encryption. If this is not defined, then {@link USERNAME} is used instead. If 
     * that is also not specified, it uses the the default alias set in the properties file referenced by
     * {@link ENCRYPT_PROPERTIES}. If that's also not set, and the keystore only contains a single key, 
     * that key will be used.
     * 
     * For the WS-Security web service provider, the "useReqSigCert" keyword can be used to accept (encrypt to) 
     * any client whose public key is in the service's truststore (defined in {@link ENCRYPT_PROPERTIES}).
     */
    public static final String ENCRYPT_USERNAME = "security.encryption.username";
    
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
    public static final String CALLBACK_HANDLER = "security.callback-handler";
    
    /**
     * The SAML CallbackHandler implementation class used to construct SAML Assertions. The value of this 
     * tag must be either:
     * a) The class name of a {@link javax.security.auth.callback.CallbackHandler} instance, which must
     * be accessible via the classpath.
     * b) A {@link javax.security.auth.callback.CallbackHandler} instance.
     */
    public static final String SAML_CALLBACK_HANDLER = "security.saml-callback-handler";
    
    /**
     * The Crypto property configuration to use for signature, if {@link SIGNATURE_CRYPTO} is not set instead.
     * The value of this tag must be either:
     * a) A Java Properties object that contains the Crypto configuration.
     * b) The path of the Crypto property file that contains the Crypto configuration.
     * c) A URL that points to the Crypto property file that contains the Crypto configuration.
     */
    public static final String SIGNATURE_PROPERTIES = "security.signature.properties";
    
    /**
     * The Crypto property configuration to use for encryption, if {@link ENCRYPT_CRYPTO} is not set instead.
     * The value of this tag must be either:
     * a) A Java Properties object that contains the Crypto configuration.
     * b) The path of the Crypto property file that contains the Crypto configuration.
     * c) A URL that points to the Crypto property file that contains the Crypto configuration.
     */
    public static final String ENCRYPT_PROPERTIES = "security.encryption.properties";
    
    /**
     * A Crypto object to be used for signature. If this is not defined then the 
     * {@link SIGNATURE_PROPERTIES} is used instead.
     */
    public static final String SIGNATURE_CRYPTO = "security.signature.crypto";
    
    /**
     * A Crypto object to be used for encryption. If this is not defined then the 
     * {@link ENCRYPT_PROPERTIES} is used instead.
     */
    public static final String ENCRYPT_CRYPTO = "security.encryption.crypto";
    
    /**
     * A message property for prepared X509 certificate to be used for encryption. 
     * If this is not defined, then the certificate will be either loaded from the 
     * keystore {@link ENCRYPT_PROPERTIES} or extracted from request (when WS-Security is used and
     * if {@link ENCRYPT_USERNAME} has value "useReqSigCert").
     */
    public static final String ENCRYPT_CERT = "security.encryption.certificate";
    
    //
    // Boolean Security configuration tags, e.g. the value should be "true" or "false".
    //
    
    /**
     * Whether to enable Certificate Revocation List (CRL) checking or not when verifying trust 
     * in a certificate. The default value is "false".
     */
    public static final String ENABLE_REVOCATION = "security.enableRevocation";
    
    /**
     * Whether to allow unsigned saml assertions as SecurityContext Principals. The default is false.
     */
    public static final String ENABLE_UNSIGNED_SAML_ASSERTION_PRINCIPAL = 
            "security.enable.unsigned-saml-assertion.principal";
    
    /**
     * Whether to validate the SubjectConfirmation requirements of a received SAML Token
     * (sender-vouches or holder-of-key). The default is true.
     */
    public static final String VALIDATE_SAML_SUBJECT_CONFIRMATION = 
        "security.validate.saml.subject.conf";
    
    /**
     * Set this to "false" if security context must not be created from JAAS Subject.
     *
     * The default value is "true".
     */
    public static final String SC_FROM_JAAS_SUBJECT = "security.sc.jaas-subject";
    
    /**
     * Enable SAML AudienceRestriction validation. If this is set to "true", then IF the
     * SAML Token contains Audience Restriction URIs, one of them must match either the
     * request URL or the Service QName. The default is "true".
     */
    public static final String AUDIENCE_RESTRICTION_VALIDATION = "security.validate.audience-restriction";
    
    //
    // Non-boolean WS-Security Configuration parameters
    //
    
    /**
     * The attribute URI of the SAML AttributeStatement where the role information is stored.
     * The default is "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role".
     */
    public static final String SAML_ROLE_ATTRIBUTENAME = "security.saml-role-attributename";
    
    /**
     * A comma separated String of regular expressions which will be applied to the subject DN of 
     * the certificate used for signature validation, after trust verification of the certificate 
     * chain associated with the  certificate.
     */
    public static final String SUBJECT_CERT_CONSTRAINTS = "security.subject.cert.constraints";
    
    public static final Set<String> COMMON_PROPERTIES;
    
    static {
        Set<String> s = new HashSet<>(Arrays.asList(new String[] {
            USERNAME, PASSWORD, SIGNATURE_USERNAME, ENCRYPT_USERNAME,
            CALLBACK_HANDLER, SAML_CALLBACK_HANDLER, SIGNATURE_PROPERTIES, 
            SIGNATURE_CRYPTO, ENCRYPT_PROPERTIES, ENCRYPT_CRYPTO,
            ENABLE_REVOCATION, SUBJECT_CERT_CONSTRAINTS, ENABLE_UNSIGNED_SAML_ASSERTION_PRINCIPAL,
            AUDIENCE_RESTRICTION_VALIDATION
        }));
        COMMON_PROPERTIES = Collections.unmodifiableSet(s);
    }
    
    protected SecurityConstants() {
        // complete
    }
}
