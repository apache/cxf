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

package org.apache.cxf.rs.security.saml.sso.jcache;

import java.net.URISyntaxException;

import org.apache.cxf.Bus;
import org.apache.cxf.rs.security.saml.sso.TokenReplayCache;

/**
 * A JCache based implementation of the SSO TokenReplayCache interface, using the SSO replay cache
 * name and default configuration file. The implementation is inherited from
 * {@link org.apache.cxf.rs.security.common.jcache.JCacheTokenReplayCache}.
 * The default TTL is 60 minutes and the max TTL is 12 hours.
 */
public class JCacheTokenReplayCache extends org.apache.cxf.rs.security.common.jcache.JCacheTokenReplayCache
    implements TokenReplayCache<String> {

    public static final String CACHE_KEY = "cxf.samlp.replay.cache";

    private static final String DEFAULT_CONFIG_URL = "/cxf-samlp-jcache.xml";

    public JCacheTokenReplayCache() throws URISyntaxException {
        this(DEFAULT_CONFIG_URL, null);
    }

    public JCacheTokenReplayCache(Bus bus) throws URISyntaxException {
        this(DEFAULT_CONFIG_URL, bus);
    }

    public JCacheTokenReplayCache(String configFile) throws URISyntaxException {
        this(configFile, null);
    }

    public JCacheTokenReplayCache(String configFile, Bus bus) throws URISyntaxException {
        super(CACHE_KEY, configFile, DEFAULT_CONFIG_URL, bus);
    }

}
