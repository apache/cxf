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
package org.apache.cxf.sts.token.validator.jwt;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;

import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.sts.token.validator.SubjectRoleParser;

/**
 * This interface defines a way to extract roles from a JWT Token
 */
public interface JWTRoleParser extends SubjectRoleParser {

    /**
     * Return the set of User/Principal roles from the token.
     * @param principal the Principal associated with the token
     * @param subject the JAAS Subject associated with a successful validation of the token
     * @param token The JWTToken
     * @return the set of User/Principal roles from the token.
     */
    Set<Principal> parseRolesFromToken(
        Principal principal, Subject subject, JwtToken token
    );
}
