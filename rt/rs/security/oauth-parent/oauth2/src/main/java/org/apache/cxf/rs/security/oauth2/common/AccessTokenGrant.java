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
package org.apache.cxf.rs.security.oauth2.common;

import java.io.Serializable;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Access Token Grant
 */
public interface AccessTokenGrant extends Serializable {
    /**
     * Returns the token grant type, example, "authorization_code"
     * @return the grant type
     */
    String getType();

    /**
     * Returns the map containing public grant parameters;
     * can be used by clients requesting the access tokens.
     *
     * @return the grant parameters
     */
    MultivaluedMap<String, String> toMap();
}
