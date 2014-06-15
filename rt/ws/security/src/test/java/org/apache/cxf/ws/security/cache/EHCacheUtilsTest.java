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

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.wss4j.common.cache.EHCacheManagerHolder;
import org.junit.Assert;
import org.junit.Test;

public class EHCacheUtilsTest extends Assert {
    @Test
    public void testUseGlobalManager() {
        Bus bus = BusFactory.getThreadDefaultBus();
      
        Configuration conf = 
                ConfigurationFactory.parseConfiguration(
                        EHCacheManagerHolder.class.getResource("/cxf-test-ehcache.xml"));
        conf.setName("myGlobalConfig");
        
        CacheManager.newInstance(conf);
        
        CacheManager manager = EHCacheUtils.getCacheManager(bus,
                EHCacheManagerHolder.class.getResource("/cxf-test-ehcache.xml"));
       
        assertFalse(manager.getName().equals("myGlobalConfig"));
        EHCacheManagerHolder.releaseCacheManger(manager);
        assertEquals(Status.STATUS_SHUTDOWN, manager.getStatus());
       
        bus.setProperty(EHCacheUtils.GLOBAL_EHCACHE_MANAGER_NAME, "myGlobalConfig");
       
        manager = EHCacheUtils.getCacheManager(bus,
                EHCacheManagerHolder.class.getResource("/cxf-test-ehcache.xml"));
       
        assertEquals("myGlobalConfig", manager.getName());
        EHCacheManagerHolder.releaseCacheManger(manager);
        assertEquals(Status.STATUS_ALIVE, manager.getStatus());
       
        manager.shutdown();
        assertEquals(Status.STATUS_SHUTDOWN, manager.getStatus());
       
        bus.setProperty(EHCacheUtils.GLOBAL_EHCACHE_MANAGER_NAME, "myGlobalConfigXXX");
       
        manager = EHCacheUtils.getCacheManager(bus,
                EHCacheManagerHolder.class.getResource("/cxf-test-ehcache.xml"));
       
        assertFalse(manager.getName().equals("myGlobalConfig"));
        EHCacheManagerHolder.releaseCacheManger(manager);
        assertEquals(Status.STATUS_SHUTDOWN, manager.getStatus());
    }
}
