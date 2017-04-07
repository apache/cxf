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

package org.apache.cxf.rs.security.jose.jwt;

public final class JwtConstants {

    public static final String CLAIM_ISSUER = "iss";
    public static final String CLAIM_SUBJECT = "sub";
    public static final String CLAIM_AUDIENCE = "aud";
    public static final String CLAIM_EXPIRY = "exp";
    public static final String CLAIM_NOT_BEFORE = "nbf";
    public static final String CLAIM_ISSUED_AT = "iat";
    public static final String CLAIM_JWT_ID = "jti";
    public static final String CLAIM_CONFIRMATION = "cnf";

    public static final String JWT_TOKEN = "jwt.token";
    public static final String JWT_CLAIMS = "jwt.claims";

    public static final String EXPECTED_CLAIM_AUDIENCE = "expected.claim.audience";

    private JwtConstants() {

    }
}
