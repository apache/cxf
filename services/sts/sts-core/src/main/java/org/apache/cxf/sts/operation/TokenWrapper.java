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

package org.apache.cxf.sts.operation;

import org.apache.cxf.ws.security.sts.provider.model.RequestedSecurityTokenType;

/**
 * This interface defines a pluggable way of "wrapping" tokens that are issued by the STS. Some Tokens may be issued
 * in a format that needs to be wrapped as part of the JAXB response.
 */
public interface TokenWrapper {

    /**
     * Wrap the Token parameter and set it on the RequestedSecurityTokenType parameter
     */
    void wrapToken(Object token, RequestedSecurityTokenType requestedTokenType);

}
