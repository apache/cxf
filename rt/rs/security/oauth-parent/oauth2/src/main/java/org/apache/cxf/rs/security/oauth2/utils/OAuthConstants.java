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

package org.apache.cxf.rs.security.oauth2.utils;

/**
 * Miscellaneous constants 
 */
public final class OAuthConstants {
    // Common OAuth2 constants
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String CLIENT_AUDIENCE = "audience";
    public static final String NONCE = "nonce";
    
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String SCOPE = "scope";
    public static final String STATE = "state";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String ACCESS_TOKEN_TYPE = "token_type";
    public static final String ACCESS_TOKEN_EXPIRES_IN = "expires_in";
    // CXF-Specific
    public static final String ACCESS_TOKEN_ISSUED_AT = "issued_at";
    public static final String GRANT_TYPE = "grant_type";
    public static final String RESPONSE_TYPE = "response_type";
    public static final String TOKEN_RESPONSE_TYPE = "token";
    public static final String REFRESH_TOKEN = "refresh_token";
    
    // Well-known grant types
    public static final String AUTHORIZATION_CODE_GRANT = "authorization_code";
    public static final String CLIENT_CREDENTIALS_GRANT = "client_credentials";
    public static final String IMPLICIT_GRANT = "implicit";
    public static final String RESOURCE_OWNER_GRANT = "password";
    public static final String REFRESH_TOKEN_GRANT = "refresh_token";
    
    // CXF-specific grant
    // The token is returned directly to a human user who copies it into a confidential client
    public static final String IMPLICIT_CONFIDENTIAL_GRANT = "urn:ietf:params:oauth:grant-type:implicit-confidential";
    public static final String DIRECT_TOKEN_GRANT = "urn:ietf:params:oauth:grant-type:direct-token-grant";
    
    // Well-known token types
    public static final String BEARER_TOKEN_TYPE = "Bearer";
    public static final String HAWK_TOKEN_TYPE = "hawk";
    
    // https://tools.ietf.org/html/rfc7636
    public static final String AUTHORIZATION_CODE_VERIFIER = "code_verifier";
    public static final String AUTHORIZATION_CODE_CHALLENGE = "code_challenge";
    public static final String AUTHORIZATION_CODE_CHALLENGE_METHOD = "code_challenge_method";
    
    // CXF-specific
    public static final String REFRESH_TOKEN_TYPE = "refresh";
        
    // Hawk token parameters
    // Set by Access Token Service
    public static final String HAWK_TOKEN_KEY = "secret";
    public static final String HAWK_TOKEN_ALGORITHM = "algorithm";
    
    // Set in Authorization header
    public static final String HAWK_TOKEN_ID = "id";
    public static final String HAWK_TOKEN_TIMESTAMP = "ts";
    public static final String HAWK_TOKEN_EXTENSION = "ext";
    public static final String HAWK_TOKEN_NONCE = "nonce";
    public static final String HAWK_TOKEN_SIGNATURE = "mac";
    
    // Mac/Hawk HMAC algorithm names
    public static final String HMAC_ALGO_SHA_1 = "hmac-sha-1";
    public static final String HMAC_ALGO_SHA_256 = "hmac-sha-256";
    
    // Token Authorization schemes
    public static final String BEARER_AUTHORIZATION_SCHEME = "Bearer";
    public static final String HAWK_AUTHORIZATION_SCHEME = "Hawk";
    public static final String ALL_AUTH_SCHEMES = "*";

    
    // Default Client Authentication Scheme
    public static final String BASIC_SCHEME = "Basic";
    
    // Authorization Code grant constants
    public static final String AUTHORIZATION_CODE_VALUE = "code";
    public static final String CODE_RESPONSE_TYPE = "code";
    public static final String SESSION_AUTHENTICITY_TOKEN = "session_authenticity_token";
    public static final String SESSION_AUTHENTICITY_TOKEN_PARAM_NAME = "session_authenticity_token_param_name";
    public static final String AUTHORIZATION_DECISION_KEY = "oauthDecision";
    public static final String AUTHORIZATION_DECISION_ALLOW = "allow";
    public static final String AUTHORIZATION_DECISION_DENY = "deny";
    
    // Resource Owner grant constants
    public static final String RESOURCE_OWNER_NAME = "username";
    public static final String RESOURCE_OWNER_PASSWORD = "password";
    
    // Error constants
    public static final String ERROR_KEY = "error";
    public static final String ERROR_DESCRIPTION_KEY = "error_description";
    public static final String ERROR_URI_KEY = "error_uri";
    
    public static final String SERVER_ERROR = "server_error";
    public static final String INVALID_REQUEST = "invalid_request";
    public static final String INVALID_GRANT = "invalid_grant";
    public static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";
    public static final String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";
    public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";
    public static final String INVALID_CLIENT = "invalid_client";
    public static final String INVALID_SCOPE = "invalid_scope";
    public static final String ACCESS_DENIED = "access_denied";
    
    // Token Revocation, Introspection
    public static final String TOKEN_ID = "token";
    public static final String TOKEN_TYPE_HINT = "token_type_hint";
    public static final String UNSUPPORTED_TOKEN_TYPE = "unsupported_token_type";
    
    // Authorization scheme constants, used internally by AccessTokenValidation client and service
    public static final String AUTHORIZATION_SCHEME_TYPE = "authScheme";
    public static final String AUTHORIZATION_SCHEME_DATA = "authSchemeData";
    
    // Default refresh token scope value - checked by CXF utility code
    public static final String REFRESH_TOKEN_SCOPE = "refreshToken";
    
    // Client Secret (JWS) Signature Algorithm
    public static final String CLIENT_SECRET_SIGNATURE_ALGORITHM = "client.secret.signature.algorithm";
    // Client Secret (JWE) Content Encryption Algorithm
    public static final String CLIENT_SECRET_CONTENT_ENCRYPTION_ALGORITHM = 
        "client.secret.content.encryption.algorithm";
    
    // Client Secret Encrypting Algorithm
    private OAuthConstants() {
    }
    
}
