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

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

public class MemoryClientCodeStateManager implements ClientCodeStateManager {
    private ConcurrentHashMap<String, MultivaluedMap<String, String>> map = 
            new ConcurrentHashMap<String, MultivaluedMap<String, String>>();
    
    @Override
    public MultivaluedMap<String, String> toRedirectState(MessageContext mc, 
                                                          MultivaluedMap<String, String> requestState) {
        String stateParam = OAuthUtils.generateRandomTokenKey();
        map.put(stateParam, requestState);
        
        MultivaluedMap<String, String> redirectMap = new MetadataMap<String, String>();
        redirectMap.putSingle(OAuthConstants.STATE, stateParam);
        return redirectMap;
    }

    @Override
    public MultivaluedMap<String, String> fromRedirectState(MessageContext mc, 
                                                            MultivaluedMap<String, String> redirectState) {
        String stateParam = redirectState.getFirst(OAuthConstants.STATE);
        return map.remove(stateParam);
    }
}
