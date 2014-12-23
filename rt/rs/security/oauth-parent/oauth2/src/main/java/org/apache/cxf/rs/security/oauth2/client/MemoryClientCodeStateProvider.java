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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

public class MemoryClientCodeStateProvider implements ClientCodeStateProvider {
    private ConcurrentHashMap<String, MultivaluedMap<String, String>> map = 
            new ConcurrentHashMap<String, MultivaluedMap<String, String>>();
    
    @Override
    public String toString(SecurityContext sc, UriInfo ui,
            MultivaluedMap<String, String> state) {
        String name = sc.getUserPrincipal().getName();
        String hashCode = Integer.toString(name.hashCode());
        map.put(hashCode, state);
        return hashCode;
    }

    @Override
    public MultivaluedMap<String, String> toState(SecurityContext sc,
            UriInfo ui, String stateParam) {
        String name = sc.getUserPrincipal().getName();
        String hashCode = Integer.toString(name.hashCode());
        return map.remove(hashCode);
    }
}
