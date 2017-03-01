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
package org.apache.cxf.rs.security.saml;

/**
 * Some constant configuration options
 */
public final class SAMLConstants {

    /**
     * This tag refers to a DOM Element representation of a SAML Token. If a SAML Token
     * is stored on the Message Context, then the SamlFormOutInterceptor and
     * SamlHeaderOutInterceptor will use this token instead of creating a new SAML Token.
     */
    public static final String SAML_TOKEN_ELEMENT = "rs-security.saml.token.element";
    public static final String WS_SAML_TOKEN_ELEMENT = "ws-security.token.element";

    private SAMLConstants() {
        // complete
    }
}
