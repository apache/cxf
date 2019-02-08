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
package org.apache.cxf.rs.security.oauth2.grants.jwt;

public final class Constants {
    public static final String JWT_BEARER_GRANT = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    public static final String CLIENT_GRANT_ASSERTION_PARAM = "assertion";

    public static final String CLIENT_AUTH_ASSERTION_PARAM = "client_assertion";
    public static final String CLIENT_AUTH_ASSERTION_TYPE = "client_assertion_type";
    public static final String CLIENT_AUTH_JWT_BEARER = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";


    private Constants() {

    }
}
