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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.ObjectName;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.ManagedComponent;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.management.annotation.ManagedAttribute;
import org.apache.cxf.management.annotation.ManagedResource;

@ManagedResource()
public class MemoryIdentityCacheStatistics implements ManagedComponent {
    private static final Logger LOG = LogUtils.getL7dLogger(MemoryIdentityCacheStatistics.class);

    private long cacheMiss;
    private long cacheHit;
    private ObjectName objectName;

    public MemoryIdentityCacheStatistics() {
    }

    public MemoryIdentityCacheStatistics(Bus bus, ManagedComponent parent) {
        if (bus != null) {
            InstrumentationManager im = bus.getExtension(InstrumentationManager.class);
            if (im != null) {
                try {
                    StringBuilder buffer = new StringBuilder();
                    ObjectName pname = parent.getObjectName();
                    String pn = pname.getKeyProperty(ManagementConstants.NAME_PROP);
                    String pt = pname.getKeyProperty(ManagementConstants.TYPE_PROP);
                    buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME).append(':')
                        .append(ManagementConstants.BUS_ID_PROP).append('=').append(bus.getId()).append(',')
                        .append(pt).append('=').append(pn).append(',')
                        .append(ManagementConstants.TYPE_PROP).append('=')
                        .append("MemoryIdentityCacheStatistics").append(',')
                        .append(ManagementConstants.NAME_PROP).append('=')
                        .append("MemoryIdentityCacheStatistics-").append(System.identityHashCode(this));
                    objectName = new ObjectName(buffer.toString());

                    im.register(this);
                } catch (JMException e) {
                    LOG.log(Level.WARNING, "Registering MemoryIdentityCacheStatistics failed.", e);
                }
            }
        }
    }

    @ManagedAttribute()
    public synchronized long getCacheMiss() {
        return cacheMiss;
    }

    @ManagedAttribute()
    public synchronized long getCacheHit() {
        return cacheHit;
    }

    protected synchronized void increaseCacheHit() {
        cacheHit++;
    }

    protected synchronized void increaseCacheMiss() {
        cacheMiss++;
    }

    public ObjectName getObjectName() throws JMException {
        return objectName;
    }

}
