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

package org.apache.cxf.security.claims.authorization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Target({ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Claim {

    String format() default "urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified";
    String name() default "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";
    String[] value();

    /**
     * If set to true then all the values of this claim have to be matched
     */
    boolean matchAll() default false;
    /**
     * If set to ClaimMode.LAX then the match will fail only if the incoming
     * assertion has the same name and format claim with non-matching values
     */
    ClaimMode mode() default ClaimMode.STRICT;
}
