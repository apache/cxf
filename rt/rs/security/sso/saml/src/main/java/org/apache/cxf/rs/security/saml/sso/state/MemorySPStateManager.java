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
package org.apache.cxf.rs.security.saml.sso.state;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MemorySPStateManager implements SPStateManager {

    private final Map<String, RequestState> requestStateMap = new ConcurrentHashMap<>(16, 0.75f, 4);

    private final Map<String, ResponseState> responseStateMap = new ConcurrentHashMap<>(16, 0.75f, 4);

    public ResponseState getResponseState(String securityContextKey) {
        return responseStateMap.get(securityContextKey);
    }

    public ResponseState removeResponseState(String securityContextKey) {
        return responseStateMap.remove(securityContextKey);
    }

    public void setResponseState(String securityContextKey, ResponseState state) {
        responseStateMap.put(securityContextKey, state);
    }

    public void setRequestState(String relayState, RequestState state) {
        requestStateMap.put(relayState, state);
    }

    public RequestState removeRequestState(String relayState) {
        return requestStateMap.remove(relayState);
    }

    public void close() throws IOException {
        requestStateMap.clear();
        responseStateMap.clear();
    }

}
