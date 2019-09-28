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

package org.apache.cxf.sts;

public final class STSConstants {

    /**
     * WS-Trust 1.3 namespace
     */
    public static final String WST_NS_05_12 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512";

    /**
     * WS-Trust 1.4 namespace
     */
    public static final String WST_NS_08_02 = "http://docs.oasis-open.org/ws-sx/ws-trust/200802";

    /**
     * Identity namespace
     */
    public static final String IDT_NS_05_05 = "http://schemas.xmlsoap.org/ws/2005/05/identity";

    /**
     * WS-Security extension namespace
     */
    public static final String WSSE_EXT_04_01 =
        "http://www.docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

    /**
     * WS-Security utility namespace
     */
    public static final String WS_UTIL_03_06 = "http://schemas.xmlsoap.org/ws/2003/06/utility";

    /**
     * Asymmetric key type (attribute of BinarySecret)
     */
    public static final String ASYMMETRIC_KEY_TYPE = WST_NS_05_12 + "/AsymmetricKey";

    /**
     * Symmetric key type (attribute of BinarySecret)
     */
    public static final String SYMMETRIC_KEY_TYPE = WST_NS_05_12 + "/SymmetricKey";

    /**
     * Nonce key type (attribute of BinarySecret)
     */
    public static final String NONCE_TYPE = WST_NS_05_12 + "/Nonce";

    /**
     * WS-Policy namespace
     */
    public static final String WSP_NS = "http://www.w3.org/ns/ws-policy";

    /**
     * WS-Policy 2004 namespace
     */
    public static final String WSP_NS_04 = "http://schemas.xmlsoap.org/ws/2004/09/policy";

    /**
     * WS-Policy 2006 namespace (deprecated)
     */
    public static final String WSP_NS_06 = "http://www.w3.org/2006/07/ws-policy";

    /**
     * WS-Addressing 2005 namespace
     */
    public static final String WSA_NS_05 = "http://www.w3.org/2005/08/addressing";

    /**
     * Symmetric key (KeyType value)
     */
    public static final String SYMMETRIC_KEY_KEYTYPE = WST_NS_05_12 + "/SymmetricKey";

    /**
     * Public key (KeyType value)
     */
    public static final String PUBLIC_KEY_KEYTYPE = WST_NS_05_12 + "/PublicKey";

    /**
     * Bearer key (KeyType value)
     */
    public static final String BEARER_KEY_KEYTYPE = WST_NS_05_12 + "/Bearer";

    /**
     * ComputedKey P-SHA1 URI
     */
    public static final String COMPUTED_KEY_PSHA1 = WST_NS_05_12 + "/CK/PSHA1";

    /**
     * Status TokenType
     */
    public static final String STATUS = WST_NS_05_12 + "/RSTR/Status";

    /**
     * Valid Status Code
     */
    public static final String VALID_CODE = WST_NS_05_12 + "/status/valid";

    /**
     * Invalid Status Code
     */
    public static final String INVALID_CODE = WST_NS_05_12 + "/status/invalid";

    /**
     * Valid Status Reason
     */
    public static final String VALID_REASON =
        "The Trust service successfully validated the input";

    /**
     * Invalid Status Reason
     */
    public static final String INVALID_REASON =
        "The Trust service did not successfully validate the input";

    /**
     * Constant to store the realms in cached Security Token properties.
     */
    public static final String TOKEN_REALM = "org.apache.cxf.sts.token.realm";

    /**
     * Constant to store whether the token is allowed to be renewed or not in the cached Security
     * Token properties.
     */
    public static final String TOKEN_RENEWING_ALLOW = "org.apache.cxf.sts.token.renewing.allow";

    /**
     * Constant to store whether the token is allowed to be renewed after it has expired or not
     * in the cached Security Token properties.
     */
    public static final String TOKEN_RENEWING_ALLOW_AFTER_EXPIRY =
        "org.apache.cxf.sts.token.renewing.allow.after.expiry";

    /**
     * Constant to specify service endpoint as certificate alias for encryption.
     * Constant is recognized by STS encryption alias is replaced with AppliesTo() address.
     * This address will be used in WSS4J crypto to search service certificate
     */
    public static final String USE_ENDPOINT_AS_CERT_ALIAS =
        "useEndpointAsCertAlias";

    private STSConstants() {
        // complete
    }

}