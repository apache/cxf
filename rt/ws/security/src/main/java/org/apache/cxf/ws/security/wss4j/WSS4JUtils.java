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
package org.apache.cxf.ws.security.wss4j;

import java.io.IOException;
import java.net.URL;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.cache.CXFEHCacheReplayCache;
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.cache.ReplayCacheFactory;

/**
 * Some common functionality that can be shared between the WSS4JInInterceptor and the
 * UsernameTokenInterceptor.
 */
public final class WSS4JUtils {

    private WSS4JUtils() {
        // complete
    }

    /**
     * Get a ReplayCache instance. It first checks to see whether caching has been explicitly 
     * enabled or disabled via the booleanKey argument. If it has been set to false then no
     * replay caching is done (for this booleanKey). If it has not been specified, then caching
     * is enabled only if we are not the initiator of the exchange. If it has been specified, then
     * caching is enabled.
     * 
     * It tries to get an instance of ReplayCache via the instanceKey argument from a 
     * contextual property, and failing that the message exchange. If it can't find any, then it
     * defaults to using an EH-Cache instance and stores that on the message exchange.
     */
    public static ReplayCache getReplayCache(
        SoapMessage message, String booleanKey, String instanceKey
    ) {
        boolean specified = false;
        Object o = message.getContextualProperty(booleanKey);
        if (o != null) {
            if (!MessageUtils.isTrue(o)) {
                return null;
            }
            specified = true;
        }

        if (!specified && MessageUtils.isRequestor(message)) {
            return null;
        }
        Endpoint ep = message.getExchange().get(Endpoint.class);
        if (ep != null && ep.getEndpointInfo() != null) {
            EndpointInfo info = ep.getEndpointInfo();
            synchronized (info) {
                ReplayCache replayCache = 
                        (ReplayCache)message.getContextualProperty(instanceKey);
                if (replayCache == null) {
                    replayCache = (ReplayCache)info.getProperty(instanceKey);
                }
                if (replayCache == null) {
                    String cacheKey = instanceKey;
                    if (info.getName() != null) {
                        cacheKey += "-" + info.getName().toString().hashCode();
                    }
                    URL configFile = getConfigFileURL(message);

                    if (ReplayCacheFactory.isEhCacheInstalled()) {
                        Bus bus = message.getExchange().getBus();
                        replayCache = new CXFEHCacheReplayCache(cacheKey, bus, configFile);
                    } else {
                        ReplayCacheFactory replayCacheFactory = ReplayCacheFactory.newInstance();
                        replayCache = replayCacheFactory.newReplayCache(cacheKey, configFile);
                    }
                    
                    info.setProperty(instanceKey, replayCache);
                }
                return replayCache;
            }
        }
        return null;
    }
    
    private static URL getConfigFileURL(Message message) {
        Object o = message.getContextualProperty(SecurityConstants.CACHE_CONFIG_FILE);
        if (o == null) {
            o = "cxf-ehcache.xml";
        }
        
        if (o instanceof String) {
            URL url = null;
            ResourceManager rm = message.getExchange().get(Bus.class).getExtension(ResourceManager.class);
            url = rm.resolveResource((String)o, URL.class);
            try {
                if (url == null) {
                    url = ClassLoaderUtils.getResource((String)o, ReplayCacheFactory.class);
                }
                if (url == null) {
                    url = new URL((String)o);
                }
                return url;
            } catch (IOException e) {
                // Do nothing
            }
        } else if (o instanceof URL) {
            return (URL)o;        
        }
        return null;
    }

}
