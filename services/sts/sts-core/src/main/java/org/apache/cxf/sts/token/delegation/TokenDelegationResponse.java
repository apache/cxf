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
package org.apache.cxf.sts.token.delegation;

import java.util.Map;

import org.apache.cxf.sts.request.ReceivedToken;

/**
 * This class encapsulates the response from a TokenDelegationHandler instance.
 */
public class TokenDelegationResponse {

    private Map<String, Object> additionalProperties;
    private ReceivedToken token;
    private boolean delegationAllowed;

    public ReceivedToken getToken() {
        return token;
    }

    public void setToken(ReceivedToken token) {
        this.token = token;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public boolean isDelegationAllowed() {
        return delegationAllowed;
    }

    public void setDelegationAllowed(boolean delegationAllowed) {
        this.delegationAllowed = delegationAllowed;
    }

}
