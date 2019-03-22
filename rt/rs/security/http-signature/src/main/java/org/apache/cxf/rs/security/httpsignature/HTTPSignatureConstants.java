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

package org.apache.cxf.rs.security.httpsignature;

/**
 * Some security constants to be used with HTTP Signature. Note that some of the configuration tags are
 * shared with some of the JOSEConstants, meaning we could consolidate these in the future.
 */
public final class HTTPSignatureConstants {

    /**
     * The keystore type. It defaults to "JKS".
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
     * The keystore alias corresponding to the key to use.
     */
    public static final String RSSEC_KEY_STORE_ALIAS = "rs.security.keystore.alias";

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
     * The signature algorithm to use. The default algorithm if not specified is "rsa-sha256".
     */
    public static final String RSSEC_SIGNATURE_ALGORITHM = "rs.security.signature.algorithm";

    /**
     * The signature properties file for signature creation. If not specified then it falls back to
     * RSSEC_SIGNATURE_PROPS.
     */
    public static final String RSSEC_SIGNATURE_OUT_PROPS = "rs.security.signature.out.properties";

    /**
     * The signature properties file for signature verification. If not specified then it falls back to
     * RSSEC_SIGNATURE_PROPS.
     */
    public static final String RSSEC_SIGNATURE_IN_PROPS = "rs.security.signature.in.properties";

    /**
     * The signature properties file for signature creation/verification.
     */
    public static final String RSSEC_SIGNATURE_PROPS = "rs.security.signature.properties";


    private HTTPSignatureConstants() {
        // complete
    }
}
