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

public class MemoryClientTokenContextManager implements ClientTokenContextManager {
    private ConcurrentHashMap<String, ClientTokenContext> map = 
            new ConcurrentHashMap<String, ClientTokenContext>();

    @Override
    public void setClientTokenContext(MessageContext mc, ClientTokenContext request) {
        map.put(getKey(mc), request);
        
    }

    private String getKey(MessageContext mc) {
        return mc.getSecurityContext().getUserPrincipal().getName();
    }

    @Override
    public ClientTokenContext getClientTokenContext(MessageContext mc) {
        // TODO: support an automatic removal based on the token expires property
        return map.remove(getKey(mc));
    }

    @Override
    public void removeClientTokenContext(MessageContext mc, ClientTokenContext request) {
        map.remove(getKey(mc));
    }
}
