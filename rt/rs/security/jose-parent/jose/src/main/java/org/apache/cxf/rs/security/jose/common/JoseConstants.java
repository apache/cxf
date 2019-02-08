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
    // Shared configuration
    //

    /**
     * The keystore type. Suitable values are "jks" or "jwk". The default value is "jwk".
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
     * The keystore alias corresponding to the key to use. You can append one of the following to this tag to
     * get the alias for more specific operations:
     *  - jwe.out
     *  - jwe.in
     *  - jws.out
     *  - jws.in
     */
    public static final String RSSEC_KEY_STORE_ALIAS = "rs.security.keystore.alias";

    /**
     * The keystore aliases corresponding to the keys to use, when using the JSON serialization form. You can
     * append one of the following to this tag to get the alias for more specific operations:
     *  - jws.out
     *  - jws.in
     */
    public static final String RSSEC_KEY_STORE_ALIASES = "rs.security.keystore.aliases";

    /**
     * The path to the keystore file.
     */
    public static final String RSSEC_KEY_STORE_FILE = "rs.security.keystore.file";

    /**
     * The KeyStore Object.
     */
    public static final String RSSEC_KEY_STORE = "rs.security.keystore";

    /**
     * A reference to a PrivateKeyPasswordProvider instance used to retrieve passwords to access keys.
     */
    public static final String RSSEC_KEY_PSWD_PROVIDER = "rs.security.key.password.provider";

    /**
     * Whether to allow using a JWK received in the header for signature validation. The default
     * is "false".
     */
    public static final String RSSEC_ACCEPT_PUBLIC_KEY = "rs.security.accept.public.key";

    /**
     * TODO documentation for these
     */
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
     * The EC Curve to use with EC keys loaded from Java Key Store.
     * JWK EC Keys are expected to use a standard "crv" property instead.
     */
    public static final String RSSEC_EC_CURVE = "rs.security.elliptic.curve";

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
     * Include the JWK public key for signature in the "jwk" header.
     */
    public static final String RSSEC_SIGNATURE_INCLUDE_PUBLIC_KEY = "rs.security.signature.include.public.key";

    /**
     * Include the X.509 certificate for signature in the "x5c" header.
     */
    public static final String RSSEC_SIGNATURE_INCLUDE_CERT = "rs.security.signature.include.cert";

    /**
     * Include the JWK key id for signature in the "kid" header.
     */
    public static final String RSSEC_SIGNATURE_INCLUDE_KEY_ID = "rs.security.signature.include.key.id";

    /**
     * Include the X.509 certificate SHA-1 digest for signature in the "x5t" header.
     */
    public static final String RSSEC_SIGNATURE_INCLUDE_CERT_SHA1 = "rs.security.signature.include.cert.sha1";

    /**
     * Include the X.509 certificate SHA-256 digest for signature in the "x5t#S256" header.
     */
    public static final String RSSEC_SIGNATURE_INCLUDE_CERT_SHA256 = "rs.security.signature.include.cert.sha256";

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
     * The encryption key algorithm to use. The default algorithm if not specified is 'RSA-OAEP' if the key is an
     * RSA key, and 'A128GCMKW' if it is an octet sequence.
     */
    public static final String RSSEC_ENCRYPTION_KEY_ALGORITHM = "rs.security.encryption.key.algorithm";

    /**
     * The encryption zip algorithm to use.
     */
    public static final String RSSEC_ENCRYPTION_ZIP_ALGORITHM = "rs.security.encryption.zip.algorithm";

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
     * Include the JWK public key for encryption in the "jwk" header.
     */
    public static final String RSSEC_ENCRYPTION_INCLUDE_PUBLIC_KEY = "rs.security.encryption.include.public.key";

    /**
     * Include the X.509 certificate for encryption the "x5c" header.
     */
    public static final String RSSEC_ENCRYPTION_INCLUDE_CERT = "rs.security.encryption.include.cert";

    /**
     * Include the JWK key id for encryption in the "kid" header.
     */
    public static final String RSSEC_ENCRYPTION_INCLUDE_KEY_ID = "rs.security.encryption.include.key.id";

    /**
     * Include the X.509 certificate SHA-1 digest for encryption in the "x5t" header.
     */
    public static final String RSSEC_ENCRYPTION_INCLUDE_CERT_SHA1 = "rs.security.encryption.include.cert.sha1";

    /**
     * Include the X.509 certificate SHA-256 digest for encryption in the "x5t#S256" header.
     */
    public static final String RSSEC_ENCRYPTION_INCLUDE_CERT_SHA256 = "rs.security.encryption.include.cert.sha256";

    /**
     * The value to be used for the "p2c" (PBES2 count) Header Parameter. The default is 4096.
     */
    public static final String RSSEC_ENCRYPTION_PBES2_COUNT = "rs.security.encryption.pbes2.count";

    //
    // JWT specific configuration
    //

    /**
     * Whether to allow unsigned JWT tokens as SecurityContext Principals. The default is false.
     */
    public static final String ENABLE_UNSIGNED_JWT_PRINCIPAL = "rs.security.enable.unsigned-jwt.principal";

    /**
     * Whether to trace JOSE headers.
     */
    public static final String JOSE_DEBUG = "jose.debug";

    private JoseConstants() {

    }
}
