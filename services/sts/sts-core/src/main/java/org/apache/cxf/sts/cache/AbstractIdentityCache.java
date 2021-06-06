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

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.management.ManagedComponent;
import org.apache.cxf.sts.IdentityMapper;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;

public abstract class AbstractIdentityCache implements IdentityCache, IdentityMapper, ManagedComponent {

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractIdentityCache.class);

    private final IdentityMapper identityMapper;
    private final Bus bus;
    private MemoryIdentityCacheStatistics statistics;

    public AbstractIdentityCache(IdentityMapper identityMapper) {
        this(null, identityMapper);
    }

    public AbstractIdentityCache(Bus bus, IdentityMapper identityMapper) {
        this.identityMapper = identityMapper;
        this.bus = bus;
    }

    public Principal mapPrincipal(String sourceRealm,
            Principal sourcePrincipal, String targetRealm) {

        final Principal targetPrincipal;
        Map<String, String> identities = this.get(sourcePrincipal.getName(), sourceRealm);
        if (identities != null) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Identities found for '" + sourcePrincipal.getName() + "@" + sourceRealm + "'");
            }
            // Identities object found for key sourceUser@sourceRealm
            String targetUser = identities.get(targetRealm);
            if (targetUser == null) {
                getStatistics().increaseCacheMiss();
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("No mapping found for realm " + targetRealm + " of user '"
                             + sourcePrincipal.getName() + "@" + sourceRealm + "'");
                }
                // User identity of target realm not cached yet
                targetPrincipal = this.identityMapper.mapPrincipal(
                        sourceRealm, sourcePrincipal, targetRealm);

                if (targetPrincipal == null || targetPrincipal.getName() == null) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Failed to map user '" + sourcePrincipal.getName()
                                    + "' [" + sourceRealm + "] to realm '"
                                    + targetRealm + "'");
                    }
                    return null;
                }

                // Add the identity for target realm to the cached entry
                identities.put(targetRealm, targetPrincipal.getName());

                // Verify whether target user has cached some identities already
                Map<String, String> cachedItem = this.get(targetPrincipal.getName(), targetRealm);
                if (cachedItem != null) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Merging mappings for '" + sourcePrincipal.getName() + "@" + sourceRealm + "'");
                    }
                    //Identities already cached for targetUser@targetRealm key pair
                    //Merge into identities object
                    this.mergeMap(identities, cachedItem);
                }

                // Update existing entries
                for (Map.Entry<String, String> entry : identities.entrySet()) {
                    this.add(entry.getValue(), entry.getKey(), identities);
                }
            } else {
                getStatistics().increaseCacheHit();
                if (LOG.isLoggable(Level.INFO)) {
                    LOG.info("Mapping '" + sourcePrincipal.getName() + "@" + sourceRealm + "' to '"
                             + targetUser + "@" + targetRealm + "' cached");
                }
                targetPrincipal = new CustomTokenPrincipal(targetUser);
            }

        } else {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("No mapping found for realm " + targetRealm + " of user '"
                        + sourcePrincipal.getName() + "@" + sourceRealm + "'");
            }
            getStatistics().increaseCacheMiss();

            // Identities object NOT found for key sourceUser@sourceRealm
            targetPrincipal = this.identityMapper.mapPrincipal(
                    sourceRealm, sourcePrincipal, targetRealm);
            identities = new HashMap<>();
            identities.put(sourceRealm, sourcePrincipal.getName());
            identities.put(targetRealm, targetPrincipal.getName());
            this.add(targetPrincipal.getName(), targetRealm, identities);
            this.add(sourcePrincipal.getName(), sourceRealm, identities);
        }
        return targetPrincipal;
    }

    public MemoryIdentityCacheStatistics getStatistics() {
        if (statistics == null) {
            this.statistics = new MemoryIdentityCacheStatistics(bus, this);
        }
        return statistics;
    }

    public void setStatistics(MemoryIdentityCacheStatistics stats) {
        this.statistics = stats;
    }

    private void mergeMap(Map<String, String> to, Map<String, String> from) {
        for (Map.Entry<String, String> entry : from.entrySet()) {
            to.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : to.entrySet()) {
            from.put(entry.getKey(), entry.getValue());
        }
    }

    protected Bus getBus() {
        return bus;
    }
}

