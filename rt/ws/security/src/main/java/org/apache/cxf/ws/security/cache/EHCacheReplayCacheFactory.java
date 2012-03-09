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

package org.apache.cxf.ws.security.cache;

import java.io.IOException;
import java.net.URL;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.cache.ReplayCache;

/**
 * A factory to return an EHCacheReplayCache instance.
 */
public class EHCacheReplayCacheFactory extends ReplayCacheFactory {
    
    public ReplayCache newReplayCache(String key, Message message) {
        URL configFileURL = getConfigFileURL(message);
        if (configFileURL == null) {
            String defaultConfigFile = "cxf-ehcache.xml";
            ResourceManager rm = message.getExchange().get(Bus.class).getExtension(ResourceManager.class);
            configFileURL = rm.resolveResource(defaultConfigFile, URL.class);
            try {
                if (configFileURL == null) {
                    configFileURL = 
                        ClassLoaderUtils.getResource(defaultConfigFile, EHCacheReplayCacheFactory.class);
                }
                if (configFileURL == null) {
                    configFileURL = new URL(defaultConfigFile);
                }
            } catch (IOException e) {
                // Do nothing
            }
        }
        if (configFileURL != null) {
            message.setContextualProperty(SecurityConstants.CACHE_CONFIG_FILE, configFileURL);
        }
        return new EHCacheReplayCache(key, configFileURL);
    }
    
}
