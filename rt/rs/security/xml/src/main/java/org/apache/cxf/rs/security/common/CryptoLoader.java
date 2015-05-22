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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;

public class CryptoLoader {
    
    private static final String CRYPTO_CACHE = "rs-security-xml-crypto.cache";
    
    public Crypto loadCrypto(String cryptoResource) throws IOException, WSSecurityException {
        URL url = ClassLoaderUtils.getResource(cryptoResource, this.getClass());
        if (url != null) {
            return loadCryptoFromURL(url);
        } else {
            return null;
        }
    }
    
    public Crypto getCrypto(Message message,
                            String cryptoKey, 
                            String propKey) 
        throws IOException, WSSecurityException {
        Crypto crypto = (Crypto)message.getContextualProperty(cryptoKey);
        if (crypto != null) {
            return crypto;
        }
        
        Object o = message.getContextualProperty(propKey);
        if (o == null) {
            return null;
        }
        
        String propResourceName = (String)o;
        
        Map<Object, Crypto> cryptoCache = getCryptoCache(message);
        crypto = cryptoCache != null ? cryptoCache.get(propResourceName) : null;
        if (crypto != null) {
            return crypto;
        }
        
        ClassLoaderHolder orig = null;
        try {
            URL url = ClassLoaderUtils.getResource(propResourceName, this.getClass());
            if (url == null) {
                ResourceManager manager = message.getExchange()
                        .getBus().getExtension(ResourceManager.class);
                ClassLoader loader = manager.resolveResource("", ClassLoader.class);
                if (loader != null) {
                    orig = ClassLoaderUtils.setThreadContextClassloader(loader);
                }
                url = manager.resolveResource(propResourceName, URL.class);
            }
            if (url == null) {
                try {
                    URI propResourceUri = URI.create(propResourceName);
                    if (propResourceUri.getScheme() != null) {
                        url = propResourceUri.toURL();
                    } else {
                        File f = new File(propResourceUri.toString());
                        if (f.exists()) { 
                            url = f.toURI().toURL();
                        }
                    }
                } catch (IOException ex) {
                    // let CryptoFactory try to load it
                }   
            }
            if (url != null) {
                crypto = loadCryptoFromURL(url);
            } else {
                crypto = CryptoFactory.getInstance(propResourceName, Thread.currentThread().getContextClassLoader());
            }
            if (cryptoCache != null) {
                cryptoCache.put(o, crypto);
            }
            return crypto;
        } finally {
            if (orig != null) {
                orig.reset();
            }
        }
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
            EndpointInfo info  = endpoint.getEndpointInfo();
            synchronized (info) {
                Map<Object, Crypto> o = 
                    CastUtils.cast((Map<?, ?>)info.getProperty(CRYPTO_CACHE));
                if (o == null) {
                    o = new ConcurrentHashMap<Object, Crypto>();
                    info.setProperty(CRYPTO_CACHE, o);
                }
                return o;
            }
        } else {
            return null;
        }
    }
}
