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
package org.apache.cxf.rs.security.oauth2.client;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

public class MemoryClientTokenContextManager implements ClientTokenContextManager {
    private ConcurrentHashMap<String, ClientTokenContext> map =
            new ConcurrentHashMap<>();

    @Override
    public void setClientTokenContext(MessageContext mc, ClientTokenContext request) {
        String key = getKey(mc, false);
        if (key == null) {
            key = OAuthUtils.generateRandomTokenKey();
            OAuthUtils.setSessionToken(mc, key, "org.apache.cxf.websso.context", 0);
        }
        map.put(key, request);

    }

    @Override
    public ClientTokenContext getClientTokenContext(MessageContext mc) {
        String key = getKey(mc, false);
        if (key != null) {
            return map.get(key);
        }
        return null;
    }

    @Override
    public ClientTokenContext removeClientTokenContext(MessageContext mc) {
        return map.remove(getKey(mc, true));
    }

    private String getKey(MessageContext mc, boolean remove) {
        return OAuthUtils.getSessionToken(mc, "org.apache.cxf.websso.context", remove);
    }
}
