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
import org.apache.cxf.ws.security.tokenstore.TokenStoreFactory;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.Status;
import org.ehcache.config.Configuration;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.xml.XmlConfiguration;

/**
 * A EH-Cache based cache to cache identities in different realms where
 * the relationship is of type FederateIdentity.
 */
@ManagedResource()
public class EHCacheIdentityCache extends AbstractIdentityCache
    implements Closeable, BusLifeCycleListener {

    private static final Logger LOG = LogUtils.getL7dLogger(EHCacheIdentityCache.class);
    private static final String KEY = "org.apache.cxf.sts.cache.EHCacheIdentityCache";
    private Cache<String, EHCacheIdentityValue> cache;
    private final CacheManager cacheManager;


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

        URL xmlConfigURL = configFileURL != null ? configFileURL : getDefaultConfigFileURL();
        Configuration xmlConfig = new XmlConfiguration(xmlConfigURL);
        cacheManager = CacheManagerBuilder.newCacheManager(xmlConfig);

        cacheManager.init();
        cache = cacheManager.getCache(KEY, String.class, EHCacheIdentityValue.class);
    }

    @Override
    public void add(String user, String realm, Map<String, String> identities) {
        cache.put(user + "@" + realm, new EHCacheIdentityValue(identities));
    }

    @ManagedOperation()
    @Override
    public Map<String, String> get(String user, String realm) {
        EHCacheIdentityValue value = cache.get(user + "@" + realm);
        if (value != null) {
            return value.getValue();
        }

        return null;
    }

    @Override
    public void remove(String user, String realm) {
        cache.remove(user + "@" + realm);
    }

    @ManagedOperation()
    public String getContent() {
        return this.cache.toString();
    }

    public synchronized void close() {
        if (cacheManager.getStatus() == Status.AVAILABLE) {
            cacheManager.removeCache(KEY);
            cacheManager.close();

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
        StringBuilder buffer = new StringBuilder(128);
        buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME).append(':');
        if (super.getBus() != null) {
            buffer.append(
                ManagementConstants.BUS_ID_PROP).append('=').append(super.getBus().getId()).append(',');
        }
        buffer.append(ManagementConstants.TYPE_PROP).append('=').append("EHCacheIdentityCache").append(',');
        buffer.append(ManagementConstants.NAME_PROP).append('=')
            .append("EHCacheIdentityCache-").append(System.identityHashCode(this));
        return new ObjectName(buffer.toString());
    }
}

