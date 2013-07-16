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

package org.apache.cxf.rs.security.saml.sso;

import java.lang.reflect.Method;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;

/**
 */
public final class EHCacheUtil {
    private static Method cacheManagerCreateMethodNoArg;
    private static Method cacheManagerCreateMethodConfigurationArg;
    static {
        // these methods are either completely available or absent (valid assumption from 2.5.0 to 2.7.2 so far)
        try {
            // from 2.5.2
            cacheManagerCreateMethodNoArg = CacheManager.class.getMethod("newInstance", (Class<?>[])null);
            cacheManagerCreateMethodConfigurationArg = CacheManager.class.getMethod("newInstance", Configuration.class);
        } catch (NoSuchMethodException e) {
            try {
                // before 2.5.2
                cacheManagerCreateMethodNoArg = CacheManager.class.getMethod("create", (Class<?>[])null);
                cacheManagerCreateMethodConfigurationArg = CacheManager.class.getMethod("create", Configuration.class);
            } catch (Throwable t) {
                // ignore
            }
        }
    }
    
    private EHCacheUtil() {
        // 
    }
    
    public static CacheConfiguration getCacheConfiguration(String key, CacheManager cacheManager) {
        CacheConfiguration cc = cacheManager.getConfiguration().getCacheConfigurations().get(key);
        if (cc == null && key.contains("-")) {
            cc = cacheManager.getConfiguration().getCacheConfigurations().get(
                    key.substring(0, key.lastIndexOf('-') - 1));
        }
        if (cc == null) {
            cc = cacheManager.getConfiguration().getDefaultCacheConfiguration();
        }
        if (cc == null) {
            cc = new CacheConfiguration();
        } else {
            cc = (CacheConfiguration)cc.clone();
        }
        cc.setName(key);
        return cc;
    }
    
    public static CacheManager createCacheManager() throws CacheException {
        try {
            return (CacheManager)cacheManagerCreateMethodNoArg.invoke(null, (Object[])null);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    public static CacheManager createCacheManager(Configuration conf) throws CacheException {
        try {
            return (CacheManager)cacheManagerCreateMethodConfigurationArg.invoke(null, new Object[]{conf});
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }
}
