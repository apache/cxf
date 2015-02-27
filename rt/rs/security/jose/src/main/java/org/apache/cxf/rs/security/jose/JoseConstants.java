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

package org.apache.cxf.rs.security.jose;

public final class JoseConstants {
    public static final String HEADER_TYPE = "typ";
    public static final String HEADER_ALGORITHM = "alg";
    public static final String HEADER_CONTENT_TYPE = "cty";
    public static final String HEADER_CRITICAL = "crit";
    
    public static final String HEADER_KEY_ID = "kid";
    public static final String HEADER_X509_URL = "x5u";
    public static final String HEADER_X509_CHAIN = "x5c";
    public static final String HEADER_X509_THUMBPRINT = "x5t";
    public static final String HEADER_X509_THUMBPRINT_SHA256 = "x5t#S256";
    public static final String HEADER_JSON_WEB_KEY = "jwk";
    public static final String HEADER_JSON_WEB_KEY_SET = "jku";
    
    public static final String JWE_HEADER_KEY_ENC_ALGORITHM = HEADER_ALGORITHM;
    public static final String JWE_HEADER_CONTENT_ENC_ALGORITHM = "enc";
    public static final String JWE_HEADER_ZIP_ALGORITHM = "zip";
    public static final String DEFLATE_ZIP_ALGORITHM = "DEF";
    
    public static final String TYPE_JWT = "JWT";
    public static final String TYPE_JOSE = "JOSE";
    public static final String TYPE_JOSE_JSON = "JOSE+JSON";
    public static final String MEDIA_TYPE_JOSE = "application/jose";
    public static final String MEDIA_TYPE_JOSE_JSON = "application/jose+json";
    
    public static final String JOSE_CONTEXT_PROPERTY = "org.apache.cxf.jose.context";
    
    private JoseConstants() {
        
    }
}
