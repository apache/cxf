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
package org.apache.cxf.rs.security.oauth2.jwk;

import java.io.InputStream;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Properties;

import org.apache.cxf.Bus;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.oauth2.utils.crypto.CryptoUtils;

public final class JwkUtils {
    public static final String JWK_KEY_STORE_TYPE = "jwk";
    private JwkUtils() {
        
    }
    public static JsonWebKeys loadPersistJwkSet(Message m, Properties props) {
        return loadPersistJwkSet(m, props, new DefaultJwkSetReaderWriter());
    }
    public static JsonWebKeys loadPersistJwkSet(Message m, Properties props, JwkSetReaderWriter reader) {
        JsonWebKeys jwkSet = (JsonWebKeys)m.getExchange().get(props.get(CryptoUtils.RSSEC_KEY_STORE_FILE));
        if (jwkSet == null) {
            jwkSet = loadJwkSet(props, m.getExchange().getBus(), reader);
            m.getExchange().put((String)props.get(CryptoUtils.RSSEC_KEY_STORE_FILE), jwkSet);
        }
        return jwkSet;
    }
    public static JsonWebKeys loadJwkSet(Properties props, Bus bus) {
        return loadJwkSet(props, bus, new DefaultJwkSetReaderWriter());
    }
    public static JsonWebKeys loadJwkSet(Properties props, Bus bus, JwkSetReaderWriter reader) {
        String keyStoreLoc = props.getProperty(CryptoUtils.RSSEC_KEY_STORE_FILE);
        try {
            InputStream is = ResourceUtils.getResourceStream(keyStoreLoc, bus);
            return reader.jsonToJwkSet(IOUtils.readStringFromStream(is));
        } catch (Exception ex) {
            throw new SecurityException(ex);
        }
    }
    public static RSAPublicKey loadPublicKey(Message m, Properties props) {
        JsonWebKey jwkKey = loadJsonWebKey(m, props);
        return jwkKey != null ? jwkKey.toRSAPublicKey() : null;
    }
    public static RSAPrivateKey loadPrivateKey(Message m, Properties props) {
        JsonWebKey jwkKey = loadJsonWebKey(m, props);
        return jwkKey != null ? jwkKey.toRSAPrivateKey() : null;
    }
    public static JsonWebKey loadJsonWebKey(Message m, Properties props) {
        JsonWebKeys jwkSet = loadPersistJwkSet(m, props);
        JsonWebKey jwkKey = null;
        String kid = props.getProperty(CryptoUtils.RSSEC_KEY_STORE_ALIAS);
        if (kid == null) {
            List<JsonWebKey> keys = jwkSet.getRsaKeys();
            if (keys != null && keys.size() == 1) {
                jwkKey = keys.get(0);
            }
        } else {
            jwkKey = jwkSet.getKey(kid);
        }
        
        return jwkKey;
    }
}
