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

package org.apache.cxf.rs.security.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;

public class CryptoLoader {

    private static final String CRYPTO_CACHE = "rs-security-xml-crypto.cache";

    public Crypto loadCrypto(String cryptoResource) throws IOException, WSSecurityException {
        URL url =
            org.apache.cxf.rt.security.utils.SecurityUtils.loadResource(cryptoResource);
        if (url != null) {
            return loadCryptoFromURL(url);
        }
        return null;
    }

    public Crypto getCrypto(Message message,
                            String cryptoKey,
                            String propKey)
        throws IOException, WSSecurityException {
        Crypto crypto =
            (Crypto)org.apache.cxf.rt.security.utils.SecurityUtils.getSecurityPropertyValue(cryptoKey, message);
        if (crypto != null) {
            return crypto;
        }

        Object o = org.apache.cxf.rt.security.utils.SecurityUtils.getSecurityPropertyValue(propKey, message);
        if (o == null) {
            return null;
        }

        String propResourceName = (String)o;

        Map<Object, Crypto> cryptoCache = getCryptoCache(message);
        crypto = cryptoCache != null ? cryptoCache.get(propResourceName) : null;
        if (crypto != null) {
            return crypto;
        }

        URL url = org.apache.cxf.rt.security.utils.SecurityUtils.loadResource(message, propResourceName);

        if (url != null) {
            crypto = loadCryptoFromURL(url);
        } else {
            crypto = CryptoFactory.getInstance(propResourceName, Thread.currentThread().getContextClassLoader());
        }
        if (cryptoCache != null && crypto != null) {
            cryptoCache.put(o, crypto);
        }

        return crypto;
    }

    public static Crypto loadCryptoFromURL(URL url) throws IOException, WSSecurityException {
        Properties props = new Properties();
        InputStream in = url.openStream();
        props.load(in);
        in.close();
        return CryptoFactory.getInstance(props);
    }

    public final Map<Object, Crypto> getCryptoCache(Message message) {
        Endpoint endpoint = message.getExchange().getEndpoint();
        if (endpoint != null) {
            EndpointInfo info = endpoint.getEndpointInfo();
            synchronized (info) {
                Map<Object, Crypto> o =
                    CastUtils.cast((Map<?, ?>)info.getProperty(CRYPTO_CACHE));
                if (o == null) {
                    o = new ConcurrentHashMap<>();
                    info.setProperty(CRYPTO_CACHE, o);
                }
                return o;
            }
        }
        return null;
    }
}
