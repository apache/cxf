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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.ObjectName;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.management.annotation.ManagedOperation;
import org.apache.cxf.management.annotation.ManagedResource;
import org.apache.cxf.sts.IdentityMapper;

/**
 * A simple in-memory HashMap based cache to cache identities in different realms where
 * the relationship is of type FederateIdentity.
 */
@ManagedResource()
public class MemoryIdentityCache extends AbstractIdentityCache {

    private static final Logger LOG = LogUtils.getL7dLogger(MemoryIdentityCache.class);

    private final Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();

    private long maxCacheItems = 10000L;

    protected MemoryIdentityCache() {
        super(null, null);
    }

    public MemoryIdentityCache(IdentityMapper identityMapper) {
        super(null, identityMapper);
    }

    public MemoryIdentityCache(Bus bus, IdentityMapper identityMapper) {
        super(bus, identityMapper);
        if (bus != null) {
            InstrumentationManager im = bus.getExtension(InstrumentationManager.class);
            if (im != null) {
                try {
                    im.register(this);
                } catch (JMException e) {
                    LOG.log(Level.WARNING, "Registering MemoryIdentityCache failed.", e);
                }
            }
        }
    }

    public long getMaxCacheItems() {
        return maxCacheItems;
    }

    public void setMaxCacheItems(long maxCacheItems) {
        this.maxCacheItems = maxCacheItems;
    }

    @Override
    public void add(String user, String realm, Map<String, String> identities) {
        if (cache.size() >= maxCacheItems) {
            cache.clear();
        }
        cache.put(user + '@' + realm, identities);
    }

    @ManagedOperation()
    @Override
    public Map<String, String> get(String user, String realm) {
        return cache.get(user + '@' + realm);
    }

    @Override
    public void remove(String user, String realm) {
        cache.remove(user + '@' + realm);
    }

    @ManagedOperation()
    public String getContent() {
        return this.cache.toString();
    }

    public ObjectName getObjectName() throws JMException {
        StringBuilder buffer = new StringBuilder(128);
        buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME).append(':');
        if (super.getBus() != null) {
            buffer.append(
                ManagementConstants.BUS_ID_PROP).append('=').append(super.getBus().getId()).append(',');
        }
        buffer.append(ManagementConstants.TYPE_PROP).append('=').append("MemoryIdentityCache").append(',');
        buffer.append(ManagementConstants.NAME_PROP).append('=')
            .append("MemoryIdentityCache-").append(System.identityHashCode(this));
        return new ObjectName(buffer.toString());
    }
}

