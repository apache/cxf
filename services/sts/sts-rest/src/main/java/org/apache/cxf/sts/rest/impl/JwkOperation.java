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
package org.apache.cxf.sts.rest.impl;

import java.security.PublicKey;
import java.util.Properties;

import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jwk.KeyOperation;
import org.apache.cxf.rt.security.rs.RSSecurityConstants;

import static java.util.Optional.ofNullable;
import static org.apache.cxf.rs.security.jose.jwk.JwkUtils.fromPublicKey;
import static org.apache.cxf.rs.security.jose.jws.JwsUtils.loadPublicVerificationKeys;

public final class JwkOperation {

    private static final String[] RS_SECURITY_PROPERTIES = {
        RSSecurityConstants.RSSEC_KEY_STORE_TYPE,
        RSSecurityConstants.RSSEC_KEY_STORE_ALIAS,
        RSSecurityConstants.RSSEC_KEY_STORE_PSWD,
        RSSecurityConstants.RSSEC_KEY_STORE_FILE,
        RSSecurityConstants.RSSEC_SIGNATURE_ALGORITHM};

    private JwkOperation() {
    }

    public static JsonWebKeys loadPublicKeys(final Message message) {
        final Properties properties = getRsProperties(message);
        final String storeType = properties.getProperty(RSSecurityConstants.RSSEC_KEY_STORE_TYPE);
        if (JoseConstants.HEADER_JSON_WEB_KEY.equals(storeType)) {
            return loadPublicJwk(message, properties);
        } else {
            return loadPublicVerificationKeys(message, properties);
        }
    }

    private static JsonWebKeys loadPublicJwk(final Message message, final Properties properties) {
        final JsonWebKey jsonWebKey = JwkUtils.loadJsonWebKey(message, properties, KeyOperation.VERIFY,
            (String) message.get(RealmSecurityConfigurationFilter.REALM_NAME_PARAM));
        final String keyType = ofNullable(jsonWebKey).map(jwk -> jwk.getKeyType().name()).orElse(null);
        PublicKey publicKey = null;
        if (JsonWebKey.KEY_TYPE_RSA.equalsIgnoreCase(keyType)) {
            publicKey = JwkUtils.toRSAPublicKey(jsonWebKey);
        } else if (JsonWebKey.KEY_TYPE_ELLIPTIC.equalsIgnoreCase(keyType)) {
            publicKey = JwkUtils.toECPublicKey(jsonWebKey);
        }

        return ofNullable(publicKey)
            .map(pk -> new JsonWebKeys(fromPublicKey(pk, properties, RSSecurityConstants.RSSEC_SIGNATURE_ALGORITHM)))
            .orElseThrow(() -> new RuntimeException("There is no public key configured"));
    }

    private static Properties getRsProperties(final Message message) {
        final Properties properties = new Properties();
        for (String name : RS_SECURITY_PROPERTIES) {
            ofNullable(message.get(name))
                .ifPresent(o -> properties.put(name, o));
        }
        return properties;
    }
}
