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

package org.apache.cxf.sts.cache;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.ObjectName;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.management.annotation.ManagedOperation;
import org.apache.cxf.management.annotation.ManagedResource;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.sts.IdentityMapper;
import org.apache.cxf.ws.security.cache.EHCacheUtils;
import org.apache.cxf.ws.security.tokenstore.TokenStoreFactory;
import org.apache.wss4j.common.cache.EHCacheManagerHolder;

/**
 * A EH-Cache based cache to cache identities in different realms where
 * the relationship is of type FederateIdentity.
 */
@ManagedResource()
public class EHCacheIdentityCache extends AbstractIdentityCache 
    implements Closeable, BusLifeCycleListener {
    
    private static final Logger LOG = LogUtils.getL7dLogger(EHCacheIdentityCache.class);
    
    private Ehcache cache;
    private CacheManager cacheManager;
    
    
    public EHCacheIdentityCache(
        IdentityMapper identityMapper, Bus b
    ) {
        this(identityMapper, EHCacheIdentityCache.class.getName(), b, null);
    }
    
    public EHCacheIdentityCache(
        IdentityMapper identityMapper, String key, Bus b, URL configFileURL
    ) {
        super(b, identityMapper);
        if (b != null) {
            b.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);
            InstrumentationManager im = b.getExtension(InstrumentationManager.class);
            if (im != null) {
                try {
                    im.register(this);
                } catch (JMException e) {
                    LOG.log(Level.WARNING, "Registering EHCacheIdentityCache failed.", e);
                }
            }
        }

        if (configFileURL != null) {
            cacheManager = EHCacheUtils.getCacheManager(b, configFileURL);
        } else {
            cacheManager = EHCacheUtils.getCacheManager(b, getDefaultConfigFileURL());
        }
        CacheConfiguration cc = EHCacheManagerHolder.getCacheConfiguration(key, cacheManager);
        
        Ehcache newCache = new Cache(cc);
        cache = cacheManager.addCacheIfAbsent(newCache);
    }
    
    @Override
    public void add(String user, String realm, Map<String, String> identities) {
        cache.put(new Element(user + "@" + realm, identities));
    }

    @SuppressWarnings("unchecked")
    @ManagedOperation()
    @Override
    public Map<String, String> get(String user, String realm) {
        Element element = cache.get(user + "@" + realm);
        if (element != null && !cache.isExpired(element)) {
            return (Map<String, String>)element.getObjectValue();
        }
        return null;
    }

    @Override
    public void remove(String user, String realm) {
        cache.remove(user + "@" + realm);       
    }
    
    @ManagedOperation()
    @Override
    public void clear() {
        cache.removeAll();
    }
    
    @ManagedOperation()
    @Override
    public int size() {
        return cache.getSize();
    }
    
    @ManagedOperation()
    public String getContent() {
        return this.cache.toString();
    }

    public void close() {
        if (cacheManager != null) {
            // this step is especially important for global shared cache manager
            if (cache != null) {
                cacheManager.removeCache(cache.getName());
            }
            
            EHCacheManagerHolder.releaseCacheManger(cacheManager);
            cacheManager = null;
            cache = null;
            if (super.getBus() != null) {
                super.getBus().getExtension(BusLifeCycleManager.class).unregisterLifeCycleListener(this);
            }
        }
    }

    public void initComplete() {
    }

    public void preShutdown() {
        close();
    }

    public void postShutdown() {
        close();
    }
    
    private URL getDefaultConfigFileURL() {
        URL url = null;
        if (super.getBus() != null) {
            ResourceManager rm = super.getBus().getExtension(ResourceManager.class);
            url = rm.resolveResource("sts-ehcache.xml", URL.class);
        }
        try {
            if (url == null) {
                url = ClassLoaderUtils.getResource("sts-ehcache.xml", TokenStoreFactory.class);
            }
            if (url == null) {
                url = new URL("sts-ehcache.xml");
            }
            return url;
        } catch (IOException e) {
            // Do nothing
        }
        return null;
    }

    public ObjectName getObjectName() throws JMException {
        StringBuilder buffer = new StringBuilder();
        buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME).append(':');
        if (super.getBus() != null) {
            buffer.append(
                ManagementConstants.BUS_ID_PROP).append('=').append(super.getBus().getId()).append(',');
        }
        buffer.append(ManagementConstants.TYPE_PROP).append('=').append("EHCacheIdentityCache").append(',');
        buffer.append(ManagementConstants.NAME_PROP).append('=')
            .append("EHCacheIdentityCache-" + System.identityHashCode(this));
        return new ObjectName(buffer.toString());
    }
}

