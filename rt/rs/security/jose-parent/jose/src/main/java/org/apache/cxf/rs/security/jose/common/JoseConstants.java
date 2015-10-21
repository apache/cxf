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

package org.apache.cxf.rs.security.jose.common;

public final class JoseConstants {
    public static final String HEADER_TYPE = "typ";
    public static final String HEADER_ALGORITHM = "alg";
    public static final String HEADER_CONTENT_TYPE = "cty";
    public static final String HEADER_CRITICAL = "crit";
    
    public static final String HEADER_KEY_ID = "kid";
    public static final String HEADER_X509_URL = "x5u";
    public static final String HEADER_X509_CHAIN = "x5c";
    public static final String HEADER_X509_THUMBPRINT = "x5t";
    public static final String HEADER_X509_THUMBPRINT_SHA256 = "x5t#S256";
    public static final String HEADER_JSON_WEB_KEY = "jwk";
    public static final String HEADER_JSON_WEB_KEY_SET = "jku";
    
    public static final String JWE_HEADER_KEY_ENC_ALGORITHM = HEADER_ALGORITHM;
    public static final String JWE_HEADER_CONTENT_ENC_ALGORITHM = "enc";
    public static final String JWE_HEADER_ZIP_ALGORITHM = "zip";
    public static final String JWE_DEFLATE_ZIP_ALGORITHM = "DEF";
    
    public static final String JWS_HEADER_B64_STATUS_HEADER = "b64";
    
    public static final String TYPE_JWT = "JWT";
    public static final String TYPE_JOSE = "JOSE";
    public static final String TYPE_JOSE_JSON = "JOSE+JSON";
    public static final String MEDIA_TYPE_JOSE = "application/jose";
    public static final String MEDIA_TYPE_JOSE_JSON = "application/jose+json";
    
    public static final String JOSE_CONTEXT_PROPERTY = "org.apache.cxf.jose.context";
    
    //
    // JOSE Configuration constants
    //
    
    // 
    // Shared Keys/keystore configuration
    //
    
    /**
     * The keystore type. Suitable values are "jks" or "jwk".
     */
    public static final String RSSEC_KEY_STORE_TYPE = "rs.security.keystore.type";
    
    /**
     * The password required to access the keystore.
     */
    public static final String RSSEC_KEY_STORE_PSWD = "rs.security.keystore.password";
    
    /**
     * The password required to access the private key (in the keystore).
     */
    public static final String RSSEC_KEY_PSWD = "rs.security.key.password";
    
    /**
     * The keystore alias corresponding to the key to use
     */
    public static final String RSSEC_KEY_STORE_ALIAS = "rs.security.keystore.alias";
    
    /**
     * The keystore aliases corresponding to the keys to use, when using the JSON serialization form.
     */
    public static final String RSSEC_KEY_STORE_ALIASES = "rs.security.keystore.aliases";
    
    /**
     * The path to the keystore file.
     */
    public static final String RSSEC_KEY_STORE_FILE = "rs.security.keystore.file";
    
    /**
     * A reference to a PrivateKeyPasswordProvider instance used to retrieve passwords to access keys.
     */
    public static final String RSSEC_KEY_PSWD_PROVIDER = "rs.security.key.password.provider";
    
    /**
     * TODO documentation for these
     */
    public static final String RSSEC_REPORT_KEY_PROP = "rs.security.report.public.key";
    public static final String RSSEC_REPORT_KEY_ID_PROP = "rs.security.report.public.key.id";
    public static final String RSSEC_ACCEPT_PUBLIC_KEY_PROP = "rs.security.accept.public.key.properties";
    public static final String RSSEC_KEY_STORE_JWKSET = "rs.security.keystore.jwkset";
    public static final String RSSEC_KEY_STORE_JWKKEY = "rs.security.keystore.jwkkey";
    
    //
    // JWS specific Configuration
    //
    
    /**
     * A reference to a PrivateKeyPasswordProvider instance used to retrieve passwords to access keys
     * for signature. If this is not specified it falls back to use the RSSEC_KEY_PSWD_PROVIDER.
     */
    public static final String RSSEC_SIGNATURE_KEY_PSWD_PROVIDER = "rs.security.signature.key.password.provider";
    
    /**
     * The signature algorithm to use. The default algorithm if not specified is 'RS256'.
     */
    public static final String RSSEC_SIGNATURE_ALGORITHM = "rs.security.signature.algorithm";
    
    /**
     * The OLD signature algorithm identifier. Use RSSEC_SIGNATURE_ALGORITHM instead.
     */
    @Deprecated
    public static final String DEPR_RSSEC_SIGNATURE_ALGORITHM = "rs.security.jws.content.signature.algorithm";
    
    /**
     * The signature properties file for compact signature creation. If not specified then it falls back to 
     * RSSEC_SIGNATURE_PROPS.
     */
    public static final String RSSEC_SIGNATURE_OUT_PROPS = "rs.security.signature.out.properties";
    
    /**
     * The signature properties file for compact signature verification. If not specified then it falls back to 
     * RSSEC_SIGNATURE_PROPS.
     */
    public static final String RSSEC_SIGNATURE_IN_PROPS = "rs.security.signature.in.properties";
    
    /**
     * The signature properties file for compact signature creation/verification.
     */
    public static final String RSSEC_SIGNATURE_PROPS = "rs.security.signature.properties";
    
    /**
     * The signature properties file for JSON Serialization signature creation. If not specified then it 
     * falls back to RSSEC_SIGNATURE_LIST_PROPS.
     */
    public static final String RSSEC_SIGNATURE_LIST_OUT_PROPS = "rs.security.signature.list.out.properties";
    
    /**
     * The signature properties file for JSON Serialization signature verification. If not specified then it 
     * falls back to RSSEC_SIGNATURE_LIST_PROPS.
     */
    public static final String RSSEC_SIGNATURE_LIST_IN_PROPS = "rs.security.signature.list.in.properties";
    
    /**
     * The signature properties file for JSON Serialization signature creation/verification.
     */
    public static final String RSSEC_SIGNATURE_LIST_PROPS = "rs.security.signature.list.properties";
    
    /**
     * TODO documentation for these
     */
    public static final String RSSEC_SIGNATURE_REPORT_KEY_PROP = "rs.security.signature.report.public.key";
    public static final String RSSEC_SIGNATURE_REPORT_KEY_ID_PROP = "rs.security.signature.report.public.key.id";
    
    //
    // JWE specific Configuration
    //
    
    /**
     * A reference to a PrivateKeyPasswordProvider instance used to retrieve passwords to access keys
     * for decryption. If this is not specified it falls back to use the RSSEC_KEY_PSWD_PROVIDER.
     */
    public static final String RSSEC_DECRYPTION_KEY_PSWD_PROVIDER = "rs.security.decryption.key.password.provider";
    
    /**
     * The encryption content algorithm to use. The default algorithm if not specified is 'A128GCM'.
     */
    public static final String RSSEC_ENCRYPTION_CONTENT_ALGORITHM = "rs.security.encryption.content.algorithm";
    
    /**
     * The OLD encryption content algorithm to use. Use RSSEC_ENCRYPTION_CONTENT_ALGORITHM instead.
     */
    @Deprecated
    public static final String DEPR_RSSEC_ENCRYPTION_CONTENT_ALGORITHM = 
        "rs.security.jwe.content.encryption.algorithm";
    
    /**
     * The encryption key algorithm to use. The default algorithm if not specified is 'RSA-OAEP' if the key is an
     * RSA key, and 'A128GCMKW' if it is an octet sequence.
     */
    public static final String RSSEC_ENCRYPTION_KEY_ALGORITHM = "rs.security.encryption.key.algorithm";
    
    /**
     * The OLD encryption key algorithm to use. Use RSSEC_ENCRYPTION_KEY_ALGORITHM instead.
     */
    @Deprecated
    public static final String DEPR_RSSEC_ENCRYPTION_KEY_ALGORITHM = "rs.security.jwe.key.encryption.algorithm";
    
    /**
     * The encryption zip algorithm to use.
     */
    public static final String RSSEC_ENCRYPTION_ZIP_ALGORITHM = "rs.security.encryption.zip.algorithm";
    
    /**
     * The OLD encryption zip algorithm to use. Use RSSEC_ENCRYPTION_ZIP_ALGORITHM instead.
     */
    @Deprecated
    public static final String DEPR_RSSEC_ENCRYPTION_ZIP_ALGORITHM = "rs.security.jwe.zip.algorithm";
    
    /**
     * The encryption properties file for encryption creation. If not specified then it falls back to 
     * RSSEC_ENCRYPTION_PROPS.
     */
    public static final String RSSEC_ENCRYPTION_OUT_PROPS = "rs.security.encryption.out.properties";
    
    /**
     * The decryption properties file for decryption. If not specified then it falls back to 
     * RSSEC_ENCRYPTION_PROPS.
     */
    public static final String RSSEC_ENCRYPTION_IN_PROPS = "rs.security.encryption.in.properties";
    
    /**
     * The encryption/decryption properties file
     */
    public static final String RSSEC_ENCRYPTION_PROPS = "rs.security.encryption.properties";
    
    /**
     * TODO documentation for these
     */
    public static final String RSSEC_ENCRYPTION_REPORT_KEY_PROP = "rs.security.encryption.report.public.key";
    public static final String RSSEC_ENCRYPTION_REPORT_KEY_ID_PROP = "rs.security.encryption.report.public.key.id";
    
    //
    // JWT specific configuration
    //
    
    /**
     * Whether to allow unsigned JWT tokens as SecurityContext Principals. The default is false.
     */
    public static final String ENABLE_UNSIGNED_JWT_PRINCIPAL = "rs.security.enable.unsigned-jwt.principal";
    
    
    
    private JoseConstants() {
        
    }
}
