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
package org.apache.cxf.ws.security;

import java.io.IOException;
import java.net.URL;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Message;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreFactory;
import org.apache.wss4j.common.ext.WSSecurityException;

/**
 * Some common functionality
 */
public final class SecurityUtils {
    
    private SecurityUtils() {
        // complete
    }

    public static CallbackHandler getCallbackHandler(Object o) throws WSSecurityException {
        CallbackHandler handler = null;
        if (o instanceof CallbackHandler) {
            handler = (CallbackHandler)o;
        } else if (o instanceof String) {
            try {
                handler = (CallbackHandler)ClassLoaderUtils.loadClass((String)o, 
                                                                      SecurityUtils.class).newInstance();
            } catch (Exception e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
            }
        }
        return handler;
    }
    
    public static URL getConfigFileURL(Message message, String configFileKey, String configFileDefault) {
        Object o = message.getContextualProperty(configFileKey);
        if (o == null) {
            o = configFileDefault;
        }
        
        return loadResource(message, o);
    }
    
    public static URL loadResource(Message message, Object o) {
        
        if (o instanceof String) {
            URL url = ClassLoaderUtils.getResource((String)o, SecurityUtils.class);
            if (url != null) {
                return url;
            }
            ClassLoaderHolder orig = null;
            try {
                ResourceManager manager = message.getExchange().get(Bus.class).getExtension(ResourceManager.class);
                ClassLoader loader = manager.resolveResource((String)o, ClassLoader.class);
                if (loader != null) {
                    orig = ClassLoaderUtils.setThreadContextClassloader(loader);
                }
                url = manager.resolveResource((String)o, URL.class);
                if (url == null) {
                    try {
                        url = new URL((String)o);
                    } catch (IOException e) {
                        // Do nothing
                    }
                }
                return url;
            } finally {
                if (orig != null) {
                    orig.reset();
                }
            }
        } else if (o instanceof URL) {
            return (URL)o;        
        }
        return null;
    }
    
    public static TokenStore getTokenStore(Message message) {
        EndpointInfo info = message.getExchange().get(Endpoint.class).getEndpointInfo();
        synchronized (info) {
            TokenStore tokenStore = 
                (TokenStore)message.getContextualProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            if (tokenStore == null) {
                tokenStore = (TokenStore)info.getProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
            }
            if (tokenStore == null) {
                TokenStoreFactory tokenStoreFactory = TokenStoreFactory.newInstance();
                String cacheKey = SecurityConstants.TOKEN_STORE_CACHE_INSTANCE;
                String cacheIdentifier = 
                    (String)message.getContextualProperty(SecurityConstants.CACHE_IDENTIFIER);
                if (cacheIdentifier != null) {
                    cacheKey += "-" + cacheIdentifier;
                } else if (info.getName() != null) {
                    int hashcode = info.getName().toString().hashCode();
                    if (hashcode < 0) {
                        cacheKey += hashcode;
                    } else {
                        cacheKey += "-" + hashcode;
                    }
                }
                tokenStore = tokenStoreFactory.newTokenStore(cacheKey, message);
                info.setProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, tokenStore);
            }
            return tokenStore;
        }
    }
}
