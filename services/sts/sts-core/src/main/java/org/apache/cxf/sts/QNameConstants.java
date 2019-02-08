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

import javax.xml.namespace.QName;

/**
 * This class holds a collection of QName constants.
 */
public final class QNameConstants {

    public static final org.apache.cxf.ws.security.sts.provider.model.ObjectFactory WS_TRUST_FACTORY
        = new org.apache.cxf.ws.security.sts.provider.model.ObjectFactory();
    public static final org.apache.cxf.ws.security.sts.provider.model.wstrust14.ObjectFactory WS_TRUST14_FACTORY
        = new org.apache.cxf.ws.security.sts.provider.model.wstrust14.ObjectFactory();
    public static final org.apache.cxf.ws.security.sts.provider.model.secext.ObjectFactory WSSE_FACTORY
        = new org.apache.cxf.ws.security.sts.provider.model.secext.ObjectFactory();
    public static final org.apache.cxf.ws.security.sts.provider.model.utility.ObjectFactory UTIL_FACTORY
        = new org.apache.cxf.ws.security.sts.provider.model.utility.ObjectFactory();

    //
    // Token Requirement QNames
    //
    public static final QName TOKEN_TYPE =
        WS_TRUST_FACTORY.createTokenType("").getName();
    public static final QName ENTROPY =
        WS_TRUST_FACTORY.createEntropy(null).getName();
    public static final QName BINARY_SECRET =
        WS_TRUST_FACTORY.createBinarySecret(null).getName();
    public static final QName ON_BEHALF_OF =
        WS_TRUST_FACTORY.createOnBehalfOf(null).getName();
    public static final QName VALIDATE_TARGET =
        WS_TRUST_FACTORY.createValidateTarget(null).getName();
    public static final QName CANCEL_TARGET =
        WS_TRUST_FACTORY.createCancelTarget(null).getName();
    public static final QName RENEW_TARGET =
        WS_TRUST_FACTORY.createRenewTarget(null).getName();
    public static final QName LIFETIME =
        WS_TRUST_FACTORY.createLifetime(null).getName();
    public static final QName REQUEST_TYPE =
        WS_TRUST_FACTORY.createRequestType("").getName();
    public static final QName CLAIMS =
        WS_TRUST_FACTORY.createClaims(null).getName();
    public static final QName RENEWING =
        WS_TRUST_FACTORY.createRenewing(null).getName();
    public static final QName PARTICIPANTS =
        WS_TRUST_FACTORY.createParticipants(null).getName();

    //
    // Key Requirement QNames
    //
    public static final QName AUTHENTICATION_TYPE =
        WS_TRUST_FACTORY.createAuthenticationType("").getName();
    public static final QName KEY_TYPE =
        WS_TRUST_FACTORY.createKeyType("").getName();
    public static final QName KEY_SIZE =
        WS_TRUST_FACTORY.createKeySize(0L).getName();
    public static final QName SIGNATURE_ALGORITHM =
        WS_TRUST_FACTORY.createSignatureAlgorithm("").getName();
    public static final QName ENCRYPTION_ALGORITHM =
        WS_TRUST_FACTORY.createEncryptionAlgorithm("").getName();
    public static final QName C14N_ALGORITHM =
        WS_TRUST_FACTORY.createCanonicalizationAlgorithm("").getName();
    public static final QName COMPUTED_KEY_ALGORITHM =
        WS_TRUST_FACTORY.createComputedKeyAlgorithm("").getName();
    public static final QName KEYWRAP_ALGORITHM =
        WS_TRUST_FACTORY.createKeyWrapAlgorithm("").getName();
    public static final QName USE_KEY =
        WS_TRUST_FACTORY.createUseKey(null).getName();
    public static final QName SIGN_WITH =
        WS_TRUST_FACTORY.createSignWith(null).getName();
    public static final QName ENCRYPT_WITH =
        WS_TRUST_FACTORY.createEncryptWith(null).getName();

    //
    // WSSE QNames
    //
    public static final QName USERNAME_TOKEN =
        WSSE_FACTORY.createUsernameToken(null).getName();
    public static final QName BINARY_SECURITY_TOKEN =
        WSSE_FACTORY.createBinarySecurityToken(null).getName();
    public static final QName PASSWORD =
        QNameConstants.WSSE_FACTORY.createPassword(null).getName();
    public static final QName NONCE =
        QNameConstants.WSSE_FACTORY.createNonce(null).getName();
    public static final QName SECURITY_TOKEN_REFERENCE =
        WSSE_FACTORY.createSecurityTokenReference(null).getName();
    public static final QName SECURITY =
        QNameConstants.WSSE_FACTORY.createSecurity(null).getName();


    //
    // WSTrust 1.4 QNames
    //
    public static final QName ACT_AS =
        WS_TRUST14_FACTORY.createActAs(null).getName();

    private QNameConstants() {
        //
    }

}
